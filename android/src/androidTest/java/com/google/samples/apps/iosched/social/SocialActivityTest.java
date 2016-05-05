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

package com.google.samples.apps.iosched.social;

import android.provider.Settings;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.google.samples.apps.iosched.testutils.BaseActivityTestRule;
import com.google.samples.apps.iosched.testutils.NavigationUtils;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class SocialActivityTest {
    @Rule
    public BaseActivityTestRule<SocialActivity> mActivityRule =
            new BaseActivityTestRule<SocialActivity>(
                    SocialActivity.class, null, true);

    /**
     * The test will fail on API < 17, due to the looping animation. On API 17+, the activity checks
     * the settings for {@link Settings.Global#ANIMATOR_DURATION_SCALE} and doesn't run the
     * animation if it is turned off.
     */
    @SdkSuppress(minSdkVersion = 17)
    @Test
    public void navigationIcon_DisplaysAsMenu() {
        NavigationUtils.checkNavigationIconIsMenu();
    }

    /**
     * The test will fail on API < 17, due to the looping animation. On API 17+, the activity checks
     * the settings for {@link Settings.Global#ANIMATOR_DURATION_SCALE} and doesn't run the
     * animation if it is turned off.
     */
    @SdkSuppress(minSdkVersion = 17)
    @Test
    public void navigationIcon_OnClick_NavigationDisplayed() {
        NavigationUtils.checkNavigationIsDisplayedWhenClickingMenuIcon();
    }

}
