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

import android.app.Activity;
import android.app.Instrumentation;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.intent.Intents;
import android.support.test.espresso.intent.matcher.IntentMatchers;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.filters.FlakyTest;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.explore.ExploreSessionsActivity;
import com.google.samples.apps.iosched.feedback.SessionFeedbackActivity;
import com.google.samples.apps.iosched.map.MapActivity;
import com.google.samples.apps.iosched.mockdata.SessionsMockCursor;
import com.google.samples.apps.iosched.mockdata.SpeakersMockCursor;
import com.google.samples.apps.iosched.mockdata.TagMetadataMockCursor;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.testutils.BaseActivityTestRule;
import com.google.samples.apps.iosched.util.TimeUtils;

import org.hamcrest.CoreMatchers;
import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasData;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.core.IsNot.not;

/**
 * Tests for {@link SessionDetailActivity} when showing a session that is not the keynote and that
 * is in user schedule, and that is ready for feedback submission. The session has no you tube url
 * and no livestream.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SessionDetailActivity_InScheduleSessionTest {

    public static final String SESSION_ID = "5b7836c8-82bf-e311-b297-00155d5066d7";

    private Uri mSessionUri = ScheduleContract.Sessions.buildSessionUri(SESSION_ID);;

    @Rule
    public BaseActivityTestRule<SessionDetailActivity> mActivityRule =
            new BaseActivityTestRule<SessionDetailActivity>(SessionDetailActivity.class,
                    // Create a stub model to simulate a session in schedule
                    new StubSessionDetailModel(mSessionUri,
                            InstrumentationRegistry.getTargetContext(),
                            SessionsMockCursor.getCursorForSessionInSchedule(),
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
        // Set up time to 5 minutes after end of session
        long timeDiff = SessionsMockCursor.END_SESSION - Config.CONFERENCE_START_MILLIS
                + 5 * TimeUtils.MINUTE;
        TimeUtils.setCurrentTimeRelativeToStartOfConference(
                InstrumentationRegistry.getTargetContext(), timeDiff);
    }

    @Test
    public void sessionTitle_ShowsCorrectTitle() {
        onView(withId(R.id.session_title)).check(matches(
                allOf(withText(SessionsMockCursor.FAKE_TITLE), isDisplayed())));
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
    public void feedbackCard_IsVisible() {
        onView(withId(R.id.give_feedback_card)).check(matches(isDisplayed()));
    }

    @Test
    @FlakyTest // due to intent checking
    public void submitFeedback_WhenClicked_IntentFired_Flaky() {
        // When clicking on the submit feedback button
        onView(allOf(withText(R.string.give_feedback),
                isDescendantOfA(withId(R.id.give_feedback_card)))).perform(click());

        // Then the intent to open the feedback activity for that session is fired
        intended(CoreMatchers.allOf(
                hasComponent(SessionFeedbackActivity.class.getName()),
                hasAction(equalTo(Intent.ACTION_VIEW)),
                hasData(mSessionUri)));
    }

    @Test
    @FlakyTest
    public void showMap_WhenClicked_IntentFired_Flaky() {
        // When clicking on the room button
        onView(withId(R.id.menu_map_room)).perform(click());

        // Then the intent to open the map activity for that room is fired
        intended(CoreMatchers.allOf(
                hasComponent(MapActivity.class.getName()),
                hasExtra(MapActivity.EXTRA_ROOM, SessionsMockCursor.FAKE_ROOM_ID)));
    }

    @Test
    public void showShare_WhenClicked_IntentFired() {
        // When clicking on the room button
        onView(withId(R.id.menu_share)).perform(click());

        // Then the intent to open the share intent chooser for that session is fired
        intended(hasAction(equalTo(Intent.ACTION_CHOOSER)));
    }

    @Test
    public void tag_OnClick_IntentFired() {
        // When clicking on tag
        onView(withText(SessionsMockCursor.FAKE_TAG)).perform(scrollTo());
        onView(withText(SessionsMockCursor.FAKE_TAG)).perform(click());

        // Then the intent to show the explore sessions for the tag is shown
        intended(CoreMatchers.allOf(
                hasComponent(ExploreSessionsActivity.class.getName()),
                hasExtra(ExploreSessionsActivity.EXTRA_FILTER_TAG,
                        TagMetadataMockCursor.TAG_ID_NAME)));
    }

    @Test
    public void liveStreamedText_IsNotVisible() {
        onView(withText(R.string.session_live_streamed)).check(matches(not(isDisplayed())));
    }

    @Test
    public void watchText_IsNotVisible() {
        onView(withId(R.id.watch)).check(matches(not(isDisplayed())));
    }

    @Test
    public void headerImage_IsNotVisible() {
        onView(withId(R.id.session_photo)).check(matches(not(isDisplayed())));
    }

    @Test
    public void speakerImage_WhenClicked_IntentFired() {
        Intent resultData = new Intent();
        resultData.putExtras(new Bundle());

        // Create the ActivityResult with the Intent.
        Intents.intending(CoreMatchers.not(IntentMatchers.isInternal())).respondWith(
                new Instrumentation.ActivityResult(Activity.RESULT_OK, resultData));

        // When clicking on speaker image
        onView(withId(R.id.session_detail_frag)).perform(swipeUp());
        onView(withId(R.id.speaker_image)).perform(click());

        // Then the intent to display the speaker url is fired
        Uri expectedSpeakerUri = Uri.parse(SpeakersMockCursor.FAKE_SPEAKER_URL);
        intended(CoreMatchers.allOf(
                hasAction(IsEqual.equalTo(Intent.ACTION_VIEW)),
                hasData(expectedSpeakerUri)));
    }


}
