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

package com.google.samples.apps.iosched.tests.ui

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.tests.FixedTimeProvider
import com.google.samples.apps.iosched.tests.FixedTimeRule
import com.google.samples.apps.iosched.tests.SetPreferencesRule
import com.google.samples.apps.iosched.tests.SyncTaskExecutorRule
import com.google.samples.apps.iosched.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Checks that the auto-scroll feature works during the conference and is not used before it starts.
 */
@RunWith(Parameterized::class)
class ScheduleScrollTest(private val duringConference: Boolean) {

    @get:Rule
    var activityRule = ActivityTestRule<MainActivity>(MainActivity::class.java)

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule
    var syncTaskExecutorRule = SyncTaskExecutorRule()

    // Sets the time to before the conference
    @get:Rule
    var timeProviderRule = getFixedTimeRule()

    // Sets the preferences so no welcome screens are shown
    @get:Rule
    var preferencesRule = SetPreferencesRule()

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): Collection<Boolean> {
            return listOf(true, false)
        }
    }

    @Test
    fun scrollTest() {
        if (duringConference) {
            onView(withText("Last session day 1"))
                .check(matches(isDisplayed()))
        } else {
            onView(withText("First session day 1"))
                .check(matches(isDisplayed()))
        }
    }

    private fun getFixedTimeRule(): FixedTimeRule {

        return if (duringConference) {
            FixedTimeRule(
                FixedTimeProvider(TimeUtils.ConferenceDays.first().end.minusMinutes(15))
            )
        } else {
            FixedTimeRule()
        }
    }
}
