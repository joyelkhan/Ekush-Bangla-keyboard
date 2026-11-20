/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.example.data.db

import android.content.Context
import com.ekush.engine.AutocorrectEngine
import com.ekush.engine.BanglaUnicode
import com.ekush.engine.ContextLM
import com.ekush.engine.DictionaryTrie
import com.ekush.engine.PhoneticEngine
import com.ekush.engine.RuleTables
import com.ekush.engine.SuggestionEngine
import com.example.data.prefs.TypingMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * Local-only repository managing the bundled Bangla/English dictionary tries,
 * on-device Room user lexicon with decay, and per-app typing mode memory.
 */
class LexiconRepository(
    private val context: Context,
    private val dao: UserLexiconDao
) {
    val allUserWords: Flow<List<UserLexiconEntity>> = dao.observeAllWords()

    val phoneticEngine: PhoneticEngine by lazy {
        PhoneticEngine(RuleTables.loadDefault())
    }

    val banglaDictionary: DictionaryTrie by lazy {
        loadAssetDictionary("dict/bn_words.tsv")
    }

    val englishDictionary: DictionaryTrie by lazy {
        loadAssetDictionary("dict/en_words.tsv")
    }

    val contextLM: ContextLM by lazy {
        ContextLM.loadDefault()
    }

    val suggestionEngine: SuggestionEngine by lazy {
        SuggestionEngine(
            banglaDict = banglaDictionary,
            englishDict = englishDictionary,
            phoneticEngine = phoneticEngine,
            contextLM = contextLM
        )
    }

    val autocorrectEngine: AutocorrectEngine by lazy {
        AutocorrectEngine(banglaDictionary)
    }

    private fun loadAssetDictionary(assetPath: String): DictionaryTrie {
        return runCatching {
            context.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { reader ->
                DictionaryTrie.fromTsvLines(reader.lineSequence())
            }
        }.getOrElse { DictionaryTrie() }
    }

    suspend fun learnWord(rawWord: String) = withContext(Dispatchers.IO) {
        val word = BanglaUnicode.normalizeNfc(rawWord.trim())
        if (word.length < 2) return@withContext
        val now = System.currentTimeMillis()
        val existing = dao.getWord(word)
        val isBangla = word.any { BanglaUnicode.isBanglaChar(it) }
        val nextFreq = (existing?.frequency ?: 0) + 1
        dao.upsertWord(
            UserLexiconEntity(
                word = word,
                isBangla = isBangla,
                frequency = nextFreq,
                lastUsedEpochMs = now
            )
        )
        // Promote locally learned word into active trie once used twice (Prompt 14)
        if (nextFreq >= 2) {
            val targetDict = if (isBangla) banglaDictionary else englishDictionary
            if (!targetDict.contains(word)) {
                targetDict.insert(word, nextFreq * 50)
            }
        }
    }

    suspend fun deleteWord(word: String) = withContext(Dispatchers.IO) {
        dao.deleteWord(word)
    }

    suspend fun clearAllWords() = withContext(Dispatchers.IO) {
        dao.clearAllWords()
    }

    suspend fun getPersonalWordMap(): Map<String, Int> = withContext(Dispatchers.IO) {
        dao.getAllWordsSnapshot().associate { it.word to it.frequency }
    }

    suspend fun applyPeriodicDecay(daysThreshold: Int = 30) = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - daysThreshold * 86_400_000L
        dao.decayStaleWords(cutoff)
    }

    suspend fun getModeForApp(packageName: String?): TypingMode? = withContext(Dispatchers.IO) {
        if (packageName.isNullOrBlank()) return@withContext null
        dao.getModeForApp(packageName)?.let { TypingMode.fromWire(it) }
    }

    suspend fun saveModeForApp(packageName: String?, mode: TypingMode) = withContext(Dispatchers.IO) {
        if (packageName.isNullOrBlank()) return@withContext
        dao.upsertAppMode(
            AppModeMemoryEntity(
                packageName = packageName,
                modeWireName = mode.wireName,
                updatedEpochMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun exportLexiconJson(): JSONArray = withContext(Dispatchers.IO) {
        val array = JSONArray()
        for (entry in dao.getAllWordsSnapshot()) {
            array.put(
                JSONObject().apply {
                    put("word", entry.word)
                    put("isBangla", entry.isBangla)
                    put("frequency", entry.frequency)
                    put("lastUsedEpochMs", entry.lastUsedEpochMs)
                }
            )
        }
        array
    }

    /**
     * Merges imported backup words with existing Room entries: keeps the higher frequency
     * and newer timestamp without deleting or downgrading local-only entries (Prompt 14).
     */
    suspend fun importLexiconJson(array: JSONArray): Int = withContext(Dispatchers.IO) {
        var count = 0
        val now = System.currentTimeMillis()
        for (i in 0 until array.length()) {
            val obj = array.optJSONObject(i) ?: continue
            val word = BanglaUnicode.normalizeNfc(obj.optString("word").trim())
            if (word.isEmpty()) continue
            val importedFreq = obj.optInt("frequency", 1).coerceAtLeast(1)
            val isBangla = obj.optBoolean("isBangla", word.any { BanglaUnicode.isBanglaChar(it) })
            val importedTs = obj.optLong("lastUsedEpochMs", now)

            val existing = dao.getWord(word)
            val mergedFreq = maxOf(existing?.frequency ?: 0, importedFreq)
            val mergedTs = maxOf(existing?.lastUsedEpochMs ?: 0L, importedTs)
            dao.upsertWord(UserLexiconEntity(word, isBangla, mergedFreq, mergedTs))
            count++
        }
        count
    }
}
