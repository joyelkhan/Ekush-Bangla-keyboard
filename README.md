# Ekush (একুশ) — Bangla Keyboard for Android

**Developer / Owner:** Abu Naser Khan  
**Company / Brand:** NaserX Labs  
**License:** GPL-3.0  
**Package / Application ID:** `com.ekush.keyboard` (`minSdk = 26`, `targetSdk = 36`)

## Overview
Ekush (একুশ) is an offline-first, open-source Bangla and English keyboard for Android combining an Avro-style phonetic engine (`:engine`), dual-page fixed Bangla layouts, and a low-latency View/Canvas `InputMethodService` with a Material 3 Jetpack Compose companion app.

## Core Features
- **Avro-Style Phonetic Engine (`:engine`)**: Pure-Kotlin longest-match phonetic converter supporting standalone vowels, dependent kar signs (`U+09BE`–`U+09CC`), automatic consonant conjuncts via hasanta (`্`), explicit hasanta (`\`), reph (`r` before consonant), yaphala (`y`), and canonical overrides.
- **Dual-Page Fixed Bangla Layouts**: National/Fixed Page 1 and Page 2 vowel/conjunct/extended layouts with long-press conjunct popups plus English QWERTY and Bangla/Latin Symbols.
- **Smart Suggestion Strip & Conservative Autocorrect**: 3-slot candidate bar with Bangla inflection stripping (e.g., `বাংলাদেশের` -> `বাংলাদেশ + ের`), personal Room lexicon learning, and 5x frequency-gated autocorrect with one-tap undo chip.
- **Privacy & Context Guards**: Zero `INTERNET` permission, automatic Latin digit enforcement in numeric/OTP/password/URI/email fields, and per-app mode memory.
- **WCAG AA Themes & Bilingual UI**: 6 presets (`Ekush`, `Night · রাত`, `Paper · কাগজ`, `Boishakh · বৈশাখ`, `River · নদী`, `Material You`), Light/Dark/System override, locally bundled `Noto Sans Bengali`, `Noto Serif Bengali`, and `Inter` fonts, plus full English and Bengali (`values-bn`) localization.

## Module Architecture
- `:engine` — Pure-Kotlin JVM module (`PhoneticEngine`, `PhoneticRuleTables`, `BanglaUnicode`, `BanglaInflection`, `DictionaryTrie`, `AutocorrectEngine`, `SuggestionEngine`).
- `:app` — Android application containing `EkushImeService`, `EkushKeyboardView` (Canvas + `AccessibilityNodeProvider`), `SuggestionStripView`, `LongPressPopupWindow`, Room database, DataStore preferences, and Jetpack Compose companion UI (`OnboardingScreen`, `SandboxScreen`, `ThemesScreen`, `SettingsScreen`, `LexiconScreen`, `AboutScreen`).

## Build & Test
```bash
gradle :engine:test
gradle :app:testDebugUnitTest
gradle :app:assembleDebug
```
