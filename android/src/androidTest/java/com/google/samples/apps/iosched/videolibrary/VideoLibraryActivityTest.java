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

import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.FlakyTest;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.mockdata.VideosMockCursor;
import com.google.samples.apps.iosched.navigation.NavigationModel;
import com.google.samples.apps.iosched.testutils.BaseActivityTestRule;
import com.google.samples.apps.iosched.testutils.MatchersHelper;
import com.google.samples.apps.iosched.testutils.NavigationUtils;
import com.google.samples.apps.iosched.testutils.ToolbarUtils;
import com.google.samples.apps.iosched.testutils.IntentUtils;

import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.intent.Intents.intended;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static android.support.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static android.support.test.espresso.matcher.ViewMatchers.hasSibling;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withChild;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.junit.Assert.assertTrue;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class VideoLibraryActivityTest {

    @Rule
    public BaseActivityTestRule<VideoLibraryActivity> mActivityRule =
            new BaseActivityTestRule<VideoLibraryActivity>(VideoLibraryActivity.class,
                    new StubVideoLibraryModel(
                            InstrumentationRegistry.getTargetContext(),
                            VideosMockCursor.getCursorForVideos(),
                            VideosMockCursor.getCursorForFilter()), true);

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

        // Then the intent to open the video is fired
        IntentUtils.checkVideoIntentIsFired(VideosMockCursor.VIDEO_YOUTUBE_LINK,
                mActivityRule.getActivity(), true);
    }

    @Test
    @FlakyTest // Getting memory errors sometimes with vector drawable allocation
    public void videosList_DifferentVideosOnSecondTime_Flaky() {
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
    public void toolbar_IsInitiallyDisplayed() {
        ToolbarUtils.checkToolbarIsCompletelyDisplayed();
    }

    @Test
    @FlakyTest
    public void toolbar_HidesAfterSwipeUp_Flaky() {
        ToolbarUtils.checkToolbarHidesAfterSwipingRecyclerViewUp(R.id.videos_card_list);
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
                .checkNavigationItemIsSelected(NavigationModel.NavigationItemEnum.VIDEO_LIBRARY);
    }
}
