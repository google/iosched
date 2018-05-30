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

package com.google.samples.apps.iosched.shared.data.signin

import com.google.samples.apps.iosched.shared.BuildConfig
import com.google.samples.apps.iosched.shared.domain.internal.DefaultScheduler
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.io.IOException

/**
 * Uses an HTTP client to hit an endpoint when the user changes.
 */
object AuthenticatedUserRegistration {

    private val client: OkHttpClient by lazy {
        val logInterceptor = HttpLoggingInterceptor()
        logInterceptor.level = HttpLoggingInterceptor.Level.BASIC

        OkHttpClient.Builder()
            .addInterceptor(logInterceptor)
            .build()
    }

    fun callRegistrationEndpoint(token: String) {
        DefaultScheduler.execute {
            val request = Request.Builder()
                .header("Authorization", "Bearer " + token)
                .url(BuildConfig.REGISTRATION_ENDPOINT_URL)
                .build()

            // Blocking call
            val response = try {
                client.newCall(request).execute()
            } catch (e: IOException) {
                Timber.e(e)
                return@execute
            }
            val body = response.body()?.string() ?: ""

            if (body.isEmpty() || !response.isSuccessful) {
                Timber.e("Network error calling registration point (response ${response.code()} )")
                return@execute
            }
            Timber.d("Registration point called, user is registered: $body")
            response.body()?.close()
        }
    }
}
