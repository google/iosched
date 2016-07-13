/*
 * Copyright 2015 Google Inc. All rights reserved.
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
package com.google.samples.apps.iosched.welcome;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.os.Bundle;
import android.support.graphics.drawable.AnimatedVectorDrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Responsible for presenting a series of fragments to the user who has just installed the app as
 * part of the welcome/onboarding experience.
 */
public class WelcomeActivity extends AppCompatActivity
        implements WelcomeFragment.WelcomeFragmentContainer {

    private static final String TAG = makeLogTag(WelcomeActivity.class);

    WelcomeFragment mContentFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_welcome);

        mContentFragment = getCurrentFragment(this);

        // If there's no fragment to use, we're done.
        if (mContentFragment == null) {
            finish();
        } else {
            // Wire up the fragment.
            FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
            fragmentTransaction.add(R.id.welcome_content, (Fragment) mContentFragment);
            fragmentTransaction.commit();

            final ImageView iv = (ImageView) findViewById(R.id.logo);
            final AnimatedVectorDrawableCompat logo =
                    AnimatedVectorDrawableCompat.create(this, R.drawable.avd_hash_io_16);
            if (iv != null && logo != null) {
                iv.setImageDrawable(logo);

                if (UIUtils.animationEnabled(getContentResolver())) {
                    logo.start();
                }
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        // Show the debug warning if debug tools are enabled and it hasn't been shown yet.
        if (!BuildConfig.SUPPRESS_DOGFOOD_WARNING &&
                BuildConfig.ENABLE_DEBUG_TOOLS && !SettingsUtils.wasDebugWarningShown(this)) {
            displayDogfoodWarningDialog();
        }
    }

    /**
     * Displays dogfood build warning and marks that the warning was shown.
     */
    private void displayDogfoodWarningDialog() {
        new AlertDialog.Builder(this)
                .setTitle(Config.DOGFOOD_BUILD_WARNING_TITLE)
                .setMessage(Config.DOGFOOD_BUILD_WARNING_TEXT)
                .setPositiveButton(android.R.string.ok, null).show();
        SettingsUtils.markDebugWarningShown(this);
    }

    /**
     * Gets the current fragment to display.
     *
     * @param context the application context.
     * @return the fragment to display, or null if there is no fragment.
     */
    private static WelcomeFragment getCurrentFragment(Context context) {
        List<WelcomeFragment> welcomeActivityContents = getWelcomeFragments();

        for (WelcomeFragment fragment : welcomeActivityContents) {
            if (fragment.shouldDisplay(context)) {
                return fragment;
            }
        }

        return null;
    }

    /**
     * Tracks whether to display this activity.
     *
     * @param context the application context.
     * @return true if the activity should be displayed, otherwise false.
     */
    public static boolean shouldDisplay(Context context) {
        WelcomeFragment fragment = getCurrentFragment(context);
        return fragment != null;
    }

    /**
     * Returns all fragments displayed by {@link WelcomeActivity}.
     */
    private static List<WelcomeFragment> getWelcomeFragments() {
        return new ArrayList<>(Arrays.asList(
                new TosFragment(),
                new ConductFragment(),
                new AccountFragment(),
                new AttendingFragment()
        ));
    }

    @Override
    public Button getPrimaryButton() {
        return (Button) findViewById(R.id.button_accept);
    }

    @Override
    public void setPrimaryButtonEnabled(Boolean enabled) {
        getPrimaryButton().setEnabled(enabled);
    }

    @Override
    public Button getSecondaryButton() {
        return (Button) findViewById(R.id.button_decline);
    }

    @Override
    public void setButtonBarVisibility(boolean isVisible) {
        findViewById(R.id.welcome_button_bar).setVisibility(isVisible ? View.VISIBLE : View.GONE);
        if (!isVisible) {
            ((ViewGroup.MarginLayoutParams) findViewById(R.id.welcome_scrolling_content)
                    .getLayoutParams()).bottomMargin = 0;
        }
    }
}
