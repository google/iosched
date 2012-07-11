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
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.BaseMultiPaneActivity;
import com.google.android.apps.iosched.ui.SessionDetailFragment;
import com.google.android.apps.iosched.ui.SessionsFragment;
import com.google.android.apps.iosched.ui.TracksFragment;
import com.google.android.apps.iosched.ui.phone.SessionDetailActivity;
import com.google.android.apps.iosched.ui.phone.SessionsActivity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.view.ViewGroup;

/**
 * A multi-pane activity, consisting of a {@link TracksDropdownFragment}, a
 * {@link SessionsFragment}, and {@link SessionDetailFragment}.
 *
 * This activity requires API level 11 or greater because {@link TracksDropdownFragment} requires
 * API level 11.
 */
public class SessionsMultiPaneActivity extends BaseMultiPaneActivity {

    private TracksDropdownFragment mTracksDropdownFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sessions);

        Intent intent = new Intent();
        intent.setData(ScheduleContract.Tracks.CONTENT_URI);
        intent.putExtra(TracksFragment.EXTRA_NEXT_TYPE, TracksFragment.NEXT_TYPE_SESSIONS);

        final FragmentManager fm = getSupportFragmentManager();
        mTracksDropdownFragment = (TracksDropdownFragment) fm.findFragmentById(
                R.id.fragment_tracks_dropdown);
        mTracksDropdownFragment.reloadFromArguments(intentToFragmentArguments(intent));
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        getActivityHelper().setupSubActivity();

        ViewGroup detailContainer = (ViewGroup)
                findViewById(R.id.fragment_container_session_detail);
        if (detailContainer != null && detailContainer.getChildCount() > 0) {
            findViewById(R.id.fragment_container_session_detail).setBackgroundColor(0xffffffff);
        }
    }

    @Override
    public FragmentReplaceInfo onSubstituteFragmentForActivityLaunch(String activityClassName) {
        if (SessionsActivity.class.getName().equals(activityClassName)) {
            return new FragmentReplaceInfo(
                    SessionsFragment.class,
                    "sessions",
                    R.id.fragment_container_sessions);
        } else if (SessionDetailActivity.class.getName().equals(activityClassName)) {
            findViewById(R.id.fragment_container_session_detail).setBackgroundColor(
                    0xffffffff);
            return new FragmentReplaceInfo(
                    SessionDetailFragment.class,
                    "session_detail",
                    R.id.fragment_container_session_detail);
        }
        return null;
    }
}
