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

package com.google.android.apps.iosched.calendar;

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.util.UIUtils;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.support.v4.app.NotificationCompat;

import java.util.ArrayList;

import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Background service to handle scheduling of starred session notification via
 * {@link android.app.AlarmManager}.
 */
public class SessionAlarmService extends IntentService {
    private static final String TAG = makeLogTag(SessionAlarmService.class);

    public static final String ACTION_NOTIFY_SESSION =
            "com.google.android.apps.iosched.action.NOTIFY_SESSION";
    public static final String ACTION_SCHEDULE_STARRED_BLOCK =
            "com.google.android.apps.iosched.action.SCHEDULE_STARRED_BLOCK";
    public static final String ACTION_SCHEDULE_ALL_STARRED_BLOCKS =
            "com.google.android.apps.iosched.action.SCHEDULE_ALL_STARRED_BLOCKS";
    public static final String EXTRA_SESSION_START =
            "com.google.android.apps.iosched.extra.SESSION_START";
    public static final String EXTRA_SESSION_END =
            "com.google.android.apps.iosched.extra.SESSION_END";
    public static final String EXTRA_SESSION_ALARM_OFFSET =
            "com.google.android.apps.iosched.extra.SESSION_ALARM_OFFSET";

    private static final int NOTIFICATION_ID = 100;

    // pulsate every 1 second, indicating a relatively high degree of urgency
    private static final int NOTIFICATION_LED_ON_MS = 100;
    private static final int NOTIFICATION_LED_OFF_MS = 1000;
    private static final int NOTIFICATION_ARGB_COLOR = 0xff0088ff; // cyan

    private static final long ONE_MINUTE_MILLIS = 1 * 60 * 1000;
    private static final long TEN_MINUTES_MILLIS = 10 * ONE_MINUTE_MILLIS;

    private static final long UNDEFINED_ALARM_OFFSET = -1;

    public SessionAlarmService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final String action = intent.getAction();
        if (ACTION_SCHEDULE_ALL_STARRED_BLOCKS.equals(action)) {
            scheduleAllStarredBlocks();
            return;
        }

        final long sessionStart = intent.getLongExtra(SessionAlarmService.EXTRA_SESSION_START, -1);
        if (sessionStart == -1) {
            return;
        }

        final long sessionEnd = intent.getLongExtra(SessionAlarmService.EXTRA_SESSION_END, -1);
        if (sessionEnd == -1) {
            return;
        }

        final long sessionAlarmOffset =
                intent.getLongExtra(SessionAlarmService.EXTRA_SESSION_ALARM_OFFSET,
                        UNDEFINED_ALARM_OFFSET);

        if (ACTION_NOTIFY_SESSION.equals(action)) {
            notifySession(sessionStart, sessionEnd, sessionAlarmOffset);
        } else if (ACTION_SCHEDULE_STARRED_BLOCK.equals(action)) {
            scheduleAlarm(sessionStart, sessionEnd, sessionAlarmOffset);
        }
    }

    private void scheduleAlarm(final long sessionStart,
            final long sessionEnd, final long alarmOffset) {

        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
        final long currentTime = System.currentTimeMillis();

        // If the session is already started, do not schedule system notification.
        if (currentTime > sessionStart) {
            return;
        }

        // By default, sets alarm to go off at 10 minutes before session start time.  If alarm
        // offset is provided, alarm is set to go off by that much time from now.
        long alarmTime;
        if (alarmOffset == UNDEFINED_ALARM_OFFSET) {
            alarmTime = sessionStart - TEN_MINUTES_MILLIS;
        } else {
            alarmTime = currentTime + alarmOffset;
        }

        final Intent alarmIntent = new Intent(
                ACTION_NOTIFY_SESSION,
                null,
                this,
                SessionAlarmService.class);

        // Setting data to ensure intent's uniqueness for different session start times.
        alarmIntent.setData(
                new Uri.Builder()
                        .authority(ScheduleContract.CONTENT_AUTHORITY)
                        .path(String.valueOf(sessionStart))
                        .build());
        alarmIntent.putExtra(SessionAlarmService.EXTRA_SESSION_START, sessionStart);
        alarmIntent.putExtra(SessionAlarmService.EXTRA_SESSION_END, sessionEnd);
        alarmIntent.putExtra(SessionAlarmService.EXTRA_SESSION_ALARM_OFFSET, alarmOffset);
        final AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

        // Schedule an alarm to be fired to notify user of added sessions are about to begin.
        am.set(AlarmManager.RTC_WAKEUP, alarmTime,
                PendingIntent.getService(this, 0, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT));
    }

    /**
     * Constructs and triggers system notification for when starred sessions are about to begin.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void notifySession(final long sessionStart,
            final long sessionEnd, final long alarmOffset) {
        long currentTime = System.currentTimeMillis();
        if (sessionStart < currentTime) {
            return;
        }

        // Avoid repeated notifications.
        if (alarmOffset == UNDEFINED_ALARM_OFFSET && UIUtils.isNotificationFiredForBlock(
                this, ScheduleContract.Blocks.generateBlockId(sessionStart, sessionEnd))) {
            return;
        }

        final ContentResolver resolver = getContentResolver();
        final Uri starredBlockUri = ScheduleContract.Blocks.buildStarredSessionsUri(
                ScheduleContract.Blocks.generateBlockId(sessionStart, sessionEnd));
        Cursor cursor = resolver.query(starredBlockUri,
                new String[]{
                        ScheduleContract.Blocks.NUM_STARRED_SESSIONS,
                        ScheduleContract.Sessions.SESSION_TITLE
                },
                null, null, null);
        int starredCount = 0;
        ArrayList<String> starredSessionTitles = new ArrayList<String>();
        while (cursor.moveToNext()) {
            starredSessionTitles.add(cursor.getString(1));
            starredCount = cursor.getInt(0);
        }

        if (starredCount < 1) {
            return;
        }

        // Generates the pending intent which gets fired when the user touches on the notification.
        Intent sessionIntent = new Intent(Intent.ACTION_VIEW, starredBlockUri);
        sessionIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, sessionIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);

        final Resources res = getResources();
        String contentText;
        int minutesLeft = (int) (sessionStart - currentTime + 59000) / 60000;
        if (minutesLeft < 1) {
            minutesLeft = 1;
        }

        if (starredCount == 1) {
            contentText = res.getString(R.string.session_notification_text_1, minutesLeft);
        } else {
            contentText = res.getQuantityString(R.plurals.session_notification_text,
                    starredCount - 1,
                    minutesLeft,
                    starredCount - 1);
        }

        // Construct a notification. Use Jelly Bean (API 16) rich notifications if possible.
        Notification notification;
        if (UIUtils.hasJellyBean()) {
            // Rich notifications
            Notification.Builder builder = new Notification.Builder(this)
                    .setContentTitle(starredSessionTitles.get(0))
                    .setContentText(contentText)
                    .setTicker(res.getQuantityString(R.plurals.session_notification_ticker,
                            starredCount,
                            starredCount))
                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                    .setLights(
                            SessionAlarmService.NOTIFICATION_ARGB_COLOR,
                            SessionAlarmService.NOTIFICATION_LED_ON_MS,
                            SessionAlarmService.NOTIFICATION_LED_OFF_MS)
                    .setSmallIcon(R.drawable.ic_stat_notification)
                    .setContentIntent(pi)
                    .setPriority(Notification.PRIORITY_MAX)
                    .setAutoCancel(true);
            if (minutesLeft > 5) {
                builder.addAction(R.drawable.ic_alarm_holo_dark,
                        String.format(res.getString(R.string.snooze_x_min), 5),
                        createSnoozeIntent(sessionStart, sessionEnd, 5));
            }
            Notification.InboxStyle richNotification = new Notification.InboxStyle(
                    builder)
                    .setBigContentTitle(res.getQuantityString(R.plurals.session_notification_title,
                            starredCount,
                            minutesLeft,
                            starredCount));

            // Adds starred sessions starting at this time block to the notification.
            for (int i = 0; i < starredCount; i++) {
                richNotification.addLine(starredSessionTitles.get(i));
            }
            notification = richNotification.build();

        } else {
            // Pre-Jelly Bean non-rich notifications
            notification = new NotificationCompat.Builder(this)
                    .setContentTitle(starredSessionTitles.get(0))
                    .setContentText(contentText)
                    .setTicker(res.getQuantityString(R.plurals.session_notification_ticker,
                            starredCount,
                            starredCount))
                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                    .setLights(
                            SessionAlarmService.NOTIFICATION_ARGB_COLOR,
                            SessionAlarmService.NOTIFICATION_LED_ON_MS,
                            SessionAlarmService.NOTIFICATION_LED_OFF_MS)
                    .setSmallIcon(R.drawable.ic_stat_notification)
                    .setContentIntent(pi)
                    .getNotification();
        }
        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, notification);
    }

    private PendingIntent createSnoozeIntent(final long sessionStart, final long sessionEnd,
            final int snoozeMinutes) {
        Intent scheduleIntent = new Intent(
                SessionAlarmService.ACTION_SCHEDULE_STARRED_BLOCK,
                null, this, SessionAlarmService.class);
        scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_START, sessionStart);
        scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_END, sessionEnd);
        scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_ALARM_OFFSET,
                snoozeMinutes * ONE_MINUTE_MILLIS);
        return PendingIntent.getService(this, 0, scheduleIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private void scheduleAllStarredBlocks() {
        final Cursor cursor = getContentResolver().query(
                ScheduleContract.Sessions.CONTENT_STARRED_URI,
                new String[] {
                        "distinct " + ScheduleContract.Sessions.BLOCK_START,
                        ScheduleContract.Sessions.BLOCK_END
                },
                null, null, null);
        if (cursor == null) {
            return;
        }

        while (cursor.moveToNext()) {
            final long sessionStart = cursor.getLong(0);
            final long sessionEnd = cursor.getLong(1);
            scheduleAlarm(sessionStart, sessionEnd, UNDEFINED_ALARM_OFFSET);
        }
    }
}
