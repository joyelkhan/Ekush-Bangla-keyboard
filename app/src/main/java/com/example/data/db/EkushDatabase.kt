/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UserLexiconEntity::class, AppModeMemoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class EkushDatabase : RoomDatabase() {
    abstract fun userLexiconDao(): UserLexiconDao

    companion object {
        @Volatile
        private var instance: EkushDatabase? = null

        fun getInstance(context: Context): EkushDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    EkushDatabase::class.java,
                    "ekush_local.db"
                ).build().also { instance = it }
            }
        }
    }
}
