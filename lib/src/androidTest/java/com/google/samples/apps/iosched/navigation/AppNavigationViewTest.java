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

import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

//import com.google.samples.apps.iosched.explore.ExploreIOActivity;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.testutils.NavigationUtils;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * UI tests for {@link AppNavigationView} when the user is logged in and attending the
 * conference.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class AppNavigationViewTest {
    private String mAccountName;

//    DISABLED: Broken
//    @Rule
//    public ActivityTestRule<ExploreIOActivity> mActivityRule =
//            new ActivityTestRule<ExploreIOActivity>(ExploreIOActivity.class) {
//
//                @Override
//                protected void beforeActivityLaunched() {
//                    // Make sure the EULA screen is not shown.
//                    SettingsUtils.markTosAccepted(InstrumentationRegistry.getTargetContext(), true);
//
//                    // Set user as logged in and attending
//                    RegistrationUtils.setRegisteredAttendee(
//                            InstrumentationRegistry.getTargetContext(), true);
//                    mAccountName = LoginUtils.setFirstAvailableAccountAsActive(
//                            InstrumentationRegistry.getTargetContext());
//                }
//            };

//    DISABLED: Broken
//    @After
//    public void cleanUpActivityStack() {
//        NavigationUtils.cleanUpActivityStack(mActivityRule);
//    }

    @Test
    public void mySchedule_WhenClicked_ActivityDisplayed() {
        NavigationUtils.clickOnNavigationItemAndCheckActivityIsDisplayed(
                R.id.myschedule_nav_item,
                R.string.title_schedule);
    }

//    DISABLED: Broken
//    @Test
//    public void explore_WhenClicked_ActivityDisplayed() {
//        // Move to a different screen first
//        NavigationUtils.clickOnNavigationItemAndCheckActivityIsDisplayed(
//                R.id.about_nav_item,
//                R.string.title_about);
//
//        NavigationUtils.clickOnNavigationItemAndCheckActivityIsDisplayed(
//                R.id.explore_nav_item,
//                R.string.title_explore);
//    }

//    DISABLED: Broken
//    @Test
//    public void about_WhenClicked_ActivityDisplayed() {
//        NavigationUtils.clickOnNavigationItemAndCheckActivityIsDisplayed(
//                R.id.about_nav_item,
//                R.string.title_about);
//    }

    @Test
    public void map_WhenClicked_ActivityDisplayed() {
        NavigationUtils.clickOnNavigationItemAndCheckActivityIsDisplayed(
                R.id.map_nav_item,
                R.string.title_map);
    }
}
