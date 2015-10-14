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
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.ImageView;

import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.settings.SettingsUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Terms of Service activity activated via
 * {@link com.google.samples.apps.iosched.core.activities.BaseActivity} functionality.
 */
public class WelcomeActivity extends AppCompatActivity implements WelcomeFragment.WelcomeFragmentContainer {
    private static final String TAG = makeLogTag(WelcomeActivity.class);
    WelcomeActivityContent mContentFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_welcome);

        mContentFragment = getCurrentFragment(this);

        // If there's no fragment to use, we're done here.
        if (mContentFragment == null) {
            finish();
        }

        // Wire up the fragment
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.welcome_content, (Fragment) mContentFragment);
        fragmentTransaction.commit();

        LOGD(TAG, "Inside Create View.");

        setupAnimation();
    }

    private void setupAnimation() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ImageView iv = (ImageView) findViewById(R.id.logo);
            AnimatedVectorDrawable logoAnim = (AnimatedVectorDrawable) getDrawable(R.drawable.io_logo_white_anim);
            iv.setImageDrawable(logoAnim);
            logoAnim.start();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Show the debug warning if debug tools are enabled and it hasn't been shown yet.
        if (BuildConfig.ENABLE_DEBUG_TOOLS && !SettingsUtils.wasDebugWarningShown(this)) {
            displayDogfoodWarningDialog();
        }
    }

    /**
     * Display dogfood build warning and mark that it was shown.
     */
    private void displayDogfoodWarningDialog() {
        new AlertDialog.Builder(this)
                .setTitle(Config.DOGFOOD_BUILD_WARNING_TITLE)
                .setMessage(Config.DOGFOOD_BUILD_WARNING_TEXT)
                .setPositiveButton(android.R.string.ok, null).show();
        SettingsUtils.markDebugWarningShown(this);
    }

    /**
     * Get the current fragment to display.
     *
     * This is the first fragment in the list that WelcomeActivityContent.shouldDisplay().
     *
     * @param context the application context.
     * @return the WelcomeActivityContent to display or null if there's none.
     */
    private static WelcomeActivityContent getCurrentFragment(Context context) {
        List<WelcomeActivityContent> welcomeActivityContents = getWelcomeFragments();

        for (WelcomeActivityContent fragment : welcomeActivityContents) {
            if (fragment.shouldDisplay(context)) {
                return fragment;
            }
        }

        return null;
    }

    /**
     * Whether to display the WelcomeActivity.
     *
     * Decided whether any of the fragments need to be displayed.
     *
     * @param context the application context.
     * @return true if the activity should be displayed.
     */
    public static boolean shouldDisplay(Context context) {
        WelcomeActivityContent fragment = getCurrentFragment(context);
        if (fragment == null) {
            return false;
        }
        return true;
    }

    /**
     * Get all WelcomeFragments for the WelcomeActivity.
     *
     * @return the List of WelcomeFragments.
     */
    private static List<WelcomeActivityContent> getWelcomeFragments() {
        return new ArrayList<WelcomeActivityContent>(Arrays.asList(
            new TosFragment(),
            new ConductFragment(),
            new AttendingFragment(),
            new AccountFragment()
        ));
    }

    @Override
    public Button getPositiveButton() {
        return (Button) findViewById(R.id.button_accept);
    }

    @Override
    public void setPositiveButtonEnabled(Boolean enabled) {
        try {
            getPositiveButton().setEnabled(enabled);
        } catch (NullPointerException e) {
            LOGD(TAG, "Positive welcome button doesn't exist to set enabled.");
        }
    }

    @Override
    public Button getNegativeButton() {
        return (Button) findViewById(R.id.button_decline);
    }

    @Override
    public void setNegativeButtonEnabled(Boolean enabled) {
        try {
            getNegativeButton().setEnabled(enabled);
        } catch (NullPointerException e) {
            LOGD(TAG, "Negative welcome button doesn't exist to set enabled.");
        }
    }

    /**
     * The definition of a Fragment for a use in the WelcomeActivity.
     */
    interface WelcomeActivityContent {
        /**
         * Whether the fragment should be displayed.
         *
         * @param context the application context.
         * @return true if the WelcomeActivityContent should be displayed.
         */
        public boolean shouldDisplay(Context context);
    }
}
