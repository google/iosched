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

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.settings.SettingsUtils;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Asks whether the user is attending the conference in person or remotely.
 */
public class AttendingFragment extends WelcomeFragment {

    private static final String TAG = makeLogTag(AttendingFragment.class);

    private WelcomeFragmentOnClickListener mPositiveClickListener;

    private WelcomeFragmentOnClickListener mNegativeClickListener;

    @Override
    public boolean shouldDisplay(Context context) {
        return !SettingsUtils.hasAnsweredLocalOrRemote(context);
    }


    @Override
    protected View.OnClickListener getPrimaryButtonListener() {
        return mPositiveClickListener;
    }

    @Override
    protected View.OnClickListener getSecondaryButtonListener() {
        return mNegativeClickListener;
    }

    @Override
    protected String getPrimaryButtonText() {
        return getResourceString(R.string.attending_in_person);
    }

    @Override
    protected String getSecondaryButtonText() {
        return getResourceString(R.string.attending_remotely);
    }

    @Override
    protected boolean shouldShowButtonBar() {
        return false;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mPositiveClickListener = new WelcomeFragmentOnClickListener(mActivity) {
            @Override
            public void onClick(View v) {
                // Ensure we don't run this fragment again
                LOGD(TAG, "Marking attending flag.");
                SettingsUtils.setAttendeeAtVenue(mActivity, true);
                SettingsUtils.markAnsweredLocalOrRemote(mActivity, true);
                doNext();
            }
        };

        mNegativeClickListener = new WelcomeFragmentOnClickListener(mActivity) {
            @Override
            public void onClick(View v) {
                LOGD(TAG, "Marking not attending flag.");
                SettingsUtils.setAttendeeAtVenue(mActivity, false);
                SettingsUtils.markAnsweredLocalOrRemote(mActivity, true);
                doNext();
            }
        };

        super.onCreateView(inflater, container, savedInstanceState);
        final View view = inflater.inflate(R.layout.welcome_attending_fragment, container, false);
        view.findViewById(R.id.attending_in_person).setOnClickListener(mPositiveClickListener);
        view.findViewById(R.id.attending_remotely).setOnClickListener(mNegativeClickListener);
        return view;
    }
}
