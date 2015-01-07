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

package com.google.samples.apps.iosched.iowear;

import static com.google.samples.apps.iosched.iowear.utils.Utils.LOGD;
import static com.google.samples.apps.iosched.iowear.utils.Utils.makeLogTag;

import com.google.samples.apps.iosched.R;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/**
 * The activity that builds the notification page.
 */
public class NotificationActivity extends Activity {

    private static final String TAG = makeLogTag("NotificationActivity");
    private TextView mSession;
    private TextView mSpeaker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);
        setupViews();

        Bundle b = this.getIntent().getExtras();
        if (null != b) {
            final String sessionId = b.getString(HomeListenerService.KEY_SESSION_ID);
            LOGD(TAG, "Received session id in NotificationActivity: " + sessionId);
            final String sessionName = b.getString(HomeListenerService.KEY_SESSION_NAME);
            final String sessionRoom = b.getString(HomeListenerService.KEY_SESSION_ROOM);
            final String speakers = getIntent()
                    .getStringExtra(HomeListenerService.KEY_SPEAKER_NAME);
            mSession.setText(sessionName);
            mSpeaker.setText(speakers + " - " + sessionRoom);
        }
    }

    private void setupViews() {
        Typeface typeFace = Typeface.createFromAsset(getAssets(), "RobotoCondensed-Light.ttf");
        mSession = (TextView) findViewById(R.id.session_name);
        mSpeaker = (TextView) findViewById(R.id.speaker_room);
        TextView title = (TextView) findViewById(R.id.title);
        mSession.setTypeface(typeFace);
        mSpeaker.setTypeface(typeFace);
        title.setTypeface(typeFace);
    }
}
