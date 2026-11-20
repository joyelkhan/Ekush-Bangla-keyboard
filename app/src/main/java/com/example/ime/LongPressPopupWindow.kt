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
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.PopupWindow
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.theme.KeyboardPalette

/**
 * Long-press alternates popup window showing Appendix 10.2 conjuncts or Bangla digits
 * with 100 ms 85%->100% scale animation honoring system "remove animations" (Blueprint Part 05.3 & 05.5).
 */
class LongPressPopupWindow(
    private val context: Context
) {
    private var popup: PopupWindow? = null
    private var popupView: AlternatesPopupView? = null
    private var currentAlternates: List<String> = emptyList()
    private var anchorScreenX: Int = 0
    private var itemWidthPx: Float = 0f

    val isShowing: Boolean
        get() = popup?.isShowing == true

    fun show(
        anchor: View,
        keyBoundsInAnchor: RectF,
        alternates: List<String>,
        palette: KeyboardPalette
    ) {
        if (alternates.isEmpty()) return
        dismiss()

        currentAlternates = alternates
        val density = context.resources.displayMetrics.density
        itemWidthPx = 46f * density
        val itemHeightPx = (48f * density).toInt()
        val totalWidthPx = (alternates.size * itemWidthPx + 12f * density).toInt()

        val view = AlternatesPopupView(context, alternates, palette).also {
            popupView = it
        }

        val win = PopupWindow(
            view,
            totalWidthPx,
            itemHeightPx,
            false
        ).apply {
            elevation = 8f * density
            isTouchable = false
            isClippingEnabled = false
        }

        val anchorLoc = IntArray(2)
        anchor.getLocationInWindow(anchorLoc)
        val keyCenterX = anchorLoc[0] + keyBoundsInAnchor.centerX()
        val desiredLeft = (keyCenterX - totalWidthPx / 2f)
            .coerceIn(4f * density, (anchor.width - totalWidthPx - 4f * density).coerceAtLeast(4f * density))
        val popupTop = anchorLoc[1] + keyBoundsInAnchor.top - itemHeightPx - 8f * density

        anchorScreenX = desiredLeft.toInt()
        popup = win
        win.showAtLocation(anchor, Gravity.NO_GRAVITY, desiredLeft.toInt(), popupTop.toInt())

        // Respect system "remove animations" setting (Blueprint Part 05.5 & 05.7)
        val animScale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        if (animScale > 0f) {
            view.scaleX = 0.85f
            view.scaleY = 0.85f
            view.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration((100f * animScale).toLong().coerceAtLeast(1L))
                .setInterpolator(DecelerateInterpolator())
                .start()
        }
    }

    fun updateSelection(touchXInAnchor: Float, anchor: View) {
        val view = popupView ?: return
        val anchorLoc = IntArray(2)
        anchor.getLocationInWindow(anchorLoc)
        val localX = anchorLoc[0] + touchXInAnchor - anchorScreenX
        val index = ((localX - 6f * context.resources.displayMetrics.density) / itemWidthPx)
            .toInt()
            .coerceIn(0, currentAlternates.lastIndex)
        view.selectedIndex = index
    }

    fun selectedAlternate(): String? {
        val idx = popupView?.selectedIndex ?: 0
        return currentAlternates.getOrNull(idx)
    }

    fun dismiss() {
        popup?.dismiss()
        popup = null
        popupView = null
        currentAlternates = emptyList()
    }

    private class AlternatesPopupView(
        context: Context,
        private val items: List<String>,
        private val palette: KeyboardPalette
    ) : View(context) {
        private val density = context.resources.displayMetrics.density
        private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.keySurface }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.border
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * density
        }
        private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.accent }
        private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            textSize = 18f * density
            typeface = runCatching { ResourcesCompat.getFont(context, R.font.noto_sans_bengali) }.getOrNull()
                ?: Typeface.DEFAULT
        }
        private val rect = RectF()
        private val cellRect = RectF()

        var selectedIndex: Int = 0
            set(value) {
                if (field != value) {
                    field = value
                    invalidate()
                }
            }

        init {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val radius = 10f * density
            rect.set(1f * density, 1f * density, width - 1f * density, height - 1f * density)
            canvas.drawRoundRect(rect, radius, radius, bgPaint)
            canvas.drawRoundRect(rect, radius, radius, borderPaint)

            val pad = 6f * density
            val cellW = (width - 2f * pad) / items.size.coerceAtLeast(1)
            for (i in items.indices) {
                val left = pad + i * cellW
                cellRect.set(left + 2f * density, 4f * density, left + cellW - 2f * density, height - 4f * density)
                val isSelected = i == selectedIndex
                if (isSelected) {
                    canvas.drawRoundRect(cellRect, 8f * density, 8f * density, selectedPaint)
                    textPaint.color = palette.onAccent
                } else {
                    textPaint.color = palette.keyLabel
                }
                val fm = textPaint.fontMetrics
                val baseline = cellRect.centerY() - (fm.ascent + fm.descent) / 2f
                canvas.drawText(items[i], cellRect.centerX(), baseline, textPaint)
            }
        }
    }
}
