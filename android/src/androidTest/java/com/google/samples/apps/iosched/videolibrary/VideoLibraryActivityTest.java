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
import android.support.test.filters.LargeTest;
import android.support.test.filters.Suppress;
import android.support.test.runner.AndroidJUnit4;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.mockdata.VideosMockCursor;
import com.google.samples.apps.iosched.navigation.NavigationModel;
import com.google.samples.apps.iosched.testutils.BaseActivityTestRule;
import com.google.samples.apps.iosched.testutils.NavigationUtils;
import com.google.samples.apps.iosched.testutils.ToolbarUtils;
import com.google.samples.apps.iosched.testutils.IntentUtils;

import org.hamcrest.Matcher;
import org.junit.Ignore;
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
    public void videosList_VideoWithNullTopicNotShown() {
        onView(withText(VideosMockCursor.VIDEO_TITLE_NULL_TOPIC)).check(doesNotExist());
    }

    @Test
    public void toolbar_IsInitiallyDisplayed() {
        ToolbarUtils.checkToolbarIsCompletelyDisplayed();
    }

    @Test
    @Suppress // TODO(b/30123797): Manual testing shows odd behavior when slow-scrolling up. This
            // needs to be researched, but this scroll hiding functionality is provided by the
            // coordinator layout so this might be a support lib bug or a nuance in the flags
            // supplied in toolbar_autohide.xml
    public void toolbar_HidesAfterSwipeUp() {
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
