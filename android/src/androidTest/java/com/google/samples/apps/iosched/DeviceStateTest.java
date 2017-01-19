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

package com.google.samples.apps.iosched;

import android.os.Build;
import android.provider.Settings;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;

import com.google.samples.apps.iosched.about.AboutActivity;
import com.google.samples.apps.iosched.testutils.BaseActivityTestRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Tests to ensure the device is setup properly to run the tests. Before making changes to a test
 * or researching a failure, make sure this set of tests pass.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class DeviceStateTest {
    private static final String LOG_TAG = makeLogTag(DeviceStateTest.class);

    @Rule
    public BaseActivityTestRule<AboutActivity> mActivityRule =
            new BaseActivityTestRule<AboutActivity>(
                    AboutActivity.class, null, true);

    @Test
    public void animator_areDisabled() {
        // Espresso tests can be flaky when animations are enabled. Test devices should have them
        // disabled. This can be toggled on the Android Device by opening the Settings App:
        // Enable Developer Options, then Developer Options -> Animator duration scale = Animation off
        float animationSetting = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            animationSetting = Settings.Global.getFloat(
                    mActivityRule.getActivity().getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    -2F);
        } else {
            //noinspection deprecation This statement can be removed once minSdk >= 17.
            animationSetting = Settings.System.getFloat(
                    mActivityRule.getActivity().getContentResolver(),
                    Settings.System.ANIMATOR_DURATION_SCALE,
                    -2F);
        }
        // -2 indicates the value was never set, by default most devices have this enabled.
        assertFalse("Setting has never been set and the default value is invalid. " +
                "Set the value. " + animationSetting, animationSetting == -2F);
        // -1 indicates an issue looking up the value.
        assertFalse("Could not determine animations settings, " + animationSetting,
                animationSetting < 0);
        // The value should be Zero as positive values mean it is enabled.
        assertFalse("Animations are enabled; they should be disabled when running tests, " +
                animationSetting, animationSetting > 0);
    }

    @Test
    public void windowAnimations_areDisabled() {
        // Espresso tests can be flaky when animations are enabled. Test devices should have them
        // disabled. This can be toggled on the Android Device by opening the Settings App:
        // Enable Developer Options, then Developer Options -> Window animation scale = Animation off
        float winAnimationSetting = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            winAnimationSetting = Settings.Global.getFloat(
                    mActivityRule.getActivity().getContentResolver(),
                    Settings.Global.WINDOW_ANIMATION_SCALE,
                    -2F);
        } else {
            //noinspection deprecation This statement can be removed once minSdk >= 17.
            winAnimationSetting = Settings.System.getFloat(
                    mActivityRule.getActivity().getContentResolver(),
                    Settings.System.WINDOW_ANIMATION_SCALE,
                    -2F);
        }
        // -2 indicates the value was never set, by default most devices have this enabled.
        assertFalse("Setting has never been set and the default value is invalid. " +
                "Set the value. " + winAnimationSetting, winAnimationSetting == -2F);
        // -1 indicates an issue looking up the value.
        assertFalse("Could not determine animations settings, " + winAnimationSetting,
                winAnimationSetting < 0);
        // The value should be Zero as positive values mean it is enabled.
        assertFalse("Animations are enabled; they should be disabled when running tests, " +
                winAnimationSetting, winAnimationSetting > 0);
    }

    @Test
    public void transitions_areDisabled() {
        // Espresso tests can be flaky when animations are enabled. Test devices should have them
        // disabled. This can be toggled on the Android Device by opening the Settings App:
        // Enable Developer Options, then Developer Options -> Transition animation scale = Animation off
        float transitionSetting = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            transitionSetting = Settings.Global.getFloat(
                    mActivityRule.getActivity().getContentResolver(),
                    Settings.Global.TRANSITION_ANIMATION_SCALE,
                    -2F);
        } else {
            //noinspection deprecation This statement can be removed once minSdk >= 17.
            transitionSetting = Settings.System.getFloat(
                    mActivityRule.getActivity().getContentResolver(),
                    Settings.System.TRANSITION_ANIMATION_SCALE,
                    -2F);
        }
        // -2 indicates the value was never set, by default most devices have this enabled.
        assertFalse("Setting has never been set and the default value is invalid. " +
                "Set the value. " + transitionSetting, transitionSetting == -2F);
        // -1 indicates an issue looking up the value.
        assertFalse("Could not determine animations settings, " + transitionSetting,
                transitionSetting < 0);
        // The value should be Zero as positive values mean it is enabled.
        assertFalse("Animations are enabled; they should be disabled when running tests, " +
                transitionSetting, transitionSetting > 0);
    }


}