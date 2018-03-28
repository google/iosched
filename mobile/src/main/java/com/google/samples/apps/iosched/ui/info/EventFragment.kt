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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.widget.Snackbar
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.util.wifi.WifiInstaller
import com.google.samples.apps.iosched.widget.EventView
import dagger.android.support.DaggerFragment

class EventFragment : DaggerFragment() {
    private var sandboxEventContent: EventView? = null
    private var codeLabsEventContent: EventView? = null
    private var officeHoursEventContent: EventView? = null
    private var afterHoursEventContent: EventView? = null
    private val wifiInstaller = WifiInstaller(WifiConfiguration().apply {
        // Must be in double quotes to tell system this is an ASCII SSID and passphrase.
        SSID = String.format("\"%s\"", context?.getString(R.string.wifi_ssid))
        preSharedKey = String.format("\"%s\"", context?.getString(R.string.wifi_ssid))
    })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val root = inflater.inflate(R.layout.fragment_info_event, container, false)
        val context = context ?: return null
        // TODO wire wifi text to RemoteConfig.

        // TODO move to data binding.
        val wifiSavePassword = root.findViewById<TextView>(R.id.wifi_password_value)
        wifiSavePassword.setOnClickListener {
            val saved = wifiInstaller.installConferenceWiFi(context)
            Snackbar.make(root, if (saved)
                R.string.wifi_install_success
            else
                R.string.wifi_install_error_message, Snackbar.LENGTH_SHORT).show()
        }
        sandboxEventContent = root.findViewById(R.id.event_sandbox) as EventView
        codeLabsEventContent = root.findViewById(R.id.event_codelabs) as EventView
        officeHoursEventContent = root.findViewById(R.id.event_officehours) as EventView
        afterHoursEventContent = root.findViewById(R.id.event_afterhours) as EventView
        // TODO: launch filtered schedule
        sandboxEventContent?.onViewSessionsClicked = { _, _ -> Unit }
        codeLabsEventContent?.onViewSessionsClicked = { _, _ -> Unit }
        officeHoursEventContent?.onViewSessionsClicked = { _, _ -> Unit }
        afterHoursEventContent?.onViewSessionsClicked = { _, _ -> Unit }
        // TODO: launch map
        sandboxEventContent?.onViewMapClicked = { _, _ -> Unit }
        codeLabsEventContent?.onViewMapClicked = { _, _ -> Unit }
        officeHoursEventContent?.onViewMapClicked = { _, _ -> Unit }
        afterHoursEventContent?.onViewMapClicked = { _, _ -> Unit }
        // TODO: launch codelabs
        sandboxEventContent?.onViewCodelabsClicked = { _, _ -> Unit }
        codeLabsEventContent?.onViewCodelabsClicked = { _, _ -> Unit }
        officeHoursEventContent?.onViewCodelabsClicked = { _, _ -> Unit }
        afterHoursEventContent?.onViewCodelabsClicked = { _, _ -> Unit }
        return root
    }
}