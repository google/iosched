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

import com.google.samples.apps.iosched.welcome.WelcomeActivity;

/**
 * Utility methods related to the onboarding flow.
 */
public class WelcomeUtils {

    /**
     * Boolean indicating whether ToS has been accepted.
     */
    static final String PREF_TOS_ACCEPTED = "pref_tos_accepted" +
            Constants.CONFERENCE_YEAR_PREF_POSTFIX;

    /**
     * Boolean indicating whether the user declined notifications during onboarding.
     */
    private static final String PREF_DECLINED_NOTIFICATIONS = "pref_declined_notifications" +
            Constants.CONFERENCE_YEAR_PREF_POSTFIX;

    /**
     * Boolean indicating whether the user explicitly refused sign in during onboarding.
     */
    private static final String PREF_USER_REFUSED_SIGN_IN = "pref_user_refused_sign_in" +
            Constants.CONFERENCE_YEAR_PREF_POSTFIX;

    /**
     * Boolean indicating whether the user explicitly refused sign in during onboarding.
     */
    private static final String PREF_USER_ATTEMPTED_SIGN_IN = "pref_user_attempted_sign_in" +
            Constants.CONFERENCE_YEAR_PREF_POSTFIX;

    /**
     * Boolean for tracking whether to show post-onboarding message card.
     */
    private static final String PREF_SHOW_POST_ONBOARDING_CARD =
            "pref_show_onboarding_card" + Constants.CONFERENCE_YEAR_PREF_POSTFIX;

    /**
     * Return true if user has accepted the {@link WelcomeActivity Tos}, false if they haven't.
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static boolean isTosAccepted(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_TOS_ACCEPTED, false);
    }

    /**
     * Mark that the user has accepted the TOS so the app doesn't ask again.
     *
     * @param context Context to be used to edit the {@link android.content.SharedPreferences}.
     */
    public static void markTosAccepted(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_TOS_ACCEPTED, true).apply();
    }

    /**
     * Return true if user has declined notifications during onboarding, otherwise false.
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static boolean hasUserDeclinedNotificationsDuringOnboarding(Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_DECLINED_NOTIFICATIONS, false);
    }

    /**
     * Mark that the user has declined notifications during onboarding.
     *
     * @param context Context to be used to edit the {@link android.content.SharedPreferences}.
     */
    public static void markUserDeclinedNotificationsDuringOnboarding(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_DECLINED_NOTIFICATIONS, true).apply();
    }

    /**
     * Return true if user attempted to sign in during onboarding, otherwise false.
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static boolean hasUserAttemptedOnboardingSignIn(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_USER_ATTEMPTED_SIGN_IN, false);
    }

    /**
     * Mark that the user attempted to sign in. Whether sign in succeeds or fails, we do not show
     * the user the onboarding sign in screen again.
     *
     * @param context Context to be used to edit the {@link android.content.SharedPreferences}.
     */
    public static void markUserAttemptedOnboardingSignIn(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_USER_ATTEMPTED_SIGN_IN, true).apply();
    }

    /**
     * Return true if user refused to sign in during onboarding, otherwise false.
     *
     * @param context Context to be used to lookup the {@link android.content.SharedPreferences}.
     */
    public static boolean hasUserRefusedOnboardingSignIn(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_USER_REFUSED_SIGN_IN, false);
    }

    /**
     * Mark that the user explicitly chose not to sign in during onboarding.
     *
     * @param context Context to be used to edit the {@link android.content.SharedPreferences}.
     */
    public static void markUserRefusedOnboardingSignIn(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_USER_REFUSED_SIGN_IN, true).apply();
    }

    /**
     * Returns true if the user has clicked on the post-onboarding card displayed on My I/O. -->
     * @param context Context to be used to edit the {@link android.content.SharedPreferences}.
     */
    public static boolean showPostOnboardingCard(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_SHOW_POST_ONBOARDING_CARD, true);
    }

    /**
     * Mark that the user explicitly chose not to sign in during onboarding.
     *
     * @param context Context to be used to edit the {@link android.content.SharedPreferences}.
     */
    public static void markHidePostOnboardingCard(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_SHOW_POST_ONBOARDING_CARD, false).apply();
    }
}
