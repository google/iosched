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

import android.net.Uri;
import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.samples.apps.iosched.util.TimeUtils;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import java.util.List;

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
     * <p/>
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
        final String[] stringHolder = {null};

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
                TextView tv = (TextView) view;
                stringHolder[0] = tv.getText().toString();
            }
        });
        return stringHolder[0];
    }

    /**
     * This returns the text for the view that has an id of {@code idOfDescendantViewToGetTextFor}
     * and is a descendant of the nth child of {@code parentViewWithMultipleChildren}, where n
     * equals {@code indexOfDescendant}. The parent view is expected to be a {@link ViewGroup} and
     * the descendant view is expected to be a {@link TextView}. This is used when there is a
     * certain randomness in the elements shown in a collection view, even with mock data.
     *
     * @see com.google.samples.apps.iosched.videolibrary.VideoLibraryModel
     */
    public static String getTextForViewGroupDescendant(
            final Matcher<View> parentViewWithMultipleChildren, final int indexOfDescendant,
            final int idOfDescendantViewToGetTextFor) {
        /**
         * We cannot use a String directly as we need to make it final to access it inside the
         * inner method but we cannot reassign a value to a final String.
         */
        final String[] stringHolder = {null};

        onView(parentViewWithMultipleChildren).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(ViewGroup.class);
            }

            @Override
            public String getDescription() {
                return "getting text from a TextView with a known id and that is a descendant of " +
                        "the nth child of a viewgroup";
            }

            @Override
            public void perform(UiController uiController, View view) {
                ViewGroup vg = (ViewGroup) view;
                View nthDescendant = vg.getChildAt(indexOfDescendant);
                TextView matchedView =
                        (TextView) nthDescendant.findViewById(idOfDescendantViewToGetTextFor);
                stringHolder[0] = matchedView.getText().toString();
            }
        });
        return stringHolder[0];
    }

    /**
     * This returns the number of descendants of the {@code parentViewWithMultipleChildren} that
     * have an id of {@code idOfDescendantViewToGetTextFor}}. The parent view is expected to be a
     * {@link ViewGroup}. This is used when there is a certain randomness in the elements shown in a
     * collection view, even with mock data, but we want to check the number of elements inside the
     * parent view.
     *
     * @see com.google.samples.apps.iosched.explore.ExploreIOActivityTest
     */
    public static int getNumberOfDescendantsForViewGroupDescendant(
            final Matcher<View> parentViewWithMultipleChildren,
            final int idOfDescendantViewToGetTextFor) {
        /**
         * We cannot use a int directly as we need to make it final to access it inside the
         * inner method but we cannot reassign a value to a final int.
         */
        final int[] intHolder = {0};

        onView(parentViewWithMultipleChildren).perform(new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(ViewGroup.class);
            }

            @Override
            public String getDescription() {
                return "getting text from a TextView with a known id and that is a descendant of " +
                        "the nth child of a viewgroup";
            }

            @Override
            public void perform(UiController uiController, View view) {
                ViewGroup vg = (ViewGroup) view;
                int descendants = vg.getChildCount();
                for (int i = 0; i < descendants; i++) {
                    View ithDescendant = vg.getChildAt(i);
                    View matchedView =
                            ithDescendant.findViewById(idOfDescendantViewToGetTextFor);
                    if (matchedView != null) {
                        intHolder[0] += 1;
                    }
                }
            }
        });
        return intHolder[0];
    }

    /**
     * Some {@link Uri} include specific times, for example all sessions after a given time. Even
     * though time can be set in tests, {@link com.google.samples.apps.iosched.util.TimeUtils
     * .getCurrentTime()} takes into account the passage of time so two separate calls to it will
     * return slightly different values. This matcher applies an approximation of 10 seconds.
     * <p/>
     * Note: it assumes the time portion of the {@link Uri} is the last segment.
     */
    public static Matcher<Uri> approximateTimeUriMatcher(final Uri expectedUri) {
        return new BaseMatcher<Uri>() {

            @Override
            public void describeTo(final Description description) {
                description.appendText("approximateTimeUriMatcher");
            }

            @Override
            public boolean matches(final Object item) {
                if (item instanceof Uri) {
                    Uri actualUri = (Uri) item;

                    // Check the last segment is a time
                    long expectedTime = 0L;
                    long actualTime = 0L;
                    try {
                        expectedTime = Long.parseLong(expectedUri.getLastPathSegment());
                    } catch (NumberFormatException e) {
                        return false;
                    }
                    try {
                        actualTime = Long.parseLong(actualUri.getLastPathSegment());
                    } catch (NumberFormatException e) {
                        return false;
                    }

                    // If the times are within 10 seconds of each other, check other segments
                    if (Math.abs(actualTime - expectedTime) < TimeUtils.SECOND * 10) {
                        List<String> actualSegments = actualUri.getPathSegments();
                        List<String> expectedSegments = expectedUri.getPathSegments();
                        // If same number of segments
                        if (actualSegments.size() == expectedSegments.size()) {
                            boolean diffFound = false;
                            // Check each segment except the last
                            for (int i = 0; i < actualSegments.size() - 1; i++) {
                                if (!actualSegments.get(i).equals(expectedSegments.get(i))) {
                                    diffFound = true;
                                }
                            }
                            // They match only if no differences were found
                            return !diffFound;
                        }
                    }
                }
                return false;
            }

            @Override
            public void describeMismatch(final Object item, final Description mismatchDescription) {
                mismatchDescription.appendText("item doesn't match uri " + expectedUri +
                        " with an approximataion of 10 seconds for the last time segment");
            }
        };
    }

}
