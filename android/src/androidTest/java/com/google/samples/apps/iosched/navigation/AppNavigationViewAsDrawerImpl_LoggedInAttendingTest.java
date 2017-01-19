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

package com.google.samples.apps.iosched.navigation;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.explore.ExploreIOActivity;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.testutils.LoginUtils;
import com.google.samples.apps.iosched.testutils.NavigationUtils;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

/**
 * UI tests for {@link AppNavigationViewAsDrawerImpl} when the user is logged in and attending the
 * conference.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AppNavigationViewAsDrawerImpl_LoggedInAttendingTest {
    private String mAccountName;

    @Rule
    public ActivityTestRule<ExploreIOActivity> mActivityRule =
            new ActivityTestRule<ExploreIOActivity>(ExploreIOActivity.class) {

                @Override
                protected void beforeActivityLaunched() {
                    // Make sure the EULA screen is not shown.
                    SettingsUtils.markTosAccepted(InstrumentationRegistry.getTargetContext(), true);

                    // Set user as logged in and attending
                    SettingsUtils
                            .setAttendeeAtVenue(InstrumentationRegistry.getTargetContext(), true);
                    mAccountName = LoginUtils.setFirstAvailableAccountAsActive(
                            InstrumentationRegistry.getTargetContext());
                }
            };

    @After
    public void cleanUpActivityStack() {
        NavigationUtils.cleanUpActivityStack(mActivityRule);
    }

    @Test
    public void accountName_IsDisplayed() {
        // Given navigation menu
        NavigationUtils.showNavigation();

        // Then the account name is shown
        onView(withText(mAccountName)).check(matches(isDisplayed()));
    }

    @Test
    public void mySchedule_WhenClicked_ActivityDisplayed() {
        NavigationUtils.clickOnNavigationItemAndCheckActivityIsDisplayed(
                R.string.navdrawer_item_my_schedule,
                R.string.title_my_schedule);
    }

    @Test
    public void explore_WhenClicked_ActivityDisplayed() {
        // Move to a different screen first
        NavigationUtils.clickOnNavigationItemAndCheckActivityIsDisplayed(R.string.description_about,
                R.string.title_about);

        NavigationUtils.clickOnNavigationItemAndCheckActivityIsDisplayed(
                R.string.navdrawer_item_explore,
                R.string.title_explore);
    }

    @Test
    public void social_NotDisplayed() {
        NavigationUtils.checkNavigationItemIsNotDisplayed(R.string.navdrawer_item_social);
    }

    @Test
    public void about_WhenClicked_ActivityDisplayed() {
        NavigationUtils.clickOnNavigationItemAndCheckActivityIsDisplayed(R.string.description_about,
                R.string.title_about);
    }

    @Test
    public void videoLibrary_WhenClicked_ActivityDisplayed() {
        NavigationUtils.clickOnNavigationItemAndCheckActivityIsDisplayed(
                R.string.navdrawer_item_video_library,
                R.string.title_video_library);
    }

    @Test
    public void settings_WhenClicked_ActivityDisplayed() {
        NavigationUtils.clickOnNavigationItemAndCheckActivityIsDisplayed(
                R.string.navdrawer_item_settings,
                R.string.title_settings);
    }

    @Test
    public void map_WhenClicked_ActivityDisplayed() {
        NavigationUtils.clickOnNavigationItemAndCheckActivityIsDisplayed(
                R.string.navdrawer_item_map,
                R.string.title_map);
    }

    @Test
    public void ioLive_NotDisplayed() {
        NavigationUtils.checkNavigationItemIsNotDisplayed(R.string.navdrawer_item_io_live);
    }
}
