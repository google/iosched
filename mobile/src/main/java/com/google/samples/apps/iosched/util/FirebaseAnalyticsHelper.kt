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
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.ktx.Firebase
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.data.prefs.DataStorePreferenceStorage.PreferencesKeys.PREF_CODELABS_INFO_SHOWN
import com.google.samples.apps.iosched.shared.data.prefs.DataStorePreferenceStorage.PreferencesKeys.PREF_CONFERENCE_TIME_ZONE
import com.google.samples.apps.iosched.shared.data.prefs.DataStorePreferenceStorage.PreferencesKeys.PREF_MY_LOCATION_OPTED_IN
import com.google.samples.apps.iosched.shared.data.prefs.DataStorePreferenceStorage.PreferencesKeys.PREF_NOTIFICATIONS_SHOWN
import com.google.samples.apps.iosched.shared.data.prefs.DataStorePreferenceStorage.PreferencesKeys.PREF_ONBOARDING
import com.google.samples.apps.iosched.shared.data.prefs.DataStorePreferenceStorage.PreferencesKeys.PREF_RECEIVE_NOTIFICATIONS
import com.google.samples.apps.iosched.shared.data.prefs.DataStorePreferenceStorage.PreferencesKeys.PREF_SEND_USAGE_STATISTICS
import com.google.samples.apps.iosched.shared.data.prefs.PreferenceStorage
import com.google.samples.apps.iosched.shared.di.ApplicationScope
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Firebase Analytics implementation of AnalyticsHelper
 */
class FirebaseAnalyticsHelper(
    @ApplicationScope private val externalScope: CoroutineScope,
    signInViewModelDelegate: SignInViewModelDelegate,
    private val preferenceStorage: PreferenceStorage
) : AnalyticsHelper {

    private var firebaseAnalytics = Firebase.analytics

    private var analyticsEnabled: Flow<Boolean> = preferenceStorage.sendUsageStatistics

    /**
     * Initialize Analytics tracker.  If the user has permitted tracking and has already signed TOS,
     * (possible except on first run), initialize analytics Immediately.
     */
    init {
        externalScope.launch { // Prevent access to preferences on main thread
            analyticsEnabled.collect {
                Timber.d("Setting Analytics enabled: $it")
                firebaseAnalytics.setAnalyticsCollectionEnabled(it)
            }
            // The listener will initialize Analytics when the TOS is signed, or enable/disable
            // Analytics based on the "anonymous data collection" setting.
            logSendUsageStatsFlagChanges()
        }

        externalScope.launch {
            // The listener will initialize Analytics when the TOS is signed, or enable/disable
            // Analytics based on the "anonymous data collection" setting.
            logSendUsageStatsFlagChanges()
        }

        externalScope.launch {
            signInViewModelDelegate.isUserSignedIn.collect { signedIn ->
                setUserSignedIn(signedIn)
                Timber.d("Updated user signed in to $signedIn")
            }
        }
        externalScope.launch {
            signInViewModelDelegate.isUserRegistered.collect { registered ->
                setUserRegistered(registered)
                Timber.d("Updated user registered to $registered")
            }
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
    private suspend fun logSendUsageStatsFlagChanges() {
        // not logged: showScheduleUiHints, selectedFilters, selectedTheme
        flowOf(
            preferenceStorage.codelabsInfoShown.map { PREF_CODELABS_INFO_SHOWN.name to it },
            preferenceStorage.myLocationOptedIn.map { PREF_MY_LOCATION_OPTED_IN.name to it },
            preferenceStorage.notificationsPreferenceShown.map {
                PREF_NOTIFICATIONS_SHOWN.name to it
            },
            preferenceStorage.onboardingCompleted.map { PREF_ONBOARDING.name to it },
            preferenceStorage.preferConferenceTimeZone.map { PREF_CONFERENCE_TIME_ZONE.name to it },
            preferenceStorage.preferToReceiveNotifications.map {
                PREF_RECEIVE_NOTIFICATIONS.name to it
            },
            preferenceStorage.sendUsageStatistics.map { PREF_SEND_USAGE_STATISTICS.name to it }
        ).flattenMerge().collect { (key, value) ->
            val action = if (value) AnalyticsActions.ENABLE else AnalyticsActions.DISABLE
            logUiEvent("Preference: $key", action)
        }
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
