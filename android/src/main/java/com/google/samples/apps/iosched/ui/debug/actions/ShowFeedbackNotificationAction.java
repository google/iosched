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
package com.google.samples.apps.iosched.ui.debug.actions;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.service.SessionAlarmService;
import com.google.samples.apps.iosched.ui.NfcBadgeActivity;
import com.google.samples.apps.iosched.ui.debug.DebugAction;
import com.google.samples.apps.iosched.util.UIUtils;

/**
 * Forces the display of a session feedback notification.
 */
public class ShowFeedbackNotificationAction implements DebugAction {
    private static int sNext = 0;

    @Override
    public void run(final Context context, final Callback callback) {
        final String sessionId = SessionAlarmService.DEBUG_SESSION_ID;
        final String sessionTitle = "Debugging with Placeholder Text";
        final String sessionRoom = "Room 1";
        final String sessionSpeaker = "Lauren Ipsum";

        Intent intent = new Intent(
                SessionAlarmService.ACTION_NOTIFY_SESSION_FEEDBACK,
                null, context, SessionAlarmService.class);
        intent.putExtra(SessionAlarmService.EXTRA_SESSION_ID, sessionId);
        intent.putExtra(SessionAlarmService.EXTRA_SESSION_START, System.currentTimeMillis()
                - 30 * 60 * 1000);
        intent.putExtra(SessionAlarmService.EXTRA_SESSION_END, System.currentTimeMillis());
        intent.putExtra(SessionAlarmService.EXTRA_SESSION_TITLE, sessionTitle);
        intent.putExtra(SessionAlarmService.EXTRA_SESSION_ROOM, sessionRoom);
        intent.putExtra(SessionAlarmService.EXTRA_SESSION_SPEAKERS, sessionSpeaker);
        context.startService(intent);
        Toast.makeText(context, "Showing debug session feedback notification.", Toast.LENGTH_LONG).show();
    }

    @Override
    public String getLabel() {
        return "show session feedback notification";
    }
}
