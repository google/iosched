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
import android.test.suitebuilder.annotation.LargeTest;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.mockdata.VideosMockCursor;
import com.google.samples.apps.iosched.testutils.BaseActivityTestRule;
import com.google.samples.apps.iosched.testutils.NavigationUtils;
import com.google.samples.apps.iosched.testutils.ToolbarUtils;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.containsString;


@RunWith(AndroidJUnit4.class)
@LargeTest
public class VideoLibraryFilteredActivityTest {

    @Rule
    public BaseActivityTestRule<VideoLibraryFilteredActivity> mActivityRule =
            new BaseActivityTestRule<VideoLibraryFilteredActivity>(
                    VideoLibraryFilteredActivity.class,
                    new StubVideoLibraryModel(
                            InstrumentationRegistry.getTargetContext(),
                            VideosMockCursor.getCursorForVideos(),
                            VideosMockCursor.getCursorForFilter()), true);

    @Test
    public void yearsSelector_HasYears() {
        // When filter panel is displayed
        onView(withId(R.id.menu_filter)).perform(click());

        // Then there are some years to select from.
        onView(withId(R.id.years_radio_group)).check(matches(
                hasDescendant(withText(containsString(VideosMockCursor.VIDEO_2014)))));
        onView(withId(R.id.years_radio_group)).check(matches(
                hasDescendant(withText(containsString(VideosMockCursor.VIDEO_2015)))));
    }

    @Test
    public void topicsSelector_HasTopics() {
        // When filter panel is displayed
        onView(withId(R.id.menu_filter)).perform(click());

        // Then there are some topics to select from.
        onView(withId(R.id.topics_radio_group)).check(matches(
                hasDescendant(withText(containsString(VideosMockCursor.VIDEO_TOPIC1)))));
        onView(withId(R.id.topics_radio_group)).check(matches(
                hasDescendant(withText(containsString(VideosMockCursor.VIDEO_TOPIC2)))));
    }

    @Test
    public void videosList_InitiallyHasClickableVideos() {
        onView(withId(R.id.videos_list)).check(matches(hasDescendant(isClickable())));
    }

    @Test
    public void toolbar_IsInitiallyDisplayed() {
        ToolbarUtils.checkToolbarIsCompletelyDisplayed();
    }

    @Test
    public void navigationIcon_DisplaysAsUp() {
        NavigationUtils.checkNavigationIconIsUp();
    }
}
