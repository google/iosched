/*
 * Copyright 2015 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.samples.apps.iosched.myschedule;

import android.content.Intent;
import android.net.Uri;
import android.os.Looper;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.IdlingResource;
import android.support.test.filters.FlakyTest;
import android.support.test.filters.LargeTest;
import android.support.test.filters.Suppress;
import android.support.test.runner.AndroidJUnit4;
import android.util.Log;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.feedback.SessionFeedbackActivity;
import com.google.samples.apps.iosched.injection.ModelProvider;
import com.google.samples.apps.iosched.mockdata.MyScheduleMockItems;
import com.google.samples.apps.iosched.mockdata.StubActivityContext;
import com.google.samples.apps.iosched.navigation.NavigationModel;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.testutils.BaseActivityTestRule;
import com.google.samples.apps.iosched.testutils.NavigationUtils;
import com.google.samples.apps.iosched.testutils.OrientationHelper;
import com.google.samples.apps.iosched.testutils.ThrottleContentObserverIdlingResource;
import com.google.samples.apps.iosched.util.TimeUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasData;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

/**
 * UI tests for {@link MyScheduleActivity} for when the user is attending the conference and the
 * second day of the conference starts in 3 hours.
 * <p/>
 * This should be run on devices with a narrow layout only (phones all orientation, tablets in
 * portrait mode)
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class MyScheduleActivityTest {

    /**
     * The {@link StubMyScheduleModel} needs a {@link android.content.Context} but at the stage it
     * is created, {@link #mActivityRule} hasn't got an {@link android.app.Activity} yet so we use
     * the instrumentation target context. However, an Actiivty Context is required by the model for
     * carrying certain actions, such as opening the session that was clicked on (which uses {@link
     * android.content.Context#startActivity(Intent)} and will not work with a non Activity context.
     * We use this {@link StubActivityContext} to later set an activity context at the start of each
     * test if needed (if the test needs to start another activity).
     */
    private StubActivityContext mActivityStubContext;

    private StubMyScheduleModel mStubMyScheduleModel;

    @Rule
    public BaseActivityTestRule<MyScheduleActivity> mActivityRule =
            new BaseActivityTestRule<MyScheduleActivity>(MyScheduleActivity.class) {

                @Override
                protected void beforeActivityLaunched() {
                    prepareActivityForInPersonAttendee();


                    // Create a stub model to simulate a user attending conference, during the
                    // second day
                    mActivityStubContext =
                            new StubActivityContext(InstrumentationRegistry.getTargetContext());
                    try {
                        /**
                         * {@link MyScheduleModel} uses a Handler, so we need to run this on the
                         * main thread. If we don't, we need to call {@link Looper#prepare()} but
                         * the test runner uses the same non UI thread for setting up each test in a
                         * test class, and therefore, upon trying to run the second test, it
                         * complains that we call {@link Looper#prepare()} on a thread that has
                         * already been prepared. By using the UI thread, we avoid this issue as
                         * the UI thread is already prepared so we don't need to manually do it.
                         */
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mStubMyScheduleModel = new StubMyScheduleModel(
                                        mActivityStubContext,
                                        MyScheduleMockItems.getItemsForAttendeeAfter(1, false),
                                        MyScheduleMockItems.getItemsForAttendeeBefore(2));
                                ModelProvider.setStubModel(mStubMyScheduleModel);
                            }
                        });
                    } catch (Throwable throwable) {
                        Log.e("DEBUG", "Error running test " + throwable);
                    }
                }
            };

    @Before
    public void setUp() {
        // Set up time to start of second day of conference
        TimeUtils.setCurrentTimeRelativeToStartOfSecondDayOfConference(
                InstrumentationRegistry.getTargetContext(), 0);

        // Don't show notifications for sessions as they get in the way of the UI
        SettingsUtils.setShowSessionReminders(InstrumentationRegistry.getTargetContext(), false);

        // Mark use as attending conference
        SettingsUtils.setAttendeeAtVenue(InstrumentationRegistry.getTargetContext(), true);
    }

    @Test
    @Suppress // Test isn't deterministic when run as part of the full test suite.
    public void day2Selected() {
        // Given a current time 3 hours after the start of the second day

        // Then the second day is selected
        onView(withText(MyScheduleMockItems.SESSION_TITLE_BEFORE)).check(matches(isDisplayed()));
    }

    @Test
    @Suppress // Test isn't deterministic when run as part of the full test suite.
    public void viewDay2_clickOnSession_opensSessionScreenIntentFired() {
        mActivityStubContext.setActivityContext(mActivityRule.getActivity());

        // When clicking on the session
        onView(withText(MyScheduleMockItems.SESSION_TITLE_BEFORE)).perform(click());

        // Then the intent with the session uri is fired
        Uri expectedSessionUri =
                ScheduleContract.Sessions.buildSessionUri(MyScheduleMockItems.SESSION_ID);
        intended(allOf(
                hasAction(equalTo(Intent.ACTION_VIEW)),
                hasData(expectedSessionUri)));
    }

    @Test
    @Suppress // Test isn't deterministic when run as part of the full test suite.
    public void viewDay1_clickOnSession_opensSessionScreenIntentFired() {
        mActivityStubContext.setActivityContext(mActivityRule.getActivity());

        // When clicking on the session
        onView(withText(MyScheduleMockItems.SESSION_TITLE_BEFORE)).perform(click());

        // Then the intent with the session uri is fired
        Uri expectedSessionUri =
                ScheduleContract.Sessions.buildSessionUri(MyScheduleMockItems.SESSION_ID);
        intended(allOf(
                hasAction(equalTo(Intent.ACTION_VIEW)),
                hasData(expectedSessionUri)));
    }

    @Test
    public void viewDay1_clickOnRateSession_opensFeedbackScreenIntentFired() {
        mActivityStubContext.setActivityContext(mActivityRule.getActivity());

        // Given day 1 visible
        showDay(1);

        // When clicking on rate session
        onView(allOf(withText(R.string.my_schedule_rate_this_session), isDisplayed()))
                .perform(click());

        // Then the intent for the feedback screen is fired
        Uri expectedSessionUri =
                ScheduleContract.Sessions.buildSessionUri(MyScheduleMockItems.SESSION_ID);
        intended(allOf(
                hasAction(equalTo(Intent.ACTION_VIEW)),
                hasData(expectedSessionUri),
                hasComponent(SessionFeedbackActivity.class.getName())));
    }

    @Test
    @Suppress // Test isn't deterministic when run as part of the full test suite.
    public void viewDay2_clickOnBrowseSession_opensSessionsListScreen() {
        mActivityStubContext.setActivityContext(mActivityRule.getActivity());

        // When clicking on browse sessions
        onView(allOf(withText(R.string.browse_sessions), isDisplayed())).perform(click());

        // Then the intent for the sessions list screen is fired
        long slotStart = Config.CONFERENCE_START_MILLIS + 1 * TimeUtils.DAY
                + MyScheduleMockItems.SESSION_AVAILABLE_SLOT_TIME_OFFSET;
        Uri expectedTimeIntervalUri =
                ScheduleContract.Sessions.buildUnscheduledSessionsInInterval(slotStart,
                        slotStart + MyScheduleMockItems.SESSION_AVAILABLE_SLOT_TIME_DURATION);
        intended(allOf(
                hasAction(equalTo(Intent.ACTION_VIEW)),
                hasData(expectedTimeIntervalUri)));
    }

    @Test
    @Suppress // Test isn't deterministic when run as part of the full test suite.
    public void viewDay2_clickOnTimeSlot_opensSessionsListScreen() {
        mActivityStubContext.setActivityContext(mActivityRule.getActivity());

        // When clicking on the time of a time slot
        long slotStart = Config.CONFERENCE_START_MILLIS + 1 * TimeUtils.DAY;
        onView(allOf(isDisplayed(), withId(R.id.start_time),
                withText(TimeUtils.formatShortTime(mActivityStubContext, new Date(slotStart)))))
                .perform(click());

        // Then the intent for the sessions list screen is fired
        Uri expectedTimeIntervalUri =
                ScheduleContract.Sessions.buildUnscheduledSessionsInInterval(slotStart,
                        slotStart + MyScheduleMockItems.SESSION_AVAILABLE_SLOT_TIME_DURATION);
        intended(allOf(
                hasAction(equalTo(Intent.ACTION_VIEW)),
                hasData(expectedTimeIntervalUri)));
    }

    @Test
    @Suppress // Test isn't deterministic when run as part of the full test suite.
    public void viewDay2_clickOnMoreButton_opensSessionsListScreen() {
        mActivityStubContext.setActivityContext(mActivityRule.getActivity());

        // When clicking on the time of the more button next to a time slot
        long slotStart = Config.CONFERENCE_START_MILLIS + 1 * TimeUtils.DAY;
        onView(allOf(isDisplayed(), withId(R.id.more), hasSibling(
                withText(TimeUtils.formatShortTime(mActivityStubContext, new Date(slotStart))))))
                .perform(click());

        // Then the intent for the sessions list screen is fired
        Uri expectedTimeIntervalUri =
                ScheduleContract.Sessions.buildUnscheduledSessionsInInterval(slotStart,
                        slotStart + MyScheduleMockItems.SESSION_AVAILABLE_SLOT_TIME_DURATION);
        intended(allOf(
                hasAction(equalTo(Intent.ACTION_VIEW)),
                hasData(expectedTimeIntervalUri)));
    }

    @Test
    @Suppress // Test isn't deterministic when run as part of the full test suite.
    public void timeSlotWithNoSessionInSchedule_MoreButton_IsNotVisible() {
        mActivityStubContext.setActivityContext(mActivityRule.getActivity());

        // More button is not visible for a time slow with no sessions in schedule
        long slotStartWithAvailableSessionsButNoneInSchedule =
                Config.CONFERENCE_START_MILLIS + 1 * TimeUtils.DAY +
                        MyScheduleMockItems.SESSION_AVAILABLE_SLOT_TIME_OFFSET;
        onView(allOf(withId(R.id.more), hasSibling(
                withText(TimeUtils.formatShortTime(mActivityStubContext,
                        new Date(slotStartWithAvailableSessionsButNoneInSchedule))))))
                .check(matches(not(isDisplayed())));
    }

    @Test
    @Suppress // Test isn't deterministic when run as part of the full test suite.
    public void timeSlotWithOneSessionInSchedule_MoreButton_IsVisible() {
        mActivityStubContext.setActivityContext(mActivityRule.getActivity());

        // More button is visible for a time slow with 1 session in schedule
        long slotStartWithOneSessionInSchedule =
                Config.CONFERENCE_START_MILLIS +
                        MyScheduleMockItems.SESSION_TITLE_AFTER_START_OFFSET;
        onView(allOf(isDisplayed(), withId(R.id.more), hasSibling(
                withText(TimeUtils.formatShortTime(mActivityStubContext,
                        new Date(slotStartWithOneSessionInSchedule))))))
                .check(matches(isDisplayed()));
    }

    @Test
    public void viewDay1_sessionVisible() {
        // Given day 1 visible
        showDay(1);

        // Then the session in the first day is displayed
        onView(withText(MyScheduleMockItems.SESSION_TITLE_AFTER)).check(matches(isDisplayed()));
    }

    @Test
    @Suppress // Test isn't deterministic when run as part of the full test suite.
    public void viewDay2_sessionVisible() {
        // Given day 2 visible

        // Then the session in the second day is displayed
        onView(withText(MyScheduleMockItems.SESSION_TITLE_BEFORE)).check(matches(isDisplayed()));
    }

    @Test
    public void navigationIcon_DisplaysAsMenu() {
        NavigationUtils.checkNavigationIconIsMenu();
    }

    @Test
    public void navigationIcon_OnClick_NavigationDisplayed() {
        NavigationUtils.checkNavigationIsDisplayedWhenClickingMenuIcon();
    }

    @Test
    public void navigation_WhenShown_CorrectItemIsSelected() {
        NavigationUtils
                .checkNavigationItemIsSelected(NavigationModel.NavigationItemEnum.MY_SCHEDULE);
    }

    /**
     * This test works only on phones, where the layout is the same for both orientations (ie tabs)
     */
    @Test
    @Suppress // Test isn't deterministic when run as part of the full test suite.
    public void orientationChange_RetainsDataAndCurrentTab_Flaky() {
        // Given day 2 visible
        showDay(2);
        onView(withText(MyScheduleMockItems.SESSION_TITLE_BEFORE)).check(matches(isDisplayed()));

        // When changing orientation
        OrientationHelper.rotateOrientation(mActivityRule);

        // Then day 2 is visible
        onView(withText(MyScheduleMockItems.SESSION_TITLE_BEFORE)).check(matches(isDisplayed()));
        // And day 0 is selectable and visible
        showDay(0);
        onView(withText(R.string.my_schedule_badgepickup)).check(matches(isDisplayed()));
        // And day 1 is selectable and visible
        showDay(1);
        onView(withText(MyScheduleMockItems.SESSION_TITLE_AFTER)).check(matches(isDisplayed()));

        // When changing orientation again
        OrientationHelper.rotateOrientation(mActivityRule);

        // Then day 1 is visible
        onView(withText(MyScheduleMockItems.SESSION_TITLE_AFTER)).check(matches(isDisplayed()));
        // And day 0 is selectable and visible
        showDay(0);
        onView(withText(R.string.my_schedule_badgepickup)).check(matches(isDisplayed()));
        // And day 2 is selectable and visible
        showDay(2);
        onView(withText(MyScheduleMockItems.SESSION_TITLE_BEFORE)).check(matches(isDisplayed()));
    }

    @Test
    public void newDataObtained_DataUpdated() {
        IdlingResource idlingResource = null;
        try {
            // Given initial data displayed
            showDay(2);
            onView(withText(MyScheduleMockItems.SESSION_TITLE_BEFORE))
                    .check(matches(isDisplayed()));
            onView(withText(MyScheduleMockItems.SESSION_TITLE_2)).check(doesNotExist());
            showDay(1);
            onView(withText(MyScheduleMockItems.SESSION_TITLE_AFTER)).check(matches(isDisplayed()));
            onView(withText(MyScheduleMockItems.SESSION_TITLE_1)).check(doesNotExist());

            // When new data is available
            mStubMyScheduleModel.setMockScheduleDataDay1(MyScheduleMockItems
                    .getItemsForAttendee(1, false, MyScheduleMockItems.SESSION_TITLE_1));
            mStubMyScheduleModel.setMockScheduleDataDay2(MyScheduleMockItems
                    .getItemsForAttendee(2, false, MyScheduleMockItems.SESSION_TITLE_2));
            mStubMyScheduleModel.fireContentObserver();
            // Wait for the ThrottleContentObserver to process the event
            idlingResource = new ThrottleContentObserverIdlingResource(
                    InstrumentationRegistry.getTargetContext());
            Espresso.registerIdlingResources(idlingResource);

            // Then the new data is shown
            onView(withText(MyScheduleMockItems.SESSION_TITLE_1)).check(matches(isDisplayed()));
            onView(withText(MyScheduleMockItems.SESSION_TITLE_AFTER))
                    .check(doesNotExist());
            showDay(2);
            onView(withText(MyScheduleMockItems.SESSION_TITLE_2)).check(matches(isDisplayed()));
            onView(withText(MyScheduleMockItems.SESSION_TITLE_BEFORE))
                    .check(doesNotExist());
        } finally {
            if (idlingResource != null) {
                Espresso.unregisterIdlingResources(idlingResource);
            }
        }
    }

    private void showDay(int day) {
        onView(withId(MyScheduleActivity.BASE_TAB_VIEW_ID + day)).perform(click());
    }


}
