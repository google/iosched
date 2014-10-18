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

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.google.samples.apps.iosched.Config;

import java.util.TimeZone;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Utilities and constants related to app preferences.
 */
public class PrefUtils  {
    private static final String TAG = makeLogTag("PrefUtils");

    /**
     * Boolean preference that when checked, indicates that the user would like to see times
     * in their local timezone throughout the app.
     */
    public static final String PREF_LOCAL_TIMES = "pref_local_times";

    /**
     * Boolean preference that when checked, indicates that the user will be attending the
     * conference.
     */
    public static final String PREF_ATTENDEE_AT_VENUE = "pref_attendee_at_venue";

    /**
     * Boolean preference that indicates whether we installed the boostrap data or not.
     */
    public static final String PREF_DATA_BOOTSTRAP_DONE = "pref_data_bootstrap_done";

    /**
     * Integer preference that indicates what conference year the application is configured
     * for. Typically, if this isn't an exact match, preferences should be wiped to re-run
     * setup.
     */
    public static final String PREF_CONFERENCE_YEAR = "pref_conference_year";

    /**
     * Boolean indicating whether we should attempt to sign in on startup (default true).
     */
    public static final String PREF_USER_REFUSED_SIGN_IN = "pref_user_refused_sign_in";

    /**
     * Boolean indicating whether the debug build warning was already shown.
     */
    public static final String PREF_DEBUG_BUILD_WARNING_SHOWN = "pref_debug_build_warning_shown";

    /** Boolean indicating whether ToS has been accepted */
    public static final String PREF_TOS_ACCEPTED = "pref_tos_accepted";

    /** Boolean indicating whether ToS has been accepted */
    public static final String PREF_DECLINED_WIFI_SETUP = "pref_declined_wifi_setup";

    /** Boolean indicating whether user has answered if they are local or remote. */
    public static final String PREF_ANSWERED_LOCAL_OR_REMOTE = "pref_answered_local_or_remote";

    /** Boolean indicating whether the user dismissed the I/O extended card. */
    public static final String PREF_DISMISSED_IO_EXTENDED_CARD = "pref_dismissed_io_extended_card";

    /** Boolean indicating whether the user has enabled BLE on the Nearby screen. */
    public static final String PREF_BLE_ENABLED = "pref_ble_enabled";

    /** Long indicating when a sync was last ATTEMPTED (not necessarily succeeded) */
    public static final String PREF_LAST_SYNC_ATTEMPTED = "pref_last_sync_attempted";

    /** Long indicating when a sync last SUCCEEDED */
    public static final String PREF_LAST_SYNC_SUCCEEDED = "pref_last_sync_succeeded";

    /** Sync interval that's currently configured */
    public static final String PREF_CUR_SYNC_INTERVAL = "pref_cur_sync_interval";

    /** Sync sessions with local calendar*/
    public static final String PREF_SYNC_CALENDAR  = "pref_sync_calendar";

    /**
     * Boolean indicating whether we performed the (one-time) welcome flow.
     */
    public static final String PREF_WELCOME_DONE = "pref_welcome_done";

    /** Boolean indicating if we can collect and Analytics */
    public static final String PREF_ANALYTICS_ENABLED = "pref_analytics_enabled";

    /** Boolean indicating whether to show session reminder notifications */
    public static final String PREF_SHOW_SESSION_REMINDERS = "pref_show_session_reminders";

    /** Boolean indicating whether to show session feedback notifications */
    public static final String PREF_SHOW_SESSION_FEEDBACK_REMINDERS
            = "pref_show_session_feedback_reminders";

    public static TimeZone getDisplayTimeZone(Context context) {
        TimeZone defaultTz = TimeZone.getDefault();
        return (isUsingLocalTime(context) && defaultTz != null)
                ? defaultTz : Config.CONFERENCE_TIMEZONE;
    }

    public static boolean isUsingLocalTime(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_LOCAL_TIMES, false);
    }

    public static void setUsingLocalTime(final Context context, final boolean usingLocalTime) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_LOCAL_TIMES, usingLocalTime).commit();
    }

    public static boolean isAttendeeAtVenue(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_ATTENDEE_AT_VENUE, true);
    }

    public static void markDataBootstrapDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_DATA_BOOTSTRAP_DONE, true).commit();
    }

    public static boolean isDataBootstrapDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_DATA_BOOTSTRAP_DONE, false);
    }

    public static void init(final Context context) {
        // Check what year we're configured for
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        int conferenceYear = sp.getInt(PREF_CONFERENCE_YEAR, 0);
        if (conferenceYear != Config.CONFERENCE_YEAR) {
            LOGD(TAG, "App not yet set up for " + PREF_CONFERENCE_YEAR + ". Resetting data.");
            // Application is configured for a different conference year. Reset preferences.
            sp.edit().clear().putInt(PREF_CONFERENCE_YEAR, Config.CONFERENCE_YEAR).commit();
        }
    }

    public static void setAttendeeAtVenue(final Context context, final boolean isAtVenue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_ATTENDEE_AT_VENUE, isAtVenue).commit();
    }

    public static void markUserRefusedSignIn(final Context context) {
        markUserRefusedSignIn(context, true);
    }

    public static void markUserRefusedSignIn(final Context context, final boolean refused) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_USER_REFUSED_SIGN_IN, refused).commit();
    }

    public static boolean hasUserRefusedSignIn(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_USER_REFUSED_SIGN_IN, false);
    }

    public static boolean wasDebugWarningShown(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_DEBUG_BUILD_WARNING_SHOWN, false);
    }

    public static void markDebugWarningShown(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_DEBUG_BUILD_WARNING_SHOWN, true).commit();
    }

    public static boolean isTosAccepted(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_TOS_ACCEPTED, false);
    }

    public static void markTosAccepted(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_TOS_ACCEPTED, true).commit();
    }

    public static boolean hasDeclinedWifiSetup(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_DECLINED_WIFI_SETUP, false);
    }

    public static void markDeclinedWifiSetup(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_DECLINED_WIFI_SETUP, true).commit();
    }

    public static boolean hasAnsweredLocalOrRemote(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_ANSWERED_LOCAL_OR_REMOTE, false);
    }

    public static void markAnsweredLocalOrRemote(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_ANSWERED_LOCAL_OR_REMOTE, true).commit();
    }

    public static boolean hasDismissedIOExtendedCard(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_DISMISSED_IO_EXTENDED_CARD, false);
    }

    public static void markDismissedIOExtendedCard(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_DISMISSED_IO_EXTENDED_CARD, true).commit();
    }

    public static boolean hasEnabledBle(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_BLE_ENABLED, false);
    }

    public static void setBleStatus(final Context context, boolean status) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_BLE_ENABLED, status).commit();
    }

    public static boolean isWelcomeDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_WELCOME_DONE, false);
    }

    public static void markWelcomeDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_WELCOME_DONE, true).commit();
    }

    public static long getLastSyncAttemptedTime(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getLong(PREF_LAST_SYNC_ATTEMPTED, 0L);
    }

    public static void markSyncAttemptedNow(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(PREF_LAST_SYNC_ATTEMPTED, UIUtils.getCurrentTime(context)).commit();
    }

    public static long getLastSyncSucceededTime(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getLong(PREF_LAST_SYNC_SUCCEEDED, 0L);
    }

    public static void markSyncSucceededNow(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(PREF_LAST_SYNC_SUCCEEDED, UIUtils.getCurrentTime(context)).commit();
    }

    /**
     * Returns whether or not we should offer to take the user to the Google I/O extended
     * website. If actively==true, will return whether we should offer actively (with a card,
     * for example); if actively==false, will return whether we should do so passively
     * (with an overflow item in the menu, for instance).
     */
    public static boolean shouldOfferIOExtended(final Context context, boolean actively) {
        boolean isRemote = !PrefUtils.isAttendeeAtVenue(context);
        boolean hasNotDismissed = !PrefUtils.hasDismissedIOExtendedCard(context);
        boolean conferenceGoingOn = !TimeUtils.hasConferenceEnded(context);

        if (actively) {
            return isRemote && hasNotDismissed && conferenceGoingOn;
        } else {
            return isRemote && conferenceGoingOn;
        }
    }

    public static boolean isAnalyticsEnabled(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_ANALYTICS_ENABLED, true);
    }

    public static boolean shouldShowSessionReminders(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_SHOW_SESSION_REMINDERS, true);
    }

    public static boolean shouldShowSessionFeedbackReminders(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_SHOW_SESSION_FEEDBACK_REMINDERS, true);
    }

    public static long getCurSyncInterval(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getLong(PREF_CUR_SYNC_INTERVAL, 0L);
    }

    public static void setCurSyncInterval(final Context context, long interval) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(PREF_CUR_SYNC_INTERVAL, interval).commit();
    }

    public static boolean shouldSyncCalendar(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_SYNC_CALENDAR, false);
    }

    public static void registerOnSharedPreferenceChangeListener(final Context context,
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.registerOnSharedPreferenceChangeListener(listener);
    }

    public static void unregisterOnSharedPreferenceChangeListener(final Context context,
                                                                  SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.unregisterOnSharedPreferenceChangeListener(listener);
    }
}
