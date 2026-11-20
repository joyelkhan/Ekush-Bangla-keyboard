/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val EkushLightColorScheme = lightColorScheme(
    primary = EkushGreen,
    onPrimary = Color.White,
    primaryContainer = EkushPaperVariant,
    onPrimaryContainer = EkushInk,
    secondary = EkushInk,
    onSecondary = EkushPaper,
    secondaryContainer = EkushPaperSurface,
    onSecondaryContainer = EkushInk,
    error = EkushRed,
    onError = Color.White,
    background = EkushPaper,
    onBackground = EkushInk,
    surface = EkushPaper,
    onSurface = EkushInk,
    surfaceVariant = EkushPaperSurface,
    onSurfaceVariant = Color(0xFF3D524A),
    outline = EkushOutlineLight
)

private val EkushDarkColorScheme = darkColorScheme(
    primary = EkushMint,
    onPrimary = EkushNight,
    primaryContainer = EkushNightVariant,
    onPrimaryContainer = EkushPaper,
    secondary = EkushPaper,
    onSecondary = EkushNight,
    secondaryContainer = EkushNightSurface,
    onSecondaryContainer = EkushPaper,
    error = EkushRed,
    onError = Color.White,
    background = EkushNight,
    onBackground = EkushPaper,
    surface = EkushNight,
    onSurface = EkushPaper,
    surfaceVariant = EkushNightSurface,
    onSurfaceVariant = Color(0xFFA4BCB2),
    outline = EkushOutlineDark
)

val EkushShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun EkushTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> EkushDarkColorScheme
        else -> EkushLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = EkushTypography,
        shapes = EkushShapes,
        content = content
    )
}
