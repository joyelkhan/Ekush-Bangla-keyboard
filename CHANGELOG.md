# Changelog

## [1.0.0] - 2026-11-20

### Added
- Pure-Kotlin `:engine` module with Avro-style phonetic transliteration, longest-match parser (`rules/bn.json`), automatic hasanta/conjunct handling, reph, yaphala, inherent-vowel syllable candidate generation, `ContextLM` bigram ranker (`dict/bn_bigrams.tsv` + `EKLM` binary loader + boundary reset), `TypoModel` Bengali layout/phonetic confusion scoring, `DictionaryTrie`, conservative `5x` autocorrect with session learn-on-revert (`tuning/autocorrect.json`), and weighted `SuggestionEngine` (`tuning/ranking.json`).
- View/Canvas-based `EkushImeService` with Phonetic, Fixed Page 1, Fixed Page 2, English QWERTY, and Symbols layouts, idle mode-badge tap cycling, 90 ms key ripple, pending hasanta highlight, cluster-aware backspace, long-press conjunct popup, and `AccessibilityNodeProvider` TalkBack nodes.
- Context-aware OTP / numeric / URI / email Latin-digit guard, session-overridable per-app mode memory, and Room-backed local lexicon with frequency decay, `>= 2` local trie promotion, and non-destructive merge-on-import SAF JSON backup/restore.
- Companion Material 3 Compose app: 3-screen onboarding flow, live phonetic & system IME test sandbox, 6 WCAG-checked themes (`Ekush`, `Night · রাত`, `Paper · কাগজ`, `Boishakh · বৈশাখ`, `River · নদী`, `Material You`), settings tree, Personal Lexicon manager, and About/Privacy/Attribution screens.
- Custom adaptive launcher icon (`অ` + Ekush Red sun-disc on Ink `#16241F`), locally bundled Noto Sans Bengali / Noto Serif Bengali / Inter static fonts, and full English & Bengali (`values-bn`) string localization.
- Privacy CI guard workflow (`.github/workflows/privacy-guard.yml`, `allowlist.txt`), R8 release hardening (`proguard-rules.pro`, `isMinifyEnabled = true`, `isShrinkResources = true`), release signing workflow (`.github/workflows/release.yml`), and offline Python corpus/LM build scripts (`scripts/build_dict.py`, `scripts/verify_dict.py`, `scripts/build_lm.py`).
