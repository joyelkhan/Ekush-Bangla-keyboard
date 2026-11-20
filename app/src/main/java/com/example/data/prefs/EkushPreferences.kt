/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.example.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.ekushDataStore: DataStore<Preferences> by preferencesDataStore(name = "ekush_settings")

enum class TypingMode(val wireName: String) {
    PHONETIC("phonetic"),
    FIXED_BANGLA("fixed_bangla"),
    ENGLISH_QWERTY("english_qwerty");

    companion object {
        fun fromWire(value: String?): TypingMode =
            entries.firstOrNull { it.wireName == value } ?: PHONETIC
    }
}

enum class EkushThemeId(val wireName: String) {
    EKUSH("ekush"),
    NIGHT("night"),
    PAPER("paper"),
    BOISHAKH("boishakh"),
    RIVER("river"),
    MATERIAL_YOU("material_you");

    companion object {
        fun fromWire(value: String?): EkushThemeId =
            entries.firstOrNull { it.wireName == value } ?: EKUSH
    }
}

enum class DarkModeOverride(val wireName: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromWire(value: String?): DarkModeOverride =
            entries.firstOrNull { it.wireName == value } ?: SYSTEM
    }
}

enum class HapticIntensity(val wireName: String, val durationMs: Long, val amplitude: Int) {
    GENTLE("gentle", 10L, 70),
    MEDIUM("medium", 16L, 140),
    STRONG("strong", 24L, 220);

    companion object {
        fun fromWire(value: String?): HapticIntensity =
            entries.firstOrNull { it.wireName == value } ?: GENTLE
    }
}

data class EkushSettings(
    val defaultMode: TypingMode = TypingMode.PHONETIC,
    val themeId: EkushThemeId = EkushThemeId.EKUSH,
    val darkModeOverride: DarkModeOverride = DarkModeOverride.SYSTEM,
    val hapticsEnabled: Boolean = true,
    val hapticIntensity: HapticIntensity = HapticIntensity.GENTLE,
    val soundEnabled: Boolean = false,
    val keyHeightDp: Int = 54,
    val gapScale: Float = 1.0f,
    val longPressDelayMs: Int = 280,
    val preferBanglaDigits: Boolean = true,
    val autoConjunctsEnabled: Boolean = true,
    val suggestionsEnabled: Boolean = true,
    val autocorrectEnabled: Boolean = true,
    val commitOnEveryKeystroke: Boolean = false,
    val perAppModeMemory: Boolean = true,
    val clipboardHistoryEnabled: Boolean = true,
    val onboardingCompleted: Boolean = false
)

class EkushPreferencesRepository(private val context: Context) {
    private object Keys {
        val DEFAULT_MODE = stringPreferencesKey("default_mode")
        val THEME_ID = stringPreferencesKey("theme_id")
        val DARK_MODE = stringPreferencesKey("dark_mode_override")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val HAPTIC_INTENSITY = stringPreferencesKey("haptic_intensity")
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val KEY_HEIGHT_DP = intPreferencesKey("key_height_dp")
        val GAP_SCALE = floatPreferencesKey("gap_scale")
        val LONG_PRESS_DELAY_MS = intPreferencesKey("long_press_delay_ms")
        val PREFER_BANGLA_DIGITS = booleanPreferencesKey("prefer_bangla_digits")
        val AUTO_CONJUNCTS = booleanPreferencesKey("auto_conjuncts")
        val SUGGESTIONS_ENABLED = booleanPreferencesKey("suggestions_enabled")
        val AUTOCORRECT_ENABLED = booleanPreferencesKey("autocorrect_enabled")
        val COMMIT_EVERY_KEY = booleanPreferencesKey("commit_every_keystroke")
        val PER_APP_MODE_MEMORY = booleanPreferencesKey("per_app_mode_memory")
        val CLIPBOARD_HISTORY = booleanPreferencesKey("clipboard_history")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
    }

    val settingsFlow: Flow<EkushSettings> = context.ekushDataStore.data.map { prefs ->
        EkushSettings(
            defaultMode = TypingMode.fromWire(prefs[Keys.DEFAULT_MODE]),
            themeId = EkushThemeId.fromWire(prefs[Keys.THEME_ID]),
            darkModeOverride = DarkModeOverride.fromWire(prefs[Keys.DARK_MODE]),
            hapticsEnabled = prefs[Keys.HAPTICS_ENABLED] ?: true,
            hapticIntensity = HapticIntensity.fromWire(prefs[Keys.HAPTIC_INTENSITY]),
            soundEnabled = prefs[Keys.SOUND_ENABLED] ?: false,
            keyHeightDp = (prefs[Keys.KEY_HEIGHT_DP] ?: 54).coerceIn(48, 72),
            gapScale = (prefs[Keys.GAP_SCALE] ?: 1.0f).coerceIn(0.6f, 1.4f),
            longPressDelayMs = (prefs[Keys.LONG_PRESS_DELAY_MS] ?: 280).coerceIn(150, 500),
            preferBanglaDigits = prefs[Keys.PREFER_BANGLA_DIGITS] ?: true,
            autoConjunctsEnabled = prefs[Keys.AUTO_CONJUNCTS] ?: true,
            suggestionsEnabled = prefs[Keys.SUGGESTIONS_ENABLED] ?: true,
            autocorrectEnabled = prefs[Keys.AUTOCORRECT_ENABLED] ?: true,
            commitOnEveryKeystroke = prefs[Keys.COMMIT_EVERY_KEY] ?: false,
            perAppModeMemory = prefs[Keys.PER_APP_MODE_MEMORY] ?: true,
            clipboardHistoryEnabled = prefs[Keys.CLIPBOARD_HISTORY] ?: true,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false
        )
    }

    suspend fun currentSettings(): EkushSettings = settingsFlow.first()

    suspend fun setDefaultMode(mode: TypingMode) = edit { it[Keys.DEFAULT_MODE] = mode.wireName }
    suspend fun setThemeId(theme: EkushThemeId) = edit { it[Keys.THEME_ID] = theme.wireName }
    suspend fun setDarkModeOverride(override: DarkModeOverride) = edit { it[Keys.DARK_MODE] = override.wireName }
    suspend fun setHapticsEnabled(enabled: Boolean) = edit { it[Keys.HAPTICS_ENABLED] = enabled }
    suspend fun setHapticIntensity(intensity: HapticIntensity) = edit { it[Keys.HAPTIC_INTENSITY] = intensity.wireName }
    suspend fun setSoundEnabled(enabled: Boolean) = edit { it[Keys.SOUND_ENABLED] = enabled }
    suspend fun setKeyHeightDp(dp: Int) = edit { it[Keys.KEY_HEIGHT_DP] = dp.coerceIn(48, 72) }
    suspend fun setGapScale(scale: Float) = edit { it[Keys.GAP_SCALE] = scale.coerceIn(0.6f, 1.4f) }
    suspend fun setLongPressDelayMs(ms: Int) = edit { it[Keys.LONG_PRESS_DELAY_MS] = ms.coerceIn(150, 500) }
    suspend fun setPreferBanglaDigits(prefer: Boolean) = edit { it[Keys.PREFER_BANGLA_DIGITS] = prefer }
    suspend fun setAutoConjunctsEnabled(enabled: Boolean) = edit { it[Keys.AUTO_CONJUNCTS] = enabled }
    suspend fun setSuggestionsEnabled(enabled: Boolean) = edit { it[Keys.SUGGESTIONS_ENABLED] = enabled }
    suspend fun setAutocorrectEnabled(enabled: Boolean) = edit { it[Keys.AUTOCORRECT_ENABLED] = enabled }
    suspend fun setCommitOnEveryKeystroke(enabled: Boolean) = edit { it[Keys.COMMIT_EVERY_KEY] = enabled }
    suspend fun setPerAppModeMemory(enabled: Boolean) = edit { it[Keys.PER_APP_MODE_MEMORY] = enabled }
    suspend fun setClipboardHistoryEnabled(enabled: Boolean) = edit { it[Keys.CLIPBOARD_HISTORY] = enabled }
    suspend fun setOnboardingCompleted(completed: Boolean) = edit { it[Keys.ONBOARDING_COMPLETED] = completed }

    suspend fun exportToJson(): JSONObject {
        val s = currentSettings()
        return JSONObject().apply {
            put("defaultMode", s.defaultMode.wireName)
            put("themeId", s.themeId.wireName)
            put("darkModeOverride", s.darkModeOverride.wireName)
            put("hapticsEnabled", s.hapticsEnabled)
            put("hapticIntensity", s.hapticIntensity.wireName)
            put("soundEnabled", s.soundEnabled)
            put("keyHeightDp", s.keyHeightDp)
            put("gapScale", s.gapScale.toDouble())
            put("longPressDelayMs", s.longPressDelayMs)
            put("preferBanglaDigits", s.preferBanglaDigits)
            put("autoConjunctsEnabled", s.autoConjunctsEnabled)
            put("suggestionsEnabled", s.suggestionsEnabled)
            put("autocorrectEnabled", s.autocorrectEnabled)
            put("commitOnEveryKeystroke", s.commitOnEveryKeystroke)
            put("perAppModeMemory", s.perAppModeMemory)
            put("clipboardHistoryEnabled", s.clipboardHistoryEnabled)
        }
    }

    suspend fun importFromJson(obj: JSONObject) {
        edit { prefs ->
            if (obj.has("defaultMode")) prefs[Keys.DEFAULT_MODE] = TypingMode.fromWire(obj.optString("defaultMode")).wireName
            if (obj.has("themeId")) prefs[Keys.THEME_ID] = EkushThemeId.fromWire(obj.optString("themeId")).wireName
            if (obj.has("darkModeOverride")) prefs[Keys.DARK_MODE] = DarkModeOverride.fromWire(obj.optString("darkModeOverride")).wireName
            if (obj.has("hapticsEnabled")) prefs[Keys.HAPTICS_ENABLED] = obj.optBoolean("hapticsEnabled", true)
            if (obj.has("hapticIntensity")) prefs[Keys.HAPTIC_INTENSITY] = HapticIntensity.fromWire(obj.optString("hapticIntensity")).wireName
            if (obj.has("soundEnabled")) prefs[Keys.SOUND_ENABLED] = obj.optBoolean("soundEnabled", false)
            if (obj.has("keyHeightDp")) prefs[Keys.KEY_HEIGHT_DP] = obj.optInt("keyHeightDp", 54).coerceIn(48, 72)
            if (obj.has("gapScale")) prefs[Keys.GAP_SCALE] = obj.optDouble("gapScale", 1.0).toFloat().coerceIn(0.6f, 1.4f)
            if (obj.has("longPressDelayMs")) prefs[Keys.LONG_PRESS_DELAY_MS] = obj.optInt("longPressDelayMs", 280).coerceIn(150, 500)
            if (obj.has("preferBanglaDigits")) prefs[Keys.PREFER_BANGLA_DIGITS] = obj.optBoolean("preferBanglaDigits", true)
            if (obj.has("autoConjunctsEnabled")) prefs[Keys.AUTO_CONJUNCTS] = obj.optBoolean("autoConjunctsEnabled", true)
            if (obj.has("suggestionsEnabled")) prefs[Keys.SUGGESTIONS_ENABLED] = obj.optBoolean("suggestionsEnabled", true)
            if (obj.has("autocorrectEnabled")) prefs[Keys.AUTOCORRECT_ENABLED] = obj.optBoolean("autocorrectEnabled", true)
            if (obj.has("commitOnEveryKeystroke")) prefs[Keys.COMMIT_EVERY_KEY] = obj.optBoolean("commitOnEveryKeystroke", false)
            if (obj.has("perAppModeMemory")) prefs[Keys.PER_APP_MODE_MEMORY] = obj.optBoolean("perAppModeMemory", true)
            if (obj.has("clipboardHistoryEnabled")) prefs[Keys.CLIPBOARD_HISTORY] = obj.optBoolean("clipboardHistoryEnabled", true)
        }
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.ekushDataStore.edit { block(it) }
    }
}
