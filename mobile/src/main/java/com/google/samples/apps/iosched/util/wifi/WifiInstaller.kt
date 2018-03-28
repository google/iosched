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
import com.google.samples.apps.iosched.shared.util.TimeUtils
import timber.log.Timber

/**
 * Installs WiFi on device given a WiFi configuration.
 */
class WifiInstaller(val conferenceWiFiConfig: WifiConfiguration) {

    // TODO use remote config and observe ssid and password.

    // Preference key and values associated with WiFi AP configuration.
    private val PREF_WIFI_AP_CONFIG = "pref_wifi_ap_config"
    private val WIFI_CONFIG_DONE = "done"
    private val WIFI_CONFIG_REQUESTED = "requested"

    fun installConferenceWiFi(context: Context): Boolean {
        var success = false

        val conferenceWifiConfig = conferenceWiFiConfig
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val netId = wifiManager.addNetwork(conferenceWifiConfig)
        if (netId != -1) {
            wifiManager.enableNetwork(netId, false)
            success = true
        }
        return success
    }

    fun uninstallConferenceWiFi(context: Context) {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        val configuredNetworks = wifiManager.configuredNetworks
        if (configuredNetworks != null) {
            for (wifiConfig in configuredNetworks) {
                if (wifiConfig.SSID == conferenceWiFiConfig.SSID) {
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
    fun shouldBypassWiFiSetup(context: Context): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

        // Is WiFi on?
        if (wifiManager.isWifiEnabled) {
            // Check for existing APs.
            val configs = wifiManager.configuredNetworks
            for (config in configs) {
                if (conferenceWiFiConfig.SSID.equals(config.SSID, ignoreCase = true)) return true
            }
        }

        return WIFI_CONFIG_DONE == getWiFiConfigStatus(context)
    }

    fun isWiFiEnabled(context: Context): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        return wifiManager.isWifiEnabled
    }

    fun isWiFiApConfigured(context: Context): Boolean {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val configs = wifiManager.configuredNetworks ?: return false

        // Check for existing APs.
        for (config in configs) {
            if (conferenceWiFiConfig.SSID.equals(config.SSID, ignoreCase = true)) return true
        }
        return false
    }

    // Stored settings_prefs associated with WiFi AP configuration.
    private fun getWiFiConfigStatus(context: Context): String? {
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        return sp.getString(PREF_WIFI_AP_CONFIG, null)
    }

    private fun setWiFiConfigStatus(context: Context, status: String) {
        if (WIFI_CONFIG_DONE != status && WIFI_CONFIG_REQUESTED != status)
            throw IllegalArgumentException("Invalid WiFi Config status: $status")
        val sp = PreferenceManager.getDefaultSharedPreferences(context)
        sp.edit().putString(PREF_WIFI_AP_CONFIG, status).apply()
    }

    fun installWiFiIfRequested(context: Context): Boolean {
        if (WIFI_CONFIG_REQUESTED == getWiFiConfigStatus(context) && isWiFiEnabled(context)) {
            installConferenceWiFi(context)
            if (isWiFiApConfigured(context)) {
                setWiFiConfigStatus(context, WIFI_CONFIG_DONE)
                return true
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
    fun shouldOfferToSetupWifi(context: Context, actively: Boolean): Boolean {
        if (!TimeUtils.conferenceWifiOfferingStarted()) {
            Timber.i("Too early to offer wifi")
            return false
        }
        if (TimeUtils.conferenceHasEnded()) {
            Timber.i("Too late to offer wifi")
            return false
        }
        if (!isWiFiEnabled(context)) {
            Timber.i("Wifi isn't enabled")
            return false
        }

        // TODO: Don't offer wifi if user is unregistered (use ObserveUserAuthStateUseCase)

        if (isWiFiApConfigured(context)) {
            Timber.i("Attendee is already setup for wifi.")
            return false
        }
        // TODO: Add setting for user to opt out of WiFi.
        return true
    }
}
