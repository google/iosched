/*
 * Copyright 2018 Google LLC
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

package com.google.samples.apps.iosched.tests

import androidx.test.core.app.ApplicationProvider
import com.google.samples.apps.iosched.shared.data.prefs.PreferenceStorage
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.components.ApplicationComponent
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Rule to be used in tests that sets the SharedPreferences needed for avoiding onboarding flows,
 * resetting filters, etc.
 */
class SetPreferencesRule : TestWatcher() {

    @InstallIn(ApplicationComponent::class)
    @EntryPoint
    interface SetPreferencesRuleEntryPoint {
        fun preferenceStorage(): PreferenceStorage
    }

    override fun starting(description: Description?) {
        super.starting(description)

        EntryPointAccessors.fromApplication(
            ApplicationProvider.getApplicationContext(),
            SetPreferencesRuleEntryPoint::class.java
        ).preferenceStorage().apply {
            onboardingCompleted = true
            scheduleUiHintsShown = true
            preferConferenceTimeZone = true
            selectedFilters = ""
            sendUsageStatistics = false
            notificationsPreferenceShown = true
        }
    }
}
