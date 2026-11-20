/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.prefs.DarkModeOverride
import com.example.data.prefs.EkushThemeId
import com.example.ui.EkushViewModel
import com.example.ui.MainAppScreen
import com.example.ui.theme.EkushTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: EkushViewModel = viewModel()
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (settings.darkModeOverride) {
                DarkModeOverride.SYSTEM -> systemDark
                DarkModeOverride.LIGHT -> false
                DarkModeOverride.DARK -> true
            }
            val dynamicColor = settings.themeId == EkushThemeId.MATERIAL_YOU

            EkushTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor
            ) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}
