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

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.settings.ConfMessageCardUtils;
import com.google.samples.apps.iosched.settings.SettingsUtils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;

/**
 * Centralized Analytics interface to ensure proper initialization and
 * consistent analytics application across the app.
 *
 * For the purposes of this application, initialization of the Analytics tracker is broken
 * into two steps.  {@link #prepareAnalytics(Context)} is called upon app creation, which sets up
 * a listener for changes to shared settings_prefs.  When the user agrees to TOS, the listener triggers
 * the actual initialization step, setting up a Google Analytics tracker.  This ensures that
 * no data is collected or accidentally sent before the TOS step, and that campaign tracking data
 * isn't accidentally deleted by starting and immediately disabling a tracker upon app creation.
 *
 */
public class AnalyticsHelper {

    private final static String TAG = LogUtils.makeLogTag(AnalyticsHelper.class);

    private static Context sAppContext = null;

    private static Tracker mTracker;

    /** Custom dimension slot number for the "attendee at venue" preference.
     * There's a finite number of custom dimensions, and they need to consistently be sent
     * in the same index in order to be tracked properly.  For each custom dimension or metric,
     * always reserve an index.
     */
    private static final int SLOT_ATTENDING_DIMENSION = 1;

    /**
     * The {@link PreferenceManager doesn't store a strong references to preference change
     * listeners.  To prevent one from being garbage collected, a strong reference must be
     * created in app code.
     */
    private static SharedPreferences.OnSharedPreferenceChangeListener sPrefListener;

    /**
     * Log a specific screen view under the {@code screenName} string.
     */
    public static void sendScreenView(String screenName) {
        if (isInitialized()) {
            mTracker.setScreenName(screenName);
            mTracker.send(new HitBuilders.AppViewBuilder().build());
            LOGD(TAG, "Screen View recorded: " + screenName);
        }
    }

    /**
     * Log a specific event under the {@code category}, {@code action}, and {@code label}.
     */
    public static void sendEvent(String category, String action, String label, long value,
                                 HitBuilders.EventBuilder eventBuilder) {
        if(isInitialized()) {
            mTracker.send(eventBuilder
                    .setCategory(category)
                    .setAction(action)
                    .setLabel(label)
                    .setValue(value)
                    .build());

            LOGD(TAG, "Event recorded: \n" +
                    "\tCategory: " + category +
                    "\tAction: " + action +
                    "\tLabel: " + label +
                    "\tValue: " + value);
        }
    }

    /**
     * Log an specific event under the {@code category}, {@code action}, and {@code label}.
     */
    public static void sendEvent(String category, String action, String label) {
        HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder();
        sendEvent(category, action, label, 0, eventBuilder);
    }

    /**
     * Log an specific event under the {@code category}, {@code action}, and {@code label}.  Attach
     * a custom dimension using the provided {@code dimensionIndex} and {@code dimensionValue}
     */
    public static void sendEventWithCustomDimension(String category, String action, String label,
                                                    int dimensionIndex, String dimensionValue) {
        // Create a new HitBuilder, populate it with the custom dimension, and send it along
        // to the rest of the event building process.
        HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder();
        eventBuilder.setCustomDimension(dimensionIndex, dimensionValue);
        sendEvent(category, action, label, 0, eventBuilder);

        LOGD(TAG, "Custom Dimension Attached:\n" +
                "\tindex: " + dimensionIndex +
                "\tvalue: " + dimensionValue);
    }

    /**
     * Sets up Analytics to be initialized when the user agrees to TOS.  If the user has already
     * done so (all runs of the app except the first run), initialize analytics Immediately. Note
     * that {@applicationContext} must be the Application level {@link Context} or this class will
     * leak the context.
     *
     * @param applicationContext  The context that will later be used to initialize Analytics.
     */
    public static void prepareAnalytics(Context applicationContext) {
        sAppContext = applicationContext;

        // The listener will initialize Analytics when the TOS is signed, or enable/disable
        // Analytics based on the "anonymous data collection" setting.
        setupPreferenceChangeListener();

        // If TOS hasn't been signed yet, it's the first run.  Exit.
        if (SettingsUtils.isTosAccepted(sAppContext)) {
            initializeAnalyticsTracker(sAppContext);
        }
    }

    /**
     * Initialize the analytics tracker in use by the application. This should only be called
     * once, when the TOS is signed. The {@code applicationContext} parameter MUST be the
     * application context or an object leak could occur.
     */
    private static synchronized void initializeAnalyticsTracker(Context applicationContext) {
        sAppContext = applicationContext;
        if (mTracker == null) {
            int useProfile;
            if (BuildConfig.DEBUG) {
                LOGD(TAG, "Analytics manager using DEBUG ANALYTICS PROFILE.");
                useProfile = R.xml.analytics_debug;
            } else {
                useProfile = R.xml.analytics_release;
            }

            try {
                mTracker = GoogleAnalytics.getInstance(applicationContext).newTracker(useProfile);
            } catch (Exception e) {
                // If anything goes wrong, force an opt-out of tracking. It's better to accidentally
                // protect privacy than accidentally collect data.
                setAnalyticsEnabled(false);
            }
        }
    }

    /**
     * Listens for preference changes.  When a preference change relevant to toggling Analytics
     * is detected, {@link AnalyticsHelper#enableOrDisableAnalyticsAsNecessary()} is called, which
     * will decide whether Analytics should be enabled or disabled based on settings_prefs and
     * application state.
     */
    private static void setupPreferenceChangeListener() {
        SharedPreferences userPrefs = PreferenceManager.getDefaultSharedPreferences(sAppContext);
        sPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {

            @Override
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {

                // Most of the preferences will use these defaults.
                String category = "Preference";

                if (key != null) {
                    if (key.equals(SettingsUtils.PREF_TOS_ACCEPTED)
                            || key.equals(SettingsUtils.PREF_ANALYTICS_ENABLED)) {

                        // If TOS is accepted, initialize the Analytics Tracker.
                        if (key.equals(SettingsUtils.PREF_TOS_ACCEPTED)
                                && prefs.getBoolean(key, false)
                                && mTracker == null) {
                            initializeAnalyticsTracker(sAppContext);
                        }

                        // Technically it's possible to just look up the values in the pref
                        // object provided and enable/disable in here, but it's safer to have all the
                        // "should analytics run" logic collected in one place.
                        enableOrDisableAnalyticsAsNecessary();
                    } else if (key.equals(SettingsUtils.PREF_LOCAL_TIMES)) {
                        String label = "Local time";
                        // ANALYTICS EVENT:  Updated "Show Local Times" setting.
                        // Contains: The checkbox state of this setting.
                        sendEvent(category, getAction(prefs, key), label);
                    } else if (key.equals(BuildConfig.PREF_ATTENDEE_AT_VENUE)) {
                        // Toggle the "Attending in person" custom dimension so we can track
                        // how venue attendee behavior contrasts with remote attendee behavior.
                        boolean attending = prefs.getBoolean(key, true);
                        // ANALYTICS EVENT:  Updated "On-Site Attendee" preference.
                        // Contains: Whether the attendee is identifying themselves as onsite or remote.
                        String attendeeType = attending ? "On-Site Attendee" : "Remote Attendee";
                        String label = "Will be at I/O";

                        sendEventWithCustomDimension(category, getAction(prefs, key), label,
                                SLOT_ATTENDING_DIMENSION,
                                attendeeType);
                    } else if (key.equals(BuildConfig.PREF_CONF_MESSAGES_ENABLED)) {
                        String label = "Conference Notification Cards";
                        // ANALYTICS EVENT:  Updated "Conference Notification Cards" setting.
                        // Contains: The checkbox state of this setting.
                        sendEvent(category, getAction(prefs, key), label);
                    } else if (key.equals(SettingsUtils.PREF_SYNC_CALENDAR)) {
                        String label = "Sync with Google Calendar";
                        // ANALYTICS EVENT:  Updated "Sync with Google Calendar" setting.
                        // Contains: The checkbox state of this setting.
                        sendEvent(category, getAction(prefs, key), label);
                    } else if (key.equals(BuildConfig.PREF_SESSION_REMINDERS_ENABLED)) {
                        String label = "Session Reminders";
                        // ANALYTICS EVENT:  Updated "Session Reminders" setting.
                        // Contains: The checkbox state of this setting.
                        sendEvent(category, getAction(prefs, key), label);
                    } else if (key.equals(BuildConfig.PREF_SESSION_FEEDBACK_REMINDERS_ENABLED)) {
                        String label = "Feedback Reminders";
                        // ANALYTICS EVENT:  Updated "Feedback Reminders" setting.
                        // Contains: The checkbox state of this setting.
                        sendEvent(category, getAction(prefs, key), label);
                    }
                }
            }
        };
        userPrefs.registerOnSharedPreferenceChangeListener(sPrefListener);
    }

    private static String getAction(SharedPreferences prefs, String key) {
        return prefs.getBoolean(key, true) ? "Checked" : "Unchecked";
    }

    /**
     * Return the current initialization state which indicates whether events can be logged.
     */
    private static boolean isInitialized() {
        // Google Analytics is initialized when this class has a reference to an app context and
        // an Analytics tracker has been created.
        return sAppContext != null // Is there an app context?
                && mTracker != null; // Is there a tracker?
    }

    /**
     * Performs the checks to determine if Analytics should be enabled.
     * @return whether or not it's safe to enable Analytics.
     */
    private static boolean shouldEnableAnalytics() {
        // Analytics shouldn't run unless all the following are true:
        // 1) A tracker has been initialized in this class (as opposed to elsewhere in the app).
        // 2) The user has accepted TOS.
        // 3) "Anonymous usage data" is enabled in settings.
        return isInitialized() // Has Analytics been initialized?
                && SettingsUtils.isTosAccepted(sAppContext) // User has accepted TOS.
                && SettingsUtils.isAnalyticsEnabled(sAppContext); // Analytics enabled in settings.
    }

    /**
     * Checks application state and settings_prefs, then explicitly either enables or
     * disables the tracker.
     */
    public static void enableOrDisableAnalyticsAsNecessary() {
        try {
            setAnalyticsEnabled(shouldEnableAnalytics());
            LOGD(TAG, "Analytics" + (isInitialized() ? "" : " not") + " initialized"
                    + ", TOS" + (SettingsUtils.isTosAccepted(sAppContext) ? "" : " not") + " accepted"
                    + ", Setting is" + (SettingsUtils.isAnalyticsEnabled(sAppContext) ? "" : " not")
                    + " checked");
        } catch (Exception e) {
            setAnalyticsEnabled(false);
        }
    }

    /**
     * Enables or disables Analytics.
     * @param enableAnalytics Whether analytics should be enabled.
     */
    private static void setAnalyticsEnabled(boolean enableAnalytics) {
        GoogleAnalytics instance  = GoogleAnalytics.getInstance(sAppContext);
        if (instance != null) {
            instance.setAppOptOut(!enableAnalytics);
            LOGD(TAG, "Analytics enabled: " + enableAnalytics);
        }

    }
}
