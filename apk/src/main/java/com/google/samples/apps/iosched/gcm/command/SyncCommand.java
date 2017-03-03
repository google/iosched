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
package com.google.samples.apps.iosched.gcm.command;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import com.google.samples.apps.iosched.gcm.GCMCommand;
import com.google.samples.apps.iosched.sync.TriggerSyncReceiver;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.util.Random;

import static com.google.samples.apps.iosched.util.LogUtils.LOGI;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class SyncCommand extends GCMCommand {
    private static final String TAG = makeLogTag("SyncCommand");
    private static final int DEFAULT_TRIGGER_SYNC_MAX_JITTER_MILLIS = 15 * 60 * 1000; // 15 minutes
    private static final Random RANDOM = new Random();

    @Override
    public void execute(Context context, String type, String extraData) {
        LOGI(TAG, "Received GCM message: " + type);
        int syncJitter;
        SyncData syncData = null;
        if (extraData != null) {
            try {
                Gson gson = new Gson();
                syncData = gson.fromJson(extraData, SyncData.class);
            } catch (JsonSyntaxException e) {
                LOGI(TAG, "Error while decoding extraData: " + e.toString());
            }
        }

        if (syncData != null && syncData.sync_jitter != 0) {
            syncJitter = syncData.sync_jitter;
        } else {
            syncJitter = DEFAULT_TRIGGER_SYNC_MAX_JITTER_MILLIS;
        }

        scheduleSync(context, syncJitter);
    }

    private void scheduleSync(Context context, int syncJitter) {
        int jitterMillis = (int) (RANDOM.nextFloat() * syncJitter);
        final String debugMessage = "Scheduling next sync for " + jitterMillis + "ms";
        LOGI(TAG, debugMessage);

        ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE))
                .set(
                        AlarmManager.RTC,
                        System.currentTimeMillis() + jitterMillis,
                        PendingIntent.getBroadcast(
                                context,
                                0,
                                new Intent(context, TriggerSyncReceiver.class),
                                PendingIntent.FLAG_CANCEL_CURRENT));

    }

    class SyncData {
        private int sync_jitter;
        SyncData() {}
    }
}
