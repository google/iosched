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
package com.google.samples.apps.iosched.session;

import android.content.Intent;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.runner.AndroidJUnit4;
import android.test.FlakyTest;
import android.test.suitebuilder.annotation.LargeTest;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.mockdata.SessionsMockCursor;
import com.google.samples.apps.iosched.mockdata.SpeakersMockCursor;
import com.google.samples.apps.iosched.mockdata.TagMetadataMockCursor;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.testutils.BaseActivityTestRule;
import com.google.samples.apps.iosched.util.TimeUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.core.IsNot.not;

/**
 * Tests for {@link SessionDetailActivity} when showing a session that is starting in 9 minutes with
 * a livestream.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SessionDetailActivity_LiveSessionIn9MinutesTest {
    public static final String SESSION_ID = "5b7836c8-82bf-e311-b297-00155d5066d7";

    private Uri mSessionUri = ScheduleContract.Sessions.buildSessionUri(SESSION_ID);

    @Rule
    public BaseActivityTestRule<SessionDetailActivity> mActivityRule =
            new BaseActivityTestRule<SessionDetailActivity>(SessionDetailActivity.class,
                    // Create a stub model to simulate a live session
                    new StubSessionDetailModel(mSessionUri,
                            InstrumentationRegistry.getTargetContext(),
                            SessionsMockCursor.getCursorForSessionWithLiveStream(),
                            SpeakersMockCursor.getCursorForSingleSpeaker(),
                            TagMetadataMockCursor.getCursorForSingleTagMetadata()), true) {
                @Override
                protected Intent getActivityIntent() {
                    // Create intent to load the session.
                    return new Intent(Intent.ACTION_VIEW, mSessionUri);
                }
            };

    @Before
    public void setTime() {
        // Set up time to 9 minutes before start of session
        long timeDiff = SessionsMockCursor.START_SESSION - Config.CONFERENCE_START_MILLIS
                - 9 * TimeUtils.MINUTE;
        TimeUtils.setCurrentTimeRelativeToStartOfConference(
                InstrumentationRegistry.getTargetContext(), timeDiff);
    }

    @Test
    public void sessionTitle_ShowsCorrectTitle() {
        onView(withId(R.id.session_title)).check(matches(
                allOf(withText(SessionsMockCursor.FAKE_TITLE), isDisplayed())));
    }

    @Test
    public void liveStreamedText_IsVisible() {
        onView(withText(R.string.session_live_streamed)).check(matches(isDisplayed()));
    }

    /**
     * TODO For an unknown reason, this test passes when run on its own or when whole class is run,
     * but it fails when tests are run at package level.
     */
    @Test
    @FlakyTest
    public void watchLiveText_IsVisible_Flaky() {
        onView(withId(R.id.watch)).check(matches(isDisplayed()));
        onView(withText(R.string.session_watch_live)).check(matches(isDisplayed()));
    }

    @Test
    public void headerImage_IsVisible() {
        onView(withId(R.id.session_photo)).check(matches(isDisplayed()));
    }

    @Test
    public void speakersSection_IsVisible() {
        onView(withId(R.id.session_detail_frag)).perform(swipeUp());
        onView(withId(R.id.session_speakers_block)).check(matches(isDisplayed()));
    }

    @Test
    public void tagSection_IsVisible() {
        onView(withId(R.id.session_tags_container)).check(matches(withEffectiveVisibility(
                ViewMatchers.Visibility.VISIBLE)));
    }

    @Test
    public void feedbackCard_IsNotVisible() {
        onView(withId(R.id.give_feedback_card)).check(matches(not(isDisplayed())));
    }

}
