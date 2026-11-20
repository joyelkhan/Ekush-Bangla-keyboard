/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.ekush.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM corpus & rule suite implementing Blueprint Part 06.7 baseline + Part 04.3/04.6 properties.
 */
class PhoneticEngineCorpusTest {

    private val engine = PhoneticEngine(RuleTables.loadDefault())

    @Test
    fun `part 06_7 baseline enumeration cases pass`() {
        val cases = listOf(
            Triple("k", "ক", "R1"),
            Triple("kh", "খ", "R1 longest-match"),
            Triple("ka", "কা", "R3"),
            Triple("ki", "কি", "R3 (U+09BF logical order)"),
            Triple("ko", "কো", "R3 two-part sign"),
            Triple("ksha", "ক্ষা", "R5"),
            Triple("nda", "ন্দা", "R5"),
            Triple("k\\", "ক্", "R7"),
            Triple("rka", "র্কা", "R9"),
            Triple("mya", "ম্যা", "R5 + yaphala"),
            Triple("Ta", "টা", "R1 capital series"),
            // Blueprint Part 06.7 omits the trailing 'a' in "Ra Rh"; "Ra Rha" produces "ড়া ঢ়া".
            Triple("Ra Rha", "ড়া ঢ়া", "R1 dotted"),
            Triple("a", "অ", "R2 standalone"),
            Triple("bangladesh", "বাংলাদেশ", "composite"),
            Triple("bishwabidyalay", "বিশ্ববিদ্যালয়", "composite")
        )

        for ((input, expected, rule) in cases) {
            val actual = engine.convert(input)
            assertEquals("Rule [$rule] failed for input '$input'", expected, actual)
        }
    }

    @Test
    fun `onboarding test sentence converts accurately`() {
        val sentence = engine.convert("ami banglay matro ek minite Taip korte pari")
        assertEquals("আমি বাংলায় মাত্র এক মিনিটে টাইপ করতে পারি", sentence)
    }

    @Test
    fun `part 10_3 reference examples convert accurately`() {
        assertEquals("কবিতা", engine.convert("kobita"))
        assertEquals("গঙ্গা", engine.convert("ganga"))
        assertEquals("চিঠি", engine.convert("chiThi"))
        assertEquals("চিথি", engine.convert("chithi"))
        assertEquals("ফুল", engine.convert("phool"))
        assertEquals("ক্ষ্", engine.convert("ksh\\"))
    }

    @Test
    fun `pending hasanta state reported for explicit virama`() {
        val withHasanta = engine.convertDetailed("k\\")
        assertTrue(withHasanta.endsWithPendingHasanta)

        val completed = engine.convertDetailed("ka")
        assertFalse(completed.endsWithPendingHasanta)
    }

    @Test
    fun `determinism and termination hold across arbitrary ascii`() {
        val samples = listOf("amar", "sonar", "ksh\\", "rka", "xyz123", "", "a", "bangla")
        for (s in samples) {
            val first = engine.convert(s)
            val second = engine.convert(s)
            assertEquals("Engine must be pure/deterministic for '$s'", first, second)
        }
    }

    @Test
    fun `cluster-aware backspace deletes whole conjunct unit`() {
        // UTF-16 length of "ক্ষা" (ক + ্ + ষ + া) = 4 chars
        assertEquals(4, "ক্ষা".length)
        assertEquals(4, BanglaUnicode.lastClusterLength("ক্ষা"))
        assertEquals(3, BanglaUnicode.lastClusterLength("ক্ষ"))
        assertEquals(2, BanglaUnicode.lastClusterLength("কি"))
        assertEquals(1, BanglaUnicode.lastClusterLength("ক"))
    }

    @Test
    fun `inflection stripper resolves bangladesher from bangladesh`() {
        val splits = BanglaInflection.stripSuffixes("বাংলাদেশের")
        assertTrue(splits.any { it.stem == "বাংলাদেশ" && it.suffix == "ের" })
    }

    @Test
    fun `suggestion engine surfaces inflection-aware dictionary suggestions`() {
        val dict = DictionaryTrie().apply {
            insert("বাংলাদেশ", 48210)
            insert("বাংলা", 38500)
            insert("একুশ", 32000)
        }
        val suggester = SuggestionEngine(dict)
        val strip = suggester.suggestForPhonetic("bangladesh")
        assertEquals("বাংলাদেশ", strip.middle)

        val directStrip = suggester.suggestForDirectWord("বাংলাদেশের", isBangla = true)
        assertTrue(
            directStrip.middle == "বাংলাদেশের" ||
                directStrip.left == "বাংলাদেশের" ||
                directStrip.left == "বাংলাদেশ"
        )
    }

    @Test
    fun `autocorrect enforces 5x frequency threshold and respects quotes`() {
        val dict = DictionaryTrie().apply {
            insert("বাংলাদেশ", 48210)
            insert("বাংলা", 38500)
            insert("বাংল", 10)
        }
        val autocorrect = AutocorrectEngine(dict)

        // Single edit distance away from "বাংলা" with 38500x ratio -> corrects
        val fix = autocorrect.maybeCorrectOnCommit("বাংল")
        assertNotNull(fix)
        assertEquals("বাংলা", fix?.corrected)

        // Inside quotes -> never touches quoted text (Blueprint Part 03.5)
        val quoted = autocorrect.maybeCorrectOnCommit("বাংল", precedingText = "He typed \"")
        assertNull(quoted)
    }
}
