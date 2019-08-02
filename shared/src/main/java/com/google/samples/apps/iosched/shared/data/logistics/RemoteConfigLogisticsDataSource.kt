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

package com.google.samples.apps.iosched.shared.data.logistics

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.samples.apps.iosched.model.ConferenceWifiInfo
import com.google.samples.apps.iosched.shared.BuildConfig
import javax.inject.Inject

class RemoteConfigLogisticsDataSource @Inject constructor(
    val firebaseRemoteConfig: FirebaseRemoteConfig
) : LogisticsDataSource {

    override fun getWifiInfo(): ConferenceWifiInfo {
        // TODO: right now, this returns what is currently in the RemoteConfig cache, and triggers
        // a fetch that will impact the cache the next time this is called. We should update
        // livedata when activateFetched() is called.
        firebaseRemoteConfig.fetch(CACHE_EXPIRATION).addOnCompleteListener {
            if (it.isSuccessful) {
                firebaseRemoteConfig.activateFetched()
            }
        }
        val ssid = firebaseRemoteConfig.getString(KEY_WIFI_SSID)
        val password = firebaseRemoteConfig.getString(KEY_WIFI_PASSWORD)
        return ConferenceWifiInfo(ssid, password)
    }

    companion object {
        val CACHE_EXPIRATION = if (BuildConfig.DEBUG) 1L else 900L
        const val KEY_WIFI_SSID = "wifi_ssid"
        const val KEY_WIFI_PASSWORD = "wifi_password"
    }
}
