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
import androidx.annotation.WorkerThread
import com.google.samples.apps.iosched.shared.BuildConfig
import java.io.IOException
import okhttp3.Cache
import okhttp3.CacheControl
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber

/**
 * Downloads session data.
 */
class ConferenceDataDownloader(
    private val context: Context,
    private val bootstrapVersion: String
) {

    private val client: OkHttpClient by lazy {
        val logInterceptor = HttpLoggingInterceptor()
        logInterceptor.level = HttpLoggingInterceptor.Level.BASIC

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

        val url = BuildConfig.CONFERENCE_DATA_URL

        Timber.d("Download started from: $url")

        val httpBuilder = HttpUrl.parse(url)?.newBuilder()
            ?: throw IllegalArgumentException("Malformed Session data URL")
        httpBuilder.addQueryParameter("bootstrapVersion", bootstrapVersion)

        val request = Request.Builder()
            .url(httpBuilder.build())
            .cacheControl(CacheControl.FORCE_NETWORK)
            .build()

        // Blocking call
        val response = client.newCall(request).execute()

        Timber.d("Downloaded bytes: ${response.body()?.contentLength() ?: 0}")

        return response ?: throw IOException("Network error")
    }

    fun fetchCached(): Response? {

        val url = BuildConfig.CONFERENCE_DATA_URL

        Timber.d("Fetching cached file for url: $url")

        val httpBuilder = HttpUrl.parse(url)?.newBuilder()
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
