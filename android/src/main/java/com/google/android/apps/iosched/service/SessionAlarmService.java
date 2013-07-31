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

package com.google.android.apps.iosched.service;

import android.graphics.Bitmap;
import android.provider.BaseColumns;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.HomeActivity;
import com.google.android.apps.iosched.ui.MapFragment;
import com.google.android.apps.iosched.util.PrefUtils;
import com.google.android.apps.iosched.util.SessionsHelper;
import com.google.android.apps.iosched.util.UIUtils;

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
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

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

    private static final long MILLI_TEN_MINUTES = 600000;
    private static final long MILLI_ONE_MINUTE = 60000;

    private static final long UNDEFINED_ALARM_OFFSET = -1;
    private static final long UNDEFINED_VALUE = -1;

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

        final long sessionStart =
                intent.getLongExtra(SessionAlarmService.EXTRA_SESSION_START, UNDEFINED_VALUE);
        if (sessionStart == UNDEFINED_VALUE) return;

        final long sessionEnd =
                intent.getLongExtra(SessionAlarmService.EXTRA_SESSION_END, UNDEFINED_VALUE);
        if (sessionEnd == UNDEFINED_VALUE) return;

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
        final long currentTime = UIUtils.getCurrentTime(this);
        // If the session is already started, do not schedule system notification.
        if (currentTime > sessionStart) return;

        // By default, sets alarm to go off at 10 minutes before session start time.  If alarm
        // offset is provided, alarm is set to go off by that much time from now.
        long alarmTime;
        if (alarmOffset == UNDEFINED_ALARM_OFFSET) {
            alarmTime = sessionStart - MILLI_TEN_MINUTES;
        } else {
            alarmTime = currentTime + alarmOffset;
        }

        final Intent notifIntent = new Intent(
                ACTION_NOTIFY_SESSION,
                null,
                this,
                SessionAlarmService.class);
        // Setting data to ensure intent's uniqueness for different session start times.
        notifIntent.setData(
                new Uri.Builder().authority("com.google.android.apps.iosched")
                        .path(String.valueOf(sessionStart)).build());
        notifIntent.putExtra(SessionAlarmService.EXTRA_SESSION_START, sessionStart);
        notifIntent.putExtra(SessionAlarmService.EXTRA_SESSION_END, sessionEnd);
        notifIntent.putExtra(SessionAlarmService.EXTRA_SESSION_ALARM_OFFSET, alarmOffset);
        PendingIntent pi = PendingIntent.getService(this,
                0,
                notifIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        final AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        // Schedule an alarm to be fired to notify user of added sessions are about to begin.
        am.set(AlarmManager.RTC_WAKEUP, alarmTime, pi);
    }

    // Starred sessions are about to begin.  Constructs and triggers system notification.
    private void notifySession(final long sessionStart,
                               final long sessionEnd, final long alarmOffset) {
        long currentTime;
        if (sessionStart < (currentTime = UIUtils.getCurrentTime(this))) return;

        // Avoid repeated notifications.
        if (alarmOffset == UNDEFINED_ALARM_OFFSET && UIUtils.isNotificationFiredForBlock(
                this, ScheduleContract.Blocks.generateBlockId(sessionStart, sessionEnd))) {
            return;
        }

        final ContentResolver cr = getContentResolver();
        final Uri starredBlockUri = ScheduleContract.Blocks.buildStarredSessionsUri(
                ScheduleContract.Blocks.generateBlockId(sessionStart, sessionEnd)
        );
        Cursor c = cr.query(starredBlockUri,
                SessionDetailQuery.PROJECTION,
                null,
                null,
                null);
        int starredCount = 0;
        ArrayList<String> starredSessionTitles = new ArrayList<String>();
        ArrayList<String> starredSessionRoomIds = new ArrayList<String>();
        String sessionId = null; // needed to get session track icon
        while (c.moveToNext()) {
            sessionId = c.getString(SessionDetailQuery.SESSION_ID);
            starredCount = c.getInt(SessionDetailQuery.NUM_STARRED_SESSIONS);
            starredSessionTitles.add(c.getString(SessionDetailQuery.SESSION_TITLE));
            starredSessionRoomIds.add(c.getString(SessionDetailQuery.ROOM_ID));
        }
        if (starredCount < 1) {
            return;
        }

        // Generates the pending intent which gets fired when the user taps on the notification.
        // NOTE: Use TaskStackBuilder to comply with Android's design guidelines
        // related to navigation from notifications.
        PendingIntent pi = TaskStackBuilder.create(this)
                .addNextIntent(new Intent(this, HomeActivity.class))
                .addNextIntent(new Intent(Intent.ACTION_VIEW, starredBlockUri))
                .getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT);

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

        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(this)
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
        if (starredCount == 1) {
            // get the track icon to show as the notification big picture
            Uri tracksUri = ScheduleContract.Sessions.buildTracksDirUri(sessionId);
            Cursor tracksCursor = cr.query(tracksUri, SessionTrackQuery.PROJECTION,
                    null, null, null);
            if (tracksCursor.moveToFirst()) {
                String trackName = tracksCursor.getString(SessionTrackQuery.TRACK_NAME);
                int trackColour = tracksCursor.getInt(SessionTrackQuery.TRACK_COLOR);
                Bitmap trackIcon = UIUtils.getTrackIconSync(getApplicationContext(),
                        trackName, trackColour);
                if (trackIcon != null) {
                    notifBuilder.setLargeIcon(trackIcon);
                }
            }
        }
        if (minutesLeft > 5) {
            notifBuilder.addAction(R.drawable.ic_alarm_holo_dark,
                    String.format(res.getString(R.string.snooze_x_min), 5),
                    createSnoozeIntent(sessionStart, sessionEnd, 5));
        }
        if (starredCount == 1 && PrefUtils.isAttendeeAtVenue(this)) {
            notifBuilder.addAction(R.drawable.ic_map_holo_dark,
                    res.getString(R.string.title_map),
                    createRoomMapIntent(starredSessionRoomIds.get(0)));
        }
        NotificationCompat.InboxStyle richNotification = new NotificationCompat.InboxStyle(
                notifBuilder)
                .setBigContentTitle(res.getQuantityString(R.plurals.session_notification_title,
                        starredCount,
                        minutesLeft,
                        starredCount));

        // Adds starred sessions starting at this time block to the notification.
        for (int i = 0; i < starredCount; i++) {
            richNotification.addLine(starredSessionTitles.get(i));
        }
        NotificationManager nm = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIFICATION_ID, richNotification.build());
    }

    private PendingIntent createSnoozeIntent(final long sessionStart, final long sessionEnd,
                                             final int snoozeMinutes) {
        Intent scheduleIntent = new Intent(
                SessionAlarmService.ACTION_SCHEDULE_STARRED_BLOCK,
                null, this, SessionAlarmService.class);
        scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_START, sessionStart);
        scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_END, sessionEnd);
        scheduleIntent.putExtra(SessionAlarmService.EXTRA_SESSION_ALARM_OFFSET,
                snoozeMinutes * MILLI_ONE_MINUTE);
        return PendingIntent.getService(this, 0, scheduleIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private PendingIntent createRoomMapIntent(final String roomId) {
        Intent mapIntent = new Intent(getApplicationContext(),
                UIUtils.getMapActivityClass(getApplicationContext()));
        mapIntent.putExtra(MapFragment.EXTRA_ROOM, roomId);
        return PendingIntent.getActivity(this, 0, mapIntent, 0);
    }

    private void scheduleAllStarredBlocks() {
        final ContentResolver cr = getContentResolver();
        final Cursor c = cr.query(ScheduleContract.Sessions.CONTENT_STARRED_URI,
                new String[]{"distinct " + ScheduleContract.Sessions.BLOCK_START,
                        ScheduleContract.Sessions.BLOCK_END},
                null,
                null,
                null);
        if (c == null) {
            return;
        }

        while (c.moveToNext()) {
            final long sessionStart = c.getLong(0);
            final long sessionEnd = c.getLong(1);
            scheduleAlarm(sessionStart, sessionEnd, UNDEFINED_ALARM_OFFSET);
        }
    }

    public interface SessionDetailQuery {
        String[] PROJECTION = {
                ScheduleContract.Sessions.SESSION_ID,
                ScheduleContract.Blocks.NUM_STARRED_SESSIONS,
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.ROOM_ID
        };

        int SESSION_ID = 0;
        int NUM_STARRED_SESSIONS = 1;
        int SESSION_TITLE = 2;
        int ROOM_ID = 3;
    }

    public interface SessionTrackQuery {
        String[] PROJECTION = {
                ScheduleContract.Tracks.TRACK_ID,
                ScheduleContract.Tracks.TRACK_NAME,
                ScheduleContract.Tracks.TRACK_COLOR
        };

        int TRACK_ID = 0;
        int TRACK_NAME = 1;
        int TRACK_COLOR = 2;
    }
}
