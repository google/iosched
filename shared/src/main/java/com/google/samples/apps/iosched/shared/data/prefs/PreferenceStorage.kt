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
import android.support.annotation.WorkerThread
import androidx.content.edit
import javax.inject.Inject

/**
 * Storage for app and user preferences.
 */
interface PreferenceStorage {
    var onboardingCompleted: Boolean
    var scheduleUiHintsShown: Boolean
}

/**
 * [PreferenceStorage] impl backed by [android.content.SharedPreferences].
 */
class SharedPreferenceStorage @Inject constructor(context: Context) :
    PreferenceStorage {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME, MODE_PRIVATE)

    @get:WorkerThread
    override var onboardingCompleted
        get() = prefs.getBoolean(
            PREF_ONBOARDING, false)
        set(value) = prefs.edit {
            putBoolean(
                PREF_ONBOARDING, value)
        }

    @get:WorkerThread
    override var scheduleUiHintsShown: Boolean
        get() = prefs.getBoolean(
            PREF_SCHED_UI_HINTS_SHOWN, false)
        set(value) = prefs.edit {
            putBoolean(
                PREF_SCHED_UI_HINTS_SHOWN, value)
        }

    companion object {
        private const val PREFS_NAME = "iosched"
        private const val PREF_ONBOARDING = "pref_onboarding"
        private const val PREF_SCHED_UI_HINTS_SHOWN = "pref_sched_ui_hints_shown"
    }
}
