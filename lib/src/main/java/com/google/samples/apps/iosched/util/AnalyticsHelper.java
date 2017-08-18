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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.samples.apps.iosched.lib.BuildConfig;
import com.google.samples.apps.iosched.lib.R;
import com.google.samples.apps.iosched.settings.SettingsUtils;


import static com.google.samples.apps.iosched.util.LogUtils.LOGD;

/**
 * Centralized Analytics interface to ensure proper initialization and
 * consistent analytics application across the app.
 * <p>
 * For the purposes of this application, initialization of the Analytics tracker is broken
 * into two steps.  {@link #prepareAnalytics(Context)} is called upon app creation, which sets up
 * a listener for changes to shared settings_prefs.  When the user agrees to TOS, the listener triggers
 * the actual initialization step, setting up a Google Analytics tracker.  This ensures that
 * no data is collected or accidentally sent before the TOS step, and that campaign tracking data
 * isn't accidentally deleted by starting and immediately disabling a tracker upon app creation.
 */
public class AnalyticsHelper {

    private final static String TAG = LogUtils.makeLogTag(AnalyticsHelper.class);

    // Always the application context
    @SuppressLint("StaticFieldLeak")
    private static Context sAppContext = null;

    /**
     * The analytics trackers.  For 2017 (crossover year), using both Google Analytics and
     * Firebase Analytics.
     */
    private static Tracker mTracker;
    private static FirebaseAnalytics mFirebaseAnalytics;
    /**
     * Custom dimension slot number for the "attendee at venue" preference.
     * There's a finite number of custom dimensions, and they need to consistently be sent
     * in the same index in order to be tracked properly.  For each custom dimension or metric,
     * always reserve an index.
     */
    // Not using ATTENDING DIMENSION anymore, but slots don't change, so keeping it here as a
    // placeholder.
    private static final int SLOT_ATTENDING_DIMENSION = 1;
    private static final int SLOT_SIGNEDIN_DIMENSION = 2;

    /**
     * The {@link PreferenceManager doesn't store a strong references to preference change
     * listeners.  To prevent one from being garbage collected, a strong reference must be
     * created in app code.
     */
    private static SharedPreferences.OnSharedPreferenceChangeListener sPrefListener;

    /**
     * Log a specific screen view under the {@code screenName} string.
     */

    private static String FA_CONTENT_TYPE_SCREENVIEW = "screen";
    private static String FA_KEY_UI_ACTION = "ui_action";
    private static String FA_CONTENT_TYPE_UI_EVENT = "ui event";


    public static void sendScreenView(String screenName, Activity activity) {
        // GA hit.
        mTracker.setScreenName(screenName);
        mTracker.send(new HitBuilders.ScreenViewBuilder().build());
        LOGD(TAG, "Screen View recorded: " + screenName);

        // FA hit.
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.ITEM_ID, screenName);
        params.putString(FirebaseAnalytics.Param.CONTENT_TYPE, FA_CONTENT_TYPE_SCREENVIEW);
        mFirebaseAnalytics.setCurrentScreen(activity, screenName, null);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, params);
    }

    /**
     * Log a specific event under the {@code category}, {@code action}, and {@code label}.
     */
    public static void sendEvent(String category, String action, String label, long value,
                                 HitBuilders.EventBuilder eventBuilder) {
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

        convertToFAEvent(category, action, label);
    }

    /*
     * When the GA crossover code is gone we can streamline this, but in the interim, it's easiest
     * to minimize the changes to hte rest of the codebase and hide as much of the dual
     * implementation in AnalyticsHelper as we can.
     */
    private static void convertToFAEvent(String category, String action, String label) {

        switch (action) {
            case "Starred":
                logUiEvent(label, "bookmarked");
                break;

            case "click":
                if (category.equals("primary nav")) {
                    logUiEvent(label, "primary nav click");
                }
                break;
            case "Feedback":
                logUiEvent(category, "rate session click");
                break;
            case "Add Events":
                logUiEvent("Day " + label, "add events click");
                break;
            case "Tag":
                logUiEvent(label, "inline tag click");
                break;

            case "Reservation":
                logUiEvent(action, "reservation click");
                break;

            case "Filters Updated":
                logUiEvent(label, "topnav filter used");
                break;

            case "Youtube Video":
                logUiEvent(label, "youtube link click");
                break;
            case "Event Info":
                logUiEvent(category, label.toLowerCase());
                break;
            case "markerclick":
                logUiEvent(label, "map pin selected");
                break;
            case "selectsession":
                logUiEvent(label, "map pin view details");
                break;

            default:
                break;
        }
    }

    private static void logUiEvent(String itemId, String action) {
        Bundle params = new Bundle();
        params.putString(FirebaseAnalytics.Param.ITEM_ID, itemId);
        params.putString(FirebaseAnalytics.Param.CONTENT_TYPE,
                FA_CONTENT_TYPE_UI_EVENT);
        params.putString(FA_KEY_UI_ACTION, action);
        mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, params);
        LOGD(TAG, "Event recorded for " + itemId + ", " + action);
    }

    /**
     * Log an specific event under the {@code category}, {@code action}, and {@code label}.
     */
    public static void sendEvent(String category, String action, String label) {
        HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder();
        sendEvent(category, action, label, 0, eventBuilder);
    }

    public static void setUserSignedIn(boolean isSignedIn) {
        // For GA
        HitBuilders.EventBuilder eventBuilder = new HitBuilders.EventBuilder();
        eventBuilder.setCustomDimension(SLOT_SIGNEDIN_DIMENSION, String.valueOf(isSignedIn));
        String action = isSignedIn ? "signed in" : "signed out";
        sendEvent("", action, action, 0, eventBuilder);

        // For FA
        mFirebaseAnalytics.setUserProperty("user_signed_in", String.valueOf(isSignedIn));
    }

    /**
     * Sets up Analytics to be initialized when the user agrees to TOS.  If the user has already
     * done so (all runs of the app except the first run), initialize analytics Immediately.
     *
     * @param context The context that will later be used to initialize Analytics.
     */
    public static void prepareAnalytics(Context context) {
        sAppContext = context.getApplicationContext();

        // The listener will initialize Analytics when the TOS is signed, or enable/disable
        // Analytics based on the "anonymous data collection" setting.
        setupPreferenceChangeListener();
        initializeAnalyticsTracker();
    }

    /**
     * Initialize the analytics tracker in use by the application.
     */
    private static synchronized void initializeAnalyticsTracker() {
        if (mTracker == null) {
            int useProfile;
            if (BuildConfig.DEBUG) {
                LOGD(TAG, "Analytics manager using DEBUG ANALYTICS PROFILE.");
                useProfile = R.xml.analytics_debug;
            } else {
                useProfile = R.xml.analytics_release;
            }

            try {
                mTracker = GoogleAnalytics.getInstance(sAppContext).newTracker(useProfile);
                mFirebaseAnalytics = FirebaseAnalytics.getInstance(sAppContext);
            } catch (Exception e) {
                // If anything goes wrong, force an opt-out of tracking. It's better to accidentally
                // protect privacy than accidentally collect data.
                setAnalyticsEnabled(false);
            }
        }
    }

    /**
     * Listens for preference changes.  When a preference change relevant to toggling Analytics
     * is detected, {@link AnalyticsHelper#autoToggleAnalytics()} is called, which
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
                    switch (key) {
                        case SettingsUtils.PREF_ANALYTICS_ENABLED:
                            if (key.equals(WelcomeUtils.PREF_TOS_ACCEPTED)
                                    && prefs.getBoolean(key, false)
                                    && mTracker == null
                                    && !isInitialized()) {
                                initializeAnalyticsTracker();
                            }

                            // Technically it's possible to just look up the values in the pref
                            // object provided and enable/disable in here, but it's safer to have all the
                            // "should analytics run" logic collected in one place.
                            LOGD(TAG, "Auto togglin'.");
                            autoToggleAnalytics();
                            break;
                        case SettingsUtils.PREF_LOCAL_TIMES: {
                            String label = "Local time";
                            // ANALYTICS EVENT:  Updated "Show Local Times" setting.
                            // Contains: The checkbox state of this setting.
                            sendEvent(category, getAction(prefs, key), label);
                            break;
                        }
                        case BuildConfig.PREF_CONF_MESSAGES_ENABLED: {
                            String label = "Conference Notification Cards";
                            // ANALYTICS EVENT:  Updated "Conference Notification Cards" setting.
                            // Contains: The checkbox state of this setting.
                            sendEvent(category, getAction(prefs, key), label);
                            break;
                        }
                        case SettingsUtils.PREF_SYNC_CALENDAR: {
                            String label = "Sync with Google Calendar";
                            // ANALYTICS EVENT:  Updated "Sync with Google Calendar" setting.
                            // Contains: The checkbox state of this setting.
                            sendEvent(category, getAction(prefs, key), label);
                            break;
                        }
                        case BuildConfig.PREF_NOTIFICATIONS_ENABLED: {
                            String label = "Session Reminders";
                            // ANALYTICS EVENT:  Updated "Session Reminders" setting.
                            // Contains: The checkbox state of this setting.
                            sendEvent(category, getAction(prefs, key), label);
                            break;
                        }
                        case BuildConfig.PREF_SESSION_FEEDBACK_REMINDERS_ENABLED: {
                            String label = "Feedback Reminders";
                            // ANALYTICS EVENT:  Updated "Feedback Reminders" setting.
                            // Contains: The checkbox state of this setting.
                            sendEvent(category, getAction(prefs, key), label);
                            break;
                        }
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
     * Checks application state and settings_prefs, then explicitly either enables or
     * disables the tracker.
     */
    private static void autoToggleAnalytics() {
        GoogleAnalytics instance = GoogleAnalytics.getInstance(sAppContext);
        try {
            // Analytics shouldn't run unless all the following are true:
            // 1) A tracker has been initialized in this class (as opposed to elsewhere in the app).
            // 2) The user has accepted TOS.
            // 3) "Anonymous usage data" is enabled in settings.
            boolean enableAnalytics = isInitialized() // Has Analytics been initialized?
                    && SettingsUtils.isAnalyticsEnabled(sAppContext); // Analytics enabled in settings.
            if (instance != null) {
                instance.setAppOptOut(!enableAnalytics);
            }
            if (mFirebaseAnalytics != null) {
                mFirebaseAnalytics.setAnalyticsCollectionEnabled(enableAnalytics);
            }

            LOGD(TAG, "Analytics" + (isInitialized() ? "" : " not") + " initialized"
                    + ", TOS" + (WelcomeUtils.isTosAccepted(sAppContext) ? "" : " not") + " accepted"
                    + ", Setting is" + (SettingsUtils.isAnalyticsEnabled(sAppContext) ? "" : " not")
                    + " checked");
        } catch (Exception e) {
            if (instance != null) {
                instance.setAppOptOut(true);
            }
            if (mFirebaseAnalytics != null) {
                mFirebaseAnalytics.setAnalyticsCollectionEnabled(false);
            }
        }
    }

    private static void setAnalyticsEnabled(boolean enabled) {
        LOGD(TAG, "Setting Analytics enabled: " + enabled);
        GoogleAnalytics instance = GoogleAnalytics.getInstance(sAppContext);
        if (instance != null) {
            instance.setAppOptOut(!enabled);
        }
        if (mFirebaseAnalytics != null) {
            mFirebaseAnalytics.setAnalyticsCollectionEnabled(enabled);
        }
    }
}
