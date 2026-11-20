/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.example

import com.example.theme.EkushPalettes
import com.example.theme.ThemeContrastValidator
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies every static preset theme satisfies Blueprint Part 02.5 / 05.7 WCAG 2.1 AA contrast ratios.
 */
class ThemeContrastValidatorTest {

    @Test
    fun `all static light and dark palettes pass WCAG AA contrast`() {
        val palettes = EkushPalettes.allStaticLightAndDark()
        assertTrue("Expected 10 static palettes", palettes.size == 10)

        for (palette in palettes) {
            val report = ThemeContrastValidator.validate(palette)
            assertTrue(
                "Palette ${palette.themeId} (dark=${palette.isDark}) failed WCAG: " +
                    "key=${report.keyLabelContrast}, fn=${report.functionKeyContrast}, " +
                    "strip=${report.suggestionStripContrast}, accent=${report.accentOnBackgroundContrast}",
                report.passes
            )
        }
    }
}
