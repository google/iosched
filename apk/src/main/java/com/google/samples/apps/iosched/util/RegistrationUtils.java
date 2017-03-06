package com.google.samples.apps.iosched.util;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.google.samples.apps.iosched.BuildConfig;

/**
 * Utility methods dealing with I/O user registration.
 */
public class RegistrationUtils {
    /**
     * Return true if the user is a registered I/O attendee.
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static boolean isRegisteredAttendee(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(BuildConfig.PREF_ATTENDEE_AT_VENUE, true);
    }

    /**
     * Sets the value indicating whether the user is a registered I/O attendee.
     *
     * @param context  Context to be used to edit the {@link android.content.SharedPreferences}.
     * @param newValue New value that will be set.
     */
    public static void setRegisteredAttendee(final Context context, final boolean newValue) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(BuildConfig.PREF_ATTENDEE_AT_VENUE, newValue).apply();
    }
}
