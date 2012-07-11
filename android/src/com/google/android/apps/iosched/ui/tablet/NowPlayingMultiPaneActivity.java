/*
 * Copyright 2011 Google Inc.
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

package com.google.android.apps.iosched.ui.tablet;

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.android.apps.iosched.ui.BaseMultiPaneActivity;
import com.google.android.apps.iosched.ui.SessionDetailFragment;
import com.google.android.apps.iosched.ui.SessionsFragment;
import com.google.android.apps.iosched.ui.phone.SessionDetailActivity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.view.ViewGroup;

/**
 * An activity that shows currently playing sessions in a two-pane view.
 */
public class NowPlayingMultiPaneActivity extends BaseMultiPaneActivity {

    private SessionsFragment mSessionsFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent();
        intent.setData(Sessions.buildSessionsAtDirUri(System.currentTimeMillis()));

        setContentView(R.layout.activity_now_playing);

        final FragmentManager fm = getSupportFragmentManager();
        mSessionsFragment = (SessionsFragment) fm.findFragmentByTag("sessions");
        if (mSessionsFragment == null) {
            mSessionsFragment = new SessionsFragment();
            mSessionsFragment.setArguments(intentToFragmentArguments(intent));
            fm.beginTransaction()
                    .add(R.id.fragment_container_sessions, mSessionsFragment, "sessions")
                    .commit();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getActivityHelper().setupSubActivity();

        ViewGroup detailContainer = (ViewGroup) findViewById(
                R.id.fragment_container_now_playing_detail);
        if (detailContainer != null && detailContainer.getChildCount() > 1) {
            findViewById(android.R.id.empty).setVisibility(View.GONE);
        }
    }

    @Override
    public FragmentReplaceInfo onSubstituteFragmentForActivityLaunch(
            String activityClassName) {
        findViewById(android.R.id.empty).setVisibility(View.GONE);
        if (SessionDetailActivity.class.getName().equals(activityClassName)) {
            clearSelectedItems();
            return new FragmentReplaceInfo(
                    SessionDetailFragment.class,
                    "session_detail",
                    R.id.fragment_container_now_playing_detail);
        }
        return null;
    }

    private void clearSelectedItems() {
        if (mSessionsFragment != null) {
            mSessionsFragment.clearCheckedPosition();
        }
    }
}
