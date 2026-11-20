/*
 * Copyright (c) 2026 Ekush contributors
 *
 * This file is part of Ekush (একুশ) Bangla Keyboard.
 * Licensed under the GNU General Public License v3.0 (GPL-3.0).
 */

package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppModeMemoryEntity
import com.example.data.db.EkushDatabase
import com.example.data.db.UserLexiconEntity
import com.example.data.prefs.TypingMode
import com.example.ime.EkushImeService
import java.util.Locale
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class EkushPrivacyAndImeRobolectricTest {

    @Test
    fun `merged manifest requests zero network permissions`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pkgInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        val requested = pkgInfo.requestedPermissions?.toList().orEmpty()
        assertFalse("INTERNET must never be declared", requested.contains(Manifest.permission.INTERNET))
        assertFalse(
            "ACCESS_NETWORK_STATE must not be declared",
            requested.contains(Manifest.permission.ACCESS_NETWORK_STATE)
        )
    }

    @Test
    fun `ime service is declared with BIND_INPUT_METHOD permission`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pkgInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_SERVICES
        )
        val serviceInfo = pkgInfo.services?.firstOrNull {
            it.name == EkushImeService::class.java.name
        }
        assertNotNull("EkushImeService must be registered in AndroidManifest", serviceInfo)
        assertEquals(Manifest.permission.BIND_INPUT_METHOD, serviceInfo?.permission)
    }

    @Test
    fun `english and bengali localized strings are present`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Ekush", context.getString(R.string.app_name))

        val bnConfig = Configuration(context.resources.configuration).apply {
            setLocale(Locale.forLanguageTag("bn"))
        }
        val bnContext = context.createConfigurationContext(bnConfig)
        assertEquals("একুশ", bnContext.getString(R.string.app_name))
        assertTrue(bnContext.getString(R.string.onboarding_step3_target).contains("বাংলায়"))
    }

    @Test
    fun `local room database stores decays and remembers per-app modes`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, EkushDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.userLexiconDao()
            dao.upsertWord(
                UserLexiconEntity(
                    word = "একুশ",
                    isBangla = true,
                    frequency = 10,
                    lastUsedEpochMs = 1_000L
                )
            )
            val loaded = dao.getWord("একুশ")
            assertEquals(10, loaded?.frequency)

            dao.decayStaleWords(cutoffEpochMs = 5_000L)
            assertEquals(5, dao.getWord("একুশ")?.frequency)

            dao.upsertAppMode(
                AppModeMemoryEntity(
                    packageName = "org.example.notes",
                    modeWireName = TypingMode.PHONETIC.wireName,
                    updatedEpochMs = 2_000L
                )
            )
            assertEquals("phonetic", dao.getModeForApp("org.example.notes"))

            // Prompt 14: verify non-destructive merge-on-import keeps higher frequency & local-only words
            val repo = com.example.data.db.LexiconRepository(context, dao)
            dao.upsertWord(UserLexiconEntity("একুশ", isBangla = true, frequency = 12, lastUsedEpochMs = 3_000L))
            dao.upsertWord(UserLexiconEntity("নিরাপদ", isBangla = true, frequency = 4, lastUsedEpochMs = 2_000L))
            val backupArray = org.json.JSONArray().apply {
                put(org.json.JSONObject().apply {
                    put("word", "একুশ")
                    put("isBangla", true)
                    put("frequency", 3) // lower than existing 12 -> must keep 12
                    put("lastUsedEpochMs", 1_000L)
                })
                put(org.json.JSONObject().apply {
                    put("word", "মাতৃভাষা")
                    put("isBangla", true)
                    put("frequency", 7)
                    put("lastUsedEpochMs", 4_000L)
                })
            }
            val importedCount = repo.importLexiconJson(backupArray)
            assertEquals(2, importedCount)
            assertEquals(12, dao.getWord("একুশ")?.frequency)
            assertEquals(4, dao.getWord("নিরাপদ")?.frequency)
            assertEquals(7, dao.getWord("মাতৃভাষা")?.frequency)
        } finally {
            db.close()
        }
    }
}
