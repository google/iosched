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

package com.google.samples.apps.iosched.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.iosched.model.Theme
import com.google.samples.apps.iosched.shared.domain.prefs.NotificationsPrefSaveActionUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetAnalyticsSettingUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetAvailableThemesUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetNotificationsSettingUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetThemeUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.domain.settings.SetAnalyticsSettingUseCase
import com.google.samples.apps.iosched.shared.domain.settings.SetThemeUseCase
import com.google.samples.apps.iosched.shared.domain.settings.SetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.shared.util.tryOffer
import com.google.samples.apps.iosched.shared.util.zip
import com.google.samples.apps.iosched.util.WhileViewSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    val setTimeZoneUseCase: SetTimeZoneUseCase,
    getTimeZoneUseCase: GetTimeZoneUseCase,
    val notificationsPrefSaveActionUseCase: NotificationsPrefSaveActionUseCase,
    getNotificationsSettingUseCase: GetNotificationsSettingUseCase,
    val setAnalyticsSettingUseCase: SetAnalyticsSettingUseCase,
    getAnalyticsSettingUseCase: GetAnalyticsSettingUseCase,
    val setThemeUseCase: SetThemeUseCase,
    getThemeUseCase: GetThemeUseCase,
    getAvailableThemesUseCase: GetAvailableThemesUseCase
) : ViewModel() {

    // Used to re-run flows on command
    private val refreshSignal = MutableSharedFlow<Unit>()
    // Used to run flows on init and also on command
    private val loadDataSignal: Flow<Unit> = flow {
        emit(Unit)
        emitAll(refreshSignal)
    }

    val uiState: StateFlow<SettingsUiModel> = zip(
        loadDataSignal.mapLatest { getTimeZoneUseCase(Unit).data ?: true },
        loadDataSignal.mapLatest { getNotificationsSettingUseCase(Unit).data ?: true },
        loadDataSignal.combine(getAnalyticsSettingUseCase(Unit)) { _, result ->
            result.data ?: false
        }
    ) {
        preferConferenceTimeZone, enableNotifications, sendUsageStatistics ->
        SettingsUiModel(
            isLoading = false,
            preferConferenceTimeZone = preferConferenceTimeZone,
            enableNotifications = enableNotifications,
            sendUsageStatistics = sendUsageStatistics
        )
    }.stateIn(viewModelScope, WhileViewSubscribed, SettingsUiModel(isLoading = true))

    // Theme setting
    val theme: StateFlow<Theme> = loadDataSignal.mapLatest {
        getThemeUseCase(Unit).data ?: Theme.SYSTEM
    }.stateIn(viewModelScope, WhileViewSubscribed, Theme.SYSTEM)

    // Theme setting
    val availableThemes: StateFlow<List<Theme>> = loadDataSignal.mapLatest {
        getAvailableThemesUseCase(Unit).data ?: listOf<Theme>()
    }.stateIn(viewModelScope, WhileViewSubscribed, listOf())

    // SIDE EFFECTS: Navigation actions
    private val _navigationActions = Channel<SettingsNavigationAction>(capacity = Channel.CONFLATED)
    // Exposed with receiveAsFlow to make sure that only one observer receives updates.
    val navigationActions = _navigationActions.receiveAsFlow()

    private suspend fun refreshData() {
        refreshSignal.emit(Unit)
    }

    fun setTimeZone(checked: Boolean) {
        viewModelScope.launch {
            setTimeZoneUseCase(checked)
            refreshData()
        }
    }

    fun setEnableNotifications(checked: Boolean) {
        viewModelScope.launch {
            notificationsPrefSaveActionUseCase(checked)
            refreshData()
        }
    }

    fun setSendUsageStatistics(checked: Boolean) {
        viewModelScope.launch {
            setAnalyticsSettingUseCase(checked)
            refreshData()
        }
    }

    fun setTheme(theme: Theme) {
        viewModelScope.launch {
            setThemeUseCase(theme)
            refreshData()
        }
    }

    fun onThemeSettingClicked() {
        _navigationActions.tryOffer(SettingsNavigationAction.NavigateToThemeSelector)
    }
}

sealed class SettingsNavigationAction {
    object NavigateToThemeSelector : SettingsNavigationAction()
}

data class SettingsUiModel(
    val isLoading: Boolean = false,
    val preferConferenceTimeZone: Boolean = false,
    val enableNotifications: Boolean = false,
    val sendUsageStatistics: Boolean = false
)
