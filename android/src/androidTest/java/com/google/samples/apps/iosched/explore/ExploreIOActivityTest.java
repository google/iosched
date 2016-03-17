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
import android.support.test.espresso.ViewInteraction;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.mockdata.ExploreMockCursor;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.session.SessionDetailActivity;
import com.google.samples.apps.iosched.testutils.BaseActivityTestRule;
import com.google.samples.apps.iosched.testutils.MatchersHelper;
import com.google.samples.apps.iosched.ui.SearchActivity;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasData;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.Is.is;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class ExploreIOActivityTest {

    @Rule
    public BaseActivityTestRule<ExploreIOActivity> mActivityRule =
            new BaseActivityTestRule<ExploreIOActivity>(ExploreIOActivity.class,
                    new StubExploreIOModel(
                            InstrumentationRegistry.getTargetContext(),
                            ExploreMockCursor.getCursorForExplore(),
                            ExploreMockCursor.getCursorForTags()), true);

    @Test
    public void Keynote_IsVisibleWithoutScrolling() {
        onView(withText(ExploreMockCursor.TITLE_KEYNOTE)).check(matches(isDisplayed()));
    }

    @Test
    public void HeaderBar_InitiallyDisplayed() {
        onView(withId(R.id.headerbar)).check(matches(isCompletelyDisplayed()));
    }

    @Test
    public void headerBar_HidesAfterSwipeUp() {
        ViewInteraction view = onView(withId(R.id.headerbar));

        // Swiping up should hide the header bar.
        onView(withId(R.id.explore_collection_view)).perform(swipeUp());
        onView(withId(R.id.explore_collection_view)).perform(swipeUp());

        // Check if the header bar is hidden.
        view.check(matches(not(isDisplayed())));
    }

    @Test
    public void SessionClicked_IntentFired() {
        // Given a visible session
        onView(withText(ExploreMockCursor.TITLE_KEYNOTE)).check(matches(isDisplayed()));

        // When clicking on the session
        onView(withText(ExploreMockCursor.TITLE_KEYNOTE)).perform(click());

        // Then the intent to open the session detail activity for that session is fired
        Uri expectedSessionUri =
                ScheduleContract.Sessions.buildSessionUri(ExploreMockCursor.KEYNOTE_ID);
        intended(allOf(
                hasComponent(SessionDetailActivity.class.getName()),
                hasData(expectedSessionUri)));
    }

    @Test
    public void MoreClicked_IntentFired() {
        // Given a visible topic
        onView(withText(ExploreMockCursor.TOPIC_TOOLS)).check(matches(isDisplayed()));

        // When clicking on the "More" button
        onView(allOf(withText(R.string.more_items_button),
                hasSibling(withText(ExploreMockCursor.TOPIC_TOOLS)))).perform(click());

        // Then the intent to open the explore sessions activity for that topic is fired
        intended(allOf(
                hasComponent(ExploreSessionsActivity.class.getName()),
                hasExtra(ExploreSessionsActivity.EXTRA_FILTER_TAG,
                        ExploreMockCursor.TOPIC_TOOLS)));

    }

    @Test
    public void SearchClicked_IntentFired() {
        // When clicking on the "search" button
        onView(withId(R.id.menu_search)).perform(click());

        // Then the intent to open the search activity is fired
        intended(hasComponent(SearchActivity.class.getName()));
    }

    @Test
    public void SeveralSessionsAvailableForTopic_TwoShown() {
        // There are 4 sessions with main topic tools, only 2 in the section topic tools should
        // be visible
        Matcher<View> parentViewMatcher = withChild(
                withChild(withText(ExploreMockCursor.TOPIC_TOOLS)));
        assertThat(MatchersHelper.getNumberOfDescendantsForViewGroupDescendant(
                parentViewMatcher, R.id.title), is(2));
    }
}
