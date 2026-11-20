/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.example.ui

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.example.R
import com.example.ui.theme.EkushGreen
import com.example.ui.theme.EkushInk
import com.example.ui.theme.EkushPaper
import com.example.ui.theme.EkushRed
import com.example.ui.theme.NotoSansBengaliFontFamily
import com.example.ui.theme.NotoSerifBengaliFontFamily

@Composable
fun OnboardingScreen(
    imeStatus: ImeActivationStatus,
    onRefreshImeStatus: () -> Unit,
    onFinishOnboarding: () -> Unit
) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }

    LifecycleResumeEffect(Unit) {
        onRefreshImeStatus()
        onPauseOrDispose { }
    }

    BackHandler(enabled = step > 0) {
        step = (step - 1).coerceAtLeast(0)
    }

    Scaffold(
        containerColor = if (step == 0) EkushPaper else MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 640.dp)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Top progress + always-visible Skip button (Part 03.11)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (step > 0) {
                        IconButton(
                            onClick = { step -= 1 },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("onboarding_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.onboarding_skip)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.size(48.dp))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(3) { idx ->
                            Surface(
                                color = if (idx <= step) {
                                    if (step == 0) EkushGreen else MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                },
                                shape = MaterialTheme.shapes.small,
                                modifier = Modifier
                                    .size(width = if (idx == step) 28.dp else 10.dp, height = 6.dp)
                            ) {}
                        }
                    }

                    TextButton(
                        onClick = onFinishOnboarding,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("onboarding_skip_button")
                    ) {
                        Text(
                            text = stringResource(R.string.onboarding_skip),
                            color = if (step == 0) EkushInk else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedContent(
                    targetState = step,
                    modifier = Modifier.weight(1f),
                    label = "onboarding_step_transition"
                ) { currentStep ->
                    when (currentStep) {
                        0 -> GreetingStep(onStart = { step = 1 })
                        1 -> EnableImeStep(
                            context = context,
                            imeStatus = imeStatus,
                            onContinue = { step = 2 }
                        )
                        else -> SixtySecondTestStep(onFinish = onFinishOnboarding)
                    }
                }
            }
        }
    }
}

@Composable
private fun GreetingStep(onStart: () -> Unit) {
    val context = LocalContext.current
    val animScale = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
    }
    val dotProgress = remember { Animatable(if (animScale == 0f) 1f else 0f) }

    LaunchedEffect(animScale) {
        if (animScale > 0f) {
            dotProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
            )
        } else {
            dotProgress.snapTo(1f)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            // Signature Ekush "অ" mark with 400ms rising red sun-disc dot (Part 02.7 & 03.11)
            Box(
                modifier = Modifier.size(152.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = EkushInk,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val p = dotProgress.value
                            val radius = size.minDimension * 0.14f * (0.4f + 0.6f * p)
                            val yOffset = size.height * (0.34f - 0.10f * p)
                            drawCircle(
                                color = EkushRed,
                                radius = radius,
                                center = Offset(size.width * 0.72f, yOffset)
                            )
                        }
                        Text(
                            text = "অ",
                            fontFamily = NotoSerifBengaliFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 72.sp,
                            color = MaterialTheme.colorScheme.primary.takeIf { false }
                                ?: androidx.compose.ui.graphics.Color(0xFF3ECF9A)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = stringResource(R.string.onboarding_step1_title),
                style = MaterialTheme.typography.displaySmall,
                color = EkushInk,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "মাতৃভাষায়, গর্বে।",
                fontFamily = NotoSerifBengaliFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                lineHeight = 32.sp,
                color = EkushGreen,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.onboarding_step1_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = EkushInk.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AssistChip(
                onClick = {},
                enabled = false,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = EkushGreen
                    )
                },
                label = {
                    Text(
                        text = stringResource(R.string.onboarding_privacy_note),
                        color = EkushInk
                    )
                }
            )

            Button(
                onClick = onStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("onboarding_start_button")
            ) {
                Text(text = stringResource(R.string.onboarding_start))
                Spacer(modifier = Modifier.size(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun EnableImeStep(
    context: Context,
    imeStatus: ImeActivationStatus,
    onContinue: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.onboarding_step2_title),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )

            SetupStepCard(
                title = stringResource(R.string.onboarding_step2_card1_title),
                description = stringResource(R.string.onboarding_step2_card1_desc),
                actionLabel = stringResource(R.string.onboarding_step2_card1_action),
                isDone = imeStatus.isEnabled,
                icon = Icons.Rounded.Settings,
                buttonTag = "onboarding_enable_ime_button",
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_INPUT_METHOD_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
            )

            SetupStepCard(
                title = stringResource(R.string.onboarding_step2_card2_title),
                description = stringResource(R.string.onboarding_step2_card2_desc),
                actionLabel = stringResource(R.string.onboarding_step2_card2_action),
                isDone = imeStatus.isDefault,
                icon = Icons.Rounded.Keyboard,
                buttonTag = "onboarding_picker_button",
                onClick = {
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
                    imm?.showInputMethodPicker()
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("onboarding_next_button")
            ) {
                Text(text = stringResource(R.string.onboarding_next))
                Spacer(modifier = Modifier.size(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun SetupStepCard(
    title: String,
    description: String,
    actionLabel: String,
    isDone: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    buttonTag: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isDone) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                    Text(
                        text = stringResource(if (isDone) R.string.onboarding_status_done else R.string.onboarding_status_pending),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            OutlinedButton(
                onClick = onClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .testTag(buttonTag)
            ) {
                Text(text = actionLabel)
            }
        }
    }
}

@Composable
private fun SixtySecondTestStep(onFinish: () -> Unit) {
    val targetSentence = stringResource(R.string.onboarding_step3_target)
    var typedText by rememberSaveable { mutableStateOf("") }
    val isMatched = typedText.trim().contains(targetSentence)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(R.string.onboarding_step3_title),
                style = MaterialTheme.typography.headlineMedium
            )

            Text(
                text = stringResource(R.string.onboarding_step3_prompt),
                style = MaterialTheme.typography.bodyLarge
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = targetSentence,
                        fontFamily = NotoSansBengaliFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        lineHeight = 28.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.onboarding_step3_hint),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedTextField(
                value = typedText,
                onValueChange = { typedText = it },
                label = { Text(stringResource(R.string.onboarding_step3_input_label)) },
                minLines = 3,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("onboarding_test_input")
            )

            if (isMatched) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_success_card")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.onboarding_step3_success),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_ekush_mark_outlined),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Button(
                onClick = onFinish,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .testTag("onboarding_finish_button")
            ) {
                Text(text = stringResource(R.string.onboarding_finish))
            }
        }
    }
}
