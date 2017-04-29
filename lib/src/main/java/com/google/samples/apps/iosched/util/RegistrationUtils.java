/*
 * Copyright (c) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.samples.apps.iosched.util;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;

import com.google.samples.apps.iosched.lib.BuildConfig;
import com.google.samples.apps.iosched.settings.SettingsUtils;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * Utility methods dealing with I/O user registration.
 */
public class RegistrationUtils {

    @Retention(SOURCE)
    @IntDef({REGSTATUS_UNKNOWN,
            REGSTATUS_UNREGISTERED,
            REGSTATUS_REGISTERED})
    public @interface RegistrationStatus {}

    public static final int REGSTATUS_UNKNOWN = -1;
    public static final int REGSTATUS_UNREGISTERED = 0;
    public static final int REGSTATUS_REGISTERED = 1;

    /**
     * Check if the user is a registered I/O attendee.
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     * @return True if the user is registered, false if they are not.
     *         Returns null if the registration status is not yet known.
     */
    @SuppressWarnings("WrongConstant")
    public static @RegistrationStatus int isRegisteredAttendee(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getInt(BuildConfig.PREF_ATTENDEE_AT_VENUE, REGSTATUS_UNKNOWN);
    }

    /**
     * Sets the value indicating whether the user is a registered I/O attendee.
     *
     * @param context  Context to be used to edit the {@link android.content.SharedPreferences}.
     * @param registered True if the user is a registered attendee.
     */
    public static void setRegisteredAttendee(final Context context, final boolean registered) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt(BuildConfig.PREF_ATTENDEE_AT_VENUE,
                registered ? REGSTATUS_REGISTERED : REGSTATUS_UNREGISTERED)
                .apply();
        SettingsUtils.updateNotificationSubscriptions(context);
    }

    /**
     * Clears the registered attendee status field, so that isRegisteredAttendee returns
     * REGSTATUS_UNKNOWN.
     *
     * @param context  Context to be used to edit the {@link android.content.SharedPreferences}.
     */
    public static void clearRegisteredAttendee(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putInt(BuildConfig.PREF_ATTENDEE_AT_VENUE, REGSTATUS_UNKNOWN).apply();
        SettingsUtils.updateNotificationSubscriptions(context);
    }
}
