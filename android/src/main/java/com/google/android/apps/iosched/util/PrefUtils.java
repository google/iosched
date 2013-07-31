/*
 * Copyright 2013 Google Inc.
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

package com.google.android.apps.iosched.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceManager;
import com.google.android.apps.iosched.Config;

import java.util.TimeZone;

/**
 * Utilities and constants related to app preferences.
 */
public class PrefUtils {
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
     * Boolean preference that when checked, indicates that the user has completed account
     * authentication and the initial set up flow.
     */
    public static final String PREF_SETUP_DONE = "pref_setup_done";

    /**
     * Integer preference that indicates what conference year the application is configured
     * for. Typically, if this isn't an exact match, preferences should be wiped to re-run
     * setup.
     */
    public static final String PREF_CONFERENCE_YEAR = "pref_conference_year";

    /**
     * Boolean indicating whether a user's DevSite profile is available. Defaults to true.
     */
    public static final String PREF_DEVSITE_PROFILE_AVAILABLE = "pref_devsite_profile_available";

    private static int sIsUsingLocalTime = -1;
    private static int sAttendeeAtVenue = -1;

    public static TimeZone getDisplayTimeZone(Context context) {
        return isUsingLocalTime(context)
                ? TimeZone.getDefault()
                : UIUtils.CONFERENCE_TIME_ZONE;
    }

    public static boolean isUsingLocalTime(Context context) {
        return isUsingLocalTime(context, false);
    }

    public static boolean isUsingLocalTime(Context context, boolean forceRequery) {
        if (sIsUsingLocalTime == -1 || forceRequery) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sIsUsingLocalTime = sp.getBoolean(PREF_LOCAL_TIMES, false) ? 1 : 0;
        }

        return sIsUsingLocalTime == 1;
    }

    public static void setUsingLocalTime(final Context context, final boolean usingLocalTime) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_LOCAL_TIMES, usingLocalTime).commit();
    }

    public static boolean isAttendeeAtVenue(final Context context) {
        return isAttendeeAtVenue(context, false);
    }

    public static boolean isAttendeeAtVenue(final Context context, boolean forceRequery) {
        if (sAttendeeAtVenue == -1 || forceRequery) {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
            sAttendeeAtVenue = sp.getBoolean(PREF_ATTENDEE_AT_VENUE, false) ? 1 : 0;
        }

        return sAttendeeAtVenue == 1;
    }

    public static void markSetupDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_SETUP_DONE, true).commit();
    }

    public static boolean isSetupDone(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);

        // Check what year we're configured for
        int conferenceYear = sp.getInt(PREF_CONFERENCE_YEAR, 0);
        if (conferenceYear != Config.CONFERENCE_YEAR) {
            // Application is configured for a different conference year. Reset
            // preferences and re-run setup.
            sp.edit().clear().putInt(PREF_CONFERENCE_YEAR, Config.CONFERENCE_YEAR).commit();
        }

        return sp.getBoolean(PREF_SETUP_DONE, false);
    }

    public static void setAttendeeAtVenue(final Context context, final boolean isAtVenue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_ATTENDEE_AT_VENUE, isAtVenue).commit();
    }

    public static void markDevSiteProfileAvailable(final Context context, final boolean isAvailable) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_DEVSITE_PROFILE_AVAILABLE, isAvailable).commit();
    }

    public static boolean isDevsiteProfileAvailable(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_DEVSITE_PROFILE_AVAILABLE, true);
    }
}
