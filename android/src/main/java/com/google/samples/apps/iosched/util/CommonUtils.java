package com.google.samples.apps.iosched.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Methods used by other utility and helper classes.
 */
public class CommonUtils {
    public static SharedPreferences getSharedPreferences(final Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context);
    }
}
