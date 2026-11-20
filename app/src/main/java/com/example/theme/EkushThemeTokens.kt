/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.example.theme

import android.content.Context
import android.os.Build
import androidx.annotation.ColorInt
import com.example.data.prefs.EkushThemeId
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Five-token keyboard palette + key semantic roles per Blueprint Part 02.5 and 05.4.
 */
data class KeyboardPalette(
    val themeId: EkushThemeId,
    val isDark: Boolean,
    @ColorInt val background: Int,
    @ColorInt val keySurface: Int,
    @ColorInt val functionKeySurface: Int,
    @ColorInt val hasantaKeySurface: Int,
    @ColorInt val hasantaKeyActiveSurface: Int,
    @ColorInt val keyLabel: Int,
    @ColorInt val altLabel: Int,
    @ColorInt val accent: Int,
    @ColorInt val onAccent: Int,
    @ColorInt val suggestionStripSurface: Int,
    @ColorInt val suggestionText: Int,
    @ColorInt val border: Int
)

object EkushPalettes {
    // Brand tokens (Blueprint Part 02.5)
    const val EKUSH_GREEN = 0xFF006A4E.toInt()
    const val EKUSH_RED = 0xFFF42A41.toInt()
    const val INK = 0xFF16241F.toInt()
    const val PAPER = 0xFFFAF7F0.toInt()
    const val NIGHT = 0xFF0E1B17.toInt()
    const val MINT_ACCENT = 0xFF3ECF9A.toInt()

    fun resolve(themeId: EkushThemeId, isDark: Boolean, context: Context? = null): KeyboardPalette {
        if (themeId == EkushThemeId.MATERIAL_YOU && context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return materialYouPalette(context, isDark)
        }
        return when (themeId) {
            EkushThemeId.EKUSH -> if (isDark) ekushDark() else ekushLight()
            EkushThemeId.NIGHT -> if (isDark) nightDark() else nightLight()
            EkushThemeId.PAPER -> if (isDark) paperDark() else paperLight()
            EkushThemeId.BOISHAKH -> if (isDark) boishakhDark() else boishakhLight()
            EkushThemeId.RIVER -> if (isDark) riverDark() else riverLight()
            EkushThemeId.MATERIAL_YOU -> if (isDark) ekushDark() else ekushLight()
        }
    }

    fun allStaticLightAndDark(): List<KeyboardPalette> = listOf(
        ekushLight(), ekushDark(),
        nightLight(), nightDark(),
        paperLight(), paperDark(),
        boishakhLight(), boishakhDark(),
        riverLight(), riverDark()
    )

    private fun ekushLight() = KeyboardPalette(
        themeId = EkushThemeId.EKUSH,
        isDark = false,
        background = PAPER,
        keySurface = 0xFFFFFFFF.toInt(),
        functionKeySurface = 0xFFE6EFEA.toInt(),
        hasantaKeySurface = 0xFFFDE8EB.toInt(),
        hasantaKeyActiveSurface = 0xFFF9C8CF.toInt(),
        keyLabel = INK,
        altLabel = 0xFF3D524A.toInt(),
        accent = EKUSH_GREEN,
        onAccent = 0xFFFFFFFF.toInt(),
        suggestionStripSurface = 0xFFF2EEE5.toInt(),
        suggestionText = INK,
        border = 0xFFD9D3C7.toInt()
    )

    private fun ekushDark() = KeyboardPalette(
        themeId = EkushThemeId.EKUSH,
        isDark = true,
        background = NIGHT,
        keySurface = INK,
        functionKeySurface = 0xFF1D332B.toInt(),
        hasantaKeySurface = 0xFF3A1D24.toInt(),
        hasantaKeyActiveSurface = 0xFF5D2430.toInt(),
        keyLabel = PAPER,
        altLabel = 0xFFA4BCB2.toInt(),
        accent = MINT_ACCENT,
        onAccent = NIGHT,
        suggestionStripSurface = 0xFF12221D.toInt(),
        suggestionText = PAPER,
        border = 0xFF283E35.toInt()
    )

    private fun nightLight() = KeyboardPalette(
        themeId = EkushThemeId.NIGHT,
        isDark = false,
        background = 0xFFEDF4F0.toInt(),
        keySurface = 0xFFFFFFFF.toInt(),
        functionKeySurface = 0xFFDDEBE4.toInt(),
        hasantaKeySurface = 0xFFFCE4E7.toInt(),
        hasantaKeyActiveSurface = 0xFFF6C2C9.toInt(),
        keyLabel = NIGHT,
        altLabel = 0xFF355046.toInt(),
        accent = EKUSH_GREEN,
        onAccent = 0xFFFFFFFF.toInt(),
        suggestionStripSurface = 0xFFE2ECE7.toInt(),
        suggestionText = NIGHT,
        border = 0xFFC8D7D0.toInt()
    )

    private fun nightDark() = KeyboardPalette(
        themeId = EkushThemeId.NIGHT,
        isDark = true,
        background = NIGHT,
        keySurface = 0xFF172722.toInt(),
        functionKeySurface = 0xFF1F3830.toInt(),
        hasantaKeySurface = 0xFF3E1C23.toInt(),
        hasantaKeyActiveSurface = 0xFF63232F.toInt(),
        keyLabel = 0xFFF5FAF7.toInt(),
        altLabel = 0xFFA8C5BA.toInt(),
        accent = MINT_ACCENT,
        onAccent = NIGHT,
        suggestionStripSurface = 0xFF12211C.toInt(),
        suggestionText = 0xFFF5FAF7.toInt(),
        border = 0xFF273F37.toInt()
    )

    private fun paperLight() = KeyboardPalette(
        themeId = EkushThemeId.PAPER,
        isDark = false,
        background = 0xFFF2EFE9.toInt(),
        keySurface = 0xFFFCFBF8.toInt(),
        functionKeySurface = 0xFFE3DFD7.toInt(),
        hasantaKeySurface = 0xFFF9E5E7.toInt(),
        hasantaKeyActiveSurface = 0xFFF1C6CC.toInt(),
        keyLabel = INK,
        altLabel = 0xFF4B5551.toInt(),
        accent = INK,
        onAccent = 0xFFFFFFFF.toInt(),
        suggestionStripSurface = 0xFFE8E4DC.toInt(),
        suggestionText = INK,
        border = 0xFFD0CBC1.toInt()
    )

    private fun paperDark() = KeyboardPalette(
        themeId = EkushThemeId.PAPER,
        isDark = true,
        background = 0xFF181A19.toInt(),
        keySurface = 0xFF252827.toInt(),
        functionKeySurface = 0xFF323634.toInt(),
        hasantaKeySurface = 0xFF3E2226.toInt(),
        hasantaKeyActiveSurface = 0xFF5D2B32.toInt(),
        keyLabel = 0xFFF2EFE9.toInt(),
        altLabel = 0xFFB5B9B7.toInt(),
        accent = 0xFFD9E3DE.toInt(),
        onAccent = 0xFF181A19.toInt(),
        suggestionStripSurface = 0xFF1F2221.toInt(),
        suggestionText = 0xFFF2EFE9.toInt(),
        border = 0xFF3B403E.toInt()
    )

    private fun boishakhLight() = KeyboardPalette(
        themeId = EkushThemeId.BOISHAKH,
        isDark = false,
        background = 0xFFFFF6E8.toInt(),
        keySurface = 0xFFFFFFFF.toInt(),
        functionKeySurface = 0xFFFBE7D2.toInt(),
        hasantaKeySurface = 0xFFFEE2E5.toInt(),
        hasantaKeyActiveSurface = 0xFFF9BDC4.toInt(),
        keyLabel = 0xFF2B1714.toInt(),
        altLabel = 0xFF6B4740.toInt(),
        accent = 0xFFB81427.toInt(), // Accessible deep red on warm cream (>=4.5:1)
        onAccent = 0xFFFFFFFF.toInt(),
        suggestionStripSurface = 0xFFF8EBDC.toInt(),
        suggestionText = 0xFF2B1714.toInt(),
        border = 0xFFE6D4BF.toInt()
    )

    private fun boishakhDark() = KeyboardPalette(
        themeId = EkushThemeId.BOISHAKH,
        isDark = true,
        background = 0xFF1E1312.toInt(),
        keySurface = 0xFF2D1E1C.toInt(),
        functionKeySurface = 0xFF3D2724.toInt(),
        hasantaKeySurface = 0xFF4C1C23.toInt(),
        hasantaKeyActiveSurface = 0xFF722631.toInt(),
        keyLabel = 0xFFFFF6E8.toInt(),
        altLabel = 0xFFD7B6AE.toInt(),
        accent = 0xFFFF6B7A.toInt(),
        onAccent = 0xFF1E1312.toInt(),
        suggestionStripSurface = 0xFF251816.toInt(),
        suggestionText = 0xFFFFF6E8.toInt(),
        border = 0xFF47302C.toInt()
    )

    private fun riverLight() = KeyboardPalette(
        themeId = EkushThemeId.RIVER,
        isDark = false,
        background = 0xFFEFF5F2.toInt(),
        keySurface = 0xFFFFFFFF.toInt(),
        functionKeySurface = 0xFFDCEAE4.toInt(),
        hasantaKeySurface = 0xFFFCE5E8.toInt(),
        hasantaKeyActiveSurface = 0xFFF6C3CA.toInt(),
        keyLabel = 0xFF102421.toInt(),
        altLabel = 0xFF35544E.toInt(),
        accent = 0xFF00706A.toInt(),
        onAccent = 0xFFFFFFFF.toInt(),
        suggestionStripSurface = 0xFFE2EDE8.toInt(),
        suggestionText = 0xFF102421.toInt(),
        border = 0xFFC7D8D1.toInt()
    )

    private fun riverDark() = KeyboardPalette(
        themeId = EkushThemeId.RIVER,
        isDark = true,
        background = 0xFF0C1C1B.toInt(),
        keySurface = 0xFF152B29.toInt(),
        functionKeySurface = 0xFF1E3C39.toInt(),
        hasantaKeySurface = 0xFF3B1D24.toInt(),
        hasantaKeyActiveSurface = 0xFF5D2632.toInt(),
        keyLabel = 0xFFEFF5F2.toInt(),
        altLabel = 0xFF9EC0BA.toInt(),
        accent = 0xFF4FD1C5.toInt(),
        onAccent = 0xFF0C1C1B.toInt(),
        suggestionStripSurface = 0xFF102321.toInt(),
        suggestionText = 0xFFEFF5F2.toInt(),
        border = 0xFF254440.toInt()
    )

    private fun materialYouPalette(context: Context, isDark: Boolean): KeyboardPalette {
        val base = if (isDark) ekushDark() else ekushLight()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return base
        return runCatching {
            val bg = context.getColor(
                if (isDark) android.R.color.system_neutral1_900 else android.R.color.system_neutral1_50
            )
            val keySurf = context.getColor(
                if (isDark) android.R.color.system_neutral1_800 else android.R.color.system_neutral1_0
            )
            val fnSurf = context.getColor(
                if (isDark) android.R.color.system_accent2_800 else android.R.color.system_accent2_100
            )
            val label = context.getColor(
                if (isDark) android.R.color.system_neutral1_50 else android.R.color.system_neutral1_900
            )
            val accent = context.getColor(
                if (isDark) android.R.color.system_accent1_200 else android.R.color.system_accent1_600
            )
            val strip = context.getColor(
                if (isDark) android.R.color.system_neutral2_900 else android.R.color.system_neutral2_100
            )
            val border = context.getColor(
                if (isDark) android.R.color.system_neutral2_700 else android.R.color.system_neutral2_200
            )
            base.copy(
                themeId = EkushThemeId.MATERIAL_YOU,
                background = bg,
                keySurface = keySurf,
                functionKeySurface = fnSurf,
                keyLabel = label,
                accent = accent,
                suggestionStripSurface = strip,
                suggestionText = label,
                border = border
            )
        }.getOrDefault(base)
    }
}

/**
 * Automated WCAG 2.1 contrast checker (Blueprint Part 02.5 & 05.7):
 * - Body / suggestion strip text >= 4.5:1
 * - Key labels against key surface >= 3.0:1
 */
object ThemeContrastValidator {
    data class Report(
        val keyLabelContrast: Double,
        val functionKeyContrast: Double,
        val suggestionStripContrast: Double,
        val accentOnBackgroundContrast: Double,
        val passes: Boolean
    )

    fun validate(palette: KeyboardPalette): Report {
        val keyContrast = contrastRatio(palette.keyLabel, palette.keySurface)
        val fnContrast = contrastRatio(palette.keyLabel, palette.functionKeySurface)
        val stripContrast = contrastRatio(palette.suggestionText, palette.suggestionStripSurface)
        val accentContrast = contrastRatio(palette.accent, palette.background)
        val passes = keyContrast >= 3.0 && fnContrast >= 3.0 && stripContrast >= 4.5 && accentContrast >= 3.0
        return Report(
            keyLabelContrast = keyContrast,
            functionKeyContrast = fnContrast,
            suggestionStripContrast = stripContrast,
            accentOnBackgroundContrast = accentContrast,
            passes = passes
        )
    }

    fun contrastRatio(@ColorInt fg: Int, @ColorInt bg: Int): Double {
        val l1 = relativeLuminance(fg)
        val l2 = relativeLuminance(bg)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    private fun relativeLuminance(@ColorInt color: Int): Double {
        fun linearize(channel: Int): Double {
            val s = channel / 255.0
            return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        val r = linearize((color ushr 16) and 0xFF)
        val g = linearize((color ushr 8) and 0xFF)
        val b = linearize(color and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }
}
