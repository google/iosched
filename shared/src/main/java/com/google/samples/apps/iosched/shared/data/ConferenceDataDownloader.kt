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
import android.support.annotation.WorkerThread
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.io.IOException


//TODO(jalc): Move
const val URL = "https://firebasestorage.googleapis.com/v0/b/iosched-playground.appspot.com/o/" +
        "conference_data.json?alt=media&token=0c0a7ce8-f582-4ab2-a66e-29f335680399"

/**
 * Downloads session data.
 */
class ConferenceDataDownloader(
        private val context: Context,
        private val bootstrapVersion: String
) {

    // TODO(jalc): Provide this, only one client should exist
    private val client: OkHttpClient by lazy {
        val logInterceptor = HttpLoggingInterceptor()
        logInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC)

        val protocols = arrayListOf(Protocol.HTTP_1_1, Protocol.HTTP_2) // Support h2

        val cacheSize = 2L * 1024 * 1024 // 2 MiB
        val cacheDir = context.getDir("conference_data", Context.MODE_PRIVATE)
        val cache = Cache(cacheDir, cacheSize)

        OkHttpClient.Builder()
                .protocols(protocols)
                .cache(cache)
                .addInterceptor(logInterceptor)
                .build()
    }

    @Throws(IOException::class)
    @WorkerThread
    fun fetch(): Response {

        Timber.d("Download started from: $URL")

        val httpBuilder = HttpUrl.parse(URL)?.newBuilder()
                ?: throw IllegalArgumentException("Malformed Session data URL")
        httpBuilder.addQueryParameter("bootstrapVersion", bootstrapVersion)

        val request = Request.Builder()
                .url(httpBuilder.build())
                .cacheControl(CacheControl.FORCE_NETWORK) // TODO(jalc): Needed?
                .build()

        // Blocking call
        val response = client.newCall(request).execute()

        //TODO Delete cache somehow

        Timber.d("Downloaded bytes: ${response.body()?.contentLength() ?: 0}")

        return response ?: throw IOException("Network error")
    }

    fun fetchCached(): Response? {
        Timber.d("Fetching cached file")

        val httpBuilder = HttpUrl.parse(URL)?.newBuilder()
                ?: throw IllegalArgumentException("Malformed Session data URL")
        httpBuilder.addQueryParameter("bootstrapVersion", bootstrapVersion)

        val request = Request.Builder()
                .url(httpBuilder.build())
                .cacheControl(CacheControl.FORCE_CACHE)
                .build()

        // Blocking call
        val response = client.newCall(request).execute()

        Timber.d("Loaded cache. Bytes: ${response.body()?.contentLength() ?: 0}")
        if (response.code() == 504) {
            return null
        }
        return response ?: throw IOException("Network error")


    }
}
