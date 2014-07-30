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

package com.google.samples.apps.iosched.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.google.samples.apps.iosched.service.SessionAlarmService;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * {@link BroadcastReceiver} to reinitialize {@link android.app.AlarmManager} for all starred
 * session blocks.
 */
public class SessionAlarmReceiver extends BroadcastReceiver {
    public static final String TAG = makeLogTag(SessionAlarmReceiver.class);

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent scheduleIntent = new Intent(
                SessionAlarmService.ACTION_SCHEDULE_ALL_STARRED_BLOCKS,
                null, context, SessionAlarmService.class);
        context.startService(scheduleIntent);
    }
}
