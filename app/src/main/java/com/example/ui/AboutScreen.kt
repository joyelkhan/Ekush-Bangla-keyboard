/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.prefs.EkushSettings
import kotlinx.coroutines.launch

@Composable
fun AboutScreen(
    settings: EkushSettings,
    lexiconCount: Int,
    snackbarHostState: SnackbarHostState,
    onReopenOnboarding: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val diagnosticCopied = stringResource(R.string.about_diagnostic_copied)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.about_title),
            style = MaterialTheme.typography.headlineMedium
        )

        // Brand & NaserX Labs card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_ekush_mark_outlined),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                    Column {
                        Text(
                            text = stringResource(R.string.about_developer_card_title),
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = stringResource(R.string.about_version_label, BuildConfig.VERSION_NAME),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.about_developer_card_desc),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Privacy Architecture (Blueprint Part 07.4)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.about_privacy_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Text(
                    text = stringResource(R.string.about_privacy_bullets),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // Attribution Table (Blueprint Part 07.2)
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.about_licenses_title),
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                Text(stringResource(R.string.about_attribution_ekush), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.about_attribution_heliboard), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.about_attribution_openbangla), style = MaterialTheme.typography.bodyMedium)
                Text(stringResource(R.string.about_attribution_fonts), style = MaterialTheme.typography.bodyMedium)
            }
        }

        // Manual diagnostic export + reopen onboarding
        Button(
            onClick = {
                val report = buildString {
                    appendLine("Ekush Bangla Keyboard Diagnostic Summary")
                    appendLine("Developer: Abu Naser Khan (NaserX Labs)")
                    appendLine("Version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    appendLine("Package: ${BuildConfig.APPLICATION_ID}")
                    appendLine("Android SDK: ${Build.VERSION.SDK_INT}")
                    appendLine("Default Mode: ${settings.defaultMode.wireName}")
                    appendLine("Theme: ${settings.themeId.wireName} (${settings.darkModeOverride.wireName})")
                    appendLine("Local Lexicon Entries: $lexiconCount")
                    appendLine("Internet Permission: NONE (0 network permissions declared)")
                }
                val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cb?.setPrimaryClip(ClipData.newPlainText("Ekush Diagnostic", report))
                scope.launch { snackbarHostState.showSnackbar(diagnosticCopied) }
            },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("about_diagnostic_button")
        ) {
            Icon(Icons.Rounded.BugReport, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.about_export_crash_report))
        }

        OutlinedButton(
            onClick = onReopenOnboarding,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .testTag("about_reopen_onboarding_button")
        ) {
            Icon(Icons.Rounded.Refresh, contentDescription = null)
            Spacer(modifier = Modifier.size(8.dp))
            Text(stringResource(R.string.about_reopen_onboarding))
        }
    }
}
