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

import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.contrib.RecyclerViewActions;

import com.google.samples.apps.iosched.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.swipeUp;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

/**
 * Helper methods for testing toolbar
 */
public class ToolbarUtils {

    public static void checkToolbarIsCompletelyDisplayed() {
        onView(withId(R.id.toolbar)).check(matches(isCompletelyDisplayed()));
    }

    public static void checkToolbarHidesAfterSwipingRecyclerViewUp(int viewResource) {
        ViewInteraction view = onView(withId(R.id.toolbar));

        // Swiping up should hide the toolbar.
        onView(withId(viewResource))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, swipeUp()));

        // Check if the toolbar is hidden.
        view.check(matches(not(isDisplayed())));
    }

    public static void checkToolbarCollapsesAfterSwipingRecyclerViewUp(int viewResource) {
        // TODO
        ViewInteraction view = onView(withId(R.id.toolbar));

        // Swiping up should hide the toolbar.
        onView(withId(viewResource))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, swipeUp()));

        // Check if the toolbar is hidden.
        view.check(matches(not(isDisplayed())));
    }
}
