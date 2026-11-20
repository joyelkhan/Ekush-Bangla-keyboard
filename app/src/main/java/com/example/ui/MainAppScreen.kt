/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R

const val ROUTE_SANDBOX = "sandbox"
const val ROUTE_THEMES = "themes"
const val ROUTE_SETTINGS = "settings"
const val ROUTE_LEXICON = "lexicon"
const val ROUTE_ABOUT = "about"

private data class EkushNavItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: EkushViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val imeStatus by viewModel.imeStatus.collectAsStateWithLifecycle()
    val latinInput by viewModel.sandboxLatinInput.collectAsStateWithLifecycle()
    val stripState by viewModel.sandboxStrip.collectAsStateWithLifecycle()
    val userWords by viewModel.userWords.collectAsStateWithLifecycle()
    val backupMessage by viewModel.backupMessage.collectAsStateWithLifecycle()

    LifecycleResumeEffect(Unit) {
        viewModel.refreshImeStatus()
        onPauseOrDispose { }
    }

    if (!settings.onboardingCompleted) {
        OnboardingScreen(
            imeStatus = imeStatus,
            onRefreshImeStatus = viewModel::refreshImeStatus,
            onFinishOnboarding = { viewModel.setOnboardingCompleted(true) }
        )
        return
    }

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_SANDBOX
    val snackbarHostState = remember { SnackbarHostState() }

    val navItems = remember {
        listOf(
            EkushNavItem(ROUTE_SANDBOX, R.string.nav_sandbox, Icons.Rounded.Keyboard, Icons.Outlined.Keyboard),
            EkushNavItem(ROUTE_THEMES, R.string.nav_themes, Icons.Rounded.Palette, Icons.Outlined.Palette),
            EkushNavItem(ROUTE_SETTINGS, R.string.nav_settings, Icons.Rounded.Settings, Icons.Outlined.Settings),
            EkushNavItem(ROUTE_LEXICON, R.string.nav_lexicon, Icons.Rounded.Book, Icons.Outlined.Book),
            EkushNavItem(ROUTE_ABOUT, R.string.nav_about, Icons.Rounded.Info, Icons.Outlined.Info)
        )
    }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge
                        )
                    }
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes), maxLines = 1) },
                                modifier = Modifier.testTag("nav_item_${item.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(modifier = Modifier.fillMaxHeight()) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes), maxLines = 1) },
                                modifier = Modifier.testTag("nav_rail_${item.route}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_SANDBOX,
                        modifier = Modifier
                            .fillMaxSize()
                            .widthIn(max = 840.dp)
                    ) {
                        composable(ROUTE_SANDBOX) {
                            SandboxScreen(
                                imeStatus = imeStatus,
                                latinInput = latinInput,
                                stripState = stripState,
                                snackbarHostState = snackbarHostState,
                                onLatinInputChange = viewModel::updateSandboxInput
                            )
                        }
                        composable(ROUTE_THEMES) {
                            ThemesScreen(
                                settings = settings,
                                onSelectTheme = viewModel::setThemeId,
                                onSelectDarkModeOverride = viewModel::setDarkModeOverride
                            )
                        }
                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(
                                settings = settings,
                                backupMessage = backupMessage,
                                snackbarHostState = snackbarHostState,
                                viewModel = viewModel
                            )
                        }
                        composable(ROUTE_LEXICON) {
                            LexiconScreen(
                                words = userWords,
                                onAddWord = viewModel::addPersonalWord,
                                onDeleteWord = viewModel::deletePersonalWord,
                                onClearAll = viewModel::clearPersonalLexicon
                            )
                        }
                        composable(ROUTE_ABOUT) {
                            AboutScreen(
                                settings = settings,
                                lexiconCount = userWords.size,
                                snackbarHostState = snackbarHostState,
                                onReopenOnboarding = { viewModel.setOnboardingCompleted(false) }
                            )
                        }
                    }
                }
            }
        }
    }
}
