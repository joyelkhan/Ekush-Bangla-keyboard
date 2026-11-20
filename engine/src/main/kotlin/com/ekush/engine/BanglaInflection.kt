/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.ekush.engine

/**
 * Lightweight Bangla inflection suffix stripper (Blueprint Part 03.4 & 04.6).
 *
 * Strips common nominal and verbal suffixes longest-first so inflected surface forms
 * (e.g., "বাংলাদেশের") match their root stem ("বাংলাদেশ") in the dictionary trie and
 * re-attach the suffix for suggestion rendering.
 */
object BanglaInflection {
    // Blueprint Part 03.4 suffixes: ের, কে, তে, য়ের, তেই
    // Additional standard Bangla nominal/case/plural suffixes marked UNVERIFIED_EXTENDED
    private val suffixesDescending: List<String> = listOf(
        "গুলোকে", // UNVERIFIED_EXTENDED
        "গুলোর",  // UNVERIFIED_EXTENDED
        "গুলোই",  // UNVERIFIED_EXTENDED
        "গুলো",   // UNVERIFIED_EXTENDED
        "গুলি",   // UNVERIFIED_EXTENDED
        "দেরকে",  // UNVERIFIED_EXTENDED
        "দের",    // UNVERIFIED_EXTENDED
        "য়ের",   // Blueprint Part 03.4
        "তেই",   // Blueprint Part 03.4
        "তেও",    // UNVERIFIED_EXTENDED
        "য়ে",    // UNVERIFIED_EXTENDED
        "কে",     // Blueprint Part 03.4
        "তে",     // Blueprint Part 03.4
        "ের",     // Blueprint Part 03.4
        "য়",     // UNVERIFIED_EXTENDED
        "ে",      // UNVERIFIED_EXTENDED
        "র"       // UNVERIFIED_EXTENDED
    ).sortedByDescending { it.length }

    data class StemMatch(
        val stem: String,
        val suffix: String
    )

    /**
     * Generates candidate (stem, suffix) splits ordered longest-suffix-first, preserving
     * at least 2 characters in the stem so short roots are not over-stripped.
     */
    fun stripSuffixes(word: String): List<StemMatch> {
        val nfc = BanglaUnicode.normalizeNfc(word)
        if (nfc.length < 3) return emptyList()
        val matches = mutableListOf<StemMatch>()
        for (suffix in suffixesDescending) {
            if (nfc.endsWith(suffix) && nfc.length - suffix.length >= 2) {
                matches += StemMatch(
                    stem = nfc.substring(0, nfc.length - suffix.length),
                    suffix = suffix
                )
            }
        }
        return matches
    }
}
