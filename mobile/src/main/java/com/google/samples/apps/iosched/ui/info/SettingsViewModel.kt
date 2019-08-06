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

package com.google.samples.apps.iosched.ui.info

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.iosched.shared.domain.prefs.NotificationsPrefSaveActionUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetAnalyticsSettingUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetNotificationsSettingUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.domain.settings.SetAnalyticsSettingUseCase
import com.google.samples.apps.iosched.shared.domain.settings.SetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.shared.result.updateOnSuccess
import kotlinx.coroutines.launch
import javax.inject.Inject

class SettingsViewModel @Inject constructor(
    val setTimeZoneUseCase: SetTimeZoneUseCase,
    getTimeZoneUseCase: GetTimeZoneUseCase,
    val notificationsPrefSaveActionUseCase: NotificationsPrefSaveActionUseCase,
    getNotificationsSettingUseCase: GetNotificationsSettingUseCase,
    val setAnalyticsSettingUseCase: SetAnalyticsSettingUseCase,
    getAnalyticsSettingUseCase: GetAnalyticsSettingUseCase
) : ViewModel() {

    // Time Zone setting
    private val preferConferenceTimeZoneResult = MutableLiveData<Boolean>()
    val preferConferenceTimeZone: LiveData<Boolean>
        get() = preferConferenceTimeZoneResult

    // Notifications setting
    private val enableNotificationsResult = MutableLiveData<Boolean>()
    val enableNotifications: LiveData<Boolean>
        get() = enableNotificationsResult

    // Analytics setting
    private val sendUsageStatisticsResult = MutableLiveData<Boolean>()
    val sendUsageStatistics: LiveData<Boolean>
        get() = sendUsageStatisticsResult

    // Notifications sign in
    private val _showSignIn = MutableLiveData<Event<Unit>>()
    val showSignIn: LiveData<Event<Unit>>
        get() = _showSignIn

    init {
        // Executing use cases in parallel
        viewModelScope.launch {
            preferConferenceTimeZoneResult.value = getTimeZoneUseCase(Unit).data ?: true
        }
        viewModelScope.launch {
            sendUsageStatisticsResult.value = getAnalyticsSettingUseCase(Unit).data ?: false
        }
        viewModelScope.launch {
            enableNotificationsResult.value = getNotificationsSettingUseCase(Unit).data ?: false
        }
    }

    fun toggleTimeZone(checked: Boolean) {
        viewModelScope.launch {
            setTimeZoneUseCase(checked).updateOnSuccess(preferConferenceTimeZoneResult)
        }
    }

    fun toggleSendUsageStatistics(checked: Boolean) {
        viewModelScope.launch {
            setAnalyticsSettingUseCase(checked).updateOnSuccess(sendUsageStatisticsResult)
        }
    }

    fun toggleEnableNotifications(checked: Boolean, isInstantApp: Boolean) {
        if (isInstantApp) {
            _showSignIn.value = Event(Unit)
            return
        }
        viewModelScope.launch {
            notificationsPrefSaveActionUseCase(checked).updateOnSuccess(enableNotificationsResult)
        }
    }
}
