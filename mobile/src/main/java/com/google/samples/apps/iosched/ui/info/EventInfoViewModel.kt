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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.ConferenceWifiInfo
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.domain.logistics.LoadWifiInfoUseCase
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDays
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.google.samples.apps.iosched.util.wifi.WifiInstaller
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject

class EventInfoViewModel @Inject constructor(
    loadWifiInfoUseCase: LoadWifiInfoUseCase,
    private val wifiInstaller: WifiInstaller,
    signInViewModelDelegate: SignInViewModelDelegate,
    private val analyticsHelper: AnalyticsHelper
) : ViewModel() {

    companion object {
        private const val SCAVENGER_HUNT_URL =
            "https://androidthings.withgoogle.com/iosearch"
        private const val ASSISTANT_APP_URL =
            "https://assistant.google.com/services/a/uid/000000449633043c"
    }

    private val _wifiConfig = MutableLiveData<Result<ConferenceWifiInfo>>()
    val wifiSsid: LiveData<String?>
    val wifiPassword: LiveData<String?>

    private val _snackbarMessage = MutableLiveData<Event<SnackbarMessage>>()
    val snackBarMessage: LiveData<Event<SnackbarMessage>>
        get() = _snackbarMessage

    val showScavengerHunt: LiveData<Boolean>

    private val _openUrlEvent = MutableLiveData<Event<String>>()
    val openUrlEvent: LiveData<Event<String>>
        get() = _openUrlEvent

    init {
        loadWifiInfoUseCase(Unit, _wifiConfig)
        wifiSsid = _wifiConfig.map {
            (it as? Result.Success)?.data?.ssid
        }
        wifiPassword = _wifiConfig.map {
            (it as? Result.Success)?.data?.password
        }
        showScavengerHunt = signInViewModelDelegate.observeRegisteredUser().map {
            checkShowScavengerHunt(it)
        }
    }

    fun onWifiConnect() {
        val ssid = wifiSsid.value
        val password = wifiPassword.value
        var success = false
        if (ssid != null && password != null) {
            success = wifiInstaller.installConferenceWifi(WifiConfiguration().apply {
                SSID = ssid
                preSharedKey = password
            })
        }
        val snackbarMessage = if (success) {
            SnackbarMessage(R.string.wifi_install_success)
        } else {
            SnackbarMessage(
                messageId = R.string.wifi_install_clipboard_message, longDuration = true
            )
        }

        _snackbarMessage.postValue(Event(snackbarMessage))
        analyticsHelper.logUiEvent("Events", AnalyticsActions.WIFI_CONNECT)
    }

    private fun checkShowScavengerHunt(isRegistered: Boolean): Boolean {
        if (isRegistered) {
            // Show scavenger hunt to attendees during the conference
            val now = ZonedDateTime.now()
            return now.isAfter(ConferenceDays.first().start) &&
                now.isBefore(ConferenceDays.last().end)
        } else {
            return false
        }
    }

    fun onClickScavengerHunt() {
        _openUrlEvent.value = Event(SCAVENGER_HUNT_URL)
        analyticsHelper.logUiEvent("Scavenger Hunt", AnalyticsActions.CLICK)
    }

    fun onClickAssistantApp() {
        _openUrlEvent.value = Event(ASSISTANT_APP_URL)
        analyticsHelper.logUiEvent("Assistant App", AnalyticsActions.CLICK)
    }
}
