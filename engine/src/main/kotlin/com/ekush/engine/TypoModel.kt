/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.ekush.engine

/**
 * Bengali-specific weighted typo model (Prompt 15):
 * prioritizes aspirated/retroflex/sibilant layout confusion pairs, dropped/duplicated hasanta (্),
 * and adjacent vowel-sign (কার) order swaps.
 */
object TypoModel {
    private const val HASANTA_CHAR = '\u09CD'

    private val confusionGroups: List<Set<Char>> = listOf(
        setOf('চ', 'ছ'),
        setOf('ত', 'ট'),
        setOf('থ', 'ঠ'),
        setOf('দ', 'ধ', 'ড', 'ঢ'),
        setOf('ব', 'ভ'),
        setOf('শ', 'ষ', 'স'),
        setOf('ন', 'ণ'),
        setOf('ক', 'খ'),
        setOf('গ', 'ঘ'),
        setOf('প', 'ফ'),
        setOf('জ', 'য', 'ঝ'),
        setOf('র', '\u09DC', '\u09DD'),
        setOf('\u09BF', '\u09C0'),
        setOf('\u09C1', '\u09C2')
    )

    private val karSet: Set<Char> = setOf(
        '\u09BE', '\u09BF', '\u09C0', '\u09C1', '\u09C2',
        '\u09C3', '\u09C7', '\u09C8', '\u09CB', '\u09CC'
    )

    fun isConfusionPair(a: Char, b: Char): Boolean {
        if (a == b) return false
        return confusionGroups.any { a in it && b in it }
    }

    /**
     * Computes normalized typo plausibility score in (0.0, 1.0]:
     * - 1.0: Known Bengali phonetic/layout confusion swap, kar/hasanta slip, or adjacent kar transposition.
     * - 0.65: Standard single-character insert/delete/transposition.
     * - 0.45: Arbitrary unrelated single-char substitution.
     */
    fun typoConfidence(typed: String, candidate: String): Double {
        val a = BanglaUnicode.normalizeNfc(typed)
        val b = BanglaUnicode.normalizeNfc(candidate)
        if (a == b || a.isEmpty() || b.isEmpty()) return 0.0

        val dist = DictionaryTrie.damerauLevenshtein(a, b, maxDist = 2)
        if (dist <= 0 || dist > 2) return 0.0

        // Dropped or extra hasanta ('্') inside a cluster
        if (dist == 1 && kotlin.math.abs(a.length - b.length) == 1) {
            val longer = if (a.length > b.length) a else b
            val shorter = if (a.length > b.length) b else a
            for (idx in longer.indices) {
                if (longer[idx] == HASANTA_CHAR &&
                    longer.removeRange(idx, idx + 1) == shorter
                ) {
                    return 0.95
                }
            }
            return 0.68
        }

        // Same-length substitution or adjacent swap
        if (a.length == b.length) {
            val diffs = a.indices.filter { a[it] != b[it] }
            if (diffs.size == 1) {
                val idx = diffs[0]
                return if (isConfusionPair(a[idx], b[idx])) 1.0 else 0.48
            }
            if (diffs.size == 2 && diffs[1] == diffs[0] + 1) {
                val i = diffs[0]
                if (a[i] == b[i + 1] && a[i + 1] == b[i]) {
                    val hasKarOrHasanta = a[i] in karSet || a[i + 1] in karSet ||
                        a[i] == HASANTA_CHAR || a[i + 1] == HASANTA_CHAR
                    return if (hasKarOrHasanta) 0.96 else 0.72
                }
            }
        }

        return if (dist == 2) 0.40 else 0.55
    }
}
