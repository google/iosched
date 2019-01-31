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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.model.TestDataRepository
import com.google.samples.apps.iosched.shared.domain.prefs.NotificationsPrefSaveActionUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetAnalyticsSettingUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetNotificationsSettingUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetThemeUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.domain.settings.SetAnalyticsSettingUseCase
import com.google.samples.apps.iosched.shared.domain.settings.SetThemeUseCase
import com.google.samples.apps.iosched.shared.domain.settings.SetTimeZoneUseCase
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.runBlockingTest
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import com.google.samples.apps.iosched.ui.info.SettingsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class SettingsViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule
    var syncTaskExecutorRule = SyncTaskExecutorRule()

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun `toggle dark mode`() = coroutineRule.runBlockingTest {
        val viewModel = createSettingsViewModel()
        // Initially false
        assertEquals(false, LiveDataTestUtil.getValue(viewModel.darkMode))

        // Toggle once
        viewModel.toggleDarkMode(true)
        assertEquals(true, LiveDataTestUtil.getValue(viewModel.darkMode))

        // Toggle back
        viewModel.toggleDarkMode(false)
        assertEquals(false, LiveDataTestUtil.getValue(viewModel.darkMode))
    }

    private fun createSettingsViewModel(): SettingsViewModel {
        val storage = FakePreferenceStorage()
        return SettingsViewModel(
            SetTimeZoneUseCase(storage, TestDataRepository, coroutineRule.testDispatcher),
            GetTimeZoneUseCase(storage, coroutineRule.testDispatcher),
            NotificationsPrefSaveActionUseCase(storage, coroutineRule.testDispatcher),
            GetNotificationsSettingUseCase(storage, coroutineRule.testDispatcher),
            SetAnalyticsSettingUseCase(storage, coroutineRule.testDispatcher),
            GetAnalyticsSettingUseCase(storage, coroutineRule.testDispatcher),
            SetThemeUseCase(storage, coroutineRule.testDispatcher),
            GetThemeUseCase(storage, coroutineRule.testDispatcher)
        )
    }
}
