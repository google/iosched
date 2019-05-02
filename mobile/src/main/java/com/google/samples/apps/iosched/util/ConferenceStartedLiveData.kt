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

package com.google.samples.apps.iosched.util

import android.os.Handler
import androidx.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.shared.di.MainThreadHandler
import com.google.samples.apps.iosched.shared.time.TimeProvider
import com.google.samples.apps.iosched.shared.util.TimeUtils
import org.threeten.bp.Duration
import javax.inject.Inject

class ConferenceStartedLiveData @Inject constructor(
    @MainThreadHandler private val handler: Handler,
    private val timeProvider: TimeProvider
) : MutableLiveData<Boolean>() {

    private val updateRunnable = Runnable { checkTime() }

    override fun onActive() {
        super.onActive()
        checkTime()
    }

    override fun onInactive() {
        super.onInactive()
        handler.removeCallbacks(updateRunnable)
    }

    private fun checkTime() {
        val timeUntilStart = Duration.between(timeProvider.now(), TimeUtils.getKeynoteStartTime())
        if (timeUntilStart.isNegative) {
            // Conference started
            postValue(true)
        } else {
            postValue(false)
            // Check again after the duration
            handler.postDelayed(updateRunnable, timeUntilStart.toMillis())
        }
    }
}
