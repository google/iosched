/*
 * Copyright (c) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.samples.apps.iosched.feedback;

import android.content.Intent;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.FlakyTest;
import android.support.test.filters.LargeTest;
import android.support.test.filters.Suppress;
import android.support.test.runner.AndroidJUnit4;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.mockdata.SessionsMockCursor;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.testutils.BaseActivityTestRule;
import com.google.samples.apps.iosched.testutils.NavigationUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.scrollTo;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.assertTrue;

/**
 * Tests for {@link SessionFeedbackActivity}.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class SessionFeedbackActivityTest {

    public static final String SESSION_ID = "5b7836c8-82bf-e311-b297-00155d5066d7";

    private Uri mSessionUri = ScheduleContract.Sessions.buildSessionUri(SESSION_ID);

    @Rule
    public BaseActivityTestRule<SessionFeedbackActivity> mActivityRule =
            new BaseActivityTestRule<SessionFeedbackActivity>(SessionFeedbackActivity.class,
                    new StubSessionFeedbackModel(mSessionUri,
                            InstrumentationRegistry.getTargetContext(),
                            SessionsMockCursor.getCursorForSessionFeedback(),
                            new FeedbackHelper(InstrumentationRegistry.getTargetContext())), true) {
                @Override
                protected Intent getActivityIntent() {
                    // Create intent to load the session.
                    return new Intent(Intent.ACTION_VIEW, mSessionUri);
                }
            };

    @Test
    public void sessionTitle_ShowsCorrectTitle() {
        onView(withId(R.id.feedback_header_session_title)).check(matches(
                allOf(withText(SessionsMockCursor.FAKE_TITLE), isDisplayed())));
    }

    @Test
    public void sessionRating_ShowsRating() {
        onView(withId(R.id.rating_bar_0)).check(matches(isDisplayed()));
    }

    @Test
    public void feedbackQuestions_ShowsAllQuestionsWhenScrolling() {
        onView(withText(R.string.session_feedback_relevance)).perform(scrollTo());
        onView(withText(R.string.session_feedback_relevance)).check(matches(isDisplayed()));
        onView(withId(R.id.session_relevant_feedback_bar)).check(matches(isDisplayed()));
        onView(withText(R.string.session_feedback_content_label_text)).perform(scrollTo());
        onView(withText(R.string.session_feedback_content_label_text))
                .check(matches(isDisplayed()));
        onView(withId(R.id.content_feedback_bar)).check(matches(isDisplayed()));
        onView(withText(R.string.session_feedback_speaker_quality)).perform(scrollTo());
        onView(withText(R.string.session_feedback_speaker_quality)).check(matches(isDisplayed()));
        onView(withId(R.id.speaker_feedback_bar)).check(matches(isDisplayed()));
        onView(withText(R.string.session_feedback_submitlink)).perform(scrollTo());
        onView(withText(R.string.session_feedback_submitlink)).check(matches(isDisplayed()));
    }

    @Test
    @Suppress // Checking activity has been destroyed doesn't always work
    public void clickOnSubmit_ActivityCloses() {
        // Whether we have feedback data or not, the activity closes upon submitting
        onView(withText(R.string.session_feedback_submitlink)).perform(scrollTo());
        onView(withText(R.string.session_feedback_submitlink)).check(matches(isDisplayed()));
        onView(withText(R.string.session_feedback_submitlink)).perform(click());
        assertTrue(mActivityRule.getActivity().isDestroyed());
    }

    @Test
    public void navigationIcon_DisplaysAsUp() {
        NavigationUtils.checkNavigationIconIsUp();
    }

}
