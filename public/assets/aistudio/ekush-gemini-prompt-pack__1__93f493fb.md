# Ekush x Google AI Studio — Prompt Pack

Companion to `bangla-keyboard-blueprint.pdf` (Ekush — Bengali keyboard, HeliBoard fork, GPL-3.0).
Goal: turn the blueprint into working code by driving Gemini in Google AI Studio, module by module.

---

## 0. Reality check (30 seconds)

| Google AI Studio CAN | Google AI Studio CANNOT |
|---|---|
| Read the whole 50-page PDF (~1M token context) | Compile an APK or run Gradle |
| Generate complete Kotlin / XML / YAML / Python files | Run unit tests or an Android emulator |
| Fix code from pasted compiler errors / logcat | See your local HeliBoard fork unless you paste files |
| Keep one long thread per module | Push to GitHub, sign APKs, check trademarks |

**Division of labor**

- AI Studio → green-field files: phonetic engine, tests, build scripts, CI, docs (the PDF is in context, so it designs to spec).
- Gemini inside Android Studio (or Gemini CLI in the repo folder) → edits inside the HeliBoard fork, where the real files must be seen.
- Android Studio → the only place that compiles, runs on a device, and packages the APK.

Skip AI Studio's **Build** mode: it generates web apps, not Android projects.

---

## 1. Setup (2 minutes)

1. Open `aistudio.google.com` → **Chat**.
2. Model: **Gemini 2.5 Pro** (or the newest Pro). Temperature **0.3** for code — never above ~0.5.
3. Click **+** → Upload file → `bangla-keyboard-blueprint.pdf`.
4. Paste the **System Instructions** below into the system-instructions field.
5. Keep **one chat per module** (the PDF stays in that chat's context). Name chats like `Ekush 01 engine`, `Ekush 02 dict`, `Ekush 03 layouts`.

### System instructions (copy-paste once per chat)

```text
You are my senior Android engineer on Project "Ekush" (একুশ), a Bengali keyboard app built as a fork of HeliBoard (GPL-3.0). The uploaded PDF is the product blueprint and the source of truth — when a decision comes from it, cite the section.

Hard rules:
1. Kotlin-first, Android platform style, no Compose unless the blueprint mandates it. Match minSdk/targetSdk from the blueprint.
2. Privacy is absolute: never generate code that requests android.permission.INTERNET or any network-capable API. Flag anything that even implies networking.
3. Output COMPLETE file contents: file path as a heading, then one fenced code block per file. Never "...", never "rest of file unchanged", no TODO placeholders.
4. No invented APIs. Use only APIs you are sure exist in the Android SDK / the HeliBoard fork. If unsure, say so and offer alternatives. Uncertain repo file paths: mark with // VERIFY instead of guessing.
5. Preserve GPL-3.0 headers from the fork; new files get a GPL-3.0 header with "Copyright (c) 2026 Ekush contributors".
6. User-visible strings ship in English AND Bengali (values-bn).
7. Comments in English, concise, only where they help a reviewer.
8. If a task is ambiguous, ask up to 3 clarifying questions BEFORE writing code.
9. Never invent dictionary words, statistics, or rule mappings you are not sure of — mark them UNVERIFIED.
```

---

## 2. The prompts (run in order)

Prompt 0 and Prompt 1 run once. Prompts 2–8 run per module. Prompts 9–10 are reusable templates.

### Prompt 0 — Smoke test (validate the upload, run once)

```text
I uploaded a 50-page blueprint for "Ekush", a Bengali keyboard app.
Prove you read it:
1. Summarize it in 15 bullets (name, stack, engine, privacy stance, roadmap).
2. List any internal contradictions or missing specs you found.
No code in this reply.
```

### Prompt 1 — Master architect (THE prompt to send after uploading)

```text
I uploaded the full product blueprint for "Ekush" — a Bengali (Bangla) keyboard for Android, built as a fork of HeliBoard (GPL-3.0).

Read the ENTIRE PDF, then act as project architect and output a BUILD PLAN, not code:

1. Module map — every module/package to create or modify in the fork, with intended file paths (mark // VERIFY where you are not sure of HeliBoard's actual paths).
2. Build order — numbered milestones matching the blueprint's Week 1–4 roadmap, each ending in something I can run or see on a real device.
3. Per milestone: (a) goal, (b) files to create/modify, (c) acceptance criteria I can verify myself, (d) the blueprint section it comes from.
4. Top 5 risks most likely to block me, each with a mitigation.
5. REPO NEEDS: the exact files from the actual HeliBoard fork you must see before you can safely write code (e.g. settings files, service classes, layout loader). I will paste them next.

Do not write implementation code in this reply.
```

### Prompt 2 — Fork & rebrand checklist

```text
Blueprint section: Branding & Licensing.
Produce a step-by-step rebrand checklist for the HeliBoard fork → Ekush:
applicationId com.ekush.keyboard, app_name (en + bn), launcher icons, about-screen credits to HeliBoard and OpenBangla (GPL-3.0 attribution rules), LICENSE/NOTICE handling, and what must NOT be removed to stay GPL-compliant.
Format: a checkbox list, each item with target file path (// VERIFY if unsure) and the exact change.
No code yet.
```

### Prompt 3 — Phonetic engine (the highest-value prompt)

```text
Blueprint section: Phonetic Engine.
Generate the Avro-style phonetic transliteration engine as PURE Kotlin (JVM only, zero Android imports, unit-testable without Robolectric).

Deliver complete files:
1. engine/PhoneticEngine.kt — public API:
   fun transliterate(input: String): TranslitResult
   TranslitResult = committed text + active-candidate list (for the suggestion strip).
2. engine/Rules.kt — the rule table as Kotlin data, structured so I can later export it to JSON:
   - consonants, independent vowels, diphthongs (oi→ৈ, ou→ৌ, ...)
   - multi-char conjunct rules (kh→খ, kkh→ক্ষ, Sh→ষ, N→ণ, ...)
   - capital-letter modifiers (R→ৃ vocalic r, and similar)
   - context rules: hasanta ্ joining for conjuncts, vowel reshaping after a consonant, word-final behavior
   Algorithm: longest-match-first, left-to-right, per word. Deterministic, no regex.
3. engine/Tokenizer.kt — splits input into transliteration runs vs passthrough (English words, digits, punctuation).
4. EngineTest.kt — JUnit tests including AT LEAST these seed vectors, which you must treat as authoritative:
   ekush→একুশ, ami→আমি, amar→আমার, bangla→বাংলা, sonar→সোনার, gan→গান, gai→গাই, boishakh→বৈশাখ, kRiShNo→কৃষ্ণ
   plus 20 more derived from your own rule table.
5. ENGINE.md — the algorithm in ≤10 bullets + a table of every rule you marked UNVERIFIED.

Mark every rule you are not certain about with UNVERIFIED — I will cross-check against OpenBangla Keyboard's public GPL rule files and fix the table.
```

### Prompt 4 — Dictionary & suggestions

```text
Blueprint section: Dictionary.
1. scripts/build_dict.py (Python 3, stdlib only): reads corpus/word_freq.tsv (word<TAB>frequency), keeps the top 50,000, and writes:
   (a) assets/dict/ekush_words.json — sorted by frequency,
   (b) assets/dict/suffix_model.json — Bengali inflection suffixes + weights per the blueprint's morphology notes.
2. engine/WordSuggestor.kt — loads both JSON files lazily on a background thread; API: fun suggest(engineOutput: TranslitResult): List<Suggestion>; ranking = corpus frequency × similarity to the engine's top candidates; must rank in <5 ms per keystroke on a mid-range phone (no allocations in the hot loop beyond the result list).
3. WordSuggestorTest.kt — 10 tests including: vowel-sign composition, ZWJ (U+200D) and ZWNJ (U+200C) edge cases, empty-corpus fallback.
Rule: the script must never invent words — it only passes through real corpus rows.
```

### Prompt 5 — Layouts & themes (run after pasting fork files)

```text
Blueprint sections: Layouts, Themes.
I am pasting below: (a) one existing keyboard layout file from my HeliBoard fork, (b) the code that loads it. Match that exact format — do not guess or invent a format.
Task:
1. Full Basic Bangla layout: basic layer + shift layer, exactly per the blueprint's layout mock (rows, keys, hasanta key, ZWJ handling).
2. Numeric and symbol layers per the blueprint.
3. The 6 themes in the fork's theme format, using the flag palette: green #006A4E, red #F42A41, off-white #FAF7F0 (dark variants included).

[PASTE THE FORK FILES HERE]
```

### Prompt 6 — IME wiring (plan first, code later)

```text
Blueprint section: Architecture.
In my fork the IME service already exists (inherited from HeliBoard). Plan the MINIMAL wiring to add Ekush's Bangla mode: layout switching Latin↔Bangla, settings entries, and where the phonetic engine plugs in.
This reply = PLAN ONLY: bullet steps + the exact fork files you need me to paste. Wait for my files before writing any code.
```

### Prompt 7 — Privacy CI guard

```text
Blueprint section: Privacy.
Generate complete files:
1. .github/workflows/privacy-guard.yml — fails CI if:
   (a) any AndroidManifest.xml in the repo declares android.permission.INTERNET or any permission outside allowlist.txt,
   (b) any build.gradle(.kts) adds a dependency outside allowlist.txt,
   (c) ./gradlew lint testDebugUnitTest fails.
2. allowlist.txt — permissive defaults, documented.
3. A comment block in the workflow explaining WHY (zero-network keyboard, blueprint Privacy section).
```

### Prompt 8 — Engine test expansion

```text
Expand the phonetic engine's tests:
1. Fuzz: 1,000 random ASCII strings → the engine must never throw, and output must contain only Bengali-block or ASCII characters.
2. Round-trip sanity on 30 vectors derived from Rules.kt.
3. A simple JVM benchmark (nanotime, chars/sec, no external libraries).
Output: complete test files only, no changes to production code.
```

### Prompt 9 — Bugfix template (reusable)

```text
BUILD FIX. Minimal change only — do not refactor.
Build output (full):
<PASTE GRADLE ERROR>

File that failed (whole file):
<PASTE FILE>

Reply: root cause in ≤5 bullets, then the complete corrected file.
```

### Prompt 10 — Fresh-chat handoff (reusable, never lose state)

```text
CONTEXT HANDOFF — Project Ekush (Bengali keyboard, HeliBoard fork, GPL-3.0). The blueprint PDF is re-uploaded.
Where we are: <2–3 sentences on the module state>
Current file we are working on (complete):
<PASTE FILE>
Task now: <one sentence>
My system-prompt rules apply. Output the complete updated file.
```

---

## 3. Discipline that keeps Gemini useful

| Do | Don't |
|---|---|
| One module per chat; name chats by module | Ask for "the whole app" or "an APK" in one prompt |
| Paste real Gradle errors and whole files | Let it guess repo paths — demand `// VERIFY` marks |
| Compile + run after every prompt, before the next | Trust rule mappings you have not cross-checked |
| Temperature 0.2–0.4; start a fresh chat when quality drops | Paste closed-source code — Avro Keyboard's engine is closed; use OpenBangla's GPL rule tables instead |
| Git-commit every AI-generated file before testing it | Let it invent dictionary words or statistics |

## 4. The daily loop (where AI Studio ends)

```text
Google AI Studio (blueprint PDF in context)
   │  generates green-field files: engine, tests, scripts, CI
   ▼
Android Studio ── paste files into the HeliBoard fork ── Build → Run on device
   │  Gradle errors / logcat
   └────────────── paste back into AI Studio (Prompt 9) ──────────┘

Repo-internal edits (renaming, fork layout XMLs): Gemini in Android Studio or Gemini CLI.
```

## 5. Rule-table verification (after Prompt 3)

The engine prompt ships seed vectors, but the full rule table must be cross-checked against public sources:

- OpenBangla Keyboard (GPL-3.0): github.com/openbangla/keyboard — phonetic rule files
- avro-python / avrophonetic ports — community-maintained rule sets

Copy any corrections into `Rules.kt`, keep the table exportable to JSON, and re-run the tests. Words the engine should master on day one: your name, your city, "ami banglay gan gai".

---

## 6. Phase 2 — Intelligence & release (added Nov 2026, after the v0.1 audit)

Baseline when this section was written: `:engine` 9/9 tests, `:app` 7/7 tests, `app-debug.apk` (19 MB) builds. Still open from the audit: starter dictionaries carry `# UNVERIFIED` tags, `namespace = "com.example"`, release signing not executed, physical-device/TalkBack E2E pending.

**Order of operations for a "super intelligent" suggestion engine: DATA → CONTEXT → ADAPTATION → TYPOS → (optional) NEURAL.** The biggest quality jump comes from verified corpus data, not from a fancy model. And it must all stay offline — the zero-permission audit result IS the product's marketing.

Maturity backlog (features a HeliBoard fork would have given you — schedule post-v1.1): number-row toggle, cursor-arrow control bar, clipboard manager with pinning, keyboard height/resize + one-handed mode, floating/split layout for tablets, glide typing (large effort, last).

### Prompt 11 — Release hardening (run before any Play upload)

```text
Context: PRODUCTION_AUDIT.md in this chat. Pre-launch hardening — no new features, complete files/patches only:
1. namespace "com.example" → com.ekush.keyboard across all modules (fix every R import); give me the exact grep/sed plan to verify zero remaining matches.
2. Release signing: GitHub Actions release job using secrets KEYSTORE_BASE64, STORE_PASSWORD, KEY_PASSWORD, KEY_ALIAS; runs assembleRelease + R8 minify; add proguard-rules.pro keep rules for Room entities, EkushImeService, and any reflection-based code.
3. APK size audit: list the 5 largest res/ and lib/ contributors and a reduction plan (vector-only icons, subset Inter to used glyphs, drop unused mipmap densities) — target < 15 MB download size.
4. Play Console readiness: Data Safety answers for a zero-permission IME ("no data collected, no data shared"), privacy policy URL requirements, content rating, target audience.
```

### Prompt 12 — Verified corpus pipeline (replaces every # UNVERIFIED tag)

```text
Blueprint section: Dictionary. The engine ships starter dictionaries tagged # UNVERIFIED. Build the verification pipeline, complete files:
1. scripts/fetch_corpus.py — runs in CI ONLY, never in the app: pulls Bengali sources (Leipzig Corpora Collection bn news 2016–2019, bnwiki Wikipedia dump, OSCAR bn subset) and emits ATTRIBUTION.md with each source's license (CC BY-SA / CC-BY terms — attribute correctly).
2. scripts/build_dict.py — normalize (NFC, unify ZWJ/ZWNJ variants), dedupe, drop single-char and non-Bengali tokens, keep top 50,000 lemmas with frequencies; output assets/dict/ekush_words.json + suffix_model.json.
3. scripts/verify_dict.py — cross-check the shipped starter dict against the corpus list; every entry becomes VERIFIED (with corpus frequency) or DROPPED; rewrites all # UNVERIFIED tags; emits a diff report.
4. CI job that FAILS if any dict asset still contains the string # UNVERIFIED.
Constraint: the app stays zero-network — fetching happens in CI only, results are committed as assets.
```

### Prompt 13 — Context ranking (bigram/trigram LM)

```text
Blueprint section: Suggestion Engine. Upgrade ranking from frequency-only to context-aware, inside :engine (pure Kotlin, no Android imports):
1. engine/ContextLM.kt — pruned bigram+trigram model loaded from assets/dict/lm.bin (compact custom binary: varint deltas, perfect-hash index, memory-mapped read); O(1) lookup; ranking must stay under 5 ms per keystroke on a 2019-class phone.
2. scripts/build_lm.py — compiles the verified corpus into the pruned binary (top ~150k bigrams, ~200k trigrams); reports final file size (target ≤ 4 MB) and held-out perplexity.
3. New score in SuggestionEngine: α·log(freq) + β·log P(word | left context) + γ·phonetic-similarity(typed roman, candidate roman); α/β/γ exposed in tuning/ranking.json.
4. ContextLMTest.kt — must include: "এই সময়"-type bigram boosts, punctuation boundary resets context, sentence-start fallback, OOV word handling, empty-context fallback to frequency-only.
5. Benchmark harness: keystrokes-to-final-word simulation over a held-out corpus slice; print before/after so we can prove the engine got smarter.
```

### Prompt 14 — Personalization wiring (finish what Room started)

```text
You already built Room-backed UserLexiconDao (insert/decay) and AppModeMemoryEntity. Wire them into ranking as complete-file patches:
1. Personal boost: score += δ·personalWeight(word), decaying with time since last use (reuse existing decay); a word typed twice is promoted into the trie with a LOCAL flag.
2. Per-app mode memory: if the user always picks Bangla Fixed in app X, auto-switch on focus change — but an explicit mode override by the user in that app wins for the session.
3. Extend the existing SAF export/import: merge-on-import (no duplicates, keep the higher frequency), never overwrite local-only entries.
4. Tests: promotion threshold, decay floor, merge conflict cases, mode-memory vs explicit override.
Constraint: all data stays in the app's private storage; no sync, no APIs that require new permissions.
```

### Prompt 15 — Bengali typo model (smarter, still conservative autocorrect)

```text
Blueprint section: Autocorrect. Replace generic edit distance with a Bengali-specific error model, complete files:
1. engine/TypoModel.kt — weighted confusion pairs derived from the layout itself (চ↔ছ, ত↔ট, ড↔ঢ, ব↔ভ, শ↔ষ↔স) plus structural slips: dropped/duplicated hasanta ্, vowel-sign (কার) order swap, conjunct split/merge.
2. AutocorrectEngine v2: still conservative — fire only when candidate frequency ≥ 5× the typed token and confidence ≥ threshold (tuning/autocorrect.json); keep the existing quote-skip rule and the OTP/numeric guard; never fire on all-caps roman or mixed-script tokens.
3. Undo chip stays; add learn-on-revert: a revert suppresses that correction pair for the rest of the session.
4. TypoModelTest.kt — 25 vectors: every confusion pair, hasanta slip, kar swap, quote skip, OTP skip, revert-learn.
```

### Prompt 16 — (Optional) On-device neural next-word spike — with kill criteria

```text
EXPERIMENT. Run only after Prompts 12–15 land and are measured.
1. Train a tiny next-word model (embedding → LSTM → softmax, or a 4-layer transformer; vocab 20k) on the verified corpus; export via LiteRT (int8 quantized); hard limits: ≤ 8 MB model, ≤ 15 ms per keystroke inference on a mid-range phone.
2. Keep the n-gram LM as the default path; the neural model runs behind a debug flag and an A/B harness comparing keystrokes-to-final-word against Prompt 13's baseline.
3. Kill criteria: if the model beats the trigram baseline by less than 10% keystroke savings, or blows the latency/size budget — close the spike, delete the artifacts, document the result in the CHANGELOG.
Constraint: fully offline inference; the zero-INTERNET merged-manifest audit must pass unchanged after merging this branch.
```
