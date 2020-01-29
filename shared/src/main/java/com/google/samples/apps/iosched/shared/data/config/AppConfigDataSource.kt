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

import androidx.lifecycle.LiveData
import com.google.samples.apps.iosched.model.ConferenceWifiInfo

interface AppConfigDataSource {

    fun getStringLiveData(key: String): LiveData<String> // TODO: change name
    fun syncStringsAsync(changedCallback: StringsChangedCallback?)
    fun getWifiInfo(): ConferenceWifiInfo
    fun isMapFeatureEnabled(): Boolean
    fun isExploreArFeatureEnabled(): Boolean
    fun isCodelabsFeatureEnabled(): Boolean
    fun isSearchScheduleFeatureEnabled(): Boolean
    fun isSearchUsingRoomFeatureEnabled(): Boolean
    fun isAssistantAppFeatureEnabled(): Boolean
}

interface StringsChangedCallback {

    /**
     * Called when any of Strings parameters are changed from the previous values.
     *
     * @param changedKeys has the list of key names whose values are changed from the previous
     * values.
     */
    fun onChanged(changedKeys: List<String>)
}
