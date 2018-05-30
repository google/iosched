/*
 * Copyright 2018 Google LLC
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

package com.google.samples.apps.iosched.shared.data.prefs

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import androidx.annotation.WorkerThread
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import javax.inject.Inject
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Storage for app and user preferences.
 */
interface PreferenceStorage {
    var onboardingCompleted: Boolean
    var scheduleUiHintsShown: Boolean
    var notificationsPreferenceShown: Boolean
    var preferToReceiveNotifications: Boolean
    var snackbarIsStopped: Boolean
    var observableSnackbarIsStopped: LiveData<Boolean>
    var sendUsageStatistics: Boolean
    var preferConferenceTimeZone: Boolean
    var selectedFilters: String?
}

/**
 * [PreferenceStorage] impl backed by [android.content.SharedPreferences].
 */
class SharedPreferenceStorage @Inject constructor(context: Context) :
    PreferenceStorage {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

    private val observableShowSnackbarResult = MutableLiveData<Boolean>()
    private val changeListener = OnSharedPreferenceChangeListener { _, key ->
        if (key == PREF_SNACKBAR_IS_STOPPED) {
            observableShowSnackbarResult.value = snackbarIsStopped
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(changeListener)
    }

    override var onboardingCompleted by BooleanPreference(prefs, PREF_ONBOARDING, false)

    override var scheduleUiHintsShown by BooleanPreference(prefs, PREF_SCHED_UI_HINTS_SHOWN, false)

    override var notificationsPreferenceShown
        by BooleanPreference(prefs, PREF_NOTIFICATIONS_SHOWN, false)

    override var preferToReceiveNotifications
        by BooleanPreference(prefs, PREF_RECEIVE_NOTIFICATIONS, true)

    override var snackbarIsStopped by BooleanPreference(prefs, PREF_SNACKBAR_IS_STOPPED, false)

    override var observableSnackbarIsStopped: LiveData<Boolean>
        get() {
            observableShowSnackbarResult.value = snackbarIsStopped
            return observableShowSnackbarResult
        }
        set(value) = throw IllegalAccessException("This property can't be changed")

    override var sendUsageStatistics by BooleanPreference(prefs, PREF_SEND_USAGE_STATISTICS, true)

    override var preferConferenceTimeZone
        by BooleanPreference(prefs, PREF_CONFERENCE_TIME_ZONE, true)

    override var selectedFilters by StringPreference(prefs, PREF_SELECTED_FILTERS, null)

    companion object {
        const val PREFS_NAME = "iosched"
        const val PREF_ONBOARDING = "pref_onboarding"
        const val PREF_SCHED_UI_HINTS_SHOWN = "pref_sched_ui_hints_shown"
        const val PREF_NOTIFICATIONS_SHOWN = "pref_notifications_shown"
        const val PREF_RECEIVE_NOTIFICATIONS = "pref_receive_notifications"
        const val PREF_SNACKBAR_IS_STOPPED = "pref_snackbar_is_stopped"
        const val PREF_SEND_USAGE_STATISTICS = "pref_send_usage_statistics"
        const val PREF_CONFERENCE_TIME_ZONE = "pref_conference_time_zone"
        const val PREF_SELECTED_FILTERS = "pref_selected_filters"
    }

    fun registerOnPreferenceChangeListener(listener: OnSharedPreferenceChangeListener) {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }
}

class BooleanPreference(
    private val preferences: SharedPreferences,
    private val name: String,
    private val defaultValue: Boolean
) : ReadWriteProperty<Any, Boolean> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): Boolean {
        return preferences.getBoolean(name, defaultValue)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: Boolean) {
        preferences.edit { putBoolean(name, value) }
    }
}

class StringPreference(
    private val preferences: SharedPreferences,
    private val name: String,
    private val defaultValue: String?
) : ReadWriteProperty<Any, String?> {

    @WorkerThread
    override fun getValue(thisRef: Any, property: KProperty<*>): String? {
        return preferences.getString(name, defaultValue)
    }

    override fun setValue(thisRef: Any, property: KProperty<*>, value: String?) {
        preferences.edit { putString(name, value) }
    }
}
