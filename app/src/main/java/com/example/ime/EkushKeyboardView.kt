/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.example.ime

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityNodeProvider
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.ColorUtils
import com.example.R
import com.example.theme.EkushPalettes
import com.example.theme.KeyboardPalette
import kotlin.math.roundToInt

/**
 * Custom View-based keyboard canvas (Blueprint Part 04.9, 05.2, 05.3, 05.5, 05.7).
 *
 * Designed for <30 ms key press feedback, zero Compose overhead inside InputMethodService,
 * full TalkBack virtual node support via ExploreByTouchHelper, and system "remove animations" respect.
 */
class EkushKeyboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Listener {
        fun onKeyPressed(key: KeySpec)
        fun onAlternateSelected(key: KeySpec, alternateText: String)
        fun onBackspaceRepeat()
    }

    private data class LaidOutKey(
        val index: Int,
        val spec: KeySpec,
        val bounds: RectF
    )

    var listener: Listener? = null

    private var rows: List<List<KeySpec>> = emptyList()
    private val laidOutKeys = mutableListOf<LaidOutKey>()
    private var palette: KeyboardPalette = EkushPalettes.resolve(
        com.example.data.prefs.EkushThemeId.EKUSH,
        isDark = false,
        context = context
    )

    private var keyHeightDp: Int = 54
    private var gapScale: Float = 1.0f
    private var longPressDelayMs: Int = 280
    private var hasantaPending: Boolean = false
    private var shiftActive: Boolean = false

    private val density = context.resources.displayMetrics.density
    private val scaledDensity = context.resources.displayMetrics.scaledDensity

    private val keyPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.8f * density
    }
    private val ripplePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val altHintPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
    }

    private val bengaliTypeface: Typeface? = runCatching {
        ResourcesCompat.getFont(context, R.font.noto_sans_bengali)
    }.getOrNull()

    private val mainHandler = Handler(Looper.getMainLooper())
    private val popupWindow = LongPressPopupWindow(context)

    private var activeKey: LaidOutKey? = null
    private var longPressTriggered = false
    private var rippleKeyIndex = -1
    private var rippleProgress = 1f

    private val a11yProvider = KeyboardAccessibilityNodeProvider()
    private var lastHoveredVirtualId = AccessibilityNodeProvider.HOST_VIEW_ID
    private val accessibilityManager =
        context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager

    private val longPressRunnable = Runnable {
        val key = activeKey ?: return@Runnable
        if (key.spec.role == KeyRole.BACKSPACE) {
            longPressTriggered = true
            startBackspaceRepeat()
        } else if (key.spec.alternates.isNotEmpty()) {
            longPressTriggered = true
            popupWindow.show(this, key.bounds, key.spec.alternates, palette)
        }
    }

    private val backspaceRepeatRunnable = object : Runnable {
        override fun run() {
            if (activeKey?.spec?.role == KeyRole.BACKSPACE) {
                listener?.onBackspaceRepeat()
                mainHandler.postDelayed(this, 65L)
            }
        }
    }

    private val rippleTickRunnable = object : Runnable {
        override fun run() {
            rippleProgress = (rippleProgress + 0.25f).coerceAtMost(1f)
            invalidate()
            if (rippleProgress < 1f) {
                mainHandler.postDelayed(this, 16L)
            } else {
                rippleKeyIndex = -1
            }
        }
    }

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        applyPalette(palette)
    }

    fun applyPalette(newPalette: KeyboardPalette) {
        palette = newPalette
        borderPaint.color = palette.border
        val baseWeight = if (palette.isDark) Typeface.BOLD else Typeface.NORMAL
        labelPaint.typeface = Typeface.create(bengaliTypeface ?: Typeface.DEFAULT, baseWeight)
        labelPaint.textSize = (18f * scaledDensity).coerceAtLeast(16f * density)
        altHintPaint.typeface = bengaliTypeface ?: Typeface.DEFAULT
        altHintPaint.textSize = (11f * scaledDensity).coerceAtLeast(10f * density)
        invalidate()
    }

    fun updateMetrics(keyHeightDp: Int, gapScale: Float, longPressDelayMs: Int) {
        val heightChanged = this.keyHeightDp != keyHeightDp || this.gapScale != gapScale
        this.keyHeightDp = keyHeightDp.coerceIn(48, 72)
        this.gapScale = gapScale.coerceIn(0.6f, 1.4f)
        this.longPressDelayMs = longPressDelayMs.coerceIn(150, 500)
        if (heightChanged) {
            requestLayout()
            layoutKeys(width, measuredHeight)
            invalidate()
        }
    }

    fun setState(
        rows: List<List<KeySpec>>,
        shiftActive: Boolean,
        hasantaPending: Boolean
    ) {
        this.rows = rows
        this.shiftActive = shiftActive
        this.hasantaPending = hasantaPending
        requestLayout()
        layoutKeys(width, computeTotalHeight())
        if (accessibilityManager?.isEnabled == true) {
            sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED)
        }
        invalidate()
    }

    private fun effectiveRowKeyHeightPx(): Float {
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val dp = if (isLandscape) 46f else keyHeightDp.toFloat()
        return dp * density
    }

    private fun computeTotalHeight(): Int {
        if (rows.isEmpty()) return 0
        val keyH = effectiveRowKeyHeightPx()
        val rowGap = 7f * gapScale * density
        val verticalPad = 8f * density
        return (rows.size * keyH + (rows.size - 1).coerceAtLeast(0) * rowGap + verticalPad * 2f).roundToInt()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = computeTotalHeight()
        setMeasuredDimension(w, resolveSize(h, heightMeasureSpec))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        layoutKeys(w, h)
    }

    private fun layoutKeys(totalWidth: Int, totalHeight: Int) {
        laidOutKeys.clear()
        if (totalWidth <= 0 || rows.isEmpty()) return

        val sidePad = 4f * density
        val topPad = 8f * density
        val hGap = 5f * gapScale * density
        val vGap = 7f * gapScale * density
        val rowKeyH = effectiveRowKeyHeightPx()
        val usableWidth = totalWidth - sidePad * 2f

        var y = topPad
        var globalIdx = 0
        for (row in rows) {
            val totalWeight = row.sumOf { it.weight.toDouble() }.toFloat().coerceAtLeast(1f)
            val totalGaps = (row.size - 1).coerceAtLeast(0) * hGap
            val unitWidth = (usableWidth - totalGaps) / totalWeight
            var x = sidePad
            for (spec in row) {
                val kw = unitWidth * spec.weight
                val rect = RectF(x, y, x + kw, y + rowKeyH)
                laidOutKeys += LaidOutKey(globalIdx++, spec, rect)
                x += kw + hGap
            }
            y += rowKeyH + vGap
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(palette.background)

        val cornerRadius = 10f * density
        for (k in laidOutKeys) {
            val isPressed = activeKey?.index == k.index
            keyPaint.color = when (k.spec.role) {
                KeyRole.HASANTA -> if (hasantaPending || isPressed) {
                    palette.hasantaKeyActiveSurface
                } else {
                    palette.hasantaKeySurface
                }
                KeyRole.SHIFT -> if (shiftActive) palette.accent else palette.functionKeySurface
                KeyRole.ENTER -> palette.accent
                KeyRole.FUNCTION, KeyRole.BACKSPACE, KeyRole.MODE_SWITCH, KeyRole.SYMBOLS ->
                    palette.functionKeySurface
                KeyRole.CHARACTER, KeyRole.SPACE -> palette.keySurface
            }

            if (isPressed && k.spec.role != KeyRole.HASANTA && k.spec.role != KeyRole.ENTER) {
                keyPaint.color = ColorUtils.blendARGB(keyPaint.color, palette.accent, 0.18f)
            }

            canvas.drawRoundRect(k.bounds, cornerRadius, cornerRadius, keyPaint)
            canvas.drawRoundRect(k.bounds, cornerRadius, cornerRadius, borderPaint)

            // 90 ms bounded ripple (Blueprint Part 05.5)
            if (rippleKeyIndex == k.index && rippleProgress < 1f) {
                canvas.save()
                val path = android.graphics.Path().apply {
                    addRoundRect(k.bounds, cornerRadius, cornerRadius, android.graphics.Path.Direction.CW)
                }
                canvas.clipPath(path)
                val alpha = ((1f - rippleProgress) * 64).toInt().coerceIn(0, 255)
                ripplePaint.color = ColorUtils.setAlphaComponent(palette.accent, alpha)
                val maxR = maxOf(k.bounds.width(), k.bounds.height()) * 0.75f
                canvas.drawCircle(k.bounds.centerX(), k.bounds.centerY(), maxR * rippleProgress, ripplePaint)
                canvas.restore()
            }

            // Primary label
            val useOnAccent = k.spec.role == KeyRole.ENTER || (k.spec.role == KeyRole.SHIFT && shiftActive)
            labelPaint.color = if (useOnAccent) palette.onAccent else palette.keyLabel
            labelPaint.textSize = when (k.spec.role) {
                KeyRole.SPACE, KeyRole.MODE_SWITCH, KeyRole.SYMBOLS -> 13f * scaledDensity
                else -> (18f * scaledDensity).coerceAtLeast(15f * density)
            }
            val fm = labelPaint.fontMetrics
            val baseline = k.bounds.centerY() - (fm.ascent + fm.descent) / 2f
            canvas.drawText(k.spec.label, k.bounds.centerX(), baseline, labelPaint)

            // Top-right alternate hint (11 sp per Blueprint Part 05.3)
            k.spec.hint?.let { hintText ->
                altHintPaint.color = if (useOnAccent) palette.onAccent else palette.altLabel
                canvas.drawText(
                    hintText,
                    k.bounds.right - 5f * density,
                    k.bounds.top + 13f * density,
                    altHintPaint
                )
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val hit = findKeyAt(event.x, event.y)
                activeKey = hit
                longPressTriggered = false
                if (hit != null) {
                    triggerRipple(hit.index)
                    mainHandler.removeCallbacks(longPressRunnable)
                    mainHandler.postDelayed(longPressRunnable, longPressDelayMs.toLong())
                }
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (popupWindow.isShowing) {
                    popupWindow.updateSelection(event.x, this)
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                mainHandler.removeCallbacks(longPressRunnable)
                mainHandler.removeCallbacks(backspaceRepeatRunnable)
                val key = activeKey
                if (longPressTriggered && popupWindow.isShowing) {
                    val chosen = popupWindow.selectedAlternate()
                    popupWindow.dismiss()
                    if (key != null && chosen != null) {
                        listener?.onAlternateSelected(key.spec, chosen)
                    }
                } else if (!longPressTriggered && key != null) {
                    listener?.onKeyPressed(key.spec)
                }
                activeKey = null
                longPressTriggered = false
                invalidate()
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelActiveTouch()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    override fun getAccessibilityNodeProvider(): AccessibilityNodeProvider = a11yProvider

    override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        if (accessibilityManager?.isTouchExplorationEnabled == true) {
            val hitId = findKeyAt(event.x, event.y)?.index ?: AccessibilityNodeProvider.HOST_VIEW_ID
            when (event.actionMasked) {
                MotionEvent.ACTION_HOVER_ENTER, MotionEvent.ACTION_HOVER_MOVE -> {
                    if (hitId != lastHoveredVirtualId) {
                        if (lastHoveredVirtualId != AccessibilityNodeProvider.HOST_VIEW_ID) {
                            sendVirtualAccessibilityEvent(lastHoveredVirtualId, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT)
                        }
                        lastHoveredVirtualId = hitId
                        if (hitId != AccessibilityNodeProvider.HOST_VIEW_ID) {
                            sendVirtualAccessibilityEvent(hitId, AccessibilityEvent.TYPE_VIEW_HOVER_ENTER)
                        }
                    }
                    return true
                }
                MotionEvent.ACTION_HOVER_EXIT -> {
                    if (lastHoveredVirtualId != AccessibilityNodeProvider.HOST_VIEW_ID) {
                        sendVirtualAccessibilityEvent(lastHoveredVirtualId, AccessibilityEvent.TYPE_VIEW_HOVER_EXIT)
                        lastHoveredVirtualId = AccessibilityNodeProvider.HOST_VIEW_ID
                    }
                    return true
                }
            }
        }
        return super.dispatchHoverEvent(event)
    }

    private fun sendVirtualAccessibilityEvent(virtualViewId: Int, eventType: Int) {
        val manager = accessibilityManager ?: return
        if (!manager.isEnabled) return
        val parentView = parent ?: return
        val event = AccessibilityEvent.obtain(eventType).apply {
            packageName = context.packageName
            className = EkushKeyboardView::class.java.name
            setSource(this@EkushKeyboardView, virtualViewId)
            val key = laidOutKeys.getOrNull(virtualViewId)
            if (key != null) {
                contentDescription = describeKey(key.spec)
            }
        }
        parentView.requestSendAccessibilityEvent(this, event)
    }

    private fun cancelActiveTouch() {
        mainHandler.removeCallbacks(longPressRunnable)
        mainHandler.removeCallbacks(backspaceRepeatRunnable)
        popupWindow.dismiss()
        activeKey = null
        longPressTriggered = false
        invalidate()
    }

    private fun startBackspaceRepeat() {
        listener?.onBackspaceRepeat()
        mainHandler.postDelayed(backspaceRepeatRunnable, 65L)
    }

    private fun triggerRipple(index: Int) {
        val animScale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        if (animScale <= 0f) return
        mainHandler.removeCallbacks(rippleTickRunnable)
        rippleKeyIndex = index
        rippleProgress = 0.2f
        mainHandler.postDelayed(rippleTickRunnable, 16L)
    }

    private fun findKeyAt(x: Float, y: Float): LaidOutKey? {
        laidOutKeys.firstOrNull { it.bounds.contains(x, y) }?.let { return it }
        // Nearest key fallback inside padding gaps so fast thumb typing never drops keys
        return laidOutKeys.minByOrNull { k ->
            val dx = x - k.bounds.centerX()
            val dy = y - k.bounds.centerY()
            dx * dx + dy * dy
        }
    }

    private fun describeKey(spec: KeySpec): String {
        val base = when (spec.role) {
            KeyRole.BACKSPACE -> context.getString(R.string.ime_a11y_backspace)
            KeyRole.ENTER -> context.getString(R.string.ime_a11y_enter)
            KeyRole.SHIFT -> context.getString(R.string.ime_a11y_shift)
            KeyRole.SPACE -> context.getString(R.string.ime_a11y_space)
            KeyRole.MODE_SWITCH -> context.getString(R.string.ime_a11y_mode_switch)
            KeyRole.SYMBOLS -> context.getString(R.string.ime_a11y_numbers)
            KeyRole.HASANTA -> context.getString(R.string.ime_a11y_hasanta)
            KeyRole.FUNCTION -> if (spec.id == "tab") context.getString(R.string.ime_a11y_tab) else spec.label
            KeyRole.CHARACTER -> spec.label
        }
        return if (spec.alternates.isNotEmpty()) {
            "$base, ${spec.alternates.joinToString(" ")}"
        } else {
            base
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancelActiveTouch()
        mainHandler.removeCallbacks(rippleTickRunnable)
    }

    /**
     * TalkBack virtual accessibility nodes for every key (Blueprint Part 05.7).
     */
    private inner class KeyboardAccessibilityNodeProvider : AccessibilityNodeProvider() {
        private val tempLocation = IntArray(2)

        override fun createAccessibilityNodeInfo(virtualViewId: Int): AccessibilityNodeInfo? {
            if (virtualViewId == HOST_VIEW_ID) {
                val info = AccessibilityNodeInfo.obtain(this@EkushKeyboardView)
                onInitializeAccessibilityNodeInfo(info)
                for (k in laidOutKeys) {
                    info.addChild(this@EkushKeyboardView, k.index)
                }
                return info
            }

            val key = laidOutKeys.getOrNull(virtualViewId) ?: return null
            val info = AccessibilityNodeInfo.obtain(this@EkushKeyboardView, virtualViewId)
            info.packageName = context.packageName
            info.className = android.widget.Button::class.java.name
            info.contentDescription = describeKey(key.spec)
            info.isEnabled = true
            info.isClickable = true
            info.isFocusable = true
            val parentRect = Rect(
                key.bounds.left.toInt(),
                key.bounds.top.toInt(),
                key.bounds.right.toInt(),
                key.bounds.bottom.toInt()
            )
            info.setBoundsInParent(parentRect)
            getLocationOnScreen(tempLocation)
            val screenRect = Rect(parentRect).apply {
                offset(tempLocation[0], tempLocation[1])
            }
            info.setBoundsInScreen(screenRect)
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_CLICK)
            info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_ACCESSIBILITY_FOCUS)
            return info
        }

        override fun performAction(virtualViewId: Int, action: Int, arguments: Bundle?): Boolean {
            if (virtualViewId == HOST_VIEW_ID) {
                return performAccessibilityAction(action, arguments)
            }
            val key = laidOutKeys.getOrNull(virtualViewId) ?: return false
            return when (action) {
                AccessibilityNodeInfo.ACTION_CLICK -> {
                    listener?.onKeyPressed(key.spec)
                    sendVirtualAccessibilityEvent(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED)
                    true
                }
                AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS -> {
                    sendVirtualAccessibilityEvent(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED)
                    true
                }
                AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS -> {
                    sendVirtualAccessibilityEvent(virtualViewId, AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUS_CLEARED)
                    true
                }
                else -> false
            }
        }
    }
}
