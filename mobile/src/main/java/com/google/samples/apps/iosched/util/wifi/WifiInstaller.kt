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

package com.google.samples.apps.iosched.util.wifi

import android.content.ClipData
import android.content.ClipboardManager
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import com.google.samples.apps.iosched.util.quoteSsidAndPassword
import com.google.samples.apps.iosched.util.unquoteSsidAndPassword
import javax.inject.Inject

/**
 * Installs WiFi on device given a WiFi configuration.
 */
class WifiInstaller @Inject constructor(
    private val wifiManager: WifiManager,
    private val clipboardManager: ClipboardManager
) {
    fun installConferenceWifi(rawWifiConfig: WifiConfiguration): Boolean {
        val conferenceWifiConfig = rawWifiConfig.quoteSsidAndPassword()
        if (!wifiManager.isWifiEnabled) {
            wifiManager.isWifiEnabled = true
        }
        var success = false
        val netId = wifiManager.addNetwork(conferenceWifiConfig)
        if (netId != -1) {
            wifiManager.enableNetwork(netId, false)
            success = true
        } else {
            val clip: ClipData =
                ClipData.newPlainText(
                    "wifi_password",
                    conferenceWifiConfig.unquoteSsidAndPassword().preSharedKey
                )
            clipboardManager.setPrimaryClip(clip)
        }
        return success
    }
}
