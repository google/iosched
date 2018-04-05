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

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.support.annotation.WorkerThread
import androidx.content.edit
import javax.inject.Inject

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

    @get:WorkerThread
    override var onboardingCompleted
        get() = prefs.getBoolean(PREF_ONBOARDING, false)
        set(value) = prefs.edit {
            putBoolean(PREF_ONBOARDING, value)
        }

    @get:WorkerThread
    override var scheduleUiHintsShown: Boolean
        get() = prefs.getBoolean(PREF_SCHED_UI_HINTS_SHOWN, false)
        set(value) = prefs.edit {
            putBoolean(PREF_SCHED_UI_HINTS_SHOWN, value)
        }

    @get:WorkerThread
    override var notificationsPreferenceShown: Boolean
        get() = prefs.getBoolean(PREF_NOTIFICATIONS_SHOWN, false)
        set(value) = prefs.edit {
            putBoolean(PREF_NOTIFICATIONS_SHOWN, value)
        }

    @get:WorkerThread
    override var preferToReceiveNotifications: Boolean
        get() = prefs.getBoolean(PREF_RECEIVE_NOTIFICATIONS, true)
        set(value) = prefs.edit {
            putBoolean(PREF_RECEIVE_NOTIFICATIONS, value)
        }

    @get:WorkerThread
    override var snackbarIsStopped: Boolean
        get() = prefs.getBoolean(PREF_SNACKBAR_IS_STOPPED, false)
        set(value) = prefs.edit { putBoolean(PREF_SNACKBAR_IS_STOPPED, value) }

    override var observableSnackbarIsStopped: LiveData<Boolean>
        get() {
            observableShowSnackbarResult.value = snackbarIsStopped
            return observableShowSnackbarResult
        }
        set(value) = throw IllegalAccessException("This property can't be changed")

    companion object {
        private const val PREFS_NAME = "iosched"
        private const val PREF_ONBOARDING = "pref_onboarding"
        private const val PREF_SCHED_UI_HINTS_SHOWN = "pref_sched_ui_hints_shown"
        private const val PREF_NOTIFICATIONS_SHOWN = "pref_notifications_shown"
        private const val PREF_RECEIVE_NOTIFICATIONS = "pref_receive_notifications"
        private const val PREF_SNACKBAR_IS_STOPPED = "pref_snackbar_is_stopped"
    }
}
