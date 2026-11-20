/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.example.ime

import android.content.ClipboardManager
import android.content.Context
import android.content.res.Configuration
import android.inputmethodservice.InputMethodService
import android.media.AudioManager
import android.text.InputType
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.LinearLayout
import com.ekush.engine.BanglaUnicode
import com.ekush.engine.ContextLM
import com.example.R
import com.example.data.db.EkushDatabase
import com.example.data.db.LexiconRepository
import com.example.data.prefs.DarkModeOverride
import com.example.data.prefs.EkushPreferencesRepository
import com.example.data.prefs.EkushSettings
import com.example.data.prefs.TypingMode
import com.example.theme.EkushPalettes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Primary Android InputMethodService for Ekush Bangla Keyboard (Blueprint Part 04.1 & 04.11).
 *
 * View-based IME host with pure-Kotlin :engine processing, local Room lexicon + mode memory,
 * OTP Latin-digit guard (Part 03.8), cluster-aware backspace, and zero network dependencies.
 */
class EkushImeService : InputMethodService(),
    EkushKeyboardView.Listener,
    SuggestionStripView.Listener {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private lateinit var prefsRepo: EkushPreferencesRepository
    private lateinit var lexiconRepo: LexiconRepository

    private var suggestionStripView: SuggestionStripView? = null
    private var keyboardView: EkushKeyboardView? = null

    private var settings: EkushSettings = EkushSettings()
    private var activeMode: TypingMode = TypingMode.PHONETIC
    private var lastBanglaMode: TypingMode = TypingMode.PHONETIC
    private var isShifted: Boolean = false
    private var isSymbolsPage: Boolean = false
    private var forceLatinDigits: Boolean = false
    private var currentPackageName: String? = null

    private val phoneticBuffer = StringBuilder()
    private val directWordBuffer = StringBuilder()
    private var lastAutocorrectOriginal: String? = null
    private var lastAutocorrectReplaced: String? = null
    private var personalWordCache: Map<String, Int> = emptyMap()
    private val sessionModeOverrides = HashMap<String, TypingMode>()
    private var recentClipboard: String? = null
    private var suggestionJob: Job? = null

    private val clipboardListener = ClipboardManager.OnPrimaryClipChangedListener {
        if (!settings.clipboardHistoryEnabled) return@OnPrimaryClipChangedListener
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return@OnPrimaryClipChangedListener
        val item = cm.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString()?.trim()
        if (!item.isNullOrEmpty()) {
            recentClipboard = item
            refreshSuggestionStrip()
        }
    }

    override fun onCreate() {
        super.onCreate()
        prefsRepo = EkushPreferencesRepository(applicationContext)
        val db = EkushDatabase.getInstance(applicationContext)
        lexiconRepo = LexiconRepository(applicationContext, db.userLexiconDao())

        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.addPrimaryClipChangedListener(clipboardListener)

        serviceScope.launch {
            prefsRepo.settingsFlow.collectLatest { latest ->
                settings = latest
                applySettingsToViews()
            }
        }

        serviceScope.launch {
            personalWordCache = lexiconRepo.getPersonalWordMap()
            lexiconRepo.applyPeriodicDecay()
        }
    }

    override fun onCreateInputView(): View {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val strip = SuggestionStripView(this).apply {
            listener = this@EkushImeService
        }
        val kb = EkushKeyboardView(this).apply {
            listener = this@EkushImeService
        }

        suggestionStripView = strip
        keyboardView = kb

        root.addView(
            strip,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        root.addView(
            kb,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        applySettingsToViews()
        renderKeyboardLayout()
        return root
    }

    override fun onStartInputView(info: EditorInfo?, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        resetBuffers()
        currentPackageName = info?.packageName
        forceLatinDigits = isNumericOrOtpField(info)

        serviceScope.launch {
            val pkg = currentPackageName
            val sessionOverride = pkg?.let { sessionModeOverrides[it] }
            val remembered = if (settings.perAppModeMemory) {
                lexiconRepo.getModeForApp(pkg)
            } else {
                null
            }
            activeMode = sessionOverride ?: remembered ?: settings.defaultMode
            if (activeMode != TypingMode.ENGLISH_QWERTY) {
                lastBanglaMode = activeMode
            }
            isSymbolsPage = forceLatinDigits
            isShifted = false
            applySettingsToViews()
            renderKeyboardLayout()
            refreshSuggestionStrip()
        }
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        commitComposingIfNeeded()
        super.onFinishInputView(finishingInput)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        applySettingsToViews()
        renderKeyboardLayout()
    }

    override fun onDestroy() {
        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.removePrimaryClipChangedListener(clipboardListener)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onKeyPressed(key: KeySpec) {
        performKeyFeedback(isModeSwitch = key.role == KeyRole.MODE_SWITCH, isSuggestion = false)
        when (key.role) {
            KeyRole.BACKSPACE -> handleBackspace()
            KeyRole.ENTER -> handleEnter()
            KeyRole.SPACE -> handleSpace()
            KeyRole.SHIFT -> {
                isShifted = !isShifted
                renderKeyboardLayout()
            }
            KeyRole.MODE_SWITCH -> toggleTypingMode()
            KeyRole.SYMBOLS -> {
                isSymbolsPage = !isSymbolsPage
                isShifted = false
                renderKeyboardLayout()
            }
            KeyRole.HASANTA -> handleTextKey(key.output, fromHasantaKey = true)
            KeyRole.CHARACTER, KeyRole.FUNCTION -> handleTextKey(key.output, fromHasantaKey = false)
        }
    }

    override fun onAlternateSelected(key: KeySpec, alternateText: String) {
        performKeyFeedback(isModeSwitch = false, isSuggestion = false)
        if (activeMode == TypingMode.PHONETIC && !isSymbolsPage) {
            commitComposingIfNeeded()
        }
        currentInputConnection?.commitText(alternateText, 1)
        if (isShifted && activeMode == TypingMode.FIXED_BANGLA) {
            isShifted = false
            renderKeyboardLayout()
        }
        refreshSuggestionStrip()
    }

    override fun onBackspaceRepeat() {
        handleBackspace()
    }

    override fun onSuggestionClicked(slotIndex: Int, word: String) {
        performKeyFeedback(isModeSwitch = false, isSuggestion = true)
        val ic = currentInputConnection ?: return
        if (activeMode == TypingMode.PHONETIC && phoneticBuffer.isNotEmpty()) {
            ic.setComposingText(word, 1)
            ic.finishComposingText()
            ic.commitText(" ", 1)
            phoneticBuffer.clear()
        } else if (directWordBuffer.isNotEmpty()) {
            ic.deleteSurroundingText(directWordBuffer.length, 0)
            ic.commitText("$word ", 1)
            directWordBuffer.clear()
        } else {
            ic.commitText("$word ", 1)
        }
        learnCommittedWord(word)
        lastAutocorrectOriginal = null
        lastAutocorrectReplaced = null
        refreshSuggestionStrip()
    }

    override fun onRevertAutocorrectClicked() {
        val ic = currentInputConnection ?: return
        val orig = lastAutocorrectOriginal ?: return
        val replaced = lastAutocorrectReplaced ?: return
        val before = ic.getTextBeforeCursor(replaced.length + 2, 0)?.toString().orEmpty()
        val withTrailingSpace = "$replaced "
        if (before.endsWith(withTrailingSpace)) {
            ic.deleteSurroundingText(withTrailingSpace.length, 0)
            ic.commitText("$orig ", 1)
        } else if (before.endsWith(replaced)) {
            ic.deleteSurroundingText(replaced.length, 0)
            ic.commitText(orig, 1)
        }
        lastAutocorrectOriginal = null
        lastAutocorrectReplaced = null
        lexiconRepo.autocorrectEngine.recordRevert(orig, replaced)
        refreshSuggestionStrip()
    }

    override fun onClipboardClicked() {
        val clip = recentClipboard ?: return
        currentInputConnection?.commitText(clip, 1)
        refreshSuggestionStrip()
    }

    override fun onModeBadgeClicked() {
        performKeyFeedback(isModeSwitch = true, isSuggestion = false)
        commitComposingIfNeeded()
        activeMode = when (activeMode) {
            TypingMode.PHONETIC -> TypingMode.FIXED_BANGLA
            TypingMode.FIXED_BANGLA -> TypingMode.ENGLISH_QWERTY
            TypingMode.ENGLISH_QWERTY -> TypingMode.PHONETIC
        }
        if (activeMode != TypingMode.ENGLISH_QWERTY) {
            lastBanglaMode = activeMode
        }
        isSymbolsPage = false
        isShifted = false
        recordExplicitModeChoice(activeMode)
        renderKeyboardLayout()
        refreshSuggestionStrip()
    }

    private fun handleTextKey(text: String, fromHasantaKey: Boolean) {
        val ic = currentInputConnection ?: return
        lastAutocorrectOriginal = null
        lastAutocorrectReplaced = null

        if (isSymbolsPage) {
            ic.commitText(text, 1)
            return
        }

        when (activeMode) {
            TypingMode.PHONETIC -> {
                if (text.length == 1 && isPhoneticBufferChar(text[0])) {
                    phoneticBuffer.append(text)
                    recomposePhonetic()
                } else {
                    commitComposingIfNeeded()
                    ic.commitText(text, 1)
                    refreshSuggestionStrip()
                }
            }
            TypingMode.FIXED_BANGLA -> {
                ic.commitText(text, 1)
                if (text != "\t") {
                    directWordBuffer.append(text)
                }
                if (isShifted) {
                    isShifted = false
                }
                // Highlight hasanta key immediately after manual hasanta tap (Blueprint Part 03.2 #4)
                renderKeyboardLayout(hasantaActiveOverride = fromHasantaKey)
                refreshSuggestionStrip()
            }
            TypingMode.ENGLISH_QWERTY -> {
                ic.commitText(text, 1)
                if (text.length == 1 && text[0].isLetter()) {
                    directWordBuffer.append(text)
                } else {
                    directWordBuffer.clear()
                }
                if (isShifted) {
                    isShifted = false
                    renderKeyboardLayout()
                }
                refreshSuggestionStrip()
            }
        }
    }

    private fun handleSpace() {
        val ic = currentInputConnection ?: return
        if (activeMode == TypingMode.PHONETIC && phoneticBuffer.isNotEmpty()) {
            commitComposingWithOptionalAutocorrect()
            ic.commitText(" ", 1)
        } else {
            if (directWordBuffer.isNotEmpty()) {
                val word = directWordBuffer.toString()
                directWordBuffer.clear()
                maybeApplyDirectAutocorrect(word)
            }
            ic.commitText(" ", 1)
        }
        refreshSuggestionStrip()
    }

    private fun handleEnter() {
        val ic = currentInputConnection ?: return
        commitComposingIfNeeded()
        val imeOptions = currentInputEditorInfo?.imeOptions ?: 0
        val actionId = imeOptions and EditorInfo.IME_MASK_ACTION
        if (actionId != EditorInfo.IME_ACTION_NONE &&
            (imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0
        ) {
            ic.performEditorAction(actionId)
        } else {
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER))
            ic.sendKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_ENTER))
        }
        refreshSuggestionStrip()
    }

    private fun handleBackspace() {
        val ic = currentInputConnection ?: return
        if (activeMode == TypingMode.PHONETIC && phoneticBuffer.isNotEmpty()) {
            phoneticBuffer.deleteCharAt(phoneticBuffer.length - 1)
            if (phoneticBuffer.isEmpty()) {
                ic.setComposingText("", 1)
                ic.finishComposingText()
                renderKeyboardLayout()
                refreshSuggestionStrip()
            } else {
                recomposePhonetic()
            }
            return
        }

        if (directWordBuffer.isNotEmpty()) {
            val dropCount = BanglaUnicode.lastClusterLength(directWordBuffer)
            directWordBuffer.delete(directWordBuffer.length - dropCount, directWordBuffer.length)
        }

        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) {
            ic.commitText("", 1)
        } else {
            // Cluster-aware backspace prevents broken conjuncts (Blueprint Part 04.11)
            val before = ic.getTextBeforeCursor(12, 0) ?: ""
            val deleteLen = BanglaUnicode.lastClusterLength(before).coerceAtLeast(1)
            ic.deleteSurroundingText(deleteLen, 0)
        }
        renderKeyboardLayout()
        refreshSuggestionStrip()
    }

    private fun recomposePhonetic() {
        val ic = currentInputConnection ?: return
        val result = lexiconRepo.phoneticEngine.convertDetailed(
            phoneticBuffer.toString(),
            autoConjuncts = settings.autoConjunctsEnabled
        )
        if (settings.commitOnEveryKeystroke) {
            // Compatibility fallback for Flutter/custom editors (Blueprint Part 04.11)
            ic.commitText(result.bangla, 1)
            phoneticBuffer.clear()
        } else {
            ic.setComposingText(result.bangla, 1)
        }
        renderKeyboardLayout(hasantaActiveOverride = result.endsWithPendingHasanta)
        refreshSuggestionStrip()
    }

    private fun commitComposingIfNeeded() {
        val ic = currentInputConnection ?: return
        if (activeMode == TypingMode.PHONETIC && phoneticBuffer.isNotEmpty()) {
            val finalBangla = lexiconRepo.phoneticEngine.convert(
                phoneticBuffer.toString(),
                autoConjuncts = settings.autoConjunctsEnabled
            )
            ic.finishComposingText()
            learnCommittedWord(finalBangla)
            phoneticBuffer.clear()
            renderKeyboardLayout()
        } else if (directWordBuffer.isNotEmpty()) {
            learnCommittedWord(directWordBuffer.toString())
            directWordBuffer.clear()
        }
    }

    private fun commitComposingWithOptionalAutocorrect() {
        val ic = currentInputConnection ?: return
        val converted = lexiconRepo.phoneticEngine.convert(
            phoneticBuffer.toString(),
            autoConjuncts = settings.autoConjunctsEnabled
        )
        val preceding = ic.getTextBeforeCursor(24, 0) ?: ""
        val correction = if (settings.autocorrectEnabled) {
            lexiconRepo.autocorrectEngine.maybeCorrectOnCommit(converted, preceding)
        } else {
            null
        }
        if (correction != null) {
            ic.setComposingText(correction.corrected, 1)
            ic.finishComposingText()
            lastAutocorrectOriginal = correction.original
            lastAutocorrectReplaced = correction.corrected
            learnCommittedWord(correction.corrected)
        } else {
            ic.finishComposingText()
            learnCommittedWord(converted)
        }
        phoneticBuffer.clear()
        renderKeyboardLayout()
    }

    private fun maybeApplyDirectAutocorrect(word: String) {
        if (!settings.autocorrectEnabled || activeMode != TypingMode.FIXED_BANGLA) {
            learnCommittedWord(word)
            return
        }
        val ic = currentInputConnection ?: return
        val preceding = ic.getTextBeforeCursor(word.length + 16, 0) ?: ""
        val fix = lexiconRepo.autocorrectEngine.maybeCorrectOnCommit(word, preceding)
        if (fix != null) {
            ic.deleteSurroundingText(word.length, 0)
            ic.commitText(fix.corrected, 1)
            lastAutocorrectOriginal = fix.original
            lastAutocorrectReplaced = fix.corrected
            learnCommittedWord(fix.corrected)
        } else {
            learnCommittedWord(word)
        }
    }

    private fun toggleTypingMode() {
        commitComposingIfNeeded()
        activeMode = when (activeMode) {
            TypingMode.PHONETIC -> {
                lastBanglaMode = TypingMode.PHONETIC
                TypingMode.ENGLISH_QWERTY
            }
            TypingMode.FIXED_BANGLA -> {
                lastBanglaMode = TypingMode.FIXED_BANGLA
                TypingMode.ENGLISH_QWERTY
            }
            TypingMode.ENGLISH_QWERTY -> lastBanglaMode
        }
        isSymbolsPage = false
        isShifted = false
        recordExplicitModeChoice(activeMode)
        renderKeyboardLayout()
        refreshSuggestionStrip()
    }

    private fun recordExplicitModeChoice(mode: TypingMode) {
        val pkg = currentPackageName
        if (!pkg.isNullOrBlank()) {
            sessionModeOverrides[pkg] = mode
        }
        if (settings.perAppModeMemory) {
            serviceScope.launch { lexiconRepo.saveModeForApp(pkg, mode) }
        }
    }

    private fun applySettingsToViews() {
        val systemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        val isDark = when (settings.darkModeOverride) {
            DarkModeOverride.SYSTEM -> systemDark
            DarkModeOverride.LIGHT -> false
            DarkModeOverride.DARK -> true
        }
        val palette = EkushPalettes.resolve(settings.themeId, isDark, this)
        suggestionStripView?.applyPalette(palette)
        suggestionStripView?.visibility = if (settings.suggestionsEnabled) View.VISIBLE else View.GONE
        keyboardView?.applyPalette(palette)
        keyboardView?.updateMetrics(
            keyHeightDp = settings.keyHeightDp,
            gapScale = settings.gapScale,
            longPressDelayMs = settings.longPressDelayMs
        )
    }

    private fun renderKeyboardLayout(hasantaActiveOverride: Boolean = false) {
        val kb = keyboardView ?: return
        val rows = when {
            isSymbolsPage -> BanglaLayouts.symbolsLayout(settings.preferBanglaDigits, forceLatinDigits)
            activeMode == TypingMode.FIXED_BANGLA && isShifted -> BanglaLayouts.fixedPage2("Bn/En")
            activeMode == TypingMode.FIXED_BANGLA -> BanglaLayouts.fixedPage1("Bn/En")
            else -> BanglaLayouts.qwertyLayout(
                mode = activeMode,
                shifted = isShifted,
                preferBanglaDigits = settings.preferBanglaDigits,
                forceLatinDigits = forceLatinDigits
            )
        }
        val pendingHasanta = hasantaActiveOverride ||
            (activeMode == TypingMode.PHONETIC &&
                phoneticBuffer.isNotEmpty() &&
                lexiconRepo.phoneticEngine.convertDetailed(
                    phoneticBuffer.toString(),
                    settings.autoConjunctsEnabled
                ).endsWithPendingHasanta)

        kb.setState(rows = rows, shiftActive = isShifted, hasantaPending = pendingHasanta)
    }

    private fun refreshSuggestionStrip() {
        val strip = suggestionStripView ?: return
        val badge = when (activeMode) {
            TypingMode.PHONETIC -> getString(R.string.mode_phonetic)
            TypingMode.FIXED_BANGLA -> getString(R.string.mode_fixed)
            TypingMode.ENGLISH_QWERTY -> getString(R.string.mode_english)
        }
        val revertText = lastAutocorrectOriginal?.let { getString(R.string.ime_revert_chip, it) }
        val clip = if (settings.clipboardHistoryEnabled) recentClipboard else null

        if (!settings.suggestionsEnabled) {
            strip.updateSuggestions(null, "", null, badge, revertText, clip)
            return
        }

        val bufferSnapshot = phoneticBuffer.toString()
        val directSnapshot = directWordBuffer.toString()
        val modeSnapshot = activeMode
        val autoConj = settings.autoConjunctsEnabled
        val personal = personalWordCache

        val activeLen = when {
            modeSnapshot == TypingMode.PHONETIC && bufferSnapshot.isNotEmpty() ->
                lexiconRepo.phoneticEngine.convert(bufferSnapshot, autoConj).length
            directSnapshot.isNotEmpty() -> directSnapshot.length
            else -> 0
        }
        val rawBefore = currentInputConnection?.getTextBeforeCursor(activeLen + 40, 0)?.toString().orEmpty()
        val beforeWord = if (activeLen > 0 && rawBefore.length >= activeLen) {
            rawBefore.dropLast(activeLen)
        } else {
            rawBefore
        }
        val prevContextWord = ContextLM.extractPrevContextWord(beforeWord)

        suggestionJob?.cancel()
        suggestionJob = serviceScope.launch {
            val state = withContext(Dispatchers.Default) {
                when {
                    modeSnapshot == TypingMode.PHONETIC && bufferSnapshot.isNotEmpty() ->
                        lexiconRepo.suggestionEngine.suggestForPhonetic(
                            bufferSnapshot,
                            personal,
                            autoConj,
                            prevContextWord
                        )
                    directSnapshot.isNotEmpty() ->
                        lexiconRepo.suggestionEngine.suggestForDirectWord(
                            directSnapshot,
                            isBangla = modeSnapshot == TypingMode.FIXED_BANGLA,
                            personalWords = personal,
                            prevContextWord = prevContextWord
                        )
                    else -> null
                }
            }
            if (state != null) {
                strip.updateSuggestions(state.left, state.middle, state.right, badge, revertText, clip)
            } else {
                strip.updateSuggestions(null, "", null, badge, revertText, clip)
            }
        }
    }

    private fun learnCommittedWord(word: String) {
        if (word.length < 2) return
        serviceScope.launch {
            lexiconRepo.learnWord(word)
            personalWordCache = lexiconRepo.getPersonalWordMap()
        }
    }

    private fun performKeyFeedback(isModeSwitch: Boolean, isSuggestion: Boolean) {
        if (settings.hapticsEnabled) {
            val constant = when {
                isModeSwitch -> HapticFeedbackConstants.LONG_PRESS
                isSuggestion -> HapticFeedbackConstants.VIRTUAL_KEY
                else -> HapticFeedbackConstants.KEYBOARD_TAP
            }
            keyboardView?.performHapticFeedback(constant)
        }
        if (settings.soundEnabled) {
            val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            am?.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.35f)
        }
    }

    private fun resetBuffers() {
        phoneticBuffer.clear()
        directWordBuffer.clear()
        lastAutocorrectOriginal = null
        lastAutocorrectReplaced = null
    }

    private fun isPhoneticBufferChar(ch: Char): Boolean =
        ch in 'a'..'z' || ch in 'A'..'Z' || ch == '\\' || ch == ']' || ch == ':' || ch == '^'

    private fun isNumericOrOtpField(info: EditorInfo?): Boolean {
        val inputType = info?.inputType ?: return false
        val typeClass = inputType and InputType.TYPE_MASK_CLASS
        val variation = inputType and InputType.TYPE_MASK_VARIATION
        return typeClass == InputType.TYPE_CLASS_NUMBER ||
            typeClass == InputType.TYPE_CLASS_PHONE ||
            variation == InputType.TYPE_NUMBER_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    }
}
