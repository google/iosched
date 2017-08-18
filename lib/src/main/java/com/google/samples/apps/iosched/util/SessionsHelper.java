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

package com.google.samples.apps.iosched.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.google.samples.apps.iosched.appwidget.ScheduleWidgetProvider;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.sync.SyncHelper;

import java.util.Date;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Helper class for dealing with common actions to take on a session.
 */
public class SessionsHelper {

    private static final String TAG = makeLogTag(SessionsHelper.class);

    private final Context mContext;

    public SessionsHelper(@NonNull Context context) {
        mContext = context;
    }

    public void setSessionStarred(Uri sessionUri, boolean starred, String title) {
        LOGD(TAG, "setSessionStarred uri=" + sessionUri + " starred=" +
                starred + " title=" + title);
        String sessionId = ScheduleContract.Sessions.getSessionId(sessionUri);
        Uri myScheduleUri = ScheduleContract.MySchedule.buildMyScheduleUri(
                AccountUtils.getActiveAccountName(mContext));

        @SuppressLint("HandlerLeak") // this is short-lived
                AsyncQueryHandler handler = new AsyncQueryHandler(mContext.getContentResolver()) {
        };
        final ContentValues values = new ContentValues();
        values.put(ScheduleContract.MySchedule.SESSION_ID, sessionId);
        values.put(ScheduleContract.MySchedule.MY_SCHEDULE_IN_SCHEDULE, starred ? 1 : 0);
        int offset = SyncUtils.getServerTimeOffset(mContext);
        values.put(ScheduleContract.MySchedule.MY_SCHEDULE_TIMESTAMP, new Date().getTime() +
                offset);
        handler.startInsert(-1, null, myScheduleUri, values);

        // ANALYTICS EVENT: Add or remove a session from the schedule
        // Contains: Session title, whether it was added or removed (starred or unstarred)
        AnalyticsHelper.sendEvent("Session", starred ? "Starred" : "Unstarred", "Session: " + title);

        // Because change listener is set to null during initialization, these
        // won't fire on pageview.
        mContext.sendBroadcast(ScheduleWidgetProvider.getRefreshBroadcastIntent(mContext, false));

        // Request an immediate user data sync to reflect the starred user sessions in the cloud
        SyncHelper.requestManualSync(true);

        // No need to manually setup calendar or notifications so they happen on sync
    }

    public void setReservationStatus(Uri sessionUri,
                                     @ScheduleContract.MyReservations.ReservationStatus int reservationStatus,
                                     String title) {
        LOGD(TAG, "setReservationStatus session uri=" + sessionUri + " reservationStatus=" +
                reservationStatus + " title=" + title);
        String accountName = AccountUtils.getActiveAccountName(mContext);
        String sessionId = ScheduleContract.Sessions.getSessionId(sessionUri);
        Uri myReservationsUri = ScheduleContract.MyReservations.buildMyReservationUri(accountName);

        @SuppressLint("HandlerLeak") // this is short-lived
                AsyncQueryHandler handler = new AsyncQueryHandler(mContext.getContentResolver()) {
        };
        final ContentValues values = new ContentValues();
        values.put(ScheduleContract.MyReservations.SESSION_ID, sessionId);
        values.put(ScheduleContract.MyReservations.MY_RESERVATION_STATUS, reservationStatus);
        values.put(ScheduleContract.MyReservations.MY_RESERVATION_ACCOUNT_NAME, accountName);
        int offset = SyncUtils.getServerTimeOffset(mContext);
        values.put(ScheduleContract.MyReservations.MY_RESERVATION_TIMESTAMP,
                System.currentTimeMillis() + offset);
        handler.startInsert(-1, null, myReservationsUri, values);
    }

    public static void showBookmarkClickedHint(View view, boolean isBookmarked) {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(
                view.getContext());

        if (sp.getBoolean(SettingsUtils.PREF_SKIP_BOOKMARK_HINTS, false)) {
            return;
        }

        // Note: isBookmarked represents the previous widget state.
        Snackbar.make(view,
                isBookmarked ? R.string.add_bookmark_hint : R.string.remove_bookmark_hint,
                Snackbar.LENGTH_LONG)
                .setAction(R.string.bookmark_hint_optout, new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        sp.edit().putBoolean(SettingsUtils.PREF_SKIP_BOOKMARK_HINTS, true).apply();
                    }
                })
                .setActionTextColor(ContextCompat.getColor(view.getContext(), R.color.aqua_marine))
                .show();
    }
}