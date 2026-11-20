/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserLexiconDao {
    @Query("SELECT * FROM user_lexicon ORDER BY frequency DESC, lastUsedEpochMs DESC")
    fun observeAllWords(): Flow<List<UserLexiconEntity>>

    @Query("SELECT * FROM user_lexicon")
    suspend fun getAllWordsSnapshot(): List<UserLexiconEntity>

    @Query("SELECT * FROM user_lexicon WHERE word = :word LIMIT 1")
    suspend fun getWord(word: String): UserLexiconEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertWord(entity: UserLexiconEntity)

    @Query("DELETE FROM user_lexicon WHERE word = :word")
    suspend fun deleteWord(word: String)

    @Query("DELETE FROM user_lexicon")
    suspend fun clearAllWords()

    /**
     * Applies periodic frequency decay to stale entries older than [cutoffEpochMs]
     * per Blueprint Part 03.4.
     */
    @Query("UPDATE user_lexicon SET frequency = MAX(1, frequency / 2) WHERE lastUsedEpochMs < :cutoffEpochMs AND frequency > 1")
    suspend fun decayStaleWords(cutoffEpochMs: Long)

    @Query("SELECT modeWireName FROM app_mode_memory WHERE packageName = :packageName LIMIT 1")
    suspend fun getModeForApp(packageName: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppMode(entity: AppModeMemoryEntity)
}
