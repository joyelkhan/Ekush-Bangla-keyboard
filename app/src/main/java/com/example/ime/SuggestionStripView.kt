/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.example.ime

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.theme.EkushPalettes
import com.example.theme.KeyboardPalette

/**
 * 44dp suggestion strip with 3 slots (left suggestion, middle live composition/mode label,
 * right suggestion) + optional autocorrect revert chip & clipboard paste chip (Blueprint Part 05.6).
 */
class SuggestionStripView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Listener {
        fun onSuggestionClicked(slotIndex: Int, word: String)
        fun onRevertAutocorrectClicked()
        fun onClipboardClicked()
        fun onModeBadgeClicked()
    }

    var listener: Listener? = null

    private var palette: KeyboardPalette = EkushPalettes.resolve(
        com.example.data.prefs.EkushThemeId.EKUSH,
        isDark = false,
        context = context
    )
    private var leftWord: String? = null
    private var middleWord: String = ""
    private var rightWord: String? = null
    private var emptyModeBadge: String = "PHONETIC"
    private var revertLabel: String? = null
    private var clipboardSnippet: String? = null

    private val density = context.resources.displayMetrics.density
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }
    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        letterSpacing = 0.08f
    }

    private val bengaliTypeface: Typeface? = runCatching {
        ResourcesCompat.getFont(context, R.font.noto_sans_bengali)
    }.getOrNull()

    private val latinTypeface: Typeface? = runCatching {
        ResourcesCompat.getFont(context, R.font.inter_medium)
    }.getOrNull()

    private var pressedSlot = -1
    private val slotRects = Array(3) { RectF() }

    init {
        isClickable = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        applyPalette(palette)
    }

    fun applyPalette(newPalette: KeyboardPalette) {
        palette = newPalette
        bgPaint.color = palette.suggestionStripSurface
        dividerPaint.color = palette.border
        dividerPaint.strokeWidth = 1f * density
        pillPaint.color = palette.keySurface
        textPaint.color = palette.suggestionText
        textPaint.textSize = 15f * density
        textPaint.typeface = bengaliTypeface ?: Typeface.DEFAULT
        badgePaint.color = palette.altLabel
        badgePaint.textSize = 11f * density
        badgePaint.typeface = latinTypeface ?: Typeface.DEFAULT_BOLD
        invalidate()
    }

    fun updateSuggestions(
        left: String?,
        middle: String,
        right: String?,
        emptyModeLabel: String,
        revertChip: String? = null,
        clipboardPreview: String? = null
    ) {
        this.leftWord = left
        this.middleWord = middle
        this.rightWord = right
        this.emptyModeBadge = emptyModeLabel
        this.revertLabel = revertChip
        this.clipboardSnippet = clipboardPreview
        contentDescription = buildAccessibilityDescription()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val desiredHeight = (44f * density).toInt()
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, resolveSize(desiredHeight, heightMeasureSpec))
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val slotW = w / 3f
        for (i in 0..2) {
            slotRects[i].set(i * slotW, 0f, (i + 1) * slotW, h.toFloat())
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, bgPaint)
        canvas.drawLine(0f, h - density * 0.5f, w, h - density * 0.5f, dividerPaint)

        // If revert chip is active right after autocorrect, show it prominently in middle
        val rev = revertLabel
        if (rev != null && middleWord.isEmpty()) {
            val pillRect = RectF(w * 0.15f, 6f * density, w * 0.85f, h - 6f * density)
            pillPaint.color = palette.hasantaKeySurface
            canvas.drawRoundRect(pillRect, 8f * density, 8f * density, pillPaint)
            textPaint.color = palette.keyLabel
            drawCenteredText(canvas, rev, pillRect, textPaint)
            return
        }

        // Empty state: show quiet mode name in small caps (Blueprint Part 05.6) + optional clip chip
        if (leftWord == null && middleWord.isEmpty() && rightWord == null) {
            drawCenteredText(canvas, emptyModeBadge.uppercase(), slotRects[1], badgePaint)
            clipboardSnippet?.let { clip ->
                val clipLabel = if (clip.length > 14) clip.take(13) + "…" else clip
                drawCenteredText(canvas, "⎘ $clipLabel", slotRects[2], badgePaint)
            }
            return
        }

        // Dividers between the 3 slots
        val divTop = 10f * density
        val divBottom = h - 10f * density
        canvas.drawLine(slotRects[1].left, divTop, slotRects[1].left, divBottom, dividerPaint)
        canvas.drawLine(slotRects[2].left, divTop, slotRects[2].left, divBottom, dividerPaint)

        // Left slot
        leftWord?.let { word ->
            if (pressedSlot == 0) {
                pillPaint.color = palette.keySurface
                canvas.drawRoundRect(insetRect(slotRects[0]), 6f * density, 6f * density, pillPaint)
            }
            textPaint.color = palette.suggestionText
            textPaint.typeface = bengaliTypeface ?: Typeface.DEFAULT
            drawCenteredText(canvas, ellipsize(word), slotRects[0], textPaint)
        }

        // Middle slot (live composing word highlighted in accent color per Blueprint Part 05.6)
        if (middleWord.isNotEmpty()) {
            if (pressedSlot == 1) {
                pillPaint.color = palette.keySurface
                canvas.drawRoundRect(insetRect(slotRects[1]), 6f * density, 6f * density, pillPaint)
            }
            textPaint.color = palette.accent
            textPaint.typeface = Typeface.create(bengaliTypeface ?: Typeface.DEFAULT, Typeface.BOLD)
            drawCenteredText(canvas, ellipsize(middleWord), slotRects[1], textPaint)
        }

        // Right slot
        rightWord?.let { word ->
            if (pressedSlot == 2) {
                pillPaint.color = palette.keySurface
                canvas.drawRoundRect(insetRect(slotRects[2]), 6f * density, 6f * density, pillPaint)
            }
            textPaint.color = palette.suggestionText
            textPaint.typeface = bengaliTypeface ?: Typeface.DEFAULT
            drawCenteredText(canvas, ellipsize(word), slotRects[2], textPaint)
        }
    }

    private fun insetRect(r: RectF): RectF =
        RectF(r.left + 4f * density, r.top + 4f * density, r.right - 4f * density, r.bottom - 4f * density)

    private fun ellipsize(s: String): String =
        if (s.length > 16) s.take(15) + "…" else s

    private fun drawCenteredText(canvas: Canvas, text: String, rect: RectF, paint: Paint) {
        val fm = paint.fontMetrics
        val baseline = rect.centerY() - (fm.ascent + fm.descent) / 2f
        canvas.drawText(text, rect.centerX(), baseline, paint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                pressedSlot = slotIndexForX(event.x)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                val s = slotIndexForX(event.x)
                if (s != pressedSlot) {
                    pressedSlot = s
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val slot = slotIndexForX(event.x)
                pressedSlot = -1
                invalidate()
                if (revertLabel != null && middleWord.isEmpty()) {
                    listener?.onRevertAutocorrectClicked()
                } else if (leftWord == null && middleWord.isEmpty() && rightWord == null && slot == 1) {
                    listener?.onModeBadgeClicked()
                } else if (leftWord == null && middleWord.isEmpty() && rightWord == null && slot == 2 && clipboardSnippet != null) {
                    listener?.onClipboardClicked()
                } else {
                    val word = when (slot) {
                        0 -> leftWord
                        1 -> middleWord.takeIf { it.isNotEmpty() }
                        2 -> rightWord
                        else -> null
                    }
                    if (word != null) {
                        listener?.onSuggestionClicked(slot, word)
                    }
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                pressedSlot = -1
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun slotIndexForX(x: Float): Int {
        val third = width / 3f
        return when {
            x < third -> 0
            x < third * 2f -> 1
            else -> 2
        }
    }

    private fun buildAccessibilityDescription(): String {
        val parts = mutableListOf<String>()
        leftWord?.let { parts += context.getString(R.string.ime_a11y_suggestion_slot, 1, it) }
        if (middleWord.isNotEmpty()) {
            parts += context.getString(R.string.ime_a11y_suggestion_slot, 2, middleWord)
        }
        rightWord?.let { parts += context.getString(R.string.ime_a11y_suggestion_slot, 3, it) }
        return if (parts.isEmpty()) emptyModeBadge else parts.joinToString(", ")
    }
}
