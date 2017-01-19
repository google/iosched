/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.session;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.myschedule.MyScheduleActivity;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.util.BeamUtils;
import com.google.samples.apps.iosched.util.LogUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import static com.google.samples.apps.iosched.util.LogUtils.LOGE;

/**
 * Displays the details about a session. This Activity is launched via an {@code Intent} with {@link
 * Intent#ACTION_VIEW} and a {@link Uri} built with {@link com.google.samples.apps.iosched
 * .provider.ScheduleContract.Sessions#buildSessionUri(String)}.
 */
public class SessionDetailActivity extends BaseActivity {

    private static final String TAG = LogUtils.makeLogTag(SessionDetailActivity.class);

    private Handler mHandler = new Handler();

    private Uri mSessionUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UIUtils.tryTranslateHttpIntent(this);
        BeamUtils.tryUpdateIntentFromBeam(this);
        boolean shouldBeFloatingWindow = shouldBeFloatingWindow();
        if (shouldBeFloatingWindow) {
            setupFloatingWindow(R.dimen.session_details_floating_width,
                    R.dimen.session_details_floating_height, 1, 0.4f);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.session_detail_act);

        setToolbarAsUp(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.finishAfterTransition(SessionDetailActivity.this);
            }
        });
        final Toolbar toolbar = getToolbar();
        // Override the icon if shouldBeFloatingWindow
        if (shouldBeFloatingWindow) {
            toolbar.setNavigationIcon(R.drawable.ic_close);
        }
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                // Do not display the Activity name in the toolbar
                toolbar.setTitle("");
            }
        });

        if (savedInstanceState == null) {
            Uri sessionUri = getIntent().getData();
            BeamUtils.setBeamSessionUri(this, sessionUri);
        }

        mSessionUri = getIntent().getData();

        if (mSessionUri == null) {
            LOGE(TAG, "SessionDetailActivity started with null session Uri!");
            finish();
            return;
        }
    }

    public Uri getSessionUri() {
        return mSessionUri;
    }

    @Override
    public Intent getParentActivityIntent() {
        return new Intent(this, MyScheduleActivity.class);
    }

    public static void startSessionDetailActivity(final Activity activity,
            final String sessionId) {
        Uri data = ScheduleContract.Sessions.buildSessionUri
                (sessionId);
        Intent sessionDetailIntent = new Intent(activity,
                SessionDetailActivity.class);
        sessionDetailIntent.setData(data);
        activity.startActivity(sessionDetailIntent);
    }
}
