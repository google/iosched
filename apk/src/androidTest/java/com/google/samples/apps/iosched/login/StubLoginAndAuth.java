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

import android.content.Intent;
import android.os.Handler;

/**
 * A stub {@link LoginAndAuth} to allow us to test different login scenarios in {@link
 * com.google.samples.apps.iosched.ui.BaseActivity}.
 */
public class StubLoginAndAuth implements LoginAndAuth {

    private String mAccountName;

    private LoginAndAuthListener mListener;

    private boolean mSuccess;

    private boolean mNewAuthentication;

    private boolean mIsStarted;

    public StubLoginAndAuth(String accountName, boolean success, boolean newAuthentication) {
        mAccountName = accountName;
        mSuccess = success;
        mNewAuthentication = newAuthentication;
    }

    /**
     * This stub login and auth helper is created before the activity is launched in the test, so we
     * need to set up the {@link LoginAndAuthListener}, ie the Activity under test, separately.
     */
    public void setListener(LoginAndAuthListener listener) {
        mListener = listener;
    }

    @Override
    public String getAccountName() {
        return mAccountName;
    }

    @Override
    public void start() {
        mIsStarted = true;

        final Handler h = new Handler();

        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (mListener == null) {
                    h.postDelayed(this, 50);
                } else {
                    if (mSuccess) {
                        mListener.onAuthSuccess(mAccountName, mNewAuthentication);
                    } else {
                        mListener.onAuthFailure(mAccountName);
                    }
                }
            }
        };

        h.postDelayed(r, 0);
    }

    @Override
    public boolean isStarted() {
        return mIsStarted;
    }

    @Override
    public void stop() {
        mIsStarted = false;
    }

    @Override
    public void retryAuthByUserRequest() {
        start();
    }

    @Override
    public boolean onActivityResult(final int requestCode, final int resultCode,
            final Intent data) {
        return false;
    }
}
