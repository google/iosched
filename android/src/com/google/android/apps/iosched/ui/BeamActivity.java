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

package com.google.android.apps.iosched.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.util.BeamUtils;

/**
 * Beam easter egg landing screen.
 */
public class BeamActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_beam);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (BeamUtils.wasLaunchedThroughBeamFirstTime(this, getIntent())) {
            BeamUtils.setBeamUnlocked(this);
            showUnlockDialog();
        }
    }

    void showUnlockDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.just_beamed)
                .setMessage(R.string.beam_unlocked_default)
                .setNegativeButton(R.string.close, null)
                .setPositiveButton(R.string.view_beam_session,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface di, int i) {
                                BeamUtils.launchBeamSession(BeamActivity.this);
                                di.dismiss();
                            }
                        })
                .create()
                .show();
    }

    void showHelpDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.title_beam)
                .setMessage(R.string.beam_unlocked_help)
                .setNegativeButton(R.string.close, null)
                .setPositiveButton(R.string.view_beam_session,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface di, int i) {
                                BeamUtils.launchBeamSession(BeamActivity.this);
                                di.dismiss();
                            }
                        })
                .create()
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getSupportMenuInflater().inflate(R.menu.beam, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_beam_help) {
            showHelpDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
