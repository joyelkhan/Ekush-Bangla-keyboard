# Release Notes — Ekush 1.0.0

**Developer:** Abu Naser Khan (NaserX Labs)  
**Application ID:** `com.ekush.keyboard`  
**Minimum Android Version:** Android 8.0 (API 26)  
**Target Android Version:** Android 16 (API 36)

## Highlights
- Complete offline Bangla & English keyboard with zero `INTERNET` permission.
- Avro-style phonetic typing alongside dual-page fixed Bangla layouts and English QWERTY.
- Live 3-slot suggestion bar with inflection handling, local personal lexicon, and undoable conservative autocorrect.
- 6 accessible themes with live WCAG contrast verification.
- First-run 3-step setup guide with a 60-second interactive Bangla typing test.

## Known Limitations
- Starter dictionaries in `assets/dict/bn_words.tsv` and `en_words.tsv` carry a small baseline word set marked `UNVERIFIED` pending full external corpus pipeline packaging.
- Physical-device verification of IME window insets, TalkBack exploration, and gesture typing feel requires testing on an Android device or emulator beyond headless Robolectric/JVM tests.
- Release APK/AAB signing requires supplying an upload keystore (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) in the release CI environment.
