/*
 * Copyright 2016 Google Inc. All rights reserved.
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
package com.google.samples.apps.iosched.testutils;

import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.view.View;
import android.widget.TextView;

import org.hamcrest.Matcher;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;

/**
 * Provides helper method to get the text from a matched view.
 */
public class MatchersHelper {

    /**
     * This returns the text for the view that has been matched with {@code matcher}. The matched
     * view is expected to be a {@link TextView}. This is different from matching a view by text,
     * this is intended to be used when matching a view by another mean (for example, by id) but
     * when we need to know the text of that view, for use later in a test.
     * <P />
     * In general, this isn't good practice as tests should be written using mock data (so we always
     * know what the data is) but this enables us to write tests that are written using real data
     * (so we don't always know what the data is but we want to verify something later in a test).
     * This is enables us to write UI tests before refactoring a feature to make it easier to mock
     * the data.
     */
    public static String getText(final Matcher<View> matcher) {
        /**
         * We cannot use a String directly as we need to make it final to access it inside the
         * inner method but we cannot reassign a value to a final String.
         */
        final String[] stringHolder = { null };

        onView(matcher).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(TextView.class);
            }

            @Override
            public String getDescription() {
                return "getting text from a TextView";
            }

            @Override
            public void perform(UiController uiController, View view) {
                TextView tv = (TextView)view;
                stringHolder[0] = tv.getText().toString();
            }
        });
        return stringHolder[0];
    }
}
