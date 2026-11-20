/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.ekush.engine

import java.io.DataInputStream
import java.io.InputStream
import kotlin.math.ln

/**
 * Pure-Kotlin offline bigram context language model (Prompt 13).
 *
 * Supports TSV transition tables (`prevWord\tword\tcount`) and compact binary streams (`EKLM` v1),
 * resets context cleanly across sentence punctuation boundaries, and returns O(1) smoothed log-prob.
 */
class ContextLM {
    private val bigramCounts = HashMap<String, HashMap<String, Int>>(256)
    private val contextTotals = HashMap<String, Int>(256)

    val pairCount: Int
        get() = bigramCounts.values.sumOf { it.size }

    fun addBigram(prevWord: String, nextWord: String, count: Int) {
        val prev = BanglaUnicode.normalizeNfc(prevWord.trim())
        val next = BanglaUnicode.normalizeNfc(nextWord.trim())
        if (prev.isEmpty() || next.isEmpty() || count <= 0) return

        val inner = bigramCounts.getOrPut(prev) { HashMap() }
        val prevCount = inner[next] ?: 0
        inner[next] = prevCount + count
        contextTotals[prev] = (contextTotals[prev] ?: 0) + count
    }

    /**
     * Returns smoothed positive log-lift score in [0.0, ~8.0] for [candidate] following [prevWord].
     * Returns 0.0 when [prevWord] is empty or has no observed transition.
     */
    fun contextBoost(prevWord: String?, candidate: String): Double {
        if (prevWord.isNullOrBlank()) return 0.0
        val cleanPrev = BanglaUnicode.normalizeNfc(prevWord.trim())
        val inner = bigramCounts[cleanPrev] ?: return 0.0
        val count = inner[BanglaUnicode.normalizeNfc(candidate)] ?: return 0.0
        val total = (contextTotals[cleanPrev] ?: count).coerceAtLeast(1)
        val prob = (count.toDouble() + 0.5) / (total.toDouble() + 10.0)
        // Map probability into positive log-lift centered above uniform baseline
        return (ln(prob * 100.0 + 1.0)).coerceAtLeast(0.0)
    }

    companion object {
        private val SENTENCE_BOUNDARY_CHARS = setOf('.', '?', '!', '।', '\n', '\r', ';', ':', '"', '“', '”', '(', ')')

        /**
         * Extracts the immediate preceding word before any composing prefix, returning null if
         * a sentence punctuation boundary separates it from the active token.
         */
        fun extractPrevContextWord(precedingText: CharSequence?): String? {
            if (precedingText.isNullOrEmpty()) return null
            val raw = precedingText.toString()
            var end = raw.length
            // Trim trailing horizontal spaces only; a newline or punctuation resets context.
            while (end > 0 && raw[end - 1] == ' ') {
                end--
            }
            if (end == 0) return null
            val lastChar = raw[end - 1]
            if (lastChar in SENTENCE_BOUNDARY_CHARS || lastChar.isWhitespace()) return null

            var start = end
            while (start > 0) {
                val ch = raw[start - 1]
                if (ch.isWhitespace() || ch in SENTENCE_BOUNDARY_CHARS) break
                start--
            }
            val token = raw.substring(start, end).trim()
            return token.takeIf { it.isNotEmpty() }
        }

        fun loadDefault(): ContextLM {
            val stream = ContextLM::class.java.classLoader?.getResourceAsStream("dict/bn_bigrams.tsv")
            if (stream != null) {
                return stream.bufferedReader(Charsets.UTF_8).use { fromTsvLines(it.lineSequence()) }
            }
            return ContextLM()
        }

        fun fromTsvLines(lines: Sequence<String>): ContextLM {
            val lm = ContextLM()
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
                val parts = trimmed.split('\t')
                if (parts.size >= 3) {
                    val count = parts[2].toIntOrNull() ?: 1
                    lm.addBigram(parts[0], parts[1], count)
                }
            }
            return lm
        }

        /**
         * Reads compact binary `EKLM` format written by `scripts/build_lm.py`.
         */
        fun fromBinaryStream(input: InputStream): ContextLM {
            val lm = ContextLM()
            DataInputStream(input).use { dis ->
                val magic = ByteArray(4)
                dis.readFully(magic)
                if (String(magic, Charsets.US_ASCII) != "EKLM") return lm
                val version = dis.readInt()
                if (version != 1) return lm
                val pairCount = dis.readInt()
                repeat(pairCount) {
                    val prev = dis.readUTF()
                    val next = dis.readUTF()
                    val count = dis.readInt()
                    lm.addBigram(prev, next, count)
                }
            }
            return lm
        }
    }
}
