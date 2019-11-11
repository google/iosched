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

package com.google.samples.apps.iosched.shared.data

import com.google.samples.apps.iosched.model.ConferenceData
import com.google.samples.apps.iosched.shared.BuildConfig

/**
 * Loads bootstrap data file from resources and parses it.
 */
object BootstrapConferenceDataSource : ConferenceDataSource {
    override fun getRemoteConferenceData(): ConferenceData? {
        throw Exception("Bootstrap data source doesn't have remote data")
    }

    override fun getOfflineConferenceData(): ConferenceData? {
        return loadAndParseBootstrapData()
    }

    fun loadAndParseBootstrapData(): ConferenceData {

        val conferenceDataStream = this.javaClass.classLoader!!
            .getResource(BuildConfig.BOOTSTRAP_CONF_DATA_FILENAME).openStream()

        return ConferenceDataJsonParser.parseConferenceData(conferenceDataStream)
    }
}
