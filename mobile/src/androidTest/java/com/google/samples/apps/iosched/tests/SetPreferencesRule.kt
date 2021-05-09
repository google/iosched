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
import com.google.samples.apps.iosched.shared.di.ApplicationScope
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Rule to be used in tests that sets the preferences needed for avoiding onboarding flows,
 * resetting filters, etc.
 */
class SetPreferencesRule : TestWatcher() {

    @InstallIn(SingletonComponent::class)
    @EntryPoint
    interface SetPreferencesRuleEntryPoint {
        fun preferenceStorage(): PreferenceStorage
        @ApplicationScope
        fun applicationScope(): CoroutineScope
    }

    override fun starting(description: Description?) {
        super.starting(description)

        EntryPointAccessors.fromApplication(
            ApplicationProvider.getApplicationContext(),
            SetPreferencesRuleEntryPoint::class.java
        ).preferenceStorage().apply {
            runBlocking {
                completeOnboarding(true)
                showScheduleUiHints(true)
                preferConferenceTimeZone(true)
                selectFilters("")
                sendUsageStatistics(false)
                showNotificationsPreference(true)
            }
        }
    }

    override fun finished(description: Description) {
        // At the end of every test, cancel the application scope
        // So DataStore is closed
        EntryPointAccessors.fromApplication(
            ApplicationProvider.getApplicationContext(),
            SetPreferencesRuleEntryPoint::class.java
        ).applicationScope().cancel()
        super.finished(description)
    }
}
