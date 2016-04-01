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

import android.support.test.espresso.NoMatchingViewException;
import android.support.test.rule.ActivityTestRule;
import android.support.v7.widget.AppCompatCheckedTextView;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.navigation.NavigationModel;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.open;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withContentDescription;
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

    public static void checkNavigationItemIsDisplayed(int navigationItemStringResource) {
        // Given navigation menu
        NavigationUtils.showNavigation();

        // Check selecting item is displayed
        onView(withText(navigationItemStringResource)).check(matches(isDisplayed()));
    }

    public static void checkNavigationItemIsNotDisplayed(int navigationItemStringResource) {
        // Given navigation menu
        NavigationUtils.showNavigation();

        // Check selecting item does not exist
        onView(withText(navigationItemStringResource)).check(doesNotExist());
    }

    public static void cleanUpActivityStack(ActivityTestRule rule) {
        NavigationUtils.showNavigation();
        // Selecting Explore activity will clean up the activity stack, leaving only the explore
        // activity, which can then be finished
        onView(withText(R.string.navdrawer_item_explore)).perform(click());
        rule.getActivity().finish();
    }

    public static void checkNavigationIsDisplayedWhenClickingMenuIcon() {
        checkNavigationItemIsDisplayed(R.string.navdrawer_item_explore);
    }

    public static void checkNavigationIconIsUp() {
        onView(withContentDescription("Close and go back")).check(matches(isDisplayed()));
    }

    public static void checkNavigationIconIsMenu() {
        onView(withContentDescription(
                "Navigation Drawer used to switch between major features of the application"))
                .check(matches(isDisplayed()));

    }

    /**
     * Checks that the {@code expectedSelectedItem} is checked in the navigation drawer, and that
     * no other items in the drawer are checked.
     */
    public static void checkNavigationItemIsSelected(
            NavigationModel.NavigationItemEnum expectedSelectedItem) {
        // Given navigation menu
        NavigationUtils.showNavigation();

        boolean selectedFound = false;

        for (int i = 0; i < NavigationModel.NavigationItemEnum.values().length; i++) {
            NavigationModel.NavigationItemEnum item =
                    NavigationModel.NavigationItemEnum.values()[i];
            try {
                // Check item is displayed
                onView(allOf(isAssignableFrom(AppCompatCheckedTextView.class),
                        withText(item.getTitleResource()))).check(matches(isDisplayed()));

                // If item is shown, check item is not activated, unless it is the requested one
                if (NavigationModel.NavigationItemEnum.values()[i].getId() ==
                        expectedSelectedItem.getId()) {
                    onView(allOf(isAssignableFrom(AppCompatCheckedTextView.class), withText(
                            item.getTitleResource())))
                            .check(matches(isChecked()));
                    selectedFound = true;
                } else {
                    onView(allOf(isAssignableFrom(AppCompatCheckedTextView.class), withText(
                            item.getTitleResource())))
                            .check(matches(not(isChecked())));
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
}
