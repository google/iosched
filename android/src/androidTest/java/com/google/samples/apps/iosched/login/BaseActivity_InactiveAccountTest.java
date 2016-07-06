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

package com.google.samples.apps.iosched.login;

import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.explore.ExploreIOActivity;
import com.google.samples.apps.iosched.testutils.LoginUtils;
import com.google.samples.apps.iosched.util.AccountUtils;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

/**
 * UI tests for {@link com.google.samples.apps.iosched.ui.BaseActivity} when the user has no active
 * account.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class BaseActivity_InactiveAccountTest {

    private static final String ACCOUNT_NAME = null;

    @Rule
    public ActivityTestRule<ExploreIOActivity> mActivityRule =
            new ActivityTestRule<ExploreIOActivity>(ExploreIOActivity.class) {

                @Override
                protected void beforeActivityLaunched() {
                    super.beforeActivityLaunched();

                    // Set inactive account
                    AccountUtils
                            .setActiveAccount(InstrumentationRegistry.getTargetContext(),
                                    ACCOUNT_NAME);

                }
            };

    @After
    public void cleanUpInactiveAccount() {
        LoginUtils.setFirstAvailableAccountAsActive(InstrumentationRegistry.getTargetContext());
    }

    /**
     * The test will fail on API < 17, due to the looping animation in {@link
     * com.google.samples.apps.iosched.welcome.WelcomeActivity}. On API 17+, the activity checks the
     * settings for {@link Settings.Global#ANIMATOR_DURATION_SCALE} and doesn't run the animation if
     * it is turned off.
     */
    @SdkSuppress(minSdkVersion = 17)
    @Test
    public void welcomeActivityForAccount_IsDisplayed() {
        onView(withText(R.string.welcome_select_account)).check(matches(isDisplayed()));
    }
}
