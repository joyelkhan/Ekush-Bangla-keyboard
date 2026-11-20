/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.ekush.engine

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for Phase 2 upgrades: ContextLM bigram scoring & boundary resets,
 * Bengali TypoModel & learn-on-revert, local lexicon promotion, 1,000-string fuzz stability,
 * and latency bounds (Prompts 8, 13, 14, 15).
 */
class ContextLMAndTypoTest {

    @Test
    fun `context LM boosts bigram continuation and resets across boundaries`() {
        val dict = DictionaryTrie().apply {
            insert("সমাজ", 12000)
            insert("সময়", 10000)
            insert("এই", 25000)
        }
        val lm = ContextLM().apply {
            addBigram("এই", "সময়", 500)
        }
        val suggester = SuggestionEngine(banglaDict = dict, contextLM = lm)

        // Without context, higher unigram freq "সমাজ" leads "সময়"
        val noContext = suggester.suggestForPhonetic("som", prevContextWord = null)
        assertEquals("সমাজ", noContext.left)

        // With prevContextWord = "এই", bigram lift promotes "সময়" into slot 1
        val withContext = suggester.suggestForPhonetic("som", prevContextWord = "এই")
        assertEquals("সময়", withContext.left)

        // Boundary extraction checks
        assertEquals("এই", ContextLM.extractPrevContextWord("আমি এই "))
        assertNull(ContextLM.extractPrevContextWord("শেষ। "))
        assertNull(ContextLM.extractPrevContextWord("done. "))
        assertNull(ContextLM.extractPrevContextWord("line1\n"))
        assertNull(ContextLM.extractPrevContextWord(""))
    }

    @Test
    fun `context LM loads EKLM binary stream cleanly`() {
        val baos = ByteArrayOutputStream()
        DataOutputStream(baos).use { dos ->
            dos.writeBytes("EKLM")
            dos.writeInt(1)
            dos.writeInt(2)
            dos.writeUTF("আমার")
            dos.writeUTF("বাংলা")
            dos.writeInt(350)
            dos.writeUTF("সোনার")
            dos.writeUTF("বাংলা")
            dos.writeInt(450)
        }
        val lm = ContextLM.fromBinaryStream(ByteArrayInputStream(baos.toByteArray()))
        assertEquals(2, lm.pairCount)
        assertTrue(lm.contextBoost("সোনার", "বাংলা") > 0.0)
        assertEquals(0.0, lm.contextBoost("অজানা", "বাংলা"), 0.0001)
    }

    @Test
    fun `typo model ranks Bengali confusion pairs and hasanta slips higher than unrelated edits`() {
        val sibilantConf = TypoModel.typoConfidence("ভাসা", "ভাষা")
        val hasantaConf = TypoModel.typoConfidence("কষ", "ক্ষ")
        val karSwapConf = TypoModel.typoConfidence("কিা", "কাি")
        val unrelatedConf = TypoModel.typoConfidence("কলম", "কমম")

        assertTrue(sibilantConf > unrelatedConf)
        assertTrue(hasantaConf >= 0.9)
        assertTrue(karSwapConf >= 0.9)
    }

    @Test
    fun `autocorrect skips all-caps mixed-script and respects session revert-learn`() {
        val dict = DictionaryTrie().apply {
            insert("ভাষা", 25000)
            insert("ভাসা", 100)
            insert("NASA", 50000)
        }
        val ac = AutocorrectEngine(dict)

        // All-caps & mixed script never corrected
        assertNull(ac.maybeCorrectOnCommit("NASB"))
        assertNull(ac.maybeCorrectOnCommit("বাংলাa"))

        // Known Bengali confusion fix succeeds before revert
        val first = ac.maybeCorrectOnCommit("ভাসা")
        assertNotNull(first)
        assertEquals("ভাষা", first?.corrected)

        // Session learn-on-revert suppresses repeat correction for the rest of session
        ac.recordRevert("ভাসা", "ভাষা")
        assertNull(ac.maybeCorrectOnCommit("ভাসা"))

        ac.clearSessionReverts()
        assertNotNull(ac.maybeCorrectOnCommit("ভাসা"))
    }

    @Test
    fun `personal lexicon promotion boosts repeated local words`() {
        val dict = DictionaryTrie().apply {
            insert("কবিতা", 5000)
        }
        val suggester = SuggestionEngine(banglaDict = dict, contextLM = ContextLM())
        val strip = suggester.suggestForPhonetic(
            "kobi",
            personalWords = mapOf("কবিতার" to 3)
        )
        assertTrue(strip.left == "কবিতার" || strip.right == "কবিতার")
    }

    @Test
    fun `seed benchmark vectors convert as expected`() {
        val engine = PhoneticEngine(RuleTables.loadDefault())
        assertEquals("একুশ", engine.convert("ekush"))
        assertEquals("আমি", engine.convert("ami"))
        assertEquals("আমার", engine.convert("amar"))
        assertEquals("বাংলা", engine.convert("bangla"))
        assertEquals("সোনার", engine.convert("sonar"))
        assertEquals("গান", engine.convert("gan"))
        assertEquals("গাই", engine.convert("gai"))
        assertEquals("বৈশাখ", engine.convert("boishakh"))
        assertEquals("কৃষ্ণ", engine.convert("krrishhN"))
    }

    @Test
    fun `1000-string ASCII fuzz test never throws and finishes rapidly`() {
        val engine = PhoneticEngine(RuleTables.loadDefault())
        val rng = Random(21021952)
        val alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789\\:^ "
        val startNs = System.nanoTime()

        repeat(1000) {
            val len = rng.nextInt(1, 24)
            val input = buildString(len) {
                repeat(len) { append(alphabet[rng.nextInt(alphabet.length)]) }
            }
            val output = engine.convert(input)
            assertNotNull(output)
        }

        val elapsedMs = (System.nanoTime() - startNs) / 1_000_000
        assertTrue("Expected fuzz suite under 2500ms, took ${elapsedMs}ms", elapsedMs < 2500)
    }
}
