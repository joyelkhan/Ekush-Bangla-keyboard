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
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ekush.engine.SuggestionEngine
import com.example.R
import com.example.ui.theme.NotoSansBengaliFontFamily
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SandboxScreen(
    imeStatus: ImeActivationStatus,
    latinInput: String,
    stripState: SuggestionEngine.StripState,
    snackbarHostState: SnackbarHostState,
    onLatinInputChange: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val copiedMessage = stringResource(R.string.sandbox_copied_message)

    var systemImeText by rememberSaveable { mutableStateOf("") }
    var otpText by rememberSaveable { mutableStateOf("") }

    val samples = listOf(
        "bangladesh",
        "bishwabidyalay",
        "kobita",
        "chiThi",
        "ksha",
        "rka",
        "amra banglay likhi"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.sandbox_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.sandbox_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        if (!imeStatus.isEnabled || !imeStatus.isDefault) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sandbox_enable_banner")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.sandbox_enable_banner),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!imeStatus.isEnabled) {
                            Button(
                                onClick = {
                                    context.startActivity(
                                        Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                },
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .testTag("sandbox_open_ime_settings_button")
                            ) {
                                Text(stringResource(R.string.onboarding_step2_card1_action))
                            }
                        }
                        FilledTonalButton(
                            onClick = {
                                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                                imm?.showInputMethodPicker()
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("sandbox_pick_ime_button")
                        ) {
                            Icon(Icons.Rounded.Keyboard, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(stringResource(R.string.onboarding_step2_card2_action))
                        }
                    }
                }
            }
        }

        // Phonetic engine sandbox card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.sandbox_phonetic_label),
                    style = MaterialTheme.typography.titleMedium
                )

                OutlinedTextField(
                    value = latinInput,
                    onValueChange = onLatinInputChange,
                    placeholder = { Text(stringResource(R.string.sandbox_phonetic_hint)) },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sandbox_phonetic_input")
                )

                // Live 3-slot Suggestion Strip mirror
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        val slots = listOf(
                            stripState.left.orEmpty(),
                            stripState.middle.substringAfterLast(' ').ifEmpty { "" },
                            stripState.right.orEmpty()
                        )
                        slots.forEachIndexed { idx, slotText ->
                            Text(
                                text = slotText.ifEmpty { "—" },
                                fontFamily = NotoSansBengaliFontFamily,
                                fontWeight = if (idx == 1) FontWeight.Bold else FontWeight.Normal,
                                color = if (idx == 1) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(horizontal = 6.dp)
                            )
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.sandbox_output_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sandbox_bangla_output")
                ) {
                    Text(
                        text = stripState.middle.ifEmpty { "—" },
                        fontFamily = NotoSansBengaliFontFamily,
                        fontSize = 22.sp,
                        lineHeight = 32.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            cb?.setPrimaryClip(ClipData.newPlainText("Ekush Bangla", stripState.middle))
                            scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                        },
                        enabled = stripState.middle.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag("sandbox_copy_button")
                    ) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(R.string.sandbox_copy_output), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    OutlinedButton(
                        onClick = { onLatinInputChange("") },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("sandbox_clear_button")
                    ) {
                        Icon(Icons.Rounded.Clear, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(stringResource(R.string.sandbox_clear))
                    }
                }

                Text(
                    text = stringResource(R.string.sandbox_sample_chips_title),
                    style = MaterialTheme.typography.labelLarge
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    samples.forEach { sample ->
                        AssistChip(
                            onClick = { onLatinInputChange(sample) },
                            label = { Text(sample) },
                            modifier = Modifier.testTag("sandbox_sample_chip_$sample")
                        )
                    }
                }
            }
        }

        // Real system IME input + OTP guard field
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = systemImeText,
                    onValueChange = { systemImeText = it },
                    label = { Text(stringResource(R.string.sandbox_system_field_label)) },
                    placeholder = { Text(stringResource(R.string.sandbox_system_field_hint)) },
                    minLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sandbox_system_ime_field")
                )

                OutlinedTextField(
                    value = otpText,
                    onValueChange = { otpText = it },
                    label = { Text(stringResource(R.string.sandbox_otp_field_label)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("sandbox_otp_field")
                )
            }
        }
    }
}
