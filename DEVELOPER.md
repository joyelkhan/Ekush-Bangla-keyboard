# Developer Guide — NaserX Labs

- **Brand:** NaserX Labs
- **Lead Developer / Owner:** Abu Naser Khan
- **Project:** Ekush (একুশ) Bangla Keyboard
- **Application ID:** `com.ekush.keyboard`
- **Gradle Namespace:** `com.example` (kept aligned with source directory layout)
- **License:** GNU General Public License v3.0 (`GPL-3.0`)

## Technology Stack
- **Language:** Kotlin (JVM 11 bytecode target)
- **Android SDK:** `minSdk = 26`, `compileSdk = 36`, `targetSdk = 36`
- **IME Surface:** Android `InputMethodService` + custom `View`/`Canvas` (`EkushKeyboardView`), `PopupWindow`, `AccessibilityNodeProvider`, `HapticFeedbackConstants`
- **Companion UI:** Jetpack Compose Material 3, Navigation Compose, Lifecycle `collectAsStateWithLifecycle`, adaptive `NavigationBar` / `NavigationRail` via `BoxWithConstraints`
- **Local Persistence:** Room 2.7 (KSP) + Jetpack DataStore Preferences + Storage Access Framework JSON import/export

## Coding & Release Standards
1. **Zero Network Footprint:** Never add `INTERNET`, `ACCESS_NETWORK_STATE`, analytics, crash-uploaders, or network SDKs.
2. **Engine Isolation:** Keep `:engine` free of `android.*` imports so phonetic and suggestion algorithms can be tested in ~3 seconds on the JVM.
3. **Bilingual Resources:** Add all user-visible copy to both `app/src/main/res/values/strings.xml` and `app/src/main/res/values-bn/strings.xml`.
4. **Wordlists:** Starter frequencies in `assets/dict/bn_words.tsv` and `en_words.tsv` are tagged `# UNVERIFIED` until regenerated from an audited corpus build pipeline.
5. **Release Signing:** Configure `KEYSTORE_PATH`, `STORE_PASSWORD`, and `KEY_PASSWORD` via environment variables at release build time; never hardcode or commit keystore secrets.
