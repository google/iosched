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

import com.google.samples.apps.iosched.shared.model.ConferenceData
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject
import javax.inject.Named

/**
 * Single point of access to session data for the presentation layer.
 *
 * The session data is loaded from the bootstrap file.
 */
open class ConferenceDataRepository @Inject constructor(
        @Named("remoteConfDatasource") private val remoteDataSource: ConferenceDataSource,
        @Named("bootstrapConfDataSource") private val boostrapDataSource: ConferenceDataSource
) {

    // In-memory cache of the conference data
    private var conferenceDataCache: ConferenceData? = null

    var dataLastUpdated = 0L
        private set

    val currentConferenceDataVersion: Int
        get() = conferenceDataCache?.version ?: 0

    var latestException: Exception? = null
        private set

    var latestUpdateSource: UpdateSource = UpdateSource.NONE
        private set

    // Prevents multiple consumers requesting data at the same time
    private val loadConfDataLock = Any()

    fun getConferenceData(forceUpdate: Boolean = false): ConferenceData {
        synchronized(loadConfDataLock) {
            if (forceUpdate || conferenceDataCache == null) {
                conferenceDataCache = loadConferenceData()
            }
            return conferenceDataCache!!
        }
    }

    fun getOfflineConferenceData(): ConferenceData {
        val localData = remoteDataSource.getOfflineConferenceData()
                ?:  boostrapDataSource.getOfflineConferenceData()!!
        return localData
    }

    private fun loadConferenceData(): ConferenceData {
        var conferenceData: ConferenceData? = null
        // Try the network data source first
        try {
            conferenceData = remoteDataSource.getConferenceData()
        } catch (e: IOException) {
            Timber.d(e)
            latestException = e
        }
        // Network data success!
        if (conferenceData != null) {
            latestException = null
            dataLastUpdated = System.currentTimeMillis()
            latestUpdateSource = UpdateSource.NETWORK
            latestException = null
            return conferenceData
        }

        // Second, try the local cache:
        conferenceData = remoteDataSource.getOfflineConferenceData()

        //Cache success!
        if (conferenceData != null) {
            latestUpdateSource = UpdateSource.CACHE
            return conferenceData
        }

        // Third, use the bootstrap file:
        conferenceData = boostrapDataSource.getOfflineConferenceData()!!
        latestUpdateSource = UpdateSource.BOOTSTRAP
        return conferenceData
    }
}