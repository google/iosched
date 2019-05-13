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

package com.google.samples.apps.iosched.shared.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.model.ConferenceWifiInfo
import com.google.samples.apps.iosched.shared.data.config.AppConfigDataSource
import com.google.samples.apps.iosched.shared.data.config.StringsChangedCallback

class FakeAppConfigDataSource : AppConfigDataSource {

    override fun getStringLiveData(key: String): LiveData<String> {
        return MutableLiveData<String>().apply {
            value = "2019-05-07T10:00-07:00"
        }
    }
    override fun syncStringsAsync(changedCallback: StringsChangedCallback?) = Unit
    override fun getWifiInfo(): ConferenceWifiInfo = ConferenceWifiInfo("", "")
    override fun isMapFeatureEnabled(): Boolean = true
    override fun isExploreArFeatureEnabled(): Boolean = true
    override fun isCodelabsFeatureEnabled(): Boolean = true
    override fun isSearchScheduleFeatureEnabled(): Boolean = false
    override fun isSearchUsingRoomFeatureEnabled(): Boolean = true
    override fun isAssistantAppFeatureEnabled(): Boolean = false
}
