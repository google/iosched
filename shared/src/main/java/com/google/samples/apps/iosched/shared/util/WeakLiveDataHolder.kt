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

package com.google.samples.apps.iosched.shared.util

import android.arch.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseUser
import com.google.samples.apps.iosched.shared.result.Result
import java.util.*

/**
 * Helper class to keep a week reference to LiveData that you want to later update.
 */
class WeakLiveDataHolder<T: Any> {
    private lateinit var lastValue: T

    private val observers = Collections.newSetFromMap(
            WeakHashMap<MutableLiveData<T>, Boolean>()
    )

    /**
     * Add a LiveData to be updated on future events.
     *
     * This will post the most recent value if one has been sent.
     */
    fun addLiveDataObserver(liveData: MutableLiveData<T>) {
        observers.add(liveData)

        if(::lastValue.isInitialized) {
            liveData.postValue(lastValue)
        }
    }

    /**
     * Notify all active observers of an update.
     */
    fun notifyAll(value: T) {
        for (liveData in observers) {
            liveData.postValue(value)
        }
        lastValue = value
    }

    fun notifyIfChanged(value: T) {
        if (!::lastValue.isInitialized || value != lastValue) {
            notifyAll(value)
        }
    }
}
