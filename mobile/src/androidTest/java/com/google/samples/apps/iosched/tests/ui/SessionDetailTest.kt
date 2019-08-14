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

package com.google.samples.apps.iosched.tests.ui

import android.view.View
import androidx.navigation.findNavController
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.data.FakeConferenceDataSource
import com.google.samples.apps.iosched.shared.data.FakeConferenceDataSource.FAKE_SESSION_ID
import com.google.samples.apps.iosched.tests.FixedTimeRule
import com.google.samples.apps.iosched.tests.SetPreferencesRule
import com.google.samples.apps.iosched.tests.SyncTaskExecutorRule
import com.google.samples.apps.iosched.ui.MainActivity
import com.google.samples.apps.iosched.ui.schedule.ScheduleFragmentDirections
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailViewHolder
import org.hamcrest.CoreMatchers.allOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Espresso tests for the details screen.
 *
 * TODO
 * * Make this work with launchFragmentInContainer
 * * Youtube intent
 * * Information is correct, titles, tags, date and time
 * * Start event
 * * Related events present
 * * Star related events
 * * Speakers present
 * * Share intent
 * * Map intent
 * * Navigate to related event
 * * Navigate to speaker
 *
 */
@RunWith(AndroidJUnit4::class)
class SessionDetailTest {

    @get:Rule
    var activityRule = ActivityTestRule(MainActivity::class.java)

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule
    var syncTaskExecutorRule = SyncTaskExecutorRule()

    // Sets the time to before the conference
    @get:Rule
    var timeProviderRule = FixedTimeRule()

    // Sets the preferences so no welcome screens are shown
    @get:Rule
    var preferencesRule = SetPreferencesRule()

    @Test
    fun details_basicViewsDisplayed() {
        navigateToDetails()

        // On the details screen, scroll down to the speaker
        onView(withId(R.id.session_detail_recycler_view))
            .perform(RecyclerViewActions.scrollToPosition<SessionDetailViewHolder>(2))

        // Check that the speaker name is displayed
        onView(
            allOf(
                withId(R.id.speaker_item_name),
                withText(FakeConferenceDataSource.FAKE_SESSION_SPEAKER_NAME)
            )
        ).check(matches(isDisplayed()))

        // Scroll down to the related events
        onView(withId(R.id.session_detail_recycler_view))
            .perform(RecyclerViewActions.scrollToPosition<SessionDetailViewHolder>(4))

        // Check that the title is correct
        onView(allOf(withId(R.id.title), withText("First session day 2")))
            .check(matches(isDisplayed()))
    }

    private fun navigateToDetails() {
        val action = ScheduleFragmentDirections.actionScheduleFragmentToSessionDetailFragment(
            FAKE_SESSION_ID
        )
        activityRule.activity.findViewById<View>(R.id.nav_host_fragment)
            .findNavController().navigate(action)
    }
}