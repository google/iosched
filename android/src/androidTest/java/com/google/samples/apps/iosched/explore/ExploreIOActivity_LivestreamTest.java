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

package com.google.samples.apps.iosched.explore;

import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.mockdata.ExploreMockCursor;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.testutils.BaseActivityTestRule;
import com.google.samples.apps.iosched.testutils.MatchersHelper;
import com.google.samples.apps.iosched.util.TimeUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasData;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;

/**
 * UI tests for {@link ExploreIOActivity} when several livestreams are available.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ExploreIOActivity_LivestreamTest {

    private long mNow = TimeUtils.getCurrentTime(InstrumentationRegistry.getTargetContext());

    @Rule
    public BaseActivityTestRule<ExploreIOActivity> mActivityRule =
            new BaseActivityTestRule<ExploreIOActivity>(ExploreIOActivity.class,
                    new StubExploreIOModel(
                            InstrumentationRegistry.getTargetContext(),
                            ExploreMockCursor.getCursorForLivestreamSessions(mNow),
                            ExploreMockCursor.getCursorForTags()), true);

    @Test
    public void firstTwoLivestreamSessions_AreVisible() {
        // The livestream card is visible
        onView(withText(R.string.live_now)).check(matches(isDisplayed()));

        // Scroll to the livestream card to ensure it is fully visible
        onView(withId(R.id.explore_card_list))
                .perform(RecyclerViewActions.scrollTo(
                        hasDescendant(withText(R.string.live_now))));

        // The first 2 sessions are visible
        onView(withText(ExploreMockCursor.TRACK_TOOLS_TITLE1)).check(matches(isDisplayed()));
        onView(withText(ExploreMockCursor.TRACK_ANDROID_TITLE1)).check(matches(isDisplayed()));
    }

    @Test
    public void livestreamCard_TapMoreButton_CorrectIntentFired() {
        // When click on move button of livestream card
        onView(allOf(withText(R.string.more_items_button),
                hasSibling(withText(R.string.live_now))
        )).perform(click());

        // Then the intent to open ExploreSessionsActivity for all sessions after the current
        // time with livestream available is fired
        Uri expectedSessionsUri =
                ScheduleContract.Sessions.buildSessionsAfterUri(mNow);
        intended(allOf(
                hasExtra(ExploreSessionsActivity.EXTRA_SHOW_LIVE_STREAM_SESSIONS, true),
                hasData(MatchersHelper.approximateTimeUriMatcher(expectedSessionsUri)),
                hasComponent(ExploreSessionsActivity.class.getName())));
    }
}
