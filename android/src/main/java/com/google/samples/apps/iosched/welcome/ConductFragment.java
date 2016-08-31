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

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import no.java.schedule.v2.R;
import com.google.samples.apps.iosched.settings.SettingsUtils;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * The code of conduct fragment in the welcome screen.
 */
public class ConductFragment extends WelcomeFragment implements WelcomeActivity.WelcomeActivityContent {
    private static final String TAG = makeLogTag(ConductFragment.class);

    @Override
    public boolean shouldDisplay(Context context) {
        return !SettingsUtils.isConductAccepted(context);
    }

    @Override
    protected View.OnClickListener getPositiveListener() {
        return new WelcomeFragmentOnClickListener(mActivity) {
            @Override
            public void onClick(View v) {
                // Ensure we don't run this fragment again
                LOGD(TAG, "Marking code of conduct flag.");
                SettingsUtils.markConductAccepted(mActivity, true);
                doNext();
            }
        };
    }

    @Override
    protected View.OnClickListener getNegativeListener() {
        return new WelcomeFragmentOnClickListener(mActivity) {
            @Override
            public void onClick(View v) {
                // Nothing to do here
                LOGD(TAG, "Need to accept Code of Conduct.");
                doFinish();
            }
        };
    }

    @Override
    protected String getPositiveText() {
        return getResourceString(R.string.accept);
    }

    @Override
    protected String getNegativeText() {
        return getResourceString(R.string.decline);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.welcome_conduct_fragment, container, false);
    }
}