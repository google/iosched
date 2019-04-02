/*
 * Copyright 2019 Google LLC
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

package com.google.samples.apps.iosched.shared.data.config

import android.content.res.Resources.NotFoundException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.samples.apps.iosched.model.ConferenceWifiInfo
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class RemoteAppConfigDataSource @Inject constructor(
    private val firebaseRemoteConfig: FirebaseRemoteConfig
) : AppConfigDataSource {

    private val _attributesLiveDataMap: Map<String, MutableLiveData<String>> = mapOf(
        Pair(WELCOME_TILE_KEY, MutableLiveData()),
        Pair(WELCOME_SUBTITLE_KEY, MutableLiveData())
    )

    init {
        firebaseRemoteConfig.activateFetched() // update active config with the last fetched values
        updateStrings()
        firebaseRemoteConfig.fetch(TimeUnit.MINUTES.toSeconds(30)).addOnCompleteListener { task ->
            // Async
            if (task.isSuccessful) {
                firebaseRemoteConfig.activateFetched()
                updateStrings()
            }
        }
    }

    private fun updateStrings() {
        _attributesLiveDataMap.map { (key, liveData) ->
            liveData.value = firebaseRemoteConfig.getString(key)
        }
    }

    override fun getStringLiveData(key: String): LiveData<String> =
        _attributesLiveDataMap[key] ?: throw NotFoundException("Value for $key not found")

    override fun getWifiInfo(): ConferenceWifiInfo {
        return ConferenceWifiInfo(
            ssid = firebaseRemoteConfig.getString(WIFI_SSID_KEY),
            password = firebaseRemoteConfig.getString(WIFI_PASSWORD_KEY)
        )
    }

    override fun isFeedFeatureEnabled(): Boolean =
        firebaseRemoteConfig.getBoolean(FEED_FEATURE_ENABLED)

    override fun isMapFeatureEnabled(): Boolean =
        firebaseRemoteConfig.getBoolean(MAP_FEATURE_ENABLED)

    override fun isExploreArFeatureEnabled(): Boolean =
        firebaseRemoteConfig.getBoolean(EXPLORE_AR_FEATURE_ENABLED)

    override fun isCodelabsFeatureEnabled(): Boolean =
        firebaseRemoteConfig.getBoolean(CODELABS_FEATURE_ENABLED)

    companion object {
        const val WIFI_SSID_KEY = "wifi_ssid"
        const val WIFI_PASSWORD_KEY = "wifi_password"
        const val WELCOME_TILE_KEY = "welcome_title"
        const val WELCOME_SUBTITLE_KEY = "welcome_subtitle"

        const val FEED_FEATURE_ENABLED = "feed_enabled"
        const val MAP_FEATURE_ENABLED = "map_enabled"
        const val EXPLORE_AR_FEATURE_ENABLED = "explore_ar_enabled"
        const val CODELABS_FEATURE_ENABLED = "codelabs_enabled"
    }
}
