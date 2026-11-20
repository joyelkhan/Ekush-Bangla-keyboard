/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.ekush.engine

import java.text.Normalizer

/**
 * Bangla Unicode helpers and grapheme-cluster boundary utilities (Blueprint Part 04.4 & Appendix 10.1).
 */
object BanglaUnicode {
    const val HASANTA_CHAR: Char = '\u09CD'
    const val HASANTA: String = "\u09CD"
    const val REPH_MARK: String = "\u09B0\u09CD"
    const val ZWJ: Char = '\u200D'
    const val ZWNJ: Char = '\u200C'
    const val ANUSVARA: Char = '\u0982'
    const val VISARGA: Char = '\u0983'
    const val CHANDRABINDU: Char = '\u0981'
    const val KHANDA_TA: Char = '\u09CE'
    const val DANDA: Char = '\u0964'
    const val DOUBLE_DANDA: Char = '\u0965'
    const val TAKA_SIGN: Char = '\u09F3'

    private const val BANGLA_BLOCK_START = '\u0980'
    private const val BANGLA_BLOCK_END = '\u09FF'

    fun normalizeNfc(input: String): String =
        Normalizer.normalize(input, Normalizer.Form.NFC)

    fun isBanglaChar(c: Char): Boolean = c in BANGLA_BLOCK_START..BANGLA_BLOCK_END

    fun isBanglaConsonant(c: Char): Boolean =
        (c in '\u0995'..'\u09B9') || c == '\u09DC' || c == '\u09DD' || c == '\u09DF'

    fun isBanglaVowelSignOrMark(c: Char): Boolean =
        (c in '\u09BE'..'\u09CC') ||
            c == '\u09D7' ||
            c == '\u0981' ||
            c == '\u0982' ||
            c == '\u0983' ||
            c == '\u09BC'

    fun isBanglaDigit(c: Char): Boolean = c in '\u09E6'..'\u09EF'

    fun toBanglaDigits(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            if (c in '0'..'9') {
                sb.append((0x09E6 + (c - '0')).toChar())
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    fun toLatinDigits(text: String): String {
        val sb = StringBuilder(text.length)
        for (c in text) {
            if (c in '\u09E6'..'\u09EF') {
                sb.append(('0'.code + (c - '\u09E6')).toChar())
            } else {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * Computes the number of UTF-16 chars belonging to the final rendered Bangla cluster (akshar)
     * so backspace deletes an entire conjunct + kar unit rather than leaving broken viramas
     * (Blueprint Part 04.3 & 04.11).
     */
    fun lastClusterLength(text: CharSequence): Int {
        if (text.isEmpty()) return 0
        var idx = text.length - 1

        // Include trailing joiners or vowel signs / nukta / anusvara marks
        while (idx > 0 && (isBanglaVowelSignOrMark(text[idx]) || text[idx] == ZWJ || text[idx] == ZWNJ)) {
            idx--
        }

        // If ending in raw hasanta (explicit hasanta), step over it to attach its base consonant
        if (idx > 0 && text[idx] == HASANTA_CHAR) {
            idx--
        }

        // Walk backward across conjunct chain: consonant + HASANTA + consonant ...
        while (idx >= 2 && text[idx - 1] == HASANTA_CHAR && isBanglaConsonant(text[idx - 2])) {
            idx -= 2
        }

        // Handle UTF-16 surrogate pairs outside Bangla block (e.g. emoji)
        if (idx > 0 && Character.isLowSurrogate(text[idx]) && Character.isHighSurrogate(text[idx - 1])) {
            idx--
        }

        return text.length - idx
    }
}
