/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.prefs.DarkModeOverride
import com.example.data.prefs.EkushSettings
import com.example.data.prefs.EkushThemeId
import com.example.theme.EkushPalettes
import com.example.theme.KeyboardPalette
import com.example.theme.ThemeContrastValidator
import com.example.ui.theme.NotoSansBengaliFontFamily

private data class ThemeCardMeta(
    val id: EkushThemeId,
    val titleRes: Int,
    val descRes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemesScreen(
    settings: EkushSettings,
    onSelectTheme: (EkushThemeId) -> Unit,
    onSelectDarkModeOverride: (DarkModeOverride) -> Unit
) {
    val context = LocalContext.current
    val systemDark = isSystemInDarkTheme()
    val previewDark = when (settings.darkModeOverride) {
        DarkModeOverride.SYSTEM -> systemDark
        DarkModeOverride.LIGHT -> false
        DarkModeOverride.DARK -> true
    }

    val activePalette = EkushPalettes.resolve(settings.themeId, previewDark, context)
    val contrastReport = ThemeContrastValidator.validate(activePalette)

    val themes = listOf(
        ThemeCardMeta(EkushThemeId.EKUSH, R.string.theme_ekush, R.string.theme_ekush_desc),
        ThemeCardMeta(EkushThemeId.NIGHT, R.string.theme_night, R.string.theme_night_desc),
        ThemeCardMeta(EkushThemeId.PAPER, R.string.theme_paper, R.string.theme_paper_desc),
        ThemeCardMeta(EkushThemeId.BOISHAKH, R.string.theme_boishakh, R.string.theme_boishakh_desc),
        ThemeCardMeta(EkushThemeId.RIVER, R.string.theme_river, R.string.theme_river_desc),
        ThemeCardMeta(EkushThemeId.MATERIAL_YOU, R.string.theme_material_you, R.string.theme_material_you_desc)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.themes_title),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = stringResource(R.string.themes_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Appearance override selector
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = stringResource(R.string.theme_appearance_mode),
                style = MaterialTheme.typography.titleMedium
            )
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val modes = listOf(
                    DarkModeOverride.SYSTEM to R.string.theme_appearance_system,
                    DarkModeOverride.LIGHT to R.string.theme_appearance_light,
                    DarkModeOverride.DARK to R.string.theme_appearance_dark
                )
                modes.forEachIndexed { index, (mode, labelRes) ->
                    SegmentedButton(
                        selected = settings.darkModeOverride == mode,
                        onClick = { onSelectDarkModeOverride(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("theme_mode_${mode.wireName}")
                    ) {
                        Text(stringResource(labelRes))
                    }
                }
            }
        }

        // Live Keyboard Palette Preview & WCAG 2.1 Audit
        Text(
            text = stringResource(R.string.theme_preview_label),
            style = MaterialTheme.typography.titleMedium
        )
        MiniKeyboardPreview(palette = activePalette)

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("theme_wcag_card")
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.VerifiedUser,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.theme_wcag_pass),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(
                            R.string.theme_wcag_ratio_format,
                            contrastReport.suggestionStripContrast,
                            contrastReport.keyLabelContrast
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Preset theme cards
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            themes.forEach { meta ->
                val palette = EkushPalettes.resolve(meta.id, previewDark, context)
                val selected = settings.themeId == meta.id
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 64.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                            shape = MaterialTheme.shapes.medium
                        )
                        .clickable { onSelectTheme(meta.id) }
                        .testTag("theme_card_${meta.id.wireName}")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 3-dot swatch (background, accent, hasanta)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(palette.background))
                                    .border(1.dp, Color(palette.border), CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(palette.accent))
                            )
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(palette.hasantaKeySurface))
                                    .border(1.dp, Color(palette.border), CircleShape)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(meta.titleRes),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(meta.descRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (selected) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniKeyboardPreview(palette: KeyboardPalette) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = Color(palette.background),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(palette.border), MaterialTheme.shapes.medium)
    ) {
        Column {
            // Suggestion strip preview
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(palette.suggestionStripSurface))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    text = "বাংলা",
                    fontFamily = NotoSansBengaliFontFamily,
                    color = Color(palette.suggestionText),
                    fontSize = 14.sp
                )
                Text(
                    text = "বাংলাদেশ",
                    fontFamily = NotoSansBengaliFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = Color(palette.accent),
                    fontSize = 14.sp
                )
                Text(
                    text = "একুশ",
                    fontFamily = NotoSansBengaliFontFamily,
                    color = Color(palette.suggestionText),
                    fontSize = 14.sp
                )
            }

            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("ক", "খ", "গ", "ঘ", "চ", "ছ", "জ", "ত").forEach { glyph ->
                        MiniKeyCap(
                            label = glyph,
                            bg = Color(palette.keySurface),
                            fg = Color(palette.keyLabel),
                            border = Color(palette.border),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Bottom row with Hasanta, Space, Enter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    MiniKeyCap(
                        label = "১২৩",
                        bg = Color(palette.functionKeySurface),
                        fg = Color(palette.keyLabel),
                        border = Color(palette.border),
                        modifier = Modifier.weight(1.3f)
                    )
                    MiniKeyCap(
                        label = "্",
                        bg = Color(palette.hasantaKeySurface),
                        fg = Color(palette.keyLabel),
                        border = Color(palette.border),
                        modifier = Modifier.weight(1f)
                    )
                    MiniKeyCap(
                        label = "একুশ · ফনেটিক",
                        bg = Color(palette.keySurface),
                        fg = Color(palette.keyLabel),
                        border = Color(palette.border),
                        modifier = Modifier.weight(3.5f)
                    )
                    MiniKeyCap(
                        label = "⏎",
                        bg = Color(palette.accent),
                        fg = Color(palette.onAccent),
                        border = Color(palette.accent),
                        modifier = Modifier.weight(1.4f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniKeyCap(
    label: String,
    bg: Color,
    fg: Color,
    border: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = bg,
        modifier = modifier
            .height(38.dp)
            .border(1.dp, border, MaterialTheme.shapes.small)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontFamily = NotoSansBengaliFontFamily,
                color = fg,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}
