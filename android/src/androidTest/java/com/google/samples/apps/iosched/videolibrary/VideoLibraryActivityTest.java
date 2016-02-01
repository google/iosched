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

package com.google.samples.apps.iosched.videolibrary;

import android.content.Intent;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.intent.rule.IntentsTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.injection.ModelProvider;
import com.google.samples.apps.iosched.mockdata.VideosMockCursor;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.testutils.MatchersHelper;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasData;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class VideoLibraryActivityTest {

    @Rule
    public IntentsTestRule<VideoLibraryActivity> mActivityRule =
            new IntentsTestRule<VideoLibraryActivity>(VideoLibraryActivity.class) {
                @Override
                protected void beforeActivityLaunched() {
                    // Make sure the EULA screen is not shown.
                    SettingsUtils.markTosAccepted(InstrumentationRegistry.getTargetContext(), true);

                    // Create a stub model to simulate a keynote session
                    ModelProvider.setStubVideoLibraryModel(new StubVideoLibraryModel(
                            InstrumentationRegistry.getTargetContext(),
                            VideosMockCursor.getCursorForVideos(),
                            VideosMockCursor.getCursorForFilter()));
                }
            };

    @Test
    public void videosList_TopicViewMoreClicked_IntentFired() {
        // Given a visible topic
        onView(withText(VideosMockCursor.VIDEO_TOPIC1)).check(matches(isDisplayed()));

        // When clicking on the "More" button
        onView(allOf(withText(R.string.more_items_button),
                hasSibling(withText(VideosMockCursor.VIDEO_TOPIC1)))).perform(click());

        // Then the intent to open the filtered activity for that topic is fired
        intended(allOf(
                hasComponent(VideoLibraryFilteredActivity.class.getName()),
                hasExtra(VideoLibraryFilteredActivity.KEY_FILTER_TOPIC,
                        VideosMockCursor.VIDEO_TOPIC1)));
    }

    @Test
    public void videosList_VideoClicked_IntentFired() {
        // Given a visible video
        onView(withText(VideosMockCursor.VIDEO_TITLE1)).check(matches(isDisplayed()));

        // When clicking on the video
        onView(withText(VideosMockCursor.VIDEO_TITLE1)).perform(click());

        // Then the intent to open the filtered activity for that topic is fired
        Uri expectedVideoUri = Uri.parse(String.format(Locale.US, Config.VIDEO_LIBRARY_URL_FMT,
                VideosMockCursor.VIDEO_YOUTUBE_LINK));
        intended(allOf(
                hasAction(equalTo(Intent.ACTION_VIEW)),
                hasData(expectedVideoUri)));
    }

    @Test
    public void videosList_DifferentVideosOnSecondTime() {
        // Given a list of videos shown for topic 2
        // The first child of the LinearLayout is used to show the topic title and the more
        // button, so the child view corresponding to the first video is at index 1
        int indexOffsetForFirstRow = 1;

        Matcher<View> parentViewMatcher =
                withChild(withChild(withText(VideosMockCursor.VIDEO_TOPIC2)));
        int resourceIdOfChildTextView = R.id.title;

        String[] titles = new String[5];
        for (int i = 0; i < titles.length; i++) {
            titles[i] = MatchersHelper
                    .getTextForViewGroupDescendant(
                            parentViewMatcher,
                            i + indexOffsetForFirstRow, resourceIdOfChildTextView);
        }

        // When leaving the screen then returning to it
        onView(withText(VideosMockCursor.VIDEO_TOPIC1)).perform(click());
        onView(withContentDescription(InstrumentationRegistry.getTargetContext()
                                                             .getString(
                                                                     R.string.close_and_go_back)))
                .perform(click());

        // Then the videos for topic 2 titles are in different order
        String[] newTitles = new String[5];
        for (int i = 0; i < newTitles.length; i++) {
            newTitles[i] = MatchersHelper
                    .getTextForViewGroupDescendant(
                            parentViewMatcher,
                            i + indexOffsetForFirstRow, resourceIdOfChildTextView);
        }

        boolean atLeastOneTitleIsDifferent = false;
        for (int i = 0; i < titles.length; i++) {
            if (!titles[i].equals(newTitles[i])) {
                atLeastOneTitleIsDifferent = true;
                break;
            }
        }
        assertTrue(atLeastOneTitleIsDifferent);
    }

    @Test
    public void videosList_VideoWithNullTopicNotShown() {
        onView(withText(VideosMockCursor.VIDEO_TITLE_NULL_TOPIC)).check(doesNotExist());
    }

    @Test
    public void headerBar_IsInitiallyDisplayed() {
        onView(withId(R.id.headerbar)).check(matches(isCompletelyDisplayed()));
    }

    @Test
    public void headerBar_HidesAfterSwipeUp() {
        ViewInteraction view = onView(withId(R.id.headerbar));

        // Swiping up should hide the header bar.
        onView(withId(R.id.videos_collection_view)).perform(swipeUp());
        onView(withId(R.id.videos_collection_view)).perform(swipeUp());

        // Check if the header bar is hidden.
        view.check(matches(not(isDisplayed())));
    }
}
