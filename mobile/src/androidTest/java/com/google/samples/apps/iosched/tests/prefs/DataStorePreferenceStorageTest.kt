/*
 * Copyright 2021 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.tests.prefs

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.core.app.ApplicationProvider
import com.google.samples.apps.iosched.shared.data.prefs.DataStorePreferenceStorage
import com.google.samples.apps.iosched.shared.data.prefs.PreferenceStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

val Context.dataStore by preferencesDataStore(name = "test")

class DataStorePreferenceStorageTest {

    private lateinit var context: Context
    private lateinit var preferenceStorage: PreferenceStorage

    @Before
    fun init() {
        context = ApplicationProvider.getApplicationContext()
        preferenceStorage = DataStorePreferenceStorage(context.dataStore)
    }

    @Test
    fun completeOnboarding() = runTest {
        preferenceStorage.completeOnboarding(true)
        val result = preferenceStorage.onboardingCompleted.first()
        assertTrue(result)
    }

    @Test
    fun showScheduleUiHints() = runTest {
        preferenceStorage.showScheduleUiHints(true)
        val result = preferenceStorage.areScheduleUiHintsShown()
        assertTrue(result)
    }

    @Test
    fun showNotificationsPreference() = runTest {
        preferenceStorage.showNotificationsPreference(true)
        val result = preferenceStorage.notificationsPreferenceShown.first()
        assertTrue(result)
    }

    @Test
    fun preferToReceiveNotifications() = runTest {
        preferenceStorage.preferToReceiveNotifications(true)
        val result = preferenceStorage.preferToReceiveNotifications.first()
        assertTrue(result)
    }

    @Test
    fun optInMyLocation() = runTest {
        preferenceStorage.optInMyLocation(true)
        val result = preferenceStorage.myLocationOptedIn.first()
        assertTrue(result)
    }

    @Test
    fun stopSnackbar() = runTest {
        preferenceStorage.stopSnackbar(true)
        val result = preferenceStorage.isSnackbarStopped()
        assertTrue(result)
    }

    @Test
    fun sendUsageStatistics() = runTest {
        preferenceStorage.sendUsageStatistics(true)
        val result = preferenceStorage.sendUsageStatistics.first()
        assertTrue(result)
    }

    @Test
    fun preferConferenceTimeZone() = runTest {
        preferenceStorage.preferConferenceTimeZone(true)
        val result = preferenceStorage.preferConferenceTimeZone.first()
        assertTrue(result)
    }

    @Test
    fun selectFilters() = runTest {
        val filters = "filter1, filter2"
        preferenceStorage.selectFilters(filters)
        val result = preferenceStorage.selectedFilters.first()
        assertEquals(filters, result)
    }

    @Test
    fun selectTheme() = runTest {
        val theme = "theme"
        preferenceStorage.selectTheme(theme)
        val result = preferenceStorage.selectedTheme.first()
        assertEquals(theme, result)
    }

    @Test
    fun showCodelabsInfo() = runTest {
        preferenceStorage.showCodelabsInfo(true)
        val result = preferenceStorage.codelabsInfoShown.first()
        assertTrue(result)
    }
}
