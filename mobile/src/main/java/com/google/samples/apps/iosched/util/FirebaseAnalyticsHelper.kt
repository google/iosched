/*
 * Copyright 2019 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.util

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.data.prefs.PreferenceStorage
import com.google.samples.apps.iosched.shared.data.prefs.SharedPreferenceStorage
import com.google.samples.apps.iosched.shared.domain.internal.DefaultScheduler
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import timber.log.Timber

/**
 * Firebase Analytics implementation of AnalyticsHelper
 */
class FirebaseAnalyticsHelper(
    context: Context,
    signInViewModelDelegate: SignInViewModelDelegate,
    preferenceStorage: PreferenceStorage
) : AnalyticsHelper {

    private var firebaseAnalytics = Firebase.analytics

    /**
     * stores a strong reference to preference change][PreferenceManager]
     */
    private var prefListener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    private var analyticsEnabled: Boolean = false
        set(enabled) {
            field = enabled
            Timber.d("Setting Analytics enabled: $enabled")
            firebaseAnalytics.setAnalyticsCollectionEnabled(enabled)
        }

    /**
     * Initialize Analytics tracker.  If the user has permitted tracking and has already signed TOS,
     * (possible except on first run), initialize analytics Immediately.
     */
    init {

        DefaultScheduler.execute { // Prevent access to preferences on main thread
            analyticsEnabled = preferenceStorage.sendUsageStatistics
        }

        Timber.d("Analytics initialized")

        // The listener will initialize Analytics when the TOS is signed, or enable/disable
        // Analytics based on the "anonymous data collection" setting.
        setupPreferenceChangeListener(context)

        signInViewModelDelegate.observeSignedInUser().observeForever { signedIn ->
            setUserSignedIn(signedIn == true)
            Timber.d("Updated user signed in to $signedIn")
        }

        signInViewModelDelegate.observeRegisteredUser().observeForever { registered ->
            setUserRegistered(registered == true)
            Timber.d("Updated user registered to $registered")
        }
    }

    override fun sendScreenView(screenName: String, activity: Activity) {
        firebaseAnalytics.run {
            setCurrentScreen(activity, screenName, null)
            logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
                param(FirebaseAnalytics.Param.ITEM_ID, screenName)
                param(FirebaseAnalytics.Param.CONTENT_TYPE, FA_CONTENT_TYPE_SCREENVIEW)
            }
            Timber.d("Screen View recorded: $screenName")
        }
    }

    override fun logUiEvent(itemId: String, action: String) {
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_ITEM) {
            param(FirebaseAnalytics.Param.ITEM_ID, itemId)
            param(FirebaseAnalytics.Param.CONTENT_TYPE, FA_CONTENT_TYPE_UI_EVENT)
            param(FA_KEY_UI_ACTION, action)
        }
        Timber.d("Event recorded for $itemId, $action")
    }

    override fun setUserSignedIn(isSignedIn: Boolean) {
        // todo(alexlucas) : Set up user properties in both dev and prod
        firebaseAnalytics.setUserProperty(UPROP_USER_SIGNED_IN, isSignedIn.toString())
    }

    override fun setUserRegistered(isRegistered: Boolean) {
        // todo(alexlucas) : Set up user properties in both dev and prod
        firebaseAnalytics.setUserProperty(UPROP_USER_REGISTERED, isRegistered.toString())
    }

    /**
     * Set up a listener for preference changes.
     */
    private fun setupPreferenceChangeListener(context: Context) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { pref, key ->
            val action = try {
                getBooleanPreferenceAction(pref, key)
            } catch (e: ClassCastException) {
                return@OnSharedPreferenceChangeListener
            }

            if (key == SharedPreferenceStorage.PREF_SEND_USAGE_STATISTICS) {
                val sendStats = pref.getBoolean(key, false)
                analyticsEnabled = sendStats
            } else {
                logUiEvent("Preference: $key", action)
            }
        }

        DefaultScheduler.execute { // Prevent access to preferences on main thread
            SharedPreferenceStorage(context).registerOnPreferenceChangeListener(listener)
        }
        prefListener = listener
        Timber.d("Preference Change Listener has been set up.")
    }

    private fun getBooleanPreferenceAction(prefs: SharedPreferences, key: String): String {
        return if (prefs.getBoolean(key, true)) AnalyticsActions.ENABLE
        else AnalyticsActions.DISABLE
    }

    companion object {
        private const val UPROP_USER_SIGNED_IN = "user_signed_in"
        private const val UPROP_USER_REGISTERED = "user_registered"

        /**
         * Log a specific screen view under the `screenName` string.
         */
        private const val FA_CONTENT_TYPE_SCREENVIEW = "screen"
        private const val FA_KEY_UI_ACTION = "ui_action"
        private const val FA_CONTENT_TYPE_UI_EVENT = "ui event"
    }
}
