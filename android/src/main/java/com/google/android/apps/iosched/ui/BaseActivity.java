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

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.util.*;

import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A base activity that handles common functionality in the app.
 */
public abstract class BaseActivity extends ActionBarActivity {
    private static final String TAG = makeLogTag(BaseActivity.class);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EasyTracker.getInstance().setContext(this);
        if (!AccountUtils.isAuthenticated(this) || !PrefUtils.isSetupDone(this)) {
            LogUtils.LOGD(TAG, "exiting:"
                    + " isAuthenticated=" + AccountUtils.isAuthenticated(this)
                    + " isSetupDone=" + PrefUtils.isSetupDone(this));
            AccountUtils.startAuthenticationFlow(this, getIntent());
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Verifies the proper version of Google Play Services exists on the device.
        PlayServicesUtils.checkGooglePlaySevices(this);
    }

    protected void setHasTabs() {
        if (!UIUtils.isTablet(this)
                && getResources().getConfiguration().orientation
                != Configuration.ORIENTATION_LANDSCAPE) {
            // Only show the tab bar's shadow
            getSupportActionBar().setBackgroundDrawable(getResources().getDrawable(
                    R.drawable.actionbar_background_noshadow));
        }
    }

    /**
     * Sets the icon.
     */
    protected void setActionBarTrackIcon(String trackName, int trackColor) {
        if (trackColor == 0) {
            getSupportActionBar().setIcon(R.drawable.actionbar_icon);
            return;
        }

        new UIUtils.TrackIconAsyncTask(trackName, trackColor) {
            @Override
            protected void onPostExecute(Bitmap bitmap) {
                BitmapDrawable outDrawable = new BitmapDrawable(getResources(), bitmap);
                getSupportActionBar().setIcon(outDrawable);
            }
        }.execute(this);
    }

    /**
     * Converts an intent into a {@link Bundle} suitable for use as fragment arguments.
     */
    protected static Bundle intentToFragmentArguments(Intent intent) {
        Bundle arguments = new Bundle();
        if (intent == null) {
            return arguments;
        }

        final Uri data = intent.getData();
        if (data != null) {
            arguments.putParcelable("_uri", data);
        }

        final Bundle extras = intent.getExtras();
        if (extras != null) {
            arguments.putAll(intent.getExtras());
        }

        return arguments;
    }

    /**
     * Converts a fragment arguments bundle into an intent.
     */
    public static Intent fragmentArgumentsToIntent(Bundle arguments) {
        Intent intent = new Intent();
        if (arguments == null) {
            return intent;
        }

        final Uri data = arguments.getParcelable("_uri");
        if (data != null) {
            intent.setData(data);
        }

        intent.putExtras(arguments);
        intent.removeExtra("_uri");
        return intent;
    }

    @Override
    public void onStart() {
        super.onStart();
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EasyTracker.getInstance().activityStop(this);
    }
}
