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

package com.google.samples.apps.iosched.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.samples.apps.iosched.welcome.WelcomeActivity;

import java.util.TimeZone;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Utilities and constants related to app settings_prefs.
 */
public class SettingsUtils {

    private static final String TAG = makeLogTag(SettingsUtils.class);

    /**
     * This is changed each year to effectively reset certain preferences that should be re-asked
     * each year. Note, res/xml/settings_prefs.xml must be updated when this value is updated.
     */
    public static final String CONFERENCE_YEAR_PREF_POSTFIX = "_2016";

    /**
     * Boolean preference indicating the user would like to see times in their local timezone
     * throughout the app.
     */
    public static final String PREF_LOCAL_TIMES = "pref_local_times";

    /**
     * Boolean indicating whether the app should attempt to sign in on startup (default true).
     */
    public static final String PREF_USER_REFUSED_SIGN_IN = "pref_user_refused_sign_in" +
            CONFERENCE_YEAR_PREF_POSTFIX;

    /**
     * Boolean indicating whether the debug build warning was already shown.
     */
    public static final String PREF_DEBUG_BUILD_WARNING_SHOWN = "pref_debug_build_warning_shown";

    /**
     * Boolean indicating whether ToS has been accepted.
     */
    public static final String PREF_TOS_ACCEPTED = "pref_tos_accepted" +
            CONFERENCE_YEAR_PREF_POSTFIX;

    /**
     * Boolean indicating whether CoC has been accepted.
     */
    private static final String PREF_CONDUCT_ACCEPTED = "pref_conduct_accepted" +
            CONFERENCE_YEAR_PREF_POSTFIX;

    /**
     * Boolean indicating whether ToS has been accepted.
     */
    public static final String PREF_DECLINED_WIFI_SETUP = "pref_declined_wifi_setup" +
            CONFERENCE_YEAR_PREF_POSTFIX;

    /**
     * Boolean indicating whether user has answered if they are local or remote.
     */
    public static final String PREF_ANSWERED_LOCAL_OR_REMOTE = "pref_answered_local_or_remote" +
            CONFERENCE_YEAR_PREF_POSTFIX;

    /**
     * Long indicating when a sync was last ATTEMPTED (not necessarily succeeded).
     */
    public static final String PREF_LAST_SYNC_ATTEMPTED = "pref_last_sync_attempted";

    /**
     * Long indicating when a sync last SUCCEEDED.
     */
    public static final String PREF_LAST_SYNC_SUCCEEDED = "pref_last_sync_succeeded";

    /**
     * Long storing the sync interval that's currently configured.
     */
    public static final String PREF_CUR_SYNC_INTERVAL = "pref_cur_sync_interval";

    /**
     * Boolean indicating app should sync sessions with local calendar
     */
    public static final String PREF_SYNC_CALENDAR = "pref_sync_calendar";

    /**
     * Boolean indicating whether the app has performed the (one-time) welcome flow.
     */
    public static final String PREF_WELCOME_DONE = "pref_welcome_done" +
            CONFERENCE_YEAR_PREF_POSTFIX;

    /**
     * Boolean indicating if the app can collect Analytics.
     */
    public static final String PREF_ANALYTICS_ENABLED = "pref_analytics_enabled";

    /**
     * Return the {@link TimeZone} the app is set to use (either user or conference).
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static TimeZone getDisplayTimeZone(Context context) {
        TimeZone defaultTz = TimeZone.getDefault();
        return (isUsingLocalTime(context) && defaultTz != null)
                ? defaultTz : Config.CONFERENCE_TIMEZONE;
    }

    /**
     * Return true if the user has indicated they want the schedule in local times, false if they
     * want to use the conference time zone. This preference is enabled/disabled by the user in the
     * {@link SettingsActivity}.
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static boolean isUsingLocalTime(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_LOCAL_TIMES, false);
    }

    /**
     * Return true if the user has indicated they're attending I/O in person. This preference can be
     * enabled/disabled by the user in the
     * {@link SettingsActivity}.
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static boolean isAttendeeAtVenue(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(BuildConfig.PREF_ATTENDEE_AT_VENUE, true);
    }

    /**
     * Mark that the app has finished loading the {@code R.raw.bootstrap_data bootstrap data}.
     *
     * @param context Context to be used to edit the {@link android.content.SharedPreferences}.
     */
    public static void markDataBootstrapDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(BuildConfig.PREF_DATA_BOOTSTRAP_DONE, true).apply();
    }

    /**
     * Return true when the {@code R.raw.bootstrap_data_json bootstrap data} has been marked loaded.
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static boolean isDataBootstrapDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(BuildConfig.PREF_DATA_BOOTSTRAP_DONE, false);
    }

    /**
     * Set the attendee preference indicating whether they'll be attending Google I/O on site.
     *
     * @param context  Context to be used to edit the {@link android.content.SharedPreferences}.
     * @param newValue New value that will be set.
     */
    public static void setAttendeeAtVenue(final Context context, final boolean newValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(BuildConfig.PREF_ATTENDEE_AT_VENUE, newValue).apply();
    }

    /**
     * Mark that the user explicitly chose not to sign in so app doesn't ask them again.
     *
     * @param context Context to be used to edit the {@link android.content.SharedPreferences}.
     */
    public static void markUserRefusedSignIn(final Context context, final boolean refused) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_USER_REFUSED_SIGN_IN, refused).apply();
    }

    /**
     * Return true if user refused to sign in, false if they haven't refused (yet).
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static boolean hasUserRefusedSignIn(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_USER_REFUSED_SIGN_IN, false);
    }

    /**
     * Return true if the
     * {@code com.google.samples.apps.iosched.welcome.WelcomeActivity.displayDogfoodWarningDialog() Dogfood Build Warning}
     * has already been marked as shown, false if not.
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static boolean wasDebugWarningShown(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_DEBUG_BUILD_WARNING_SHOWN, false);
    }

    /**
     * Mark the
     * {@code com.google.samples.apps.iosched.welcome.WelcomeActivity.displayDogfoodWarningDialog() Dogfood Build Warning}
     * shown to user.
     *
     * @param context Context to be used to edit the {@link android.content.SharedPreferences}.
     */
    public static void markDebugWarningShown(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_DEBUG_BUILD_WARNING_SHOWN, true).apply();
    }

    /**
     * Return true if user has accepted the
     * {@link WelcomeActivity Tos}, false if they haven't (yet).
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static boolean isTosAccepted(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_TOS_ACCEPTED, false);
    }

    /**
     * Return true if user has accepted the Code of
     * {@link com.google.samples.apps.iosched.welcome.ConductFragment Conduct}, false if they haven't (yet).
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static boolean isConductAccepted(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_CONDUCT_ACCEPTED, false);
    }

    /**
     * Mark {@code newValue whether} the user has accepted the TOS so the app doesn't ask again.
     *
     * @param context Context to be used to edit the {@link android.content.SharedPreferences}.
     * @param newValue New value that will be set.
     */
    public static void markTosAccepted(final Context context, boolean newValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_TOS_ACCEPTED, newValue).apply();
    }

    /**
     * Mark {@code newValue whether} the user has accepted the Code of Conduct so the app doesn't ask again.
     *
     * @param context Context to be used to edit the {@link android.content.SharedPreferences}.
     * @param newValue New value that will be set.
     */
    public static void markConductAccepted(final Context context, boolean newValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_CONDUCT_ACCEPTED, newValue).apply();
    }

    /**
     * Return true if user has already declined WiFi setup, but false if they haven't yet.
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static boolean hasDeclinedWifiSetup(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_DECLINED_WIFI_SETUP, false);
    }

    /**
     * Mark that the user has explicitly declined WiFi setup assistance.
     *
     * @param context  Context to be used to edit the {@link android.content.SharedPreferences}.
     * @param newValue New value that will be set.
     */
    public static void markDeclinedWifiSetup(final Context context, boolean newValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_DECLINED_WIFI_SETUP, newValue).apply();
    }

    /**
     * Returns true if user has already indicated whether they're a local or remote I/O attendee,
     * false if they haven't answered yet.
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static boolean hasAnsweredLocalOrRemote(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_ANSWERED_LOCAL_OR_REMOTE, false);
    }

    /**
     * Mark that the user answered whether they're a local or remote I/O attendee.
     *
     * @param context  Context to be used to edit the {@link android.content.SharedPreferences}.
     * @param newValue New value that will be set.
     */
    public static void markAnsweredLocalOrRemote(final Context context, boolean newValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_ANSWERED_LOCAL_OR_REMOTE, newValue).apply();
    }

    /**
     * Return true if the first-app-run-activities have already been executed.
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static boolean isFirstRunProcessComplete(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_WELCOME_DONE, false);
    }

    /**
     * Mark {@code newValue whether} this is the first time the first-app-run-processes have run.
     * Managed by {@link com.google.samples.apps.iosched.ui.BaseActivity the}
     * {@link BaseActivity two} base activities.
     *
     * @param context  Context to be used to edit the {@link android.content.SharedPreferences}.
     * @param newValue New value that will be set.
     */
    public static void markFirstRunProcessesDone(final Context context, boolean newValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_WELCOME_DONE, newValue).apply();
    }

    /**
     * Return a long representing the last time a sync was attempted (regardless of success).
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static long getLastSyncAttemptedTime(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getLong(PREF_LAST_SYNC_ATTEMPTED, 0L);
    }

    /**
     * Mark a sync was attempted (stores current time as 'last sync attempted' preference).
     *
     * @param context Context to be used to edit the {@link android.content.SharedPreferences}.
     */
    public static void markSyncAttemptedNow(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(PREF_LAST_SYNC_ATTEMPTED, TimeUtils.getCurrentTime(context)).apply();
    }

    /**
     * Return a long representing the last time a sync succeeded.
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static long getLastSyncSucceededTime(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getLong(PREF_LAST_SYNC_SUCCEEDED, 0L);
    }

    /**
     * Mark that a sync succeeded (stores current time as 'last sync succeeded' preference).
     *
     * @param context Context to be used to edit the {@link android.content.SharedPreferences}.
     */
    public static void markSyncSucceededNow(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(PREF_LAST_SYNC_SUCCEEDED, TimeUtils.getCurrentTime(context)).apply();
    }

    /**
     * Return true if analytics are enabled, false if user has disabled them.
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static boolean isAnalyticsEnabled(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_ANALYTICS_ENABLED, true);
    }

    /**
     * Return true if session reminders are enabled, false if user has disabled them.
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static boolean shouldShowSessionReminders(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(BuildConfig.PREF_SESSION_REMINDERS_ENABLED, false);
    }

    /**
     * @param context  Context to be used to edit the {@link android.content.SharedPreferences}.
     * @param show Whether app should show session reminders
     */
    public static void setShowSessionReminders(final Context context, boolean show) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(BuildConfig.PREF_SESSION_REMINDERS_ENABLED, show).apply();
    }

    /**
     * @param context  Context to be used to edit the {@link android.content.SharedPreferences}.
     * @param show Whether app should show session feedback reminders
     */
    public static void setShowSessionFeedbackReminders(final Context context, boolean show) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(BuildConfig.PREF_SESSION_FEEDBACK_REMINDERS_ENABLED, show).apply();
    }

    /**
     * Return true if session feedback reminders are enabled, false if user has disabled them.
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static boolean shouldShowSessionFeedbackReminders(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(BuildConfig.PREF_SESSION_FEEDBACK_REMINDERS_ENABLED, false);
    }

    /**
     * Return a long representing the current data sync interval time.
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static long getCurSyncInterval(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getLong(PREF_CUR_SYNC_INTERVAL, 0L);
    }

    /**
     * Set a new interval for the data sync time.
     *
     * @param context  Context to be used to edit the {@link android.content.SharedPreferences}.
     * @param newValue New value that will be set.
     */
    public static void setCurSyncInterval(final Context context, long newValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putLong(PREF_CUR_SYNC_INTERVAL, newValue).apply();
    }

    /**
     * Return true if calendar sync is enabled, false if disabled.
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static boolean shouldSyncCalendar(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_SYNC_CALENDAR, false);
    }

    /**
     * Helper method to register a settings_prefs listener. This method does not automatically handle
     * {@code unregisterOnSharedPreferenceChangeListener() un-registering} the listener at the end
     * of the {@code context} lifecycle.
     *
     * @param context  Context to be used to lookup the {@link android.content.SharedPreferences}.
     * @param listener Listener to register.
     */
    public static void registerOnSharedPreferenceChangeListener(final Context context,
                                                                SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.registerOnSharedPreferenceChangeListener(listener);
    }

    /**
     * Helper method to un-register a settings_prefs listener typically registered with
     * {@code registerOnSharedPreferenceChangeListener()}
     *
     * @param context  Context to be used to lookup the {@link android.content.SharedPreferences}.
     * @param listener Listener to un-register.
     */
    public static void unregisterOnSharedPreferenceChangeListener(final Context context,
                                                                  SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.unregisterOnSharedPreferenceChangeListener(listener);
    }
}
