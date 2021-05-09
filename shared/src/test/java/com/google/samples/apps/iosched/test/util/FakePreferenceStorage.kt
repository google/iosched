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

package com.google.samples.apps.iosched.test.util

import com.google.samples.apps.iosched.shared.data.prefs.PreferenceStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakePreferenceStorage(
    onboardingCompleted: Boolean = false,
    notificationsPreferenceShown: Boolean = false,
    preferToReceiveNotifications: Boolean = false,
    myLocationOptedIn: Boolean = false,
    preferConferenceTimeZone: Boolean = true,
    sendUsageStatistics: Boolean = false,
    selectedFilters: String = "",
    selectedTheme: String = "",
    codelabsInfoShown: Boolean = true,
    var snackbarIsStopped: Boolean = false,
    var scheduleUiHintsShown: Boolean = false,
) : PreferenceStorage {
    private val _onboardingCompleted = MutableStateFlow(onboardingCompleted)
    override val onboardingCompleted: Flow<Boolean> = _onboardingCompleted

    private val _notificationsPreferenceShown =
        MutableStateFlow(notificationsPreferenceShown)
    override val notificationsPreferenceShown = _notificationsPreferenceShown

    private val _preferToReceiveNotifications =
        MutableStateFlow(preferToReceiveNotifications)
    override val preferToReceiveNotifications = _preferToReceiveNotifications

    private val _myLocationOptedIn = MutableStateFlow(myLocationOptedIn)
    override val myLocationOptedIn = _myLocationOptedIn

    private val _sendUsageStatistics = MutableStateFlow(sendUsageStatistics)
    override val sendUsageStatistics = _sendUsageStatistics

    private val _preferConferenceTimeZone = MutableStateFlow(preferConferenceTimeZone)
    override val preferConferenceTimeZone = _preferConferenceTimeZone

    private val _selectedFilters = MutableStateFlow(selectedFilters)
    override val selectedFilters = _selectedFilters

    private val _selectedTheme = MutableStateFlow(selectedTheme)
    override val selectedTheme = _selectedTheme

    private val _codelabsInfoShown = MutableStateFlow(codelabsInfoShown)
    override val codelabsInfoShown = _codelabsInfoShown

    override suspend fun completeOnboarding(complete: Boolean) {
        _onboardingCompleted.value = complete
    }

    override suspend fun areScheduleUiHintsShown() = scheduleUiHintsShown

    override suspend fun showScheduleUiHints(show: Boolean) {
        scheduleUiHintsShown = show
    }

    override suspend fun showNotificationsPreference(show: Boolean) {
        _notificationsPreferenceShown.value = show
    }

    override suspend fun preferToReceiveNotifications(prefer: Boolean) {
        _preferToReceiveNotifications.value = prefer
    }

    override suspend fun optInMyLocation(optIn: Boolean) {
        _myLocationOptedIn.value = optIn
    }

    override suspend fun stopSnackbar(stop: Boolean) {
        snackbarIsStopped = stop
    }

    override suspend fun isSnackbarStopped(): Boolean {
        return snackbarIsStopped
    }

    override suspend fun sendUsageStatistics(send: Boolean) {
        _sendUsageStatistics.value = send
    }

    override suspend fun preferConferenceTimeZone(preferConferenceTimeZone: Boolean) {
        _preferConferenceTimeZone.value = preferConferenceTimeZone
    }

    override suspend fun selectFilters(filters: String) {
        _selectedFilters.value = filters
    }

    override suspend fun selectTheme(theme: String) {
        _selectedTheme.value = theme
    }

    override suspend fun showCodelabsInfo(show: Boolean) {
        _codelabsInfoShown.value = show
    }
}
