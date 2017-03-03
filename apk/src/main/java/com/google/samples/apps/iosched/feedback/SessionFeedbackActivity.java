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

package com.google.samples.apps.iosched.feedback;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.View;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.myschedule.MyScheduleActivity;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.util.BeamUtils;

import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Displays the questions and rating bars, as well as a comment box, for the user to provide
 * feedback on a session. The {@code mSessionUri} should be passed with the {@link
 * android.content.Intent} starting this Activity.
 */
public class SessionFeedbackActivity extends BaseActivity {

    private final static String TAG = makeLogTag(SessionFeedbackActivity.class);

    private Uri mSessionUri = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.session_feedback_act);

        if (savedInstanceState == null) {
            Uri sessionUri = getIntent().getData();
            BeamUtils.setBeamSessionUri(this, sessionUri);
        }

        mSessionUri = getIntent().getData();

        if (mSessionUri == null) {
            LOGE(TAG, "SessionFeedbackActivity started with null data URI!");
            finish();
        }

        setToolbarAsUp(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                NavUtils.navigateUpTo(SessionFeedbackActivity.this,
                        getParentActivityIntent());
            }
        });
    }

    @Override
    public Intent getParentActivityIntent() {
        // Up to this session's track details, or Home if no track is available
        if (mSessionUri != null) {
            return new Intent(Intent.ACTION_VIEW, mSessionUri);
        } else {
            return new Intent(this, MyScheduleActivity.class);
        }
    }

    public Uri getSessionUri() {
        return mSessionUri;
    }
}
