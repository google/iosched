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

package com.google.samples.apps.iosched.testutils;

import android.support.test.rule.ActivityTestRule;

import com.google.samples.apps.iosched.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.open;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;

/**
 * Methods to help with navigation testing
 */
public class NavigationUtils {

    public static void showNavigation() {
        onView(withId(R.id.drawer_layout)).perform(open()); // Open Drawer
    }

    public static void checkScreenTitleIsDisplayed(int stringResource) {
        onView(allOf(withParent(withId(R.id.toolbar_actionbar)),
                withText(stringResource))).check(matches(isDisplayed()));
    }

    public static void clickOnNavigationItemAndCheckActivityIsDisplayed(
            int navigationItemStringResource, int expectedTitleResource) {
        // Given navigation menu
        NavigationUtils.showNavigation();

        // When selecting item
        onView(withText(navigationItemStringResource)).perform(click());

        // Then screen is showing
        NavigationUtils.checkScreenTitleIsDisplayed(expectedTitleResource);
    }

    public static void cleanUpActivityStack(ActivityTestRule rule) {
        NavigationUtils.showNavigation();
        // Selecting Explore activity will clean up the activity stack, leaving only the explore
        // activity, which can then be finished
        onView(withText(R.string.navdrawer_item_explore)).perform(click());
        rule.getActivity().finish();
    }

}
