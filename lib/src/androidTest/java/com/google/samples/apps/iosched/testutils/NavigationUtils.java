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

import android.support.annotation.IdRes;
import android.support.test.espresso.NoMatchingViewException;
import android.support.test.rule.ActivityTestRule;
import android.view.View;

import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.navigation.NavigationModel.NavigationItemEnum;

import org.hamcrest.Matcher;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertTrue;

/**
 * Methods to help with navigation testing
 */
public class NavigationUtils {

    public static void checkScreenTitleIsDisplayed(int stringResource) {
        onView(allOf(withParent(withId(R.id.toolbar)),
                withText(stringResource))).check(matches(isDisplayed()));
    }

    public static void clickOnNavigationItemAndCheckActivityIsDisplayed(
            @IdRes int navigationId, int expectedTitleResource) {
        // When selecting item
        onView(getNavigationItemMatcher(navigationId)).perform(click());

        // Then screen is showing
        NavigationUtils.checkScreenTitleIsDisplayed(expectedTitleResource);
    }

//    DISABLED: Broken
//    public static void cleanUpActivityStack(ActivityTestRule rule) {
//        // Selecting Explore activity will clean up the activity stack, leaving only the explore
//        // activity, which can then be finished
//        onView(getNavigationItemMatcher(R.id.explore_nav_item)).perform(click());
//        rule.getActivity().finish();
//    }

    /**
     * Checks that the {@code expectedSelectedItem} is checked in the navigation drawer, and that no
     * other items in the drawer are checked.
     */
    public static void checkNavigationItemIsSelected(NavigationItemEnum expected) {
        boolean selectedFound = false;

        for (int i = 0; i < NavigationItemEnum.values().length; i++) {
            NavigationItemEnum item = NavigationItemEnum.values()[i];
            try {
                // Check item is displayed
                onView(getNavigationItemMatcher(item.getId()))
                        .check(matches(isDisplayed()));

                // If item is shown, check item is not activated, unless it is the requested one
                if (item.getId() == expected.getId()) {
                    onView(getSelectedItem(item)).check(matches(isDisplayed()));
                    selectedFound = true;
                } else {
                    onView(getSelectedItem(item)).check(matches(not(isDisplayed())));
                }

            } catch (NoMatchingViewException e) {
                // Not all navigation items in the enum are shown. This test doesn't aim to
                // check that the correct items are shown, but that only the selected one among
                // those shown is shown as selected. Tests in the navigation package check
                // correct items are shown.
            }
        }

        // Sanity check to ensure the tests in the try/catch block weren't all skipped
        assertTrue(selectedFound);
    }

    private static Matcher<View> getNavigationItemMatcher(@IdRes final int id) {
        return withId(id);
    }

    private static Matcher<View> getSelectedItem(NavigationItemEnum item) {
        return allOf(withText(item.getTitleResource()), withId(R.id.largeLabel));
    }
}
