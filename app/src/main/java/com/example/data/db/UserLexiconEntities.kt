/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Local-only learned word entry stored in Room (Blueprint Part 03.4 & 04.8).
 */
@Entity(tableName = "user_lexicon")
data class UserLexiconEntity(
    @PrimaryKey val word: String,
    val isBangla: Boolean,
    val frequency: Int,
    val lastUsedEpochMs: Long
)

/**
 * Local-only per-app typing mode memory (Blueprint Part 03.4 & 03.7).
 */
@Entity(tableName = "app_mode_memory")
data class AppModeMemoryEntity(
    @PrimaryKey val packageName: String,
    val modeWireName: String,
    val updatedEpochMs: Long
)
