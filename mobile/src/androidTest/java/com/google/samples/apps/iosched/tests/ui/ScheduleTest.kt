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

import android.provider.Settings
import android.view.View
import androidx.test.InstrumentationRegistry
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasFocus
import androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withParent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.data.FakeConferenceDataSource
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDays
import com.google.samples.apps.iosched.tests.FixedTimeRule
import com.google.samples.apps.iosched.tests.SetPreferencesRule
import com.google.samples.apps.iosched.tests.SyncTaskExecutorRule
import com.google.samples.apps.iosched.ui.MainActivity
import com.google.samples.apps.iosched.ui.schedule.day.SessionViewHolder
import com.google.samples.apps.iosched.ui.schedule.filters.ScheduleFilterAdapter
import com.google.samples.apps.iosched.widget.BottomSheetBehavior
import org.hamcrest.CoreMatchers.not
import org.hamcrest.core.AllOf.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Basic Espresso tests for the schedule screen.
 */
@RunWith(AndroidJUnit4::class)
class ScheduleTest {

    @get:Rule
    var activityRule = ActivityTestRule<MainActivity>(MainActivity::class.java)

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule
    var syncTaskExecutorRule = SyncTaskExecutorRule()

    // Sets the time to before the conference
    @get:Rule
    var timeProviderRule = FixedTimeRule()

    // Sets the preferences so no welcome screens are shown
    @get:Rule
    var preferencesRule = SetPreferencesRule()

    private val resources = InstrumentationRegistry.getTargetContext().resources

    @Before
    fun disableBottomSheetAnimations() {
        val behavior = BottomSheetBehavior.from(
            activityRule.activity.findViewById<View>(R.id.filter_sheet)
        )
        behavior.isAnimationDisabled = true
    }

    @Test
    fun clickOnAgenda_showsAgenda() {
        onView(withText(R.string.agenda)).perform(click())
        onView(withText("Breakfast")).check(matches(isDisplayed()))
    }

    @Test
    fun allDays_areClicked_showsSessions() {
        // Each of the days should be displayed
        ConferenceDays.forEachIndexed { i, conferenceDay ->
            val dayTitle = conferenceDay.formatMonthDay()
            onView(withText(dayTitle)).perform(click())
            onView(withText("First session day ${i + 1}"))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun clickOnFirstItem_detailsShow() {
        onView(allOf(withId(R.id.recyclerview), hasFocus()))
            .perform(RecyclerViewActions.actionOnItemAtPosition<SessionViewHolder>(0, click()))

        onView(
            allOf(
                withId(R.id.session_detail_with_video_title),
                withText("First session day 1")
            )
        )
            .check(matches(isDisplayed()))
    }

    /**
     * This test requires animations to be disabled in the device.
     */
    @Test
    fun clickFilters_showFilters() {
        checkAnimationsDisabled()

        onView(withId(R.id.filter_fab)).perform(click())

        val uncheckedFilterContentDesc =
            getDisabledFilterContDesc(FakeConferenceDataSource.FAKE_SESSION_TAG_NAME)
        val checkedFilterContentDesc =
            getActiveFilterContDesc(FakeConferenceDataSource.FAKE_SESSION_TAG_NAME)

        // Scroll to the filter
        onView(allOf(withId(R.id.recyclerview), withParent(withId(R.id.filter_sheet))))
            .perform(
                RecyclerViewActions.scrollTo<ScheduleFilterAdapter.FilterViewHolder>(
                    hasDescendant(withContentDescription(uncheckedFilterContentDesc))
                )
            )

        onView(withContentDescription(uncheckedFilterContentDesc))
            .check(matches(isDisplayed()))
            .perform(click())

        // Check that the filter is enabled
        onView(
            allOf(
                withId(R.id.filter_label),
                withContentDescription(checkedFilterContentDesc),
                not(withParent(withId(R.id.filter_description_tags)))
            )
        )
            .check(matches(isDisplayed()))
            .perform(click())
    }

    @Test
    fun filters_applyAFilter() {
        checkAnimationsDisabled()
        val sessionName = FakeConferenceDataSource.FAKE_SESSION_NAME

        // Apply the filter that will show the session
        applyFilter(FakeConferenceDataSource.FAKE_SESSION_TAG_NAME)

        // Check that it's displayed now
        onView(withText(sessionName))
            .check(matches(isDisplayed()))
    }

    @Test
    fun filters_clearFilters() {
        // Apply the filter that will show the session
        val filter = FakeConferenceDataSource.FAKE_SESSION_TAG_NAME
        applyFilter(filter)

        // Clear
        onView(withId(R.id.clear_filters_shortcut)).perform(click())

        onView(
            allOf(
                withId(R.id.filter_label),
                withContentDescription(getActiveFilterContDesc(filter)),
                withParent(withId(R.id.filter_description_tags))
            )
        ).check(matches(not(isCompletelyDisplayed())))
    }

    private fun applyFilter(filter: String) {
        // Open the filters sheet
        onView(withId(R.id.filter_fab)).perform(click())

        // Get the content description of the view we need to click on
        val uncheckedFilterContentDesc =
            resources.getString(R.string.a11y_filter_not_applied, filter)

        onView(allOf(withId(R.id.recyclerview), withParent(withId(R.id.filter_sheet))))
            .check(matches(isDisplayed()))

        // Scroll to the filter
        onView(allOf(withId(R.id.recyclerview), withParent(withId(R.id.filter_sheet))))
            .perform(
                RecyclerViewActions.scrollTo<ScheduleFilterAdapter.FilterViewHolder>(
                    hasDescendant(withContentDescription(uncheckedFilterContentDesc))
                )
            )

        // Click on the filter
        onView(withContentDescription(uncheckedFilterContentDesc))
            .check(matches(isDisplayed()))
            .perform(click())

        pressBack()
    }

    private fun getDisabledFilterContDesc(filter: String) =
        resources.getString(R.string.a11y_filter_not_applied, filter)

    private fun getActiveFilterContDesc(filter: String) =
        resources.getString(R.string.a11y_filter_applied, filter)

    private fun checkAnimationsDisabled() {
        val scale = Settings.Global.getFloat(
            InstrumentationRegistry.getTargetContext().contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )

        if (scale > 0) {
            throw Exception(
                "Device must have animations disabled. " +
                    "Developer options -> Animator duration scale"
            )
        }
    }
}
