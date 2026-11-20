/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.prefs.EkushSettings
import com.example.data.prefs.HapticIntensity
import com.example.data.prefs.TypingMode
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: EkushSettings,
    backupMessage: String?,
    snackbarHostState: SnackbarHostState,
    viewModel: EkushViewModel
) {
    val exportSuccess = stringResource(R.string.settings_backup_exported)
    val backupError = stringResource(R.string.settings_backup_error)
    val importedFormat = stringResource(R.string.settings_backup_imported)

    LaunchedEffect(backupMessage) {
        if (!backupMessage.isNullOrBlank()) {
            snackbarHostState.showSnackbar(backupMessage)
            viewModel.clearBackupMessage()
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportBackup(uri, exportSuccess, backupError)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.importBackup(
                uri = uri,
                successFormatter = { count -> String.format(importedFormat, count) },
                errorMsg = backupError
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium
        )

        // 1. Typing Section
        SettingsSectionCard(title = stringResource(R.string.settings_section_typing)) {
            Text(
                text = stringResource(R.string.settings_default_mode),
                style = MaterialTheme.typography.titleMedium
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val modes = listOf(
                    TypingMode.PHONETIC to R.string.mode_phonetic,
                    TypingMode.FIXED_BANGLA to R.string.mode_fixed,
                    TypingMode.ENGLISH_QWERTY to R.string.mode_english
                )
                modes.forEachIndexed { index, (mode, res) ->
                    SegmentedButton(
                        selected = settings.defaultMode == mode,
                        onClick = { viewModel.setDefaultMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("settings_mode_${mode.wireName}")
                    ) {
                        Text(stringResource(res), maxLines = 1)
                    }
                }
            }

            HorizontalDivider()

            SettingSwitchRow(
                title = stringResource(R.string.settings_auto_conjuncts),
                subtitle = stringResource(R.string.settings_auto_conjuncts_desc),
                checked = settings.autoConjunctsEnabled,
                tag = "settings_switch_auto_conjuncts",
                onCheckedChange = viewModel::setAutoConjunctsEnabled
            )

            SettingSwitchRow(
                title = stringResource(R.string.settings_bangla_digits),
                subtitle = stringResource(R.string.settings_bangla_digits_desc),
                checked = settings.preferBanglaDigits,
                tag = "settings_switch_bangla_digits",
                onCheckedChange = viewModel::setPreferBanglaDigits
            )

            SettingSwitchRow(
                title = stringResource(R.string.settings_flutter_fallback),
                subtitle = stringResource(R.string.settings_flutter_fallback_desc),
                checked = settings.commitOnEveryKeystroke,
                tag = "settings_switch_commit_every_key",
                onCheckedChange = viewModel::setCommitOnEveryKeystroke
            )
        }

        // 2. Languages Section
        SettingsSectionCard(title = stringResource(R.string.settings_section_languages)) {
            SettingSwitchRow(
                title = stringResource(R.string.settings_per_app_memory),
                subtitle = stringResource(R.string.settings_per_app_memory_desc),
                checked = settings.perAppModeMemory,
                tag = "settings_switch_per_app_memory",
                onCheckedChange = viewModel::setPerAppModeMemory
            )
        }

        // 3. Typing Experience Section
        SettingsSectionCard(title = stringResource(R.string.settings_section_experience)) {
            SettingSwitchRow(
                title = stringResource(R.string.settings_haptics),
                subtitle = stringResource(R.string.settings_haptics_desc),
                checked = settings.hapticsEnabled,
                tag = "settings_switch_haptics",
                onCheckedChange = viewModel::setHapticsEnabled
            )

            if (settings.hapticsEnabled) {
                Text(
                    text = stringResource(R.string.settings_haptic_intensity),
                    style = MaterialTheme.typography.titleMedium
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val levels = listOf(
                        HapticIntensity.GENTLE to R.string.haptic_gentle,
                        HapticIntensity.MEDIUM to R.string.haptic_medium,
                        HapticIntensity.STRONG to R.string.haptic_strong
                    )
                    levels.forEachIndexed { idx, (intensity, labelRes) ->
                        SegmentedButton(
                            selected = settings.hapticIntensity == intensity,
                            onClick = { viewModel.setHapticIntensity(intensity) },
                            shape = SegmentedButtonDefaults.itemShape(index = idx, count = levels.size),
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("settings_haptic_${intensity.wireName}")
                        ) {
                            Text(stringResource(labelRes))
                        }
                    }
                }
            }

            SettingSwitchRow(
                title = stringResource(R.string.settings_sound),
                subtitle = stringResource(R.string.settings_sound_desc),
                checked = settings.soundEnabled,
                tag = "settings_switch_sound",
                onCheckedChange = viewModel::setSoundEnabled
            )

            HorizontalDivider()

            Text(
                text = stringResource(R.string.settings_key_height, settings.keyHeightDp),
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = settings.keyHeightDp.toFloat(),
                onValueChange = { viewModel.setKeyHeightDp(it.roundToInt()) },
                valueRange = 48f..72f,
                modifier = Modifier.testTag("settings_slider_key_height")
            )

            Text(
                text = stringResource(R.string.settings_gap_scale, settings.gapScale),
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = settings.gapScale,
                onValueChange = { viewModel.setGapScale((it * 10f).roundToInt() / 10f) },
                valueRange = 0.6f..1.4f,
                modifier = Modifier.testTag("settings_slider_gap_scale")
            )

            Text(
                text = stringResource(R.string.settings_long_press_delay, settings.longPressDelayMs),
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = settings.longPressDelayMs.toFloat(),
                onValueChange = { viewModel.setLongPressDelayMs((it / 10f).roundToInt() * 10) },
                valueRange = 150f..500f,
                modifier = Modifier.testTag("settings_slider_long_press")
            )
        }

        // 4. Suggestions & Autocorrect Section
        SettingsSectionCard(title = stringResource(R.string.settings_section_suggestions)) {
            SettingSwitchRow(
                title = stringResource(R.string.settings_suggestions_enabled),
                subtitle = stringResource(R.string.settings_suggestions_enabled_desc),
                checked = settings.suggestionsEnabled,
                tag = "settings_switch_suggestions",
                onCheckedChange = viewModel::setSuggestionsEnabled
            )

            SettingSwitchRow(
                title = stringResource(R.string.settings_autocorrect),
                subtitle = stringResource(R.string.settings_autocorrect_desc),
                checked = settings.autocorrectEnabled,
                tag = "settings_switch_autocorrect",
                onCheckedChange = viewModel::setAutocorrectEnabled
            )
        }

        // 5. Clipboard & Local SAF Backup Section
        SettingsSectionCard(title = stringResource(R.string.settings_section_clipboard_backup)) {
            SettingSwitchRow(
                title = stringResource(R.string.settings_clipboard_history),
                subtitle = stringResource(R.string.settings_clipboard_history_desc),
                checked = settings.clipboardHistoryEnabled,
                tag = "settings_switch_clipboard",
                onCheckedChange = viewModel::setClipboardHistoryEnabled
            )

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { exportLauncher.launch("ekush-backup.json") },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("settings_export_button")
                ) {
                    Icon(Icons.Rounded.FileUpload, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.settings_export_backup))
                }
                OutlinedButton(
                    onClick = { importLauncher.launch(arrayOf("application/json", "text/plain")) },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .testTag("settings_import_button")
                ) {
                    Icon(Icons.Rounded.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(stringResource(R.string.settings_import_backup))
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag)
        )
    }
}
