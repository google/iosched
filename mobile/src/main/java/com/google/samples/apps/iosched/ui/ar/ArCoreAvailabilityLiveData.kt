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

package com.google.samples.apps.iosched.ui.ar

import android.content.Context
import android.os.Handler
import androidx.lifecycle.LiveData
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.Availability
import java.util.concurrent.TimeUnit

class ArCoreAvailabilityLiveData(context: Context) : LiveData<Availability>() {

    private val appContext = context.applicationContext
    private val handler = Handler()
    private val checkerRunnable = object : Runnable {
        override fun run() {
            val availability = ArCoreApk.getInstance().checkAvailability(appContext)
            if (availability.isTransient) {
                // If the availability is transient, we need to call availability check again
                // as in https://developers.google.com/ar/develop/java/enable-arcore#check_supported
                handler.postDelayed(this, TimeUnit.SECONDS.toMillis(INTERVAL_SECOND))
            } else {
                value = availability
                handler.removeCallbacks(this)
            }
        }
    }

    override fun onActive() {
        super.onActive()
        handler.post(checkerRunnable)
    }

    override fun onInactive() {
        super.onInactive()
        handler.removeCallbacks(checkerRunnable)
    }

    companion object {
        private const val INTERVAL_SECOND = 1L
    }
}
