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
import android.widget.TextView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.R.id
import com.google.samples.apps.iosched.tests.SetPreferencesRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso tests for Settings screen
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsTest {

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    // Sets the preferences so no welcome screens are shown
    @get:Rule(order = 1)
    var preferencesRule = SetPreferencesRule()

    @get:Rule(order = 2)
    val composeTestRule = AndroidComposeTestRule(
        activityRule = MainActivityTestRule(R.id.navigation_settings),
        activityProvider = { it.activity }
    )

    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun settings_basicViewsDisplayed() {
        // Settings title
        onView(allOf(instanceOf(TextView::class.java), withParent(withId(id.toolbar))))
            .check(matches(withText(R.string.settings_title)))

        // Preference toggle
        composeTestRule.onNodeWithText(resources.getString(R.string.settings_enable_notifications))
            .assertIsDisplayed()

        // TODO: toUpperCase can cause problems in devices with some Locales. Watch out!
        // About label
        composeTestRule.onNodeWithText(resources.getString(R.string.about_title).toUpperCase())
            .assertIsDisplayed()
    }
}
