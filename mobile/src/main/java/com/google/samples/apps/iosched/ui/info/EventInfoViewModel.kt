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

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.net.wifi.WifiConfiguration
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.domain.logistics.LoadWifiInfoUseCase
import com.google.samples.apps.iosched.shared.model.ConferenceWifiInfo
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.util.wifi.WifiInstaller
import javax.inject.Inject

class EventInfoViewModel @Inject constructor(
    loadWifiInfoUseCase: LoadWifiInfoUseCase,
    private val wifiInstaller: WifiInstaller
) : ViewModel() {

    private val _wifiConfig = MutableLiveData<Result<ConferenceWifiInfo>>()
    val wifiSsid: LiveData<String?>
    val wifiPassword: LiveData<String?>

    private val _snackbarMessage = MutableLiveData<Event<SnackbarMessage>>()
    val snackBarMessage: LiveData<Event<SnackbarMessage>>
        get() = _snackbarMessage

    init {
        loadWifiInfoUseCase(Unit, _wifiConfig)
        wifiSsid = _wifiConfig.map {
            (it as? Result.Success)?.data?.ssid
        }
        wifiPassword = _wifiConfig.map {
            (it as? Result.Success)?.data?.password
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
        val snackbarMessage =
            if (success)
                SnackbarMessage(R.string.wifi_install_success)
            else
                SnackbarMessage(
                    messageId = R.string.wifi_install_clipboard_message, longDuration = true
                )

        _snackbarMessage.postValue(Event(snackbarMessage))
    }
}
