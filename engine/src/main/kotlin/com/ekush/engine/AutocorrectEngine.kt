/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.ekush.engine

import java.util.Collections

/**
 * Conservative autocorrect tuning loaded from `tuning/autocorrect.json` (Blueprint Part 03.5 & 04.6).
 */
data class AutocorrectConfig(
    val maxEditDistance: Int = 2,
    val allowTransposition: Boolean = true,
    val minCandidateFrequency: Int = 500,
    val minFrequencyRatio: Double = 5.0,
    val minTypoConfidence: Double = 0.45,
    val neverTouchQuotedText: Boolean = true,
    val neverTouchComposingRegion: Boolean = true
) {
    companion object {
        fun loadDefault(): AutocorrectConfig {
            val stream = AutocorrectConfig::class.java.classLoader
                ?.getResourceAsStream("tuning/autocorrect.json")
            if (stream != null) {
                val jsonText = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val obj = SimpleJsonParser(jsonText).parseObject()
                return AutocorrectConfig(
                    maxEditDistance = (obj["maxEditDistance"] as? Number)?.toInt() ?: 2,
                    allowTransposition = obj["allowTransposition"] as? Boolean ?: true,
                    minCandidateFrequency = (obj["minCandidateFrequency"] as? Number)?.toInt() ?: 500,
                    minFrequencyRatio = (obj["minFrequencyRatio"] as? Number)?.toDouble() ?: 5.0,
                    minTypoConfidence = (obj["minTypoConfidence"] as? Number)?.toDouble() ?: 0.45,
                    neverTouchQuotedText = obj["neverTouchQuotedText"] as? Boolean ?: true,
                    neverTouchComposingRegion = obj["neverTouchComposingRegion"] as? Boolean ?: true
                )
            }
            return AutocorrectConfig()
        }
    }
}

/**
 * Conservative Bengali-aware autocorrect engine (Blueprint Part 03.5 & Prompt 15):
 * - Automatic correction only on single-edit errors scored via TypoModel.
 * - Candidate frequency must exceed `minCandidateFrequency` AND be at least 5x the typed form's frequency.
 * - Never touches quoted text, all-caps abbreviations, or mixed-script tokens.
 * - Session learn-on-revert suppresses reverted pairs until reset.
 */
class AutocorrectEngine(
    private val dictionary: DictionaryTrie,
    private val config: AutocorrectConfig = AutocorrectConfig.loadDefault()
) {
    data class CorrectionResult(
        val original: String,
        val corrected: String,
        val confidence: Double = 1.0
    )

    private val suppressedSessionPairs: MutableSet<Pair<String, String>> =
        Collections.synchronizedSet(HashSet())

    fun recordRevert(original: String, corrected: String) {
        val origNfc = BanglaUnicode.normalizeNfc(original)
        val corrNfc = BanglaUnicode.normalizeNfc(corrected)
        if (origNfc.isNotEmpty() && corrNfc.isNotEmpty()) {
            suppressedSessionPairs += origNfc to corrNfc
        }
    }

    fun clearSessionReverts() {
        suppressedSessionPairs.clear()
    }

    fun maybeCorrectOnCommit(
        word: String,
        precedingText: CharSequence = ""
    ): CorrectionResult? {
        val nfc = BanglaUnicode.normalizeNfc(word)
        if (nfc.length < 2) return null

        // Skip all-caps English abbreviations and mixed-script tokens
        val hasAsciiUpperOnly = nfc.all { it in 'A'..'Z' }
        val hasBangla = nfc.any { BanglaUnicode.isBanglaChar(it) }
        val hasAsciiLetter = nfc.any { it in 'a'..'z' || it in 'A'..'Z' }
        if (hasAsciiUpperOnly || (hasBangla && hasAsciiLetter)) {
            return null
        }

        // Blueprint Part 03.5: never touch quoted text
        if (config.neverTouchQuotedText && isInsideQuotes(precedingText)) {
            return null
        }

        // Preserve legitimate inflected forms if stem exists in dictionary
        val stemKnown = BanglaInflection.stripSuffixes(nfc).any { dictionary.contains(it.stem) }
        if (stemKnown) return null

        val typedFreq = dictionary.frequencyOf(nfc)
        val candidates = dictionary.fuzzyLookup(nfc, maxDistance = 1, limit = 8)
        val best = candidates
            .asSequence()
            .filter { (entry, dist) ->
                dist == 1 &&
                    (nfc to entry.word) !in suppressedSessionPairs &&
                    entry.frequency >= config.minCandidateFrequency &&
                    entry.frequency >= typedFreq.coerceAtLeast(1) * config.minFrequencyRatio
            }
            .map { (entry, _) ->
                val conf = TypoModel.typoConfidence(nfc, entry.word)
                Triple(entry, conf, conf * kotlin.math.ln(entry.frequency.toDouble() + 1.0))
            }
            .filter { (_, conf, _) -> conf >= config.minTypoConfidence }
            .maxByOrNull { (_, _, rankScore) -> rankScore }
            ?: return null

        return CorrectionResult(
            original = nfc,
            corrected = best.first.word,
            confidence = best.second
        )
    }

    private fun isInsideQuotes(precedingText: CharSequence): Boolean {
        var doubleQuotes = 0
        var singleQuotes = 0
        for (i in 0 until precedingText.length) {
            when (precedingText[i]) {
                '"', '“', '”' -> doubleQuotes++
                '\'', '‘', '’' -> singleQuotes++
            }
        }
        return (doubleQuotes % 2 == 1) || (singleQuotes % 2 == 1)
    }
}
