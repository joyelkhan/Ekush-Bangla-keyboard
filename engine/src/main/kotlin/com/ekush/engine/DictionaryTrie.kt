/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.ekush.engine

/**
 * Pure-Kotlin frequency-ranked Prefix Trie dictionary for Bangla & English suggestions
 * (Blueprint Part 04.5 & 04.6).
 */
class DictionaryTrie {
    private class Node {
        val children = HashMap<Char, Node>(4)
        var frequency: Int = 0
        var word: String? = null
    }

    data class Entry(
        val word: String,
        val frequency: Int
    )

    private val root = Node()
    private val allEntries = ArrayList<Entry>(4096)

    val size: Int
        get() = allEntries.size

    fun insert(rawWord: String, frequency: Int = 1) {
        val word = BanglaUnicode.normalizeNfc(rawWord.trim())
        if (word.isEmpty()) return
        var cur = root
        for (ch in word) {
            cur = cur.children.getOrPut(ch) { Node() }
        }
        if (cur.word == null) {
            cur.word = word
            cur.frequency = frequency.coerceAtLeast(1)
            allEntries += Entry(word, cur.frequency)
        } else {
            // Blueprint Appendix 10.4: merge duplicate surface form frequencies
            cur.frequency += frequency.coerceAtLeast(1)
        }
    }

    fun frequencyOf(word: String): Int {
        val nfc = BanglaUnicode.normalizeNfc(word)
        var cur = root
        for (ch in nfc) {
            cur = cur.children[ch] ?: return 0
        }
        return cur.frequency
    }

    fun contains(word: String): Boolean = frequencyOf(word) > 0

    /**
     * Returns top frequency-ranked completions starting with [prefix].
     */
    fun prefixLookup(prefix: String, limit: Int = 6): List<Entry> {
        val nfc = BanglaUnicode.normalizeNfc(prefix)
        if (nfc.isEmpty()) return emptyList()
        var cur = root
        for (ch in nfc) {
            cur = cur.children[ch] ?: return emptyList()
        }
        val matches = ArrayList<Entry>()
        collect(cur, matches, maxCollect = 64)
        return matches.sortedByDescending { it.frequency }.take(limit)
    }

    private fun collect(node: Node, out: MutableList<Entry>, maxCollect: Int) {
        if (out.size >= maxCollect) return
        val w = node.word
        if (w != null) {
            out += Entry(w, node.frequency)
        }
        for (child in node.children.values) {
            collect(child, out, maxCollect)
            if (out.size >= maxCollect) return
        }
    }

    /**
     * Returns entries within bounded Damerau-Levenshtein distance (transposition-aware,
     * Blueprint Part 04.6).
     */
    fun fuzzyLookup(query: String, maxDistance: Int = 2, limit: Int = 5): List<Pair<Entry, Int>> {
        val nfc = BanglaUnicode.normalizeNfc(query)
        if (nfc.isEmpty()) return emptyList()
        val lenDiff = maxDistance
        val results = ArrayList<Pair<Entry, Int>>()
        for (entry in allEntries) {
            if (kotlin.math.abs(entry.word.length - nfc.length) > lenDiff) continue
            val dist = damerauLevenshtein(nfc, entry.word, maxDistance)
            if (dist in 1..maxDistance) {
                results += entry to dist
            }
        }
        return results
            .sortedWith(compareBy<Pair<Entry, Int>> { it.second }.thenByDescending { it.first.frequency })
            .take(limit)
    }

    companion object {
        /**
         * Loads tab-separated `word[TAB]freq` dictionary lines (Appendix 10.4 format).
         */
        fun fromTsvLines(lines: Sequence<String>): DictionaryTrie {
            val trie = DictionaryTrie()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
                val parts = trimmed.split('\t')
                val word = parts[0]
                val freq = parts.getOrNull(1)?.toIntOrNull() ?: 1
                trie.insert(word, freq)
            }
            return trie
        }

        /**
         * Optimal string alignment (restricted Damerau-Levenshtein) with early cutoff when
         * distance exceeds [maxDist].
         */
        fun damerauLevenshtein(a: String, b: String, maxDist: Int): Int {
            val m = a.length
            val n = b.length
            if (kotlin.math.abs(m - n) > maxDist) return maxDist + 1
            val dp = Array(m + 1) { IntArray(n + 1) }
            for (i in 0..m) dp[i][0] = i
            for (j in 0..n) dp[0][j] = j

            for (i in 1..m) {
                var rowMin = Int.MAX_VALUE
                for (j in 1..n) {
                    val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                    var best = minOf(
                        dp[i - 1][j] + 1,
                        dp[i][j - 1] + 1,
                        dp[i - 1][j - 1] + cost
                    )
                    if (i > 1 && j > 1 && a[i - 1] == b[j - 2] && a[i - 2] == b[j - 1]) {
                        best = minOf(best, dp[i - 2][j - 2] + 1)
                    }
                    dp[i][j] = best
                    if (best < rowMin) rowMin = best
                }
                if (rowMin > maxDist) return maxDist + 1
            }
            return dp[m][n]
        }
    }
}
