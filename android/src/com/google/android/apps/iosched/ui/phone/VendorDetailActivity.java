/*
 * Copyright 2012 Google Inc.
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

package com.google.android.apps.iosched.ui.phone;

import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.HomeActivity;
import com.google.android.apps.iosched.ui.SimpleSinglePaneActivity;
import com.google.android.apps.iosched.ui.TrackInfoHelperFragment;
import com.google.android.apps.iosched.ui.VendorDetailFragment;

import com.actionbarsherlock.view.MenuItem;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.NavUtils;

/**
 * A single-pane activity that shows a {@link VendorDetailFragment}.
 */
public class VendorDetailActivity extends SimpleSinglePaneActivity implements
        VendorDetailFragment.Callbacks,
        TrackInfoHelperFragment.Callbacks {

    private String mTrackId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected Fragment onCreatePane() {
        return new VendorDetailFragment();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            // Up to this session's track details, or Home if no track is available
            Intent parentIntent;
            if (mTrackId != null) {
                parentIntent = new Intent(Intent.ACTION_VIEW,
                        ScheduleContract.Tracks.buildTrackUri(mTrackId));
            } else {
                parentIntent = new Intent(this, HomeActivity.class);
            }

            NavUtils.navigateUpTo(this, parentIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onTrackInfoAvailable(String trackId, String trackName, int trackColor) {
        mTrackId = trackId;
        setTitle(trackName);
        setActionBarColor(trackColor);
    }

    @Override
    public void onTrackIdAvailable(final String trackId) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                FragmentManager fm = getSupportFragmentManager();
                if (fm.findFragmentByTag("track_info") == null) {
                    fm.beginTransaction()
                            .add(TrackInfoHelperFragment.newFromTrackUri(
                                    ScheduleContract.Tracks.buildTrackUri(trackId)),
                                    "track_info")
                            .commit();
                }
            }
        });
    }
}
