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

package com.google.samples.apps.iosched.service;

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
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.BaseMapActivity;
import com.google.samples.apps.iosched.ui.BrowseSessionsActivity;
import com.google.samples.apps.iosched.ui.MyScheduleActivity;
import com.google.samples.apps.iosched.ui.SessionFeedbackActivity;
import com.google.samples.apps.iosched.util.FeedbackUtils;
import com.google.samples.apps.iosched.util.PrefUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Background service to handle scheduling of starred session notification via
 * {@link android.app.AlarmManager}.
 */
public class SessionAlarmService extends IntentService
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = makeLogTag(SessionAlarmService.class);

    public static final String ACTION_NOTIFY_SESSION =
            "com.google.samples.apps.iosched.action.NOTIFY_SESSION";
    public static final String ACTION_NOTIFY_SESSION_FEEDBACK =
            "com.google.samples.apps.iosched.action.NOTIFY_SESSION_FEEDBACK";
    public static final String ACTION_SCHEDULE_FEEDBACK_NOTIFICATION =
            "com.google.samples.apps.iosched.action.SCHEDULE_FEEDBACK_NOTIFICATION";
    public static final String ACTION_SCHEDULE_STARRED_BLOCK =
            "com.google.samples.apps.iosched.action.SCHEDULE_STARRED_BLOCK";
    public static final String ACTION_SCHEDULE_ALL_STARRED_BLOCKS =
            "com.google.samples.apps.iosched.action.SCHEDULE_ALL_STARRED_BLOCKS";
    public static final String EXTRA_SESSION_START =
            "com.google.samples.apps.iosched.extra.SESSION_START";
    public static final String EXTRA_SESSION_END =
            "com.google.samples.apps.iosched.extra.SESSION_END";
    public static final String EXTRA_SESSION_ALARM_OFFSET =
            "com.google.samples.apps.iosched.extra.SESSION_ALARM_OFFSET";
    public static final String EXTRA_SESSION_ID =
            "com.google.samples.apps.iosched.extra.SESSION_ID";
    public static final String EXTRA_SESSION_TITLE =
            "com.google.samples.apps.iosched.extra.SESSION_TITLE";
    public static final String EXTRA_SESSION_ROOM =
            "com.google.samples.apps.iosched.extra.SESSION_ROOM";
    public static final String EXTRA_SESSION_SPEAKERS =
            "com.google.samples.apps.iosched.extra.SESSION_SPEAKERS";

    public static final int NOTIFICATION_ID = 100;
    public static final int FEEDBACK_NOTIFICATION_ID = 101;

    // pulsate every 1 second, indicating a relatively high degree of urgency
    private static final int NOTIFICATION_LED_ON_MS = 100;
    private static final int NOTIFICATION_LED_OFF_MS = 1000;
    private static final int NOTIFICATION_ARGB_COLOR = 0xff0088ff; // cyan

    private static final long MILLI_TEN_MINUTES = 600000;
    private static final long MILLI_FIVE_MINUTES = 300000;
    private static final long MILLI_ONE_MINUTE = 60000;

    private static final long UNDEFINED_ALARM_OFFSET = -1;
    private static final long UNDEFINED_VALUE = -1;
    public static final String ACTION_NOTIFICATION_DISMISSAL
            = "com.google.sample.apps.iosched.ACTION_NOTIFICATION_DISMISSAL";
    private GoogleApiClient mGoogleApiClient;
    public static final String KEY_SESSION_ID = "session-id";
    private static final String KEY_SESSION_NAME = "session-name";
    private static final String KEY_SPEAKER_NAME = "speaker-name";
    private static final String KEY_SESSION_ROOM = "session-room";
    public static final String PATH_FEEDBACK = "/iowear/feedback";

    // special session ID that identifies a debug notification
    public static final String DEBUG_SESSION_ID = "debug-session-id";

    public SessionAlarmService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mGoogleApiClient.blockingConnect(2000, TimeUnit.MILLISECONDS);
        final String action = intent.getAction();

        LOGD(TAG, "SessionAlarmService handling " + action);

        if (ACTION_SCHEDULE_ALL_STARRED_BLOCKS.equals(action)) {
            LOGD(TAG, "Scheduling all starred blocks.");
            scheduleAllStarredBlocks();
            scheduleAllStarredSessionFeedbacks();
            return;
        } else if (ACTION_NOTIFY_SESSION_FEEDBACK.equals(action)) {
            LOGD(TAG, "Showing session feedback notification.");
            notifySessionFeedback(DEBUG_SESSION_ID.equals(intent.getStringExtra(EXTRA_SESSION_ID)));
            return;
        }

        final long sessionEnd = intent.getLongExtra(SessionAlarmService.EXTRA_SESSION_END,
                UNDEFINED_VALUE);
        if (sessionEnd == UNDEFINED_VALUE) {
            LOGD(TAG, "IGNORING ACTION -- missing sessionEnd parameter");
            return;
        }

        final long sessionAlarmOffset =
                intent.getLongExtra(SessionAlarmService.EXTRA_SESSION_ALARM_OFFSET,
                        UNDEFINED_ALARM_OFFSET);
        LOGD(TAG, "Session alarm offset is: " + sessionAlarmOffset);

        // Feedback notifications have a slightly different set of extras.
        if (ACTION_SCHEDULE_FEEDBACK_NOTIFICATION.equals(action)) {
            final String sessionId = intent.getStringExtra(SessionAlarmService.EXTRA_SESSION_ID);
            final String sessionTitle = intent.getStringExtra(
                    SessionAlarmService.EXTRA_SESSION_TITLE);
            if (sessionTitle == null || sessionEnd == UNDEFINED_VALUE ||
                    sessionId == null) {
                LOGE(TAG, "Attempted to schedule for feedback without providing extras.");
                return;
            }
            LOGD(TAG, "Scheduling feedback alarm for session: " + sessionTitle);
            scheduleFeedbackAlarm(sessionEnd, sessionAlarmOffset, sessionTitle);
            return;
        }

        final long sessionStart =
                intent.getLongExtra(SessionAlarmService.EXTRA_SESSION_START, UNDEFINED_VALUE);
        if (sessionStart == UNDEFINED_VALUE) {
            LOGD(TAG, "IGNORING ACTION -- no session start parameter.");
            return;
        }

        if (ACTION_NOTIFY_SESSION.equals(action)) {
            LOGD(TAG, "Notifying about sessions starting at " +
                    sessionStart + " = " + (new Date(sessionStart)).toString());
            LOGD(TAG, "-> Alarm offset: " + sessionAlarmOffset);
            notifySession(sessionStart, sessionAlarmOffset);
        } else if (ACTION_SCHEDULE_STARRED_BLOCK.equals(action)) {
            LOGD(TAG, "Scheduling session alarm.");
            LOGD(TAG, "-> Session start: " + sessionStart + " = " + (new Date(sessionStart))
                    .toString());
            LOGD(TAG, "-> Session end: " + sessionEnd + " = " + (new Date(sessionEnd)).toString());
            LOGD(TAG, "-> Alarm offset: " + sessionAlarmOffset);
            scheduleAlarm(sessionStart, sessionEnd, sessionAlarmOffset);
        }
    }

    public void scheduleFeedbackAlarm(final long sessionEnd,
            final long alarmOffset, final String sessionTitle) {
        // By default, feedback alarms fire 5 minutes before session end time. If alarm offset is
        // provided, alarm is set to go off that much time from now (useful for testing).
        long alarmTime;
        if (alarmOffset == UNDEFINED_ALARM_OFFSET) {
            alarmTime = sessionEnd - MILLI_FIVE_MINUTES;
        } else {
            alarmTime = UIUtils.getCurrentTime(this) + alarmOffset;
        }

        LOGD(TAG, "Scheduling session feedback alarm for session '" + sessionTitle + "'");
        LOGD(TAG, "  -> end time: " + sessionEnd + " = " + (new Date(sessionEnd)).toString());
        LOGD(TAG, "  -> alarm time: " + alarmTime + " = " + (new Date(alarmTime)).toString());

        final Intent feedbackIntent = new Intent(
                ACTION_NOTIFY_SESSION_FEEDBACK,
                null,
                this,
                SessionAlarmService.class);
        PendingIntent pi = PendingIntent.getService(
                this, 1, feedbackIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        final AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, alarmTime, pi);
    }

    private void scheduleAlarm(final long sessionStart,
            final long sessionEnd, final long alarmOffset) {

        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(NOTIFICATION_ID);
        final long currentTime = UIUtils.getCurrentTime(this);
        // If the session is already started, do not schedule system notification.
        if (currentTime > sessionStart) {
            LOGD(TAG, "Not scheduling alarm because target time is in the past: " + sessionStart);
            return;
        }

        // By default, sets alarm to go off at 10 minutes before session start time.  If alarm
        // offset is provided, alarm is set to go off by that much time from now.
        long alarmTime;
        if (alarmOffset == UNDEFINED_ALARM_OFFSET) {
            alarmTime = sessionStart - MILLI_TEN_MINUTES;
        } else {
            alarmTime = currentTime + alarmOffset;
        }

        LOGD(TAG, "Scheduling alarm for " + alarmTime + " = " + (new Date(alarmTime)).toString());

        final Intent notifIntent = new Intent(
                ACTION_NOTIFY_SESSION,
                null,
                this,
                SessionAlarmService.class);
        // Setting data to ensure intent's uniqueness for different session start times.
        notifIntent.setData(
                new Uri.Builder().authority("com.google.samples.apps.iosched")
                        .path(String.valueOf(sessionStart)).build()
        );
        notifIntent.putExtra(SessionAlarmService.EXTRA_SESSION_START, sessionStart);
        LOGD(TAG, "-> Intent extra: session start " + sessionStart);
        notifIntent.putExtra(SessionAlarmService.EXTRA_SESSION_END, sessionEnd);
        LOGD(TAG, "-> Intent extra: session end " + sessionEnd);
        notifIntent.putExtra(SessionAlarmService.EXTRA_SESSION_ALARM_OFFSET, alarmOffset);
        LOGD(TAG, "-> Intent extra: session alarm offset " + alarmOffset);
        PendingIntent pi = PendingIntent.getService(this,
                0,
                notifIntent,
                PendingIntent.FLAG_CANCEL_CURRENT);
        final AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        // Schedule an alarm to be fired to notify user of added sessions are about to begin.
        LOGD(TAG, "-> Scheduling RTC_WAKEUP alarm at " + alarmTime);
        am.set(AlarmManager.RTC_WAKEUP, alarmTime, pi);
    }

    // A starred session is about to end; notify the user to provide session feedback.
    // Constructs and triggers a system notification. Does nothing if the session has already
    // concluded.
    private void notifySessionFeedback(boolean debug) {
        LOGD(TAG, "Considering firing notification for session feedback.");

        if (debug) {
            LOGD(TAG, "Note: this is a debug notification.");
        }

        // Don't fire notification if this feature is disabled in settings
        if (!PrefUtils.shouldShowSessionFeedbackReminders(this)) {
            LOGD(TAG, "Skipping session feedback notification. Disabled in settings.");
            return;
        }

        final Cursor c = getContentResolver().query(
                ScheduleContract.Sessions.CONTENT_MY_SCHEDULE_URI,
                SessionsNeedingFeedbackQuery.PROJECTION,
                SessionsNeedingFeedbackQuery.WHERE_CLAUSE, null, null);
        if (c == null) {
            return;
        }

        List<String> needFeedbackIds = new ArrayList<String>();
        List<String> needFeedbackTitles = new ArrayList<String>();
        while (c.moveToNext()) {
            String sessionId = c.getString(SessionsNeedingFeedbackQuery.SESSION_ID);
            String sessionTitle = c.getString(SessionsNeedingFeedbackQuery.SESSION_TITLE);

            // Avoid repeated notifications.
            if (UIUtils.isFeedbackNotificationFiredForSession(this, sessionId)) {
                LOGD(TAG, "Skipping repeated session feedback notification for session '"
                        + sessionTitle + "'");
                continue;
            }

            needFeedbackIds.add(sessionId);
            needFeedbackTitles.add(sessionTitle);
        }

        if (needFeedbackIds.size() == 0) {
            // the user has already been notified of all sessions needing feedback
            return;
        }

        LOGD(TAG, "Going forward with session feedback notification for "
                + needFeedbackIds.size() + " session(s).");

        final Resources res = getResources();

        // this is used to synchronize deletion of notifications on phone and wear
        Intent dismissalIntent = new Intent(ACTION_NOTIFICATION_DISMISSAL);
        // TODO: fix Wear dismiss integration
        //dismissalIntent.putExtra(KEY_SESSION_ID, sessionId);
        PendingIntent dismissalPendingIntent = PendingIntent
                .getService(this, (int) new Date().getTime(), dismissalIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        String provideFeedbackTicker = res.getString(R.string.session_feedback_notification_ticker);
        NotificationCompat.Builder notifBuilder = new NotificationCompat.Builder(this)
                .setColor(getResources().getColor(R.color.theme_primary))
                .setContentText(provideFeedbackTicker)
                .setTicker(provideFeedbackTicker)
                .setLights(
                        SessionAlarmService.NOTIFICATION_ARGB_COLOR,
                        SessionAlarmService.NOTIFICATION_LED_ON_MS,
                        SessionAlarmService.NOTIFICATION_LED_OFF_MS)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setPriority(Notification.PRIORITY_LOW)
                .setLocalOnly(true) // make it local to the phone
                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)
                .setDeleteIntent(dismissalPendingIntent)
                .setAutoCancel(true);

        if (needFeedbackIds.size() == 1) {
            // Only 1 session needs feedback
            Uri sessionUri = ScheduleContract.Sessions.buildSessionUri(needFeedbackIds.get(0));
            PendingIntent pi = TaskStackBuilder.create(this)
                    .addNextIntent(new Intent(this, MyScheduleActivity.class))
                    .addNextIntent(new Intent(Intent.ACTION_VIEW, sessionUri, this,
                            SessionFeedbackActivity.class))
                    .getPendingIntent(1, PendingIntent.FLAG_CANCEL_CURRENT);

            notifBuilder.setContentTitle(needFeedbackTitles.get(0))
                    .setContentIntent(pi);
        } else {
            // Show information about several sessions that need feedback
            PendingIntent pi = TaskStackBuilder.create(this)
                    .addNextIntent(new Intent(this, MyScheduleActivity.class))
                    .getPendingIntent(1, PendingIntent.FLAG_CANCEL_CURRENT);

            NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
            inboxStyle.setBigContentTitle(provideFeedbackTicker);
            for (String title : needFeedbackTitles) {
                inboxStyle.addLine(title);
            }

            notifBuilder.setContentTitle(
                    getResources().getQuantityString(R.plurals.session_plurals,
                            needFeedbackIds.size(), needFeedbackIds.size()))
                    .setStyle(inboxStyle)
                    .setContentIntent(pi);
        }

        NotificationManager nm = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        LOGD(TAG, "Now showing session feedback notification!");
        nm.notify(FEEDBACK_NOTIFICATION_ID, notifBuilder.build());

        for (int i = 0; i < needFeedbackIds.size(); i++) {
            setupNotificationOnWear(needFeedbackIds.get(i), null, needFeedbackTitles.get(i), null);
        }
    }

    /**
     * Builds corresponding notification for the Wear device that is paired to this handset. This
     * is done by adding a Data Item to teh Data Store; the Wear device will be notified to build a
     * local notification.
     */
    private void setupNotificationOnWear(String sessionId, String sessionRoom, String sessionName,
            String speaker) {
        if (!mGoogleApiClient.isConnected()) {
            Log.e(TAG, "setupNotificationOnWear(): Failed to send data item since there was no "
                    + "connectivity to Google API Client");
            return;
        }
        PutDataMapRequest putDataMapRequest = PutDataMapRequest
                .create(FeedbackUtils.getFeedbackPath(sessionId));
        putDataMapRequest.getDataMap().putLong("time", new Date().getTime());
        putDataMapRequest.getDataMap().putString(KEY_SESSION_ID, sessionId);
        putDataMapRequest.getDataMap().putString(KEY_SESSION_NAME, sessionName);
        putDataMapRequest.getDataMap().putString(KEY_SPEAKER_NAME, speaker);
        putDataMapRequest.getDataMap().putString(KEY_SESSION_ROOM, sessionRoom);

        PutDataRequest request = putDataMapRequest.asPutDataRequest();

        Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(DataApi.DataItemResult dataItemResult) {
                        LOGD(TAG, "setupNotificationOnWear(): Sending notification result success:"
                                        + dataItemResult.getStatus().isSuccess()
                        );
                    }
                });
    }

    // Starred sessions are about to begin.  Constructs and triggers system notification.
    private void notifySession(final long sessionStart, final long alarmOffset) {
        long currentTime = UIUtils.getCurrentTime(this);
        final long intervalEnd = sessionStart + MILLI_TEN_MINUTES;
        LOGD(TAG, "Considering notifying for time interval.");
        LOGD(TAG, "    Interval start: " + sessionStart + "=" + (new Date(sessionStart)).toString());
        LOGD(TAG, "    Interval end: " + intervalEnd + "=" + (new Date(intervalEnd)).toString());
        LOGD(TAG, "    Current time is: " + currentTime + "=" + (new Date(currentTime)).toString());
        if (sessionStart < currentTime) {
            LOGD(TAG, "Skipping session notification (too late -- time interval already started)");
            return;
        }

        if (!PrefUtils.shouldShowSessionReminders(this)) {
            // skip if disabled in settings
            LOGD(TAG, "Skipping session notification for sessions. Disabled in settings.");
            return;
        }

        // Avoid repeated notifications.
        if (alarmOffset == UNDEFINED_ALARM_OFFSET && UIUtils.isNotificationFiredForBlock(
                this, ScheduleContract.Blocks.generateBlockId(sessionStart, intervalEnd))) {
            LOGD(TAG, "Skipping session notification (already notified)");
            return;
        }

        final ContentResolver cr = getContentResolver();

        LOGD(TAG, "Looking for sessions in interval "  + sessionStart + " - " + intervalEnd);
        Cursor c = cr.query(
                ScheduleContract.Sessions.CONTENT_MY_SCHEDULE_URI,
                SessionDetailQuery.PROJECTION,
                ScheduleContract.Sessions.STARTING_AT_TIME_INTERVAL_SELECTION,
                ScheduleContract.Sessions.buildAtTimeIntervalArgs(sessionStart, intervalEnd),
                null);
        int starredCount = c.getCount();
        LOGD(TAG, "# starred sessions in that interval: " + c.getCount());
        String singleSessionId = null;
        String singleSessionRoomId = null;
        ArrayList<String> starredSessionTitles = new ArrayList<String>();
        while (c.moveToNext()) {
            singleSessionId = c.getString(SessionDetailQuery.SESSION_ID);
            singleSessionRoomId = c.getString(SessionDetailQuery.ROOM_ID);
            starredSessionTitles.add(c.getString(SessionDetailQuery.SESSION_TITLE));
            LOGD(TAG, "-> Title: " + c.getString(SessionDetailQuery.SESSION_TITLE));
        }
        if (starredCount < 1) {
            return;
        }

        // Generates the pending intent which gets fired when the user taps on the notification.
        // NOTE: Use TaskStackBuilder to comply with Android's design guidelines
        // related to navigation from notifications.
        Intent baseIntent = new Intent(this, MyScheduleActivity.class);
        baseIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        TaskStackBuilder taskBuilder = TaskStackBuilder.create(this)
                .addNextIntent(baseIntent);

        // For a single session, tapping the notification should open the session details (b/15350787)
        if (starredCount == 1) {
            taskBuilder.addNextIntent(new Intent(Intent.ACTION_VIEW,
                    ScheduleContract.Sessions.buildSessionUri(singleSessionId)));
        }

        PendingIntent pi = taskBuilder.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT);

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
                .setColor(getResources().getColor(R.color.theme_primary))
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
            notifBuilder.addAction(R.drawable.ic_alarm_holo_dark,
                    String.format(res.getString(R.string.snooze_x_min), 5),
                    createSnoozeIntent(sessionStart, intervalEnd, 5));
        }
        if (starredCount == 1 && PrefUtils.isAttendeeAtVenue(this)) {
            notifBuilder.addAction(R.drawable.ic_map_holo_dark,
                    res.getString(R.string.title_map),
                    createRoomMapIntent(singleSessionRoomId));
        }
        String bigContentTitle;
        if (starredCount == 1 && starredSessionTitles.size() > 0) {
            bigContentTitle = starredSessionTitles.get(0);
        } else {
            bigContentTitle = res.getQuantityString(R.plurals.session_notification_title,
                    starredCount,
                    minutesLeft,
                    starredCount);
        }
        NotificationCompat.InboxStyle richNotification = new NotificationCompat.InboxStyle(
                notifBuilder)
                .setBigContentTitle(bigContentTitle);

        // Adds starred sessions starting at this time block to the notification.
        for (int i = 0; i < starredCount; i++) {
            richNotification.addLine(starredSessionTitles.get(i));
        }
        NotificationManager nm = (NotificationManager) getSystemService(
                Context.NOTIFICATION_SERVICE);
        LOGD(TAG, "Now showing notification.");
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
        mapIntent.putExtra(BaseMapActivity.EXTRA_ROOM, roomId);
        mapIntent.putExtra(BaseMapActivity.EXTRA_DETACHED_MODE, true);
        return TaskStackBuilder
                .create(getApplicationContext())
                .addNextIntent(new Intent(this, BrowseSessionsActivity.class))
                .addNextIntent(mapIntent)
                .getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT);
    }

    private void scheduleAllStarredBlocks() {
        final ContentResolver cr = getContentResolver();
        final Cursor c = cr.query(ScheduleContract.Sessions.CONTENT_MY_SCHEDULE_URI,
                new String[]{"distinct " + ScheduleContract.Sessions.SESSION_START,
                        ScheduleContract.Sessions.SESSION_END,
                        ScheduleContract.Sessions.SESSION_IN_MY_SCHEDULE},
                null,
                null,
                null
        );
        if (c == null) {
            return;
        }

        while (c.moveToNext()) {
            final long sessionStart = c.getLong(0);
            final long sessionEnd = c.getLong(1);
            scheduleAlarm(sessionStart, sessionEnd, UNDEFINED_ALARM_OFFSET);
        }
    }

    // Schedules feedback alarms for all starred sessions.
    private void scheduleAllStarredSessionFeedbacks() {
        final ContentResolver cr = getContentResolver();
        // TODO: Should we also check that SESSION_IN_MY_SCHEDULE is true?
        final Cursor c = cr.query(ScheduleContract.Sessions.CONTENT_MY_SCHEDULE_URI,
                new String[]{
                        ScheduleContract.Sessions.SESSION_TITLE,
                        ScheduleContract.Sessions.SESSION_END,
                        ScheduleContract.Sessions.SESSION_IN_MY_SCHEDULE,
                },
                null,
                null,
                null
        );
        if (c == null) {
            return;
        }
        while (c.moveToNext()) {
            final String sessionTitle = c.getString(0);
            final long sessionEnd = c.getLong(1);
            scheduleFeedbackAlarm(sessionEnd, UNDEFINED_ALARM_OFFSET, sessionTitle);
        }
    }

    public interface SessionDetailQuery {

        String[] PROJECTION = {
                ScheduleContract.Sessions.SESSION_ID,
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.ROOM_ID,
                ScheduleContract.Sessions.SESSION_IN_MY_SCHEDULE
        };

        int SESSION_ID = 0;
        int SESSION_TITLE = 1;
        int ROOM_ID = 2;
    }

    public interface SessionsNeedingFeedbackQuery {
        String[] PROJECTION = {
                ScheduleContract.Sessions.SESSION_ID,
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.SESSION_IN_MY_SCHEDULE,
                ScheduleContract.Sessions.HAS_GIVEN_FEEDBACK,
        };

        int SESSION_ID = 0;
        int SESSION_TITLE = 1;

        public static final String WHERE_CLAUSE =
                ScheduleContract.Sessions.HAS_GIVEN_FEEDBACK + "=0";
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Connected to Google Api Service");
        }
    }

    @Override
    public void onConnectionSuspended(int cause) {
        // Ignore
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "Disconnected from Google Api Service");
        }
    }

}
