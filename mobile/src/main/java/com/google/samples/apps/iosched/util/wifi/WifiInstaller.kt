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

import android.content.Context
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.preference.PreferenceManager
import com.google.samples.apps.iosched.shared.data.prefs.PreferenceStorage
import com.google.samples.apps.iosched.shared.util.TimeUtils
import timber.log.Timber
import javax.inject.Inject

/**
 * Installs WiFi on device given a WiFi configuration.
 */
class WifiInstaller @Inject constructor(private val wifiManager: WifiManager) {
    fun installConferenceWifi(conferenceWifiConfig: WifiConfiguration): Boolean {
        var success = false
        val netId = wifiManager.addNetwork(conferenceWifiConfig)
        if (netId != -1) {
            wifiManager.enableNetwork(netId, false)
            success = true
        }
        return success
    }

    fun uninstallConferenceWiFi(ssid: String) {
        val configuredNetworks = wifiManager.configuredNetworks
        if (configuredNetworks != null) {
            for (wifiConfig in configuredNetworks) {
                if (wifiConfig.SSID == ssid) {
                    Timber.w("Removing network: ${wifiConfig.networkId}")
                    wifiManager.removeNetwork(wifiConfig.networkId)
                }
            }
        }
    }

    /**
     * Helper method to decide whether to bypass conference WiFi setup.  Return true if
     * WiFi AP is already configured (WiFi adapter enabled) or WiFi configuration is complete
     * as per shared preference.
     */
    fun isWifiApConfigured(ssid: String): Boolean {
        // Is WiFi on?
        if (wifiManager.isWifiEnabled) {
            // Check for existing APs.
            val configs = wifiManager.configuredNetworks ?: return false
            for (config in configs) {
                if (ssid.equals(config.SSID, ignoreCase = true)) return true
            }
        }
        return false
    }

    /**
     * Returns whether we should or should not offer to set up wifi. If asCard == true
     * this will decide whether or not to offer wifi setup actively (as a card, for instance).
     * If asCard == false, this will return whether or not to offer wifi setup passively
     * (in the overflow menu, for instance).
     */
    fun shouldOfferToSetupWifi(ssid: String): Boolean {
        if (!TimeUtils.conferenceWifiOfferingStarted()) {
            Timber.i("Too early to offer wifi")
            return false
        }
        if (TimeUtils.conferenceHasEnded()) {
            Timber.i("Too late to offer wifi")
            return false
        }
        if (!wifiManager.isWifiEnabled) {
            Timber.i("Wifi isn't enabled")
            return false
        }

        // TODO: Don't offer wifi if user is unregistered (use ObserveUserAuthStateUseCase)

        if (isWifiApConfigured(ssid)) {
            Timber.i("Attendee is already setup for wifi.")
            return false
        }
        // TODO: Add setting for user to opt out of WiFi.
        return true
    }
}
