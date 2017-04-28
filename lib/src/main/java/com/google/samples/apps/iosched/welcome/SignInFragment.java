/*
 * Copyright (c) 2017 Google Inc.
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

package com.google.samples.apps.iosched.welcome;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.util.AnalyticsHelper;
import com.google.samples.apps.iosched.util.WelcomeUtils;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGI;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Fragment that provides optional auth functionality to users.
 */
public class SignInFragment extends WelcomeFragment {

    public SignInFragment() {}

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(R.layout.welcome_sign_in_fragment, container, false);
    }

    @Override
    public boolean shouldDisplay(final Context context) {
        /* Display if the user hasn't attempted signed in or if the user has refused sign in. */
        return !WelcomeUtils.hasUserAttemptedOnboardingSignIn(context) &&
                !WelcomeUtils.hasUserRefusedOnboardingSignIn(context);
    }

    @Override
    protected String getPrimaryButtonText() {
        return getString(R.string.signin_prompt);
    }

    @Override
    protected String getSecondaryButtonText() {
        return getString(R.string.signin_prompt_skip);
    }

    @Override
    protected View.OnClickListener getPrimaryButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                // Save that the user tried to sign in, so that the onboarding sign-in screen
                // is not displayed again.
                WelcomeUtils.markUserAttemptedOnboardingSignIn(mActivity);
                // Let the activity handle the sign in flow.
                ((WelcomeActivity) mActivity).signIn();
            }
        };
    }

    @Override
    protected View.OnClickListener getSecondaryButtonListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                WelcomeUtils.markUserRefusedOnboardingSignIn(mActivity);
                ((WelcomeActivity) mActivity).doNext();
            }
        };
    }

    @Override
    protected int getHeaderColorRes() {
        return R.color.neon_blue;
    }

    @Override
    protected int getLogoDrawableRes() {
        return R.drawable.io_logo_onboarding;
    }
}