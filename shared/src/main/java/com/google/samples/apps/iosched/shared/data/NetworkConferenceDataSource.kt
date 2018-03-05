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

import android.content.Context
import android.net.ConnectivityManager
import com.google.samples.apps.iosched.shared.model.ConferenceData
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

/**
 * Downloads and parses conference data.
 */
//TODO(jalc): pass bootstrap version
class NetworkConferenceDataSource @Inject constructor(val context: Context) : ConferenceDataSource {

    override fun getConferenceData(): ConferenceData? {
        if (!isNetworkConnected(context)) {

            Timber.d("Network not connected")
            return null
        }

        Timber.d("Trying to download data from network")
        val responseSource = try {
            ConferenceDataDownloader(context, "1").fetch() //TODO(jalc): pass bootstrap version
        } catch(e: IOException) {
            return null
        }
        val body = responseSource.body()?.byteStream() ?: return null

        Timber.d("Parsing new data")
        val parsedData = try {
            ConferenceDataJsonParser.parseConferenceData(body)
        } catch (e: RuntimeException) {
            Timber.e(e, "Error parsing cached data")
            null
        }
        responseSource.close()
        return parsedData
    }

    /**
     * Returns the cached conference data or null if there's no cache.
     */
    override fun getOfflineConferenceData(): ConferenceData? {

        val responseSource = try {
            ConferenceDataDownloader(context, "1").fetchCached() //TODO(jalc): pass version
        } catch(e: IOException) {
            return null
        }
        val body = responseSource?.body()?.byteStream()
        if (body == null) {
            Timber.i("No cache found")
            return null
        }
        Timber.d("Parsing cached data:")
        val parsedData = try {
            ConferenceDataJsonParser.parseConferenceData(body)
        } catch (e: RuntimeException) {
            Timber.e(e, "Error parsing cached data")
            null
        }
        responseSource.close()
        return parsedData
    }
}

fun isNetworkConnected(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val activeNetwork = cm.activeNetworkInfo
    return activeNetwork != null && activeNetwork.isConnectedOrConnecting
}