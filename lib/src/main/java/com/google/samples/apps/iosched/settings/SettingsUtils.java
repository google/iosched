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

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.lib.BuildConfig;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.util.Constants;
import com.google.samples.apps.iosched.util.RegistrationUtils;
import com.google.samples.apps.iosched.util.TimeUtils;

import java.util.TimeZone;

/**
 * Utilities and constants related to app settings_prefs.
 */
public class SettingsUtils {

    /**
     * This is changed each year to effectively reset certain preferences that should be re-asked
     * each year. Note, res/xml/settings_prefs.xml must be updated when this value is updated.
     */

    /**
     * Boolean preference indicating the user would like to see times in their local timezone
     * throughout the app.
     */
    public static final String PREF_LOCAL_TIMES = "pref_local_times";
    /**
     * Boolean indicating whether ToS has been accepted.
     */
    public static final String PREF_DECLINED_WIFI_SETUP = "pref_declined_wifi_setup" +
            Constants.CONFERENCE_YEAR_PREF_POSTFIX;
    /**
     * Boolean indicating app should sync sessions with local calendar
     */
    public static final String PREF_SYNC_CALENDAR = "pref_sync_calendar";
    /**
     * Boolean indicating if the app can collect Analytics.
     */
    public static final String PREF_ANALYTICS_ENABLED = "pref_analytics_enabled";
    /**
     * Firebase messaging topic for notifications applying to all app users who enabled
     * notifications.
     */
    public static final String GENERIC_NEWS_TOPIC = "generic_news";
    /**
     * Firebase messaging topic for notifications applying to users that are conference attendees
     * who enabled notifications.
     */
    public static final String ONSITE_NEWS_TOPIC = "generic_news";
    /**
     * Firebase messaging topic for notifications applying to users that are not attendees
     * but enabled notifications.
     */
    public static final String OFFSITE_NEWS_TOPIC = "generic_news";
    /**
     * Boolean indicating whether the debug build warning was already shown.
     */
    private static final String PREF_DEBUG_BUILD_WARNING_SHOWN = "pref_debug_build_warning_shown";
    /**
     * Long indicating when a sync was last ATTEMPTED (not necessarily succeeded).
     */
    private static final String PREF_LAST_SYNC_ATTEMPTED = "pref_last_sync_attempted";
    /**
     * Long indicating when a sync last SUCCEEDED.
     */
    private static final String PREF_LAST_SYNC_SUCCEEDED = "pref_last_sync_succeeded";
    /**
     * Long storing the sync interval that's currently configured.
     */
    private static final String PREF_CUR_SYNC_INTERVAL = "pref_cur_sync_interval";
    /**
     * Boolean indicating whether the app has performed the (one-time) welcome flow.
     */
    private static final String PREF_WELCOME_DONE = "pref_welcome_done" +
            Constants.CONFERENCE_YEAR_PREF_POSTFIX;
    /**
     * Boolean indicating the user has opted-out of getting bookmark hints.
     */
    public static final String PREF_SKIP_BOOKMARK_HINTS = "pref_skip_bookmark_hints";

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
     * {@link SettingsActivity}. If not explicitly set, it will default to conference time if
     * they are attending and local time if they are not attending.
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static boolean isUsingLocalTime(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_LOCAL_TIMES, RegistrationUtils.isRegisteredAttendee(context) !=
                RegistrationUtils.REGSTATUS_REGISTERED);
    }

    /**
     * Set whether the user wants to use their local time zone or the conference
     * time zone.
     *
     * @param usingLocalTime true if they want to use their local timezone, false if they want to
     *                       use the conference's time zone.
     */
    public static void setUsingLocalTime(Context context, boolean usingLocalTime) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_LOCAL_TIMES, usingLocalTime).apply();
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
     * Return true if notifications are enabled, false if user has disabled them.
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static boolean shouldShowNotifications(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(BuildConfig.PREF_NOTIFICATIONS_ENABLED, false);
    }

    /**
     * @param context Context to be used to edit the {@link android.content.SharedPreferences}.
     * @param show    Whether app should send anonymous usage statistics
     */
    public static void setEnableAnalytics(final Context context, boolean show) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_ANALYTICS_ENABLED, show).apply();
    }

    /**
     * @param context Context to be used to edit the {@link android.content.SharedPreferences}.
     * @param show    Whether app should show notifications
     */
    public static void setShowNotifications(final Context context, boolean show) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(BuildConfig.PREF_NOTIFICATIONS_ENABLED, show).apply();
        updateNotificationSubscriptions(context);
    }

    /**
     * @param context Context to be used to edit the {@link android.content.SharedPreferences}.
     * @param show    Whether app should show session feedback reminders
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
    static void registerOnSharedPreferenceChangeListener(final Context context,
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
    static void unregisterOnSharedPreferenceChangeListener(final Context context,
            SharedPreferences.OnSharedPreferenceChangeListener listener) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.unregisterOnSharedPreferenceChangeListener(listener);
    }

    public static void updateNotificationSubscriptions(Context context) {
        if (shouldShowNotifications(context)) {
            FirebaseMessaging.getInstance().subscribeToTopic(GENERIC_NEWS_TOPIC);
            if (RegistrationUtils.isRegisteredAttendee(context) ==
                    RegistrationUtils.REGSTATUS_REGISTERED) {
                FirebaseMessaging.getInstance().subscribeToTopic(ONSITE_NEWS_TOPIC);
            } else {
                FirebaseMessaging.getInstance().subscribeToTopic(OFFSITE_NEWS_TOPIC);
            }
        } else {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(GENERIC_NEWS_TOPIC);
            FirebaseMessaging.getInstance().unsubscribeFromTopic(ONSITE_NEWS_TOPIC);
            FirebaseMessaging.getInstance().unsubscribeFromTopic(OFFSITE_NEWS_TOPIC);
        }
    }
}
