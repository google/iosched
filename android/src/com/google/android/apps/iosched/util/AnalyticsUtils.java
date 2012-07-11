/*
 * Copyright 2011 Google Inc.
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

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

/**
 * Helper singleton class for the Google Analytics tracking library.
 */
public class AnalyticsUtils {
    private static final String TAG = "AnalyticsUtils";

    GoogleAnalyticsTracker mTracker;
    private Context mApplicationContext;

    /**
     * The analytics tracking code for the app.
     */
    // TODO: insert your Analytics UA code here.
    private static final String UACODE = "INSERT_YOUR_ANALYTICS_UA_CODE_HERE";

    private static final int VISITOR_SCOPE = 1;
    private static final String FIRST_RUN_KEY = "firstRun";
    private static final boolean ANALYTICS_ENABLED = true;

    private static AnalyticsUtils sInstance;

    /**
     * Returns the global {@link AnalyticsUtils} singleton object, creating one if necessary.
     */
    public static AnalyticsUtils getInstance(Context context) {
        if (!ANALYTICS_ENABLED) {
            return sEmptyAnalyticsUtils;
        }

        if (sInstance == null) {
            if (context == null) {
                return sEmptyAnalyticsUtils;
            }
            sInstance = new AnalyticsUtils(context);
        }

        return sInstance;
    }

    private AnalyticsUtils(Context context) {
        if (context == null) {
            // This should only occur for the empty Analytics utils object.
            return;
        }

        mApplicationContext = context.getApplicationContext();
        mTracker = GoogleAnalyticsTracker.getInstance();

        // Unfortunately this needs to be synchronous.
        mTracker.start(UACODE, 300, mApplicationContext);

        Log.d(TAG, "Initializing Analytics");

        // Since visitor CV's should only be declared the first time an app runs, check if
        // it's run before. Add as necessary.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(
                mApplicationContext);
        final boolean firstRun = prefs.getBoolean(FIRST_RUN_KEY, true);
        if (firstRun) {
            Log.d(TAG, "Analytics firstRun");

            String apiLevel = Integer.toString(Build.VERSION.SDK_INT);
            String model = Build.MODEL;
            mTracker.setCustomVar(1, "apiLevel", apiLevel, VISITOR_SCOPE);
            mTracker.setCustomVar(2, "model", model, VISITOR_SCOPE);

            // Close out so we never run this block again, unless app is removed & =
            // reinstalled.
            prefs.edit().putBoolean(FIRST_RUN_KEY, false).commit();
        }
    }

    public void trackEvent(final String category, final String action, final String label,
            final int value) {
        // We wrap the call in an AsyncTask since the Google Analytics library writes to disk
        // on its calling thread.
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    mTracker.trackEvent(category, action, label, value);
                    Log.d(TAG, "iosched Analytics trackEvent: "
                            + category + " / " + action + " / " + label + " / " + value);
                } catch (Exception e) {
                    // We don't want to crash if there's an Analytics library exception.
                    Log.w(TAG, "iosched Analytics trackEvent error: "
                            + category + " / " + action + " / " + label + " / " + value, e);
                }
                return null;
            }
        }.execute();
    }

    public void trackPageView(final String path) {
        // We wrap the call in an AsyncTask since the Google Analytics library writes to disk
        // on its calling thread.
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    mTracker.trackPageView(path);
                    Log.d(TAG, "iosched Analytics trackPageView: " + path);
                } catch (Exception e) {
                    // We don't want to crash if there's an Analytics library exception.
                    Log.w(TAG, "iosched Analytics trackPageView error: " + path, e);
                }
                return null;
            }
        }.execute();
    }

    /**
     * Empty instance for use when Analytics is disabled or there was no Context available.
     */
    private static AnalyticsUtils sEmptyAnalyticsUtils = new AnalyticsUtils(null) {
        @Override
        public void trackEvent(String category, String action, String label, int value) {}

        @Override
        public void trackPageView(String path) {}
    };
}
