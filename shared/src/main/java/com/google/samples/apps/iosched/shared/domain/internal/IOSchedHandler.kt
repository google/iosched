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

package com.google.samples.apps.iosched.shared.domain.internal

import android.os.Handler
import android.os.Looper

/**
 * A forwarding interface for [Handler] to support mocking in tests.
 */
interface IOSchedHandler {
    /**
     * See [Handler.post]
     */
    fun post(runnable: Runnable): Boolean

    /**
     * See [Handler.postDelayed]
     */
    fun postDelayed(runnable: Runnable, millis: Long): Boolean

    /**
     * See [Handler.removeCallbacks]
     */
    fun removeCallbacks(runnable: Runnable)
}

/**
 * Main thread handler to be used across ioshced.
 */
class IOSchedMainHandler : IOSchedHandler {
    private val handler = Handler(Looper.getMainLooper())

    override fun post(runnable: Runnable) = handler.post(runnable)

    override fun postDelayed(runnable: Runnable, millis: Long) =
        handler.postDelayed(runnable, millis)

    override fun removeCallbacks(runnable: Runnable) = handler.removeCallbacks(runnable)
}
