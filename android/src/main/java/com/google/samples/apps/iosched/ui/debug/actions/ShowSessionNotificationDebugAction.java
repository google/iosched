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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.BaseMapActivity;
import com.google.samples.apps.iosched.ui.debug.DebugAction;
import com.google.samples.apps.iosched.util.UIUtils;

/**
* Show a notification that a session is about to start. Simplified version of the one shown at
* SessionAlarmService.
*/
public class ShowSessionNotificationDebugAction implements DebugAction {
    @Override
    public void run(Context context, Callback callback) {

        Intent i = new Intent(Intent.ACTION_VIEW,
                ScheduleContract.Sessions.buildSessionUri("__keynote__"));

        PendingIntent pi = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_CANCEL_CURRENT);
        Intent mapIntent = new Intent(context,
                UIUtils.getMapActivityClass(context));
        mapIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK |
                Intent.FLAG_ACTIVITY_TASK_ON_HOME);
        mapIntent.putExtra(BaseMapActivity.EXTRA_ROOM, "keynote");
        PendingIntent piMap = TaskStackBuilder
                .create(context)
                .addNextIntent(mapIntent)
                .getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT);

         //= PendingIntent.getActivity(context, 0, mapIntent, 0);

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(context)
                .setContentTitle("test notification")
                .setContentText("yep, this is a test")
                .setTicker("hey, you got a test")
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentIntent(pi)
                .setPriority(Notification.PRIORITY_MAX)
                .setAutoCancel(true);
        notifBuilder.addAction(R.drawable.ic_map_holo_dark,
                context.getString(R.string.title_map),
                piMap);

        NotificationCompat.InboxStyle richNotification = new NotificationCompat.InboxStyle(
                notifBuilder)
                .setBigContentTitle(context.getResources().getQuantityString(R.plurals.session_notification_title,
                        1,
                        8,
                        1));

        NotificationManager nm = (NotificationManager) context.getSystemService(
                Context.NOTIFICATION_SERVICE);
        nm.notify(32534, richNotification.build());


    }

    @Override
    public String getLabel() {
        return "Show \"about to start\" notif";
    }
}
