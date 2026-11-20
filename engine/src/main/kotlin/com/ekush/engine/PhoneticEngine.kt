/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.ekush.engine

/**
 * Pure-Kotlin Avro-style Bangla phonetic engine (Blueprint Part 04.3 & 04.10).
 *
 * Design guarantees:
 * - Zero Android dependencies for fast JVM unit testing.
 * - Deterministic pure conversion: `convert(buffer)` depends only on `buffer` and `rules`.
 * - Terminates in linear time O(n) on buffer length.
 */
class PhoneticEngine(
    private val rules: RuleTables = RuleTables.loadDefault()
) {
    data class ConversionResult(
        val bangla: String,
        val endsWithPendingHasanta: Boolean
    )

    private sealed interface Token {
        data class Consonant(val unit: String) : Token
        data class Vowel(val unit: String) : Token
        data class Symbol(val text: String) : Token
        data object ExplicitHasanta : Token
        data object Space : Token
        data class Passthrough(val ch: Char) : Token
    }

    private data class Syllable(
        val cluster: List<String>,
        val vowel: String?,
        val endsWithExplicitHasanta: Boolean,
        val isRephContext: Boolean
    )

    /**
     * Converts buffered Latin phonetic input into NFC-normalized Bangla text.
     */
    fun convert(buffer: String, autoConjuncts: Boolean = true): String =
        convertDetailed(buffer, autoConjuncts).bangla

    /**
     * Converts buffered Latin phonetic input and exposes pending-hasanta state for UI key feedback
     * (Blueprint Part 03.2 #4).
     */
    fun convertDetailed(buffer: String, autoConjuncts: Boolean = true): ConversionResult {
        if (buffer.isEmpty()) return ConversionResult("", endsWithPendingHasanta = false)

        // Split on whitespace so multi-word buffers evaluate canonical word mappings per token
        if (buffer.any { it.isWhitespace() }) {
            val out = StringBuilder()
            var wordStart = -1
            for (i in buffer.indices) {
                val ch = buffer[i]
                if (ch.isWhitespace()) {
                    if (wordStart != -1) {
                        out.append(convertSingleWord(buffer.substring(wordStart, i), autoConjuncts))
                        wordStart = -1
                    }
                    out.append(ch)
                } else if (wordStart == -1) {
                    wordStart = i
                }
            }
            if (wordStart != -1) {
                out.append(convertSingleWord(buffer.substring(wordStart), autoConjuncts))
            }
            val normalized = BanglaUnicode.normalizeNfc(out.toString())
            return ConversionResult(
                bangla = normalized,
                endsWithPendingHasanta = normalized.endsWith(BanglaUnicode.HASANTA)
            )
        }

        val converted = convertSingleWord(buffer, autoConjuncts)
        val normalized = BanglaUnicode.normalizeNfc(converted)
        return ConversionResult(
            bangla = normalized,
            endsWithPendingHasanta = normalized.endsWith(BanglaUnicode.HASANTA)
        )
    }

    /**
     * Generates plausible phonetic candidates (strict rule output + natural Romanized variants)
     * so SuggestionEngine can rank dictionary-backed Bangla forms even when `o`/`a`/`ng` are ambiguous.
     */
    fun generateCandidates(buffer: String, autoConjuncts: Boolean = true): List<String> {
        if (buffer.isEmpty()) return emptyList()
        val primary = convert(buffer, autoConjuncts)
        val candidates = linkedSetOf(primary)

        // Variant 1: treat post-consonant 'o' as Bengali inherent vowel (no o-kar, and no cross-o conjunct)
        if ('o' in buffer) {
            val tokens = tokenize(buffer)
            val inherentVariant = StringBuilder()
            var chunk = mutableListOf<Token>()
            for (idx in tokens.indices) {
                val t = tokens[idx]
                if (t is Token.Vowel && t.unit == "o" && idx > 0 && tokens[idx - 1] is Token.Consonant) {
                    if (chunk.isNotEmpty()) {
                        inherentVariant.append(renderTokens(chunk, autoConjuncts))
                        chunk = mutableListOf()
                    }
                } else {
                    chunk += t
                }
            }
            if (chunk.isNotEmpty()) {
                inherentVariant.append(renderTokens(chunk, autoConjuncts))
            }
            if (inherentVariant.isNotEmpty()) {
                candidates += BanglaUnicode.normalizeNfc(inherentVariant.toString())
            }
        }

        // Variant 2: leading 'a' as 'aa' (amar -> আমার)
        if (buffer.startsWith("a") && !buffer.startsWith("aa")) {
            candidates += convertSingleWord("a$buffer", autoConjuncts)
        }

        return candidates.toList()
    }

    private fun convertSingleWord(word: String, autoConjuncts: Boolean): String {
        if (autoConjuncts) {
            rules.canonicalWordOverrides[word]?.let { return it }
        }
        val tokens = tokenize(word)
        return renderTokens(tokens, autoConjuncts)
    }

    /**
     * S2/S3: Greedy longest-prefix tokenizer over rule tables. Guaranteed to advance >= 1 char per step.
     */
    private fun tokenize(buf: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        while (i < buf.length) {
            val ch = buf[i]
            if (ch.isWhitespace()) {
                tokens += Token.Space
                i++
                continue
            }
            if (ch == '\\' || ch == ']') {
                tokens += Token.ExplicitHasanta
                i++
                continue
            }
            var matchedUnit: String? = null
            for (candidate in rules.allKeysDescending) {
                if (i + candidate.length <= buf.length &&
                    buf.regionMatches(i, candidate, 0, candidate.length, ignoreCase = false)
                ) {
                    matchedUnit = candidate
                    break
                }
            }
            if (matchedUnit != null) {
                when {
                    rules.isConsonantUnit(matchedUnit) -> tokens += Token.Consonant(matchedUnit)
                    rules.isVowelUnit(matchedUnit) -> tokens += Token.Vowel(matchedUnit)
                    else -> tokens += Token.Symbol(rules.symbolOf(matchedUnit))
                }
                i += matchedUnit.length
            } else {
                tokens += Token.Passthrough(ch)
                i++
            }
        }
        return tokens
    }

    private fun renderTokens(tokens: List<Token>, autoConjuncts: Boolean): String {
        val out = StringBuilder()
        var i = 0
        while (i < tokens.size) {
            when (val tok = tokens[i]) {
                is Token.Space -> {
                    out.append(' ')
                    i++
                }
                is Token.Symbol -> {
                    out.append(tok.text)
                    i++
                }
                is Token.Passthrough -> {
                    out.append(tok.ch)
                    i++
                }
                is Token.ExplicitHasanta -> {
                    out.append(BanglaUnicode.HASANTA)
                    i++
                }
                is Token.Vowel -> {
                    // R2: standalone vowel when not preceded by a consonant cluster
                    out.append(rules.standaloneVowelOf(tok.unit))
                    i++
                }
                is Token.Consonant -> {
                    // Collect maximal consonant cluster [C1, C2, ...]
                    val cluster = mutableListOf<String>()
                    while (i < tokens.size && tokens[i] is Token.Consonant) {
                        val unit = (tokens[i] as Token.Consonant).unit
                        // 'ng' acts as anusvara (ং) unless followed immediately by g/gh/k (e.g., ganga)
                        if (unit == "ng") {
                            val next = tokens.getOrNull(i + 1)
                            val isNasalConjunct = next is Token.Consonant &&
                                (next.unit == "g" || next.unit == "gh" || next.unit == "k" || next.unit == "kh")
                            if (!isNasalConjunct && cluster.isEmpty()) {
                                out.append("ং")
                                i++
                                break
                            }
                        }
                        cluster += unit
                        i++
                        if (!autoConjuncts) break
                    }

                    if (cluster.isEmpty()) continue

                    // Check trailing explicit hasanta (R7) or trailing vowel (R3)
                    var explicitHasanta = false
                    var vowel: String? = null
                    if (i < tokens.size && tokens[i] is Token.ExplicitHasanta) {
                        explicitHasanta = true
                        i++
                    } else if (i < tokens.size && tokens[i] is Token.Vowel) {
                        vowel = (tokens[i] as Token.Vowel).unit
                        i++
                    }

                    // R9: leading 'r' before another consonant becomes reph (র্ + C), except r+r
                    val isReph = autoConjuncts &&
                        cluster.size >= 2 &&
                        cluster[0] == "r" &&
                        cluster[1] != "r" &&
                        cluster[1] != "y"

                    val effectiveCluster = if (isReph) cluster.drop(1) else cluster
                    val syllable = Syllable(
                        cluster = effectiveCluster,
                        vowel = vowel,
                        endsWithExplicitHasanta = explicitHasanta,
                        isRephContext = isReph
                    )
                    out.append(renderSyllable(syllable))
                }
            }
        }
        return out.toString()
    }

    private fun renderSyllable(syl: Syllable): String {
        // R7: explicit hasanta escape -> cluster + raw virama
        if (syl.endsWithExplicitHasanta) {
            return (if (syl.isRephContext) BanglaUnicode.REPH_MARK else "") +
                mapCluster(syl.cluster) + BanglaUnicode.HASANTA
        }

        // R9: reph prefix + remaining cluster
        val prefix = if (syl.isRephContext) BanglaUnicode.REPH_MARK else ""
        // R5: auto-conjunct C1 + hasanta + C2
        val cluster = prefix + mapCluster(syl.cluster)
        // R3: vowel kar after consonant cluster
        val kar = syl.vowel?.let { rules.karOf(it) } ?: ""
        return cluster + kar
    }

    private fun mapCluster(cluster: List<String>): String =
        cluster.joinToString(BanglaUnicode.HASANTA) { unit ->
            if (unit == "ng") "ঙ" else rules.consonantOf(unit)
        }
}
