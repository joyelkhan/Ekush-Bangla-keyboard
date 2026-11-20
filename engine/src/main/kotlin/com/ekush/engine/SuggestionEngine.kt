/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.ekush.engine

import kotlin.math.ln

data class RankingConfig(
    val alphaFrequencyWeight: Double = 1.0,
    val betaContextWeight: Double = 2.4,
    val gammaPhoneticWeight: Double = 1.8,
    val deltaPersonalWeight: Double = 2.2,
    val personalPromotionThreshold: Int = 2
) {
    companion object {
        fun loadDefault(): RankingConfig {
            val stream = RankingConfig::class.java.classLoader
                ?.getResourceAsStream("tuning/ranking.json")
            if (stream != null) {
                val jsonText = stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val obj = SimpleJsonParser(jsonText).parseObject()
                return RankingConfig(
                    alphaFrequencyWeight = (obj["alphaFrequencyWeight"] as? Number)?.toDouble() ?: 1.0,
                    betaContextWeight = (obj["betaContextWeight"] as? Number)?.toDouble() ?: 2.4,
                    gammaPhoneticWeight = (obj["gammaPhoneticWeight"] as? Number)?.toDouble() ?: 1.8,
                    deltaPersonalWeight = (obj["deltaPersonalWeight"] as? Number)?.toDouble() ?: 2.2,
                    personalPromotionThreshold = (obj["personalPromotionThreshold"] as? Number)?.toInt() ?: 2
                )
            }
            return RankingConfig()
        }
    }
}

/**
 * Combines phonetic candidates, bundled dictionary trie, Bangla inflection stemmer,
 * ContextLM bigram lift, and local Room user lexicon boosts into the 3-slot suggestion strip
 * (Blueprint Part 03.4 & Prompts 13–14).
 */
class SuggestionEngine(
    private val banglaDict: DictionaryTrie,
    private val englishDict: DictionaryTrie = DictionaryTrie(),
    private val phoneticEngine: PhoneticEngine = PhoneticEngine(),
    private val contextLM: ContextLM = ContextLM.loadDefault(),
    private val rankingConfig: RankingConfig = RankingConfig.loadDefault()
) {
    data class StripState(
        val left: String?,
        val middle: String,
        val right: String?
    )

    /**
     * Builds the 3-slot suggestion strip for Phonetic mode:
     * - middle: live phonetic rendering
     * - left / right: context-, frequency-, phonetic-, and personalization-ranked Bangla candidates
     */
    fun suggestForPhonetic(
        latinBuffer: String,
        personalWords: Map<String, Int> = emptyMap(),
        autoConjuncts: Boolean = true,
        prevContextWord: String? = null
    ): StripState {
        val middle = phoneticEngine.convert(latinBuffer, autoConjuncts)
        if (middle.isEmpty()) return StripState(null, "", null)

        val scored = linkedMapOf<String, Double>()
        val candidates = phoneticEngine.generateCandidates(latinBuffer, autoConjuncts)

        fun addCandidate(word: String, baseFreq: Int, phoneticBonus: Double = 0.0) {
            if (word == middle) return
            val freqTerm = rankingConfig.alphaFrequencyWeight * ln(baseFreq.toDouble() + 1.0)
            val contextTerm = rankingConfig.betaContextWeight * contextLM.contextBoost(prevContextWord, word)
            val phoneticTerm = rankingConfig.gammaPhoneticWeight * phoneticBonus
            val uses = personalWords[word] ?: 0
            val personalTerm = if (uses > 0) {
                val promotionMult = if (uses >= rankingConfig.personalPromotionThreshold) 1.5 else 1.0
                rankingConfig.deltaPersonalWeight * ln(uses.toDouble() + 1.0) * promotionMult
            } else {
                0.0
            }
            val total = freqTerm + contextTerm + phoneticTerm + personalTerm
            scored[word] = maxOf(scored[word] ?: 0.0, total)
        }

        for ((candIdx, cand) in candidates.withIndex()) {
            val exactBonus = if (candIdx == 0) 2.5 else 1.8
            if (cand != middle && banglaDict.contains(cand)) {
                addCandidate(cand, banglaDict.frequencyOf(cand), phoneticBonus = exactBonus)
            }
            for (entry in banglaDict.prefixLookup(cand, limit = 8)) {
                val prefixBonus = if (entry.word == cand) exactBonus else 1.1
                addCandidate(entry.word, entry.frequency, phoneticBonus = prefixBonus)
            }
            for (split in BanglaInflection.stripSuffixes(cand)) {
                val stemFreq = banglaDict.frequencyOf(split.stem)
                if (stemFreq > 0) {
                    addCandidate(split.stem + split.suffix, stemFreq, phoneticBonus = 1.4)
                    addCandidate(split.stem, stemFreq / 2, phoneticBonus = 0.8)
                }
            }
        }

        // Direct personal lexicon matches & promotions (on-device Room words)
        for ((word, uses) in personalWords) {
            if (word != middle && (word.startsWith(middle) || candidates.any { word.startsWith(it) })) {
                val effectiveBase = if (uses >= rankingConfig.personalPromotionThreshold) 200 else 40
                addCandidate(word, baseFreq = maxOf(banglaDict.frequencyOf(word), effectiveBase), phoneticBonus = 1.6)
            }
        }

        // Typo-aware fuzzy fallback when prefix matches are sparse
        if (scored.size < 2 && middle.length >= 2) {
            for ((entry, _) in banglaDict.fuzzyLookup(middle, maxDistance = 2, limit = 4)) {
                val conf = TypoModel.typoConfidence(middle, entry.word)
                addCandidate(entry.word, entry.frequency / 2, phoneticBonus = conf)
            }
        }

        val top = scored.entries.sortedByDescending { it.value }.map { it.key }
        return StripState(
            left = top.getOrNull(0),
            middle = middle,
            right = top.getOrNull(1)
        )
    }

    /**
     * Builds the 3-slot suggestion strip for Fixed Bangla or English QWERTY mode.
     */
    fun suggestForDirectWord(
        currentWord: String,
        isBangla: Boolean,
        personalWords: Map<String, Int> = emptyMap(),
        prevContextWord: String? = null
    ): StripState {
        val nfc = BanglaUnicode.normalizeNfc(currentWord)
        if (nfc.isEmpty()) return StripState(null, "", null)

        val dict = if (isBangla) banglaDict else englishDict
        val scored = linkedMapOf<String, Double>()

        fun addCandidate(word: String, baseFreq: Int, exactBonus: Double = 0.0) {
            val freqTerm = rankingConfig.alphaFrequencyWeight * ln(baseFreq.toDouble() + 1.0)
            val contextTerm = if (isBangla) {
                rankingConfig.betaContextWeight * contextLM.contextBoost(prevContextWord, word)
            } else {
                0.0
            }
            val uses = personalWords[word] ?: 0
            val personalTerm = if (uses > 0) {
                val mult = if (uses >= rankingConfig.personalPromotionThreshold) 1.5 else 1.0
                rankingConfig.deltaPersonalWeight * ln(uses.toDouble() + 1.0) * mult
            } else {
                0.0
            }
            val total = freqTerm + contextTerm + personalTerm + exactBonus
            scored[word] = maxOf(scored[word] ?: 0.0, total)
        }

        for (entry in dict.prefixLookup(nfc, limit = 8)) {
            addCandidate(entry.word, entry.frequency, exactBonus = if (entry.word == nfc) 2.0 else 0.5)
        }
        if (isBangla) {
            for (split in BanglaInflection.stripSuffixes(nfc)) {
                val stemFreq = dict.frequencyOf(split.stem)
                if (stemFreq > 0) {
                    addCandidate(split.stem + split.suffix, stemFreq, exactBonus = 1.0)
                    addCandidate(split.stem, stemFreq / 2, exactBonus = 0.4)
                }
            }
        }
        for ((word, uses) in personalWords) {
            if (word.startsWith(nfc, ignoreCase = !isBangla)) {
                val base = maxOf(dict.frequencyOf(word), if (uses >= rankingConfig.personalPromotionThreshold) 200 else 40)
                addCandidate(word, base, exactBonus = 1.4)
            }
        }
        if (scored.isEmpty() && nfc.length >= 2) {
            for ((entry, _) in dict.fuzzyLookup(nfc, maxDistance = 1, limit = 4)) {
                val conf = if (isBangla) TypoModel.typoConfidence(nfc, entry.word) else 0.7
                addCandidate(entry.word, entry.frequency, exactBonus = conf)
            }
        }

        val ordered = scored.entries.sortedByDescending { it.value }.map { it.key }
        val middle = ordered.firstOrNull() ?: nfc
        val sides = ordered.filter { it != middle }
        return StripState(
            left = sides.getOrNull(0),
            middle = middle,
            right = sides.getOrNull(1)
        )
    }
}
