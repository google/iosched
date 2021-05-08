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

import android.net.wifi.WifiConfiguration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.ConferenceWifiInfo
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.domain.logistics.LoadWifiInfoUseCase
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.shared.util.tryOffer
import com.google.samples.apps.iosched.ui.messages.SnackbarMessage
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.util.WhileViewSubscribed
import com.google.samples.apps.iosched.util.wifi.WifiInstaller
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class EventInfoViewModel @Inject constructor(
    loadWifiInfoUseCase: LoadWifiInfoUseCase,
    private val wifiInstaller: WifiInstaller,
    private val analyticsHelper: AnalyticsHelper,
    private val snackbarMessageManager: SnackbarMessageManager
) : ViewModel() {

    companion object {
        private const val ASSISTANT_APP_URL =
            "https://assistant.google.com/services/invoke/uid/0000009fca77b068"
    }

    val wifiConfig: StateFlow<ConferenceWifiInfo?> = flow {
        emit(loadWifiInfoUseCase(Unit).data)
    }.stateIn(viewModelScope, SharingStarted.Lazily, null)

    val showWifi: StateFlow<Boolean> = wifiConfig.map {
        it?.ssid?.isNotBlank() == true && it.password.isNotBlank()
    }.stateIn(viewModelScope, WhileViewSubscribed, false)

    private val _navigationActions = Channel<EventInfoNavigationAction>(Channel.CONFLATED)
    val navigationActions = _navigationActions.receiveAsFlow()

    fun onWifiConnect() {
        val config = wifiConfig.value ?: return
        val success = wifiInstaller.installConferenceWifi(
            WifiConfiguration().apply {
                SSID = config.ssid
                preSharedKey = config.password
            }
        )
        val snackbarMessage = if (success) {
            SnackbarMessage(R.string.wifi_install_success)
        } else {
            SnackbarMessage(
                messageId = R.string.wifi_install_clipboard_message, longDuration = true
            )
        }

        snackbarMessageManager.addMessage(snackbarMessage)
        analyticsHelper.logUiEvent("Events", AnalyticsActions.WIFI_CONNECT)
    }

    fun onClickAssistantApp() {
        _navigationActions.tryOffer(EventInfoNavigationAction.OpenUrl(ASSISTANT_APP_URL))
        analyticsHelper.logUiEvent("Assistant App", AnalyticsActions.CLICK)
    }
}

sealed class EventInfoNavigationAction {
    data class OpenUrl(val url: String) : EventInfoNavigationAction()
}
