/*
 * Copyright 2014 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.lib.BuildConfig;
import com.google.samples.apps.iosched.settings.SettingsUtils;

import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.LOGI;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class WiFiUtils {

    // Preference key and values associated with WiFi AP configuration.
    private static final String PREF_WIFI_AP_CONFIG = "pref_wifi_ap_config";
    private static final String WIFI_CONFIG_DONE = "done";
    private static final String WIFI_CONFIG_REQUESTED = "requested";

    private static final String TAG = makeLogTag(WiFiUtils.class);

    public static boolean installConferenceWiFi(final Context context) {
        boolean success = false;

        WifiConfiguration conferenceWifiConfig = getConferenceWifiConfig();
        final WifiManager wifiManager = (WifiManager)
                context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        int netId = wifiManager.addNetwork(conferenceWifiConfig);
        if (netId != -1) {
            wifiManager.enableNetwork(netId, false);
            success = wifiManager.saveConfiguration();
        }
        return success;
    }

    public static void uninstallConferenceWiFi(final Context context) {
        final WifiManager wifiManager = (WifiManager)
                context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration wifiConfig: configuredNetworks) {
                if (wifiConfig.SSID.equals(BuildConfig.WIFI_SSID)) {
                    LOGW(TAG, "Removing network: " + wifiConfig.networkId);
                    wifiManager.removeNetwork(wifiConfig.networkId);
                }
            }
        }
    }

    /**
     * Helper method to decide whether to bypass conference WiFi setup.  Return true if
     * WiFi AP is already configured (WiFi adapter enabled) or WiFi configuration is complete
     * as per shared preference.
     */
    public static boolean shouldBypassWiFiSetup(final Context context) {
        final WifiManager wifiManager = (WifiManager)
                context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        // Is WiFi on?
        if (wifiManager.isWifiEnabled()) {
            // Check for existing APs.
            final List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
            for (WifiConfiguration config : configs) {
                if (BuildConfig.WIFI_SSID.equalsIgnoreCase(config.SSID)) return true;
            }
        }

        return WIFI_CONFIG_DONE.equals(getWiFiConfigStatus(context));
    }

    public static boolean isWiFiEnabled(final Context context) {
        final WifiManager wifiManager = (WifiManager)
                context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    public static boolean isWiFiApConfigured(final Context context) {
        final WifiManager wifiManager = (WifiManager)
                context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        final List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();

        if (configs == null) return false;

        // Check for existing APs.
        for (WifiConfiguration config : configs) {
            if (BuildConfig.WIFI_SSID.equalsIgnoreCase(config.SSID)) return true;
        }
        return false;
    }

    // Stored settings_prefs associated with WiFi AP configuration.
    private static String getWiFiConfigStatus(final Context context) {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString(PREF_WIFI_AP_CONFIG, null);
    }

    private static void setWiFiConfigStatus(final Context context, final String status) {
        if (!WIFI_CONFIG_DONE.equals(status) && !WIFI_CONFIG_REQUESTED.equals(status))
            throw new IllegalArgumentException("Invalid WiFi Config status: " + status);
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putString(PREF_WIFI_AP_CONFIG, status).apply();
    }

    public static boolean installWiFiIfRequested(final Context context) {
        if (WIFI_CONFIG_REQUESTED.equals(getWiFiConfigStatus(context)) && isWiFiEnabled(context)) {
            installConferenceWiFi(context);
            if (isWiFiApConfigured(context)) {
                setWiFiConfigStatus(context, WiFiUtils.WIFI_CONFIG_DONE);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns whether we should or should not offer to set up wifi. If asCard == true
     * this will decide whether or not to offer wifi setup actively (as a card, for instance).
     * If asCard == false, this will return whether or not to offer wifi setup passively
     * (in the overflow menu, for instance).
     */
    public static boolean shouldOfferToSetupWifi(final Context context, boolean actively) {
        long now = TimeUtils.getCurrentTime(context);
        if (now < Config.WIFI_SETUP_OFFER_START) {
            LOGI(TAG, "Too early to offer wifi");
            return false;
        }
        if (now > Config.CONFERENCE_END_MILLIS) {
            LOGI(TAG, "Too late to offer wifi");
            return false;
        }
        if (!WiFiUtils.isWiFiEnabled(context)) {
            LOGI(TAG, "Wifi isn't enabled");
            return false;
        }
        if (RegistrationUtils.isRegisteredAttendee(context) !=
                RegistrationUtils.REGSTATUS_REGISTERED) {
            LOGI(TAG, "Attendee isn't on-site so wifi wouldn't matter");
            return false;
        }
        if (WiFiUtils.isWiFiApConfigured(context)) {
            LOGI(TAG, "Attendee is already setup for wifi.");
            return false;
        }
        if (actively && SettingsUtils.hasDeclinedWifiSetup(context)) {
            LOGI(TAG, "Attendee opted out of wifi.");
            return false;
        }
        return true;
    }

    private static WifiConfiguration getConferenceWifiConfig() {
        WifiConfiguration conferenceConfig = new WifiConfiguration();

        // Must be in double quotes to tell system this is an ASCII SSID and passphrase.
        conferenceConfig.SSID = String.format("\"%s\"", BuildConfig.WIFI_SSID);
        conferenceConfig.preSharedKey = String.format("\"%s\"", BuildConfig.WIFI_PASSPHRASE);

        return conferenceConfig;
    }
}
