/*
 * Copyright 2019 Google LLC
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

package com.google.samples.apps.iosched.ui.settings

import com.google.samples.apps.iosched.model.Theme.BATTERY_SAVER
import com.google.samples.apps.iosched.shared.domain.prefs.NotificationsPrefSaveActionUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetAnalyticsSettingUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetAvailableThemesUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetNotificationsSettingUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetThemeUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.domain.settings.SetAnalyticsSettingUseCase
import com.google.samples.apps.iosched.shared.domain.settings.SetThemeUseCase
import com.google.samples.apps.iosched.shared.domain.settings.SetTimeZoneUseCase
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.runBlockingTest
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the [SettingsViewModel]
 */
class SettingsViewModelTest {

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val testDispatcher = coroutineRule.testDispatcher

    @Test
    fun initialValues_matchStorage() = coroutineRule.runBlockingTest {
        val viewModel = createSettingsViewModel()

        val prefs = FakePreferenceStorage()

        assertEquals(prefs.preferConferenceTimeZone, viewModel.preferConferenceTimeZone.first())
        assertEquals(prefs.preferToReceiveNotifications, viewModel.enableNotifications.first())
        assertEquals(prefs.sendUsageStatistics, viewModel.sendUsageStatistics.first())
        assertEquals(BATTERY_SAVER, viewModel.theme.first())
    }

    @Test
    fun toggleBooleanSettings() = coroutineRule.runBlockingTest {
        val viewModel = createSettingsViewModel()

        val initialTimeZone = viewModel.preferConferenceTimeZone.first()
        val initialNotifications = viewModel.enableNotifications.first()
        val initialSendUsageStatistics = viewModel.sendUsageStatistics.first()

        viewModel.toggleTimeZone()
        viewModel.toggleEnableNotifications()
        viewModel.toggleSendUsageStatistics()

        assertEquals(!initialTimeZone, viewModel.preferConferenceTimeZone.first())
        assertEquals(!initialNotifications, viewModel.enableNotifications.first())
        assertEquals(!initialSendUsageStatistics, viewModel.sendUsageStatistics.first())
    }

    @Test
    fun clickOnChooseTheme_navigationActionTriggered() = coroutineRule.runBlockingTest {
        val viewModel = createSettingsViewModel()
        viewModel.onThemeSettingClicked()

        assertTrue(
            viewModel.navigationActions.first() is SettingsNavigationAction.NavigateToThemeSelector
        )
    }

    private fun createSettingsViewModel(): SettingsViewModel {
        val preferenceStorage = FakePreferenceStorage()

        val getTimeZoneUseCase =
            GetTimeZoneUseCase(preferenceStorage, testDispatcher)
        val setTimeZoneUseCase =
            SetTimeZoneUseCase(preferenceStorage, testDispatcher)

        val notificationsPrefSaveActionUseCase =
            NotificationsPrefSaveActionUseCase(preferenceStorage, testDispatcher)
        val getNotificationsSettingUseCase =
            GetNotificationsSettingUseCase(preferenceStorage, testDispatcher)

        val setAnalyticsSettingUseCase =
            SetAnalyticsSettingUseCase(preferenceStorage, testDispatcher)
        val getAnalyticsSettingUseCase =
            GetAnalyticsSettingUseCase(preferenceStorage, testDispatcher)

        val setThemeUseCase = SetThemeUseCase(preferenceStorage, testDispatcher)
        val getThemeUseCase = GetThemeUseCase(preferenceStorage, testDispatcher)

        val getAvailableThemesUseCase =
            GetAvailableThemesUseCase(testDispatcher)

        return SettingsViewModel(
            getTimeZoneUseCase = getTimeZoneUseCase,
            setTimeZoneUseCase = setTimeZoneUseCase,
            notificationsPrefSaveActionUseCase = notificationsPrefSaveActionUseCase,
            getNotificationsSettingUseCase = getNotificationsSettingUseCase,
            setAnalyticsSettingUseCase = setAnalyticsSettingUseCase,
            getAnalyticsSettingUseCase = getAnalyticsSettingUseCase,
            setThemeUseCase = setThemeUseCase,
            getThemeUseCase = getThemeUseCase,
            getAvailableThemesUseCase = getAvailableThemesUseCase
        )
    }
}
