# Production Audit — Ekush (একুশ) Bangla Keyboard

**Company / Brand:** NaserX Labs  
**Developer / Owner:** Abu Naser Khan  
**Application ID:** `com.ekush.keyboard` (`namespace = "com.example"` retained per AI Studio Android build structure)  
**Version:** `1.0.0` (`versionCode = 1`)  
**SDK Targets:** `minSdk = 26`, `compileSdk = 36`, `targetSdk = 36`

---

## 1. Architecture & Engine Audit
- **Module Separation (`:engine` vs `:app`):**
  - `:engine` is a pure-Kotlin JVM library with zero `android.*` dependencies: `PhoneticEngine`, `PhoneticRuleTables` (`rules/bn.json`), `BanglaUnicode`, `BanglaInflection`, `DictionaryTrie`, `ContextLM` (`dict/bn_bigrams.tsv` + `EKLM` binary support), `TypoModel`, `AutocorrectEngine` (v2 with session learn-on-revert), and `SuggestionEngine` (`tuning/ranking.json`).
  - `:app` isolates the low-latency View/Canvas `InputMethodService` (`com.example.ime.*`) from DataStore/Room (`com.example.data.*`), WCAG theme validation (`com.example.theme.*`), and the Material 3 Compose companion app (`com.example.ui.*`).
- **Local Personalization:**
  - `LexiconRepository` promotes local words used `>= 2` times into the active trie, applies 30-day frequency decay, and merges imported JSON backups non-destructively (retaining maximum frequency and preserving local-only entries).
  - `EkushImeService` honors session mode overrides over remembered defaults, supports mode cycling via the idle mode badge, extracts preceding context words for `ContextLM`, and suppresses reverted autocorrect pairs for the active session.

## 2. UI/UX & Accessibility Assessment
- **Typography:** Full Bengali+Latin static `.ttf` fonts (`Noto Sans Bengali`, `Noto Serif Bengali`, `Inter`) are bundled in `app/src/main/res/font/`.
- **Theme Contrast:** All 10 static light/dark variants (`Ekush`, `Night`, `Paper`, `Boishakh`, `River`) pass WCAG 2.1 checks (`>= 4.5:1` strip text, `>= 3.0:1` keycap labels).
- **Accessibility & Responsiveness:**
  - `EkushKeyboardView` exposes TalkBack nodes via `android.view.accessibility.AccessibilityNodeProvider`.
  - Compose navigation adapts via `BoxWithConstraints` between `NavigationBar` (`< 600 dp`) and `NavigationRail` (`>= 600 dp`) with `>= 48.dp` touch targets.

## 3. Security, Privacy & Release Hardening Review
- **Zero-Permission Verified:** Merged debug & release manifests declare zero `INTERNET`, `ACCESS_NETWORK_STATE`, storage, location, camera, or microphone permissions. `EkushImeService` requires `android.permission.BIND_INPUT_METHOD`.
- **CI Privacy Guard:** `.github/workflows/privacy-guard.yml` + `allowlist.txt` fail CI if any manifest requests network/unlisted permissions or Gradle adds network dependencies.
- **Release Hardening:** `release` buildType enables `isMinifyEnabled = true` and `isShrinkResources = true` backed by explicit `app/proguard-rules.pro` rules for `EkushImeService`, Room entities/DAOs, ViewModels, and `:engine` classes. Verified via `:app:minifyReleaseWithR8`.

## 4. Build & Test Verification Results
- `compile_applet`: **PASSED**
- `gradle :engine:test`: **PASSED** — 16 tests, 0 failures (`PhoneticEngineCorpusTest` [9], `ContextLMAndTypoTest` [7] including 1,000-string ASCII fuzz & seed vectors).
- `gradle :app:testDebugUnitTest`: **PASSED** — 7 tests, 0 failures (`EkushPrivacyAndImeRobolectricTest` [4] including manifest zero-network & merge-on-import, `ThemeContrastValidatorTest` [1], `ExampleRobolectricTest` [1], `ExampleUnitTest` [1]).
- `gradle :app:minifyReleaseWithR8`: **PASSED** (release bytecode & resource shrinking verified cleanly).
- `gradle :app:assembleDebug`: **PASSED** — `app/build/outputs/apk/debug/app-debug.apk` (19 MB).
- `gradle :app:assembleRelease` / `bundleRelease`: **NOT EXECUTED** (upload keystore `KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD` supplied externally via `.github/workflows/release.yml` or CI secrets).
- Physical Device / Emulator UI & TalkBack Exploration: **REQUIRES PHYSICAL DEVICE TESTING**.

## 5. Remaining External Tasks
1. Feed audited Bengali corpora into `scripts/build_dict.py`, `scripts/verify_dict.py`, and `scripts/build_lm.py` to replace starter `# UNVERIFIED` dictionary/bigram counts (`bn_words.tsv`, `en_words.tsv`, `bn_bigrams.tsv`).
2. Supply production upload keystore credentials (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) in CI or local release environment to sign `app-release.apk` / `app-release.aab`.
3. Validate IME window insets, TalkBack exploration, and keyboard feel on physical Android devices (API 26–36).
