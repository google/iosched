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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;

import java.util.List;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class WiFiUtils {
    // Preference key and values associated with WiFi AP configuration.
    public static final String PREF_WIFI_AP_CONFIG = "pref_wifi_ap_config";
    public static final String WIFI_CONFIG_DONE = "done";
    public static final String WIFI_CONFIG_REQUESTED = "requested";

    private static final String TAG = makeLogTag(WiFiUtils.class);

    public static void installConferenceWiFi(final Context context) {
        // Create config
        WifiConfiguration config = new WifiConfiguration();
        // Must be in double quotes to tell system this is an ASCII SSID and passphrase.
        config.SSID = String.format("\"%s\"", Config.WIFI_SSID);
        config.preSharedKey = String.format("\"%s\"", Config.WIFI_PASSPHRASE);

        // Store config
        final WifiManager wifiManager =
                (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int netId = wifiManager.addNetwork(config);
        if (netId != -1) {
            wifiManager.enableNetwork(netId, false);
            boolean result = wifiManager.saveConfiguration();
            if (!result) {
                Log.e(TAG, "Unknown error while calling WiFiManager.saveConfiguration()");
                Toast.makeText(context,
                        context.getResources().getString(R.string.wifi_install_error_message),
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(TAG, "Unknown error while calling WiFiManager.addNetwork()");
            Toast.makeText(context,
                    context.getResources().getString(R.string.wifi_install_error_message),
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Helper method to decide whether to bypass conference WiFi setup.  Return true if
     * WiFi AP is already configured (WiFi adapter enabled) or WiFi configuration is complete
     * as per shared preference.
     */
    public static boolean shouldBypassWiFiSetup(final Context context) {
        final WifiManager wifiManager =
                (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

        // Is WiFi on?
        if (wifiManager.isWifiEnabled()) {
            // Check for existing APs.
            final List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();
            final String conferenceSSID = String.format("\"%s\"", Config.WIFI_SSID);
            for(WifiConfiguration config : configs) {
                if (conferenceSSID.equalsIgnoreCase(config.SSID)) return true;
            }
        }

        return WIFI_CONFIG_DONE.equals(getWiFiConfigStatus(context));
    }

    public static boolean isWiFiEnabled(final Context context) {
        final WifiManager wifiManager =
                (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        return wifiManager.isWifiEnabled();
    }

    public static boolean isWiFiApConfigured(final Context context) {
        final WifiManager wifiManager =
                (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        final List<WifiConfiguration> configs = wifiManager.getConfiguredNetworks();

        if (configs == null) return false;

        // Check for existing APs.
        final String conferenceSSID = String.format("\"%s\"", Config.WIFI_SSID);
        for(WifiConfiguration config : configs) {
            if (conferenceSSID.equalsIgnoreCase(config.SSID)) return true;
        }
        return false;
    }

    // Stored preferences associated with WiFi AP configuration.
    public static String getWiFiConfigStatus(final Context context) {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getString(PREF_WIFI_AP_CONFIG, null);
    }

    public static void setWiFiConfigStatus(final Context context, final String status) {
        if (!WIFI_CONFIG_DONE.equals(status) && !WIFI_CONFIG_REQUESTED.equals(status))
            throw new IllegalArgumentException("Invalid WiFi Config status: " + status);
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putString(PREF_WIFI_AP_CONFIG, status).commit();
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

    public static void showWiFiDialog(Activity activity) {
        FragmentManager fm = activity.getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        Fragment prev = fm.findFragmentByTag("dialog_wifi");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);
        WiFiDialog.newInstance(isWiFiEnabled(activity)).show(ft, "dialog_wifi");
    }

    public static class WiFiDialog extends DialogFragment {
        private static final String ARG_WIFI_ENABLED
                = "com.google.samples.apps.iosched.ARG_WIFI_ENABLED";

        private boolean mWiFiEnabled;

        public static WiFiDialog newInstance(boolean wiFiEnabled) {
            WiFiDialog wiFiDialogFragment = new WiFiDialog();

            Bundle args = new Bundle();
            args.putBoolean(ARG_WIFI_ENABLED, wiFiEnabled);
            wiFiDialogFragment.setArguments(args);

            return wiFiDialogFragment;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final int padding =
                    getResources().getDimensionPixelSize(R.dimen.content_padding_normal);
            final TextView wifiTextView = new TextView(getActivity());
            int dialogCallToActionText;
            int dialogPositiveButtonText;

            mWiFiEnabled = getArguments().getBoolean(ARG_WIFI_ENABLED);
            if (mWiFiEnabled) {
                dialogCallToActionText = R.string.calltoaction_wifi_configure;
                dialogPositiveButtonText = R.string.wifi_dialog_button_configure;
            } else {
                dialogCallToActionText = R.string.calltoaction_wifi_settings;
                dialogPositiveButtonText = R.string.wifi_dialog_button_settings;
            }
            wifiTextView.setText(Html.fromHtml(getString(R.string.description_setup_wifi_body) +
                    getString(dialogCallToActionText)));
            wifiTextView.setMovementMethod(LinkMovementMethod.getInstance());
            wifiTextView.setPadding(padding, padding, padding, padding);
            final Context context = getActivity();

            return new AlertDialog.Builder(context)
                    .setTitle(R.string.description_configure_wifi)
                    .setView(wifiTextView)
                    .setPositiveButton(dialogPositiveButtonText,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    // Attempt to configure the Wi-Fi access point.
                                    if (mWiFiEnabled) {
                                        installConferenceWiFi(context);
                                        if (WiFiUtils.isWiFiApConfigured(context)) {
                                            WiFiUtils.setWiFiConfigStatus(
                                                    context,
                                                    WiFiUtils.WIFI_CONFIG_DONE);
                                        }
                                        // Launch Wi-Fi settings screen for user to enable Wi-Fi.
                                    } else {
                                        WiFiUtils.setWiFiConfigStatus(context,
                                                WiFiUtils.WIFI_CONFIG_REQUESTED);
                                        final Intent wifiIntent =
                                                new Intent(Settings.ACTION_WIFI_SETTINGS);
                                        wifiIntent.addFlags(
                                                Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                                        startActivity(wifiIntent);
                                    }
                                    dialog.dismiss();
                                }
                            }
                    )
                    .create();
        }
    }

    /**
     * Returns whether we should or should not offer to set up wifi. If asCard == true
     * this will decide whether or not to offer wifi setup actively (as a card, for instance).
     * If asCard == false, this will return whether or not to offer wifi setup passively
     * (in the overflow menu, for instance).
     */
    public static boolean shouldOfferToSetupWifi(final Context context, boolean actively) {
        long now = UIUtils.getCurrentTime(context);
        if (now < Config.WIFI_SETUP_OFFER_START) {
            // too early to offer
            return false;
        }
        if (now > Config.CONFERENCE_END_MILLIS) {
            // too late
            return false;
        }
        if (!WiFiUtils.isWiFiEnabled(context)) {
            // no wifi, no offer
            return false;
        }
        if (!PrefUtils.isAttendeeAtVenue(context)) {
            // wifi setup not relevant
            return false;
        }
        if (WiFiUtils.isWiFiApConfigured(context)) {
            // already set up
            return false;
        }
        if (actively && PrefUtils.hasDeclinedWifiSetup(context)) {
            // user said no
            return false;
        }
        return true;
    }
}
