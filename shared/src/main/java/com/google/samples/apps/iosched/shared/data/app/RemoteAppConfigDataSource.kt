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

package com.google.samples.apps.iosched.shared.data.app

import android.content.res.Resources.NotFoundException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
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
        firebaseRemoteConfig
            .activateFetched() // updating the remote config with the last fetched values
        updateStrings()
        firebaseRemoteConfig.fetch(TimeUnit.MINUTES.toSeconds(30)).addOnCompleteListener { task ->
            // Async
            if (task.isSuccessful) {
                firebaseRemoteConfig.activateFetched()
                updateStrings()
            }
        }
    }

    override fun getStringLiveData(key: String): LiveData<String> =
        _attributesLiveDataMap[key] ?: throw NotFoundException("Value for $key not found")

    override fun isFeature1Enabled(): Boolean =
        firebaseRemoteConfig.getBoolean(FEATURE_1_KEY)

    private fun updateStrings() {
        _attributesLiveDataMap.map { (key, liveData) ->
            liveData.value = firebaseRemoteConfig.getString(key)
        }
    }

    override fun isFeedEnabled(): Boolean =
        firebaseRemoteConfig.getBoolean(FEED_FEATURE_KEY)

    companion object {
        // Placeholder keys
        const val WELCOME_TILE_KEY = "welcome_title"
        const val WELCOME_SUBTITLE_KEY = "welcome_subtitle"

        const val FEATURE_1_KEY = "feature_1"
        const val FEED_FEATURE_KEY = "enable_feed"
    }
}
