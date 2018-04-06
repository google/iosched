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

import android.databinding.BindingAdapter
import android.net.wifi.WifiConfiguration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import com.google.android.material.widget.Snackbar
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentInfoEventBinding
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.util.wifi.WifiInstaller
import dagger.android.support.DaggerFragment

class EventFragment : DaggerFragment() {
    private lateinit var binding: FragmentInfoEventBinding
    private val wifiInstaller = WifiInstaller(WifiConfiguration().apply {
        // Must be in double quotes to tell system this is an ASCII SSID and passphrase.
        SSID = String.format("\"%s\"", context?.getString(R.string.wifi_ssid))
        preSharedKey = String.format("\"%s\"", context?.getString(R.string.wifi_ssid))
    })

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        binding = FragmentInfoEventBinding.inflate(inflater, container, false)
        val context = context ?: return null
        // TODO wire wifi text to RemoteConfig.

        binding.wifiPasswordValue.setOnClickListener {
            val saved = wifiInstaller.installConferenceWiFi(context)
            Snackbar.make(binding.root, if (saved)
                R.string.wifi_install_success
            else
                R.string.wifi_install_error_message, Snackbar.LENGTH_SHORT).show()
        }
        // TODO: launch filtered schedule
        // TODO: launch map
        // TODO: launch codelabs
        binding.eventSandbox.apply {
            onViewSessionsClicked = { _, _ -> Unit }
            onViewMapClicked = { _, _ -> Unit }
            onViewCodelabsClicked = { _, _ -> Unit }
        }
        binding.eventCodelabs.apply {
            onViewSessionsClicked = { _, _ -> Unit }
            onViewMapClicked = { _, _ -> Unit }
            onViewCodelabsClicked = { _, _ -> Unit }
        }
        binding.eventOfficehours.apply {
            onViewSessionsClicked = { _, _ -> Unit }
            onViewMapClicked = { _, _ -> Unit }
            onViewCodelabsClicked = { _, _ -> Unit }
        }
        binding.eventAfterhours.apply {
            onViewSessionsClicked = { _, _ -> Unit }
            onViewMapClicked = { _, _ -> Unit }
            onViewCodelabsClicked = { _, _ -> Unit }
        }
        return binding.root
    }
}

@BindingAdapter("countdownVisibility")
fun countdownVisibility(countdown: View, ignored: Boolean?) {
    countdown.visibility = if (TimeUtils.conferenceHasStarted()) GONE else VISIBLE
}
