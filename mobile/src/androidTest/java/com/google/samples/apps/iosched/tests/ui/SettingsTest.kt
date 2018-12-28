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

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.tests.SetPreferencesRule
import com.google.samples.apps.iosched.tests.SyncTaskExecutorRule
import org.hamcrest.CoreMatchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso tests for Settings screen
 */
@RunWith(AndroidJUnit4::class)
class SettingsTest {

    @get:Rule
    var activityRule = MainActivityTestRule(R.id.navigation_settings)

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule
    var syncTaskExecutorRule = SyncTaskExecutorRule()

    // Sets the preferences so no welcome screens are shown
    @get:Rule
    var preferencesRule = SetPreferencesRule()

    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun settings_basicViewsDisplayed() {
        // Setting title
        onView(allOf(withText(resources.getString(R.string.settings_title)), withId(R.id.title)))
            .check(matches(isDisplayed()))
        // Preference toggle
        onView(withText(resources.getString(R.string.settings_enable_notifications)))
            .check(matches(isDisplayed()))
        // About label
        onView(withText(resources.getString(R.string.about_title)))
            .check(matches(isDisplayed()))
        // FAQ link
        onView(withText(resources.getString(R.string.faq_title)))
            .check(matches(isDisplayed()))
        // Version
        onView(withText(resources.getString(R.string.version_name)))
            .check(matches(isDisplayed()))
    }
}
