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

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.HomeActivity;
import com.google.android.apps.iosched.ui.SessionDetailFragment;
import com.google.android.apps.iosched.ui.SimpleSinglePaneActivity;
import com.google.android.apps.iosched.ui.TrackInfoHelperFragment;
import com.google.android.apps.iosched.util.BeamUtils;
import com.google.android.apps.iosched.util.UIUtils;

import com.actionbarsherlock.view.MenuItem;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.NavUtils;

/**
 * A single-pane activity that shows a {@link SessionDetailFragment}.
 */
public class SessionDetailActivity extends SimpleSinglePaneActivity implements
        TrackInfoHelperFragment.Callbacks {

    private String mTrackId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (BeamUtils.wasLaunchedThroughBeamFirstTime(this, getIntent())) {
            BeamUtils.setBeamUnlocked(this);
            showFirstBeamDialog();
        }

        BeamUtils.tryUpdateIntentFromBeam(this);
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Uri sessionUri = getIntent().getData();
            BeamUtils.setBeamSessionUri(this, sessionUri);
            trySetBeamCallback();
            getSupportFragmentManager().beginTransaction()
                    .add(TrackInfoHelperFragment.newFromSessionUri(sessionUri),
                            "track_info")
                    .commit();
        }
    }

    @Override
    protected Fragment onCreatePane() {
        return new SessionDetailFragment();
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

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void trySetBeamCallback() {
        if (UIUtils.hasICS()) {
            BeamUtils.setBeamCompleteCallback(this, new NfcAdapter.OnNdefPushCompleteCallback() {
                @Override
                public void onNdefPushComplete(NfcEvent event) {
                    // Beam has been sent
                    if (!BeamUtils.isBeamUnlocked(SessionDetailActivity.this)) {
                        BeamUtils.setBeamUnlocked(SessionDetailActivity.this);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showFirstBeamDialog();
                            }
                        });
                    }
                }
            });
        }
    }

    private void showFirstBeamDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.just_beamed)
                .setMessage(R.string.beam_unlocked_session)
                .setNegativeButton(R.string.close, null)
                .setPositiveButton(R.string.view_beam_session,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface di, int i) {
                                BeamUtils.launchBeamSession(SessionDetailActivity.this);
                                di.dismiss();
                            }
                        })
                .create()
                .show();
    }
}
