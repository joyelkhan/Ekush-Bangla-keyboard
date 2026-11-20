/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.example.ui

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ekush.engine.SuggestionEngine
import com.example.data.db.EkushDatabase
import com.example.data.db.LexiconRepository
import com.example.data.db.UserLexiconEntity
import com.example.data.prefs.DarkModeOverride
import com.example.data.prefs.EkushPreferencesRepository
import com.example.data.prefs.EkushSettings
import com.example.data.prefs.EkushThemeId
import com.example.data.prefs.HapticIntensity
import com.example.data.prefs.TypingMode
import com.example.ime.EkushImeService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class ImeActivationStatus(
    val isEnabled: Boolean = false,
    val isDefault: Boolean = false
)

class EkushViewModel(application: Application) : AndroidViewModel(application) {
    private val prefsRepo = EkushPreferencesRepository(application)
    private val lexiconRepo = LexiconRepository(
        application,
        EkushDatabase.getInstance(application).userLexiconDao()
    )

    val settings: StateFlow<EkushSettings> = prefsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = EkushSettings()
    )

    val userWords: StateFlow<List<UserLexiconEntity>> = lexiconRepo.allUserWords.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    private val _imeStatus = MutableStateFlow(ImeActivationStatus())
    val imeStatus: StateFlow<ImeActivationStatus> = _imeStatus.asStateFlow()

    private val _sandboxLatinInput = MutableStateFlow("ami banglay matro ek minite Taip korte pari")
    val sandboxLatinInput: StateFlow<String> = _sandboxLatinInput.asStateFlow()

    private val _sandboxStrip = MutableStateFlow(
        SuggestionEngine.StripState(null, "", null)
    )
    val sandboxStrip: StateFlow<SuggestionEngine.StripState> = _sandboxStrip.asStateFlow()

    private val _backupMessage = MutableStateFlow<String?>(null)
    val backupMessage: StateFlow<String?> = _backupMessage.asStateFlow()

    init {
        refreshImeStatus()
        updateSandboxInput(_sandboxLatinInput.value)
    }

    fun refreshImeStatus() {
        val ctx = getApplication<Application>()
        val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
        val ekushComponent = ComponentName(ctx, EkushImeService::class.java)
        val enabled = imm?.enabledInputMethodList?.any {
            it.packageName == ctx.packageName && it.component == ekushComponent
        } == true

        val defaultId = Settings.Secure.getString(
            ctx.contentResolver,
            Settings.Secure.DEFAULT_INPUT_METHOD
        ).orEmpty()
        val isDefault = defaultId == ekushComponent.flattenToShortString() ||
            defaultId == ekushComponent.flattenToString()

        _imeStatus.value = ImeActivationStatus(isEnabled = enabled, isDefault = isDefault)
    }

    fun updateSandboxInput(input: String) {
        _sandboxLatinInput.value = input
        val autoConjuncts = settings.value.autoConjunctsEnabled
        viewModelScope.launch(Dispatchers.Default) {
            val tokens = input.trim().split(Regex("\\s+"))
            val lastWord = tokens.lastOrNull().orEmpty()
            val fullBangla = lexiconRepo.phoneticEngine.convert(input, autoConjuncts)
            val wordStrip = if (lastWord.isNotEmpty()) {
                lexiconRepo.suggestionEngine.suggestForPhonetic(lastWord, autoConjuncts = autoConjuncts)
            } else {
                SuggestionEngine.StripState(null, "", null)
            }
            _sandboxStrip.value = SuggestionEngine.StripState(
                left = wordStrip.left,
                middle = fullBangla,
                right = wordStrip.right
            )
        }
    }

    fun setDefaultMode(mode: TypingMode) = viewModelScope.launch { prefsRepo.setDefaultMode(mode) }
    fun setThemeId(themeId: EkushThemeId) = viewModelScope.launch { prefsRepo.setThemeId(themeId) }
    fun setDarkModeOverride(override: DarkModeOverride) = viewModelScope.launch { prefsRepo.setDarkModeOverride(override) }
    fun setHapticsEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setHapticsEnabled(enabled) }
    fun setHapticIntensity(intensity: HapticIntensity) = viewModelScope.launch { prefsRepo.setHapticIntensity(intensity) }
    fun setSoundEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setSoundEnabled(enabled) }
    fun setKeyHeightDp(dp: Int) = viewModelScope.launch { prefsRepo.setKeyHeightDp(dp) }
    fun setGapScale(scale: Float) = viewModelScope.launch { prefsRepo.setGapScale(scale) }
    fun setLongPressDelayMs(ms: Int) = viewModelScope.launch { prefsRepo.setLongPressDelayMs(ms) }
    fun setPreferBanglaDigits(prefer: Boolean) = viewModelScope.launch { prefsRepo.setPreferBanglaDigits(prefer) }
    fun setAutoConjunctsEnabled(enabled: Boolean) = viewModelScope.launch {
        prefsRepo.setAutoConjunctsEnabled(enabled)
        updateSandboxInput(_sandboxLatinInput.value)
    }
    fun setSuggestionsEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setSuggestionsEnabled(enabled) }
    fun setAutocorrectEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setAutocorrectEnabled(enabled) }
    fun setCommitOnEveryKeystroke(enabled: Boolean) = viewModelScope.launch { prefsRepo.setCommitOnEveryKeystroke(enabled) }
    fun setPerAppModeMemory(enabled: Boolean) = viewModelScope.launch { prefsRepo.setPerAppModeMemory(enabled) }
    fun setClipboardHistoryEnabled(enabled: Boolean) = viewModelScope.launch { prefsRepo.setClipboardHistoryEnabled(enabled) }
    fun setOnboardingCompleted(completed: Boolean) = viewModelScope.launch { prefsRepo.setOnboardingCompleted(completed) }

    fun addPersonalWord(word: String) = viewModelScope.launch {
        lexiconRepo.learnWord(word)
    }

    fun deletePersonalWord(word: String) = viewModelScope.launch {
        lexiconRepo.deleteWord(word)
    }

    fun clearPersonalLexicon() = viewModelScope.launch {
        lexiconRepo.clearAllWords()
    }

    fun exportBackup(uri: Uri, successMsg: String, errorMsg: String) = viewModelScope.launch {
        val result = runCatching {
            val root = JSONObject().apply {
                put("version", 1)
                put("settings", prefsRepo.exportToJson())
                put("lexicon", lexiconRepo.exportLexiconJson())
            }
            withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(root.toString(2).toByteArray(Charsets.UTF_8))
                } ?: error("Null output stream")
            }
        }
        _backupMessage.value = if (result.isSuccess) successMsg else errorMsg
    }

    fun importBackup(uri: Uri, successFormatter: (Int) -> String, errorMsg: String) = viewModelScope.launch {
        val result = runCatching {
            val jsonText = withContext(Dispatchers.IO) {
                getApplication<Application>().contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                    it.readText()
                } ?: error("Null input stream")
            }
            val root = JSONObject(jsonText)
            root.optJSONObject("settings")?.let { prefsRepo.importFromJson(it) }
            val count = root.optJSONArray("lexicon")?.let { lexiconRepo.importLexiconJson(it) } ?: 0
            count
        }
        _backupMessage.value = result.fold(
            onSuccess = { count -> successFormatter(count) },
            onFailure = { errorMsg }
        )
    }

    fun clearBackupMessage() {
        _backupMessage.value = null
    }
}
