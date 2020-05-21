/*
 * Copyright 2020 Google LLC
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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * LiveData that runs a transformation [block] every interval and also whenever a source
 * LiveData changes.
 */
class IntervalMediatorLiveData<in P, T>(
    source: LiveData<P>,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val intervalMs: Long,
    private val block: (P?) -> T?
) : MediatorLiveData<T>() {

    private val intervalScope = CoroutineScope(dispatcher + SupervisorJob())
    // Nullable because sometimes onInactive is called without onActive first.
    private var intervalJob: Job? = null

    private var lastEmitted: P? = null

    init {
        addSource(source) {
            lastEmitted = it
            // We're on the main thread, so use a coroutine in case the transformation is expensive.
            intervalScope.launch {
                postValue(block(lastEmitted))
            }
        }
    }

    override fun onActive() {
        super.onActive()
        // Loop until canceled.
        intervalJob = intervalScope.launch {
            while (isActive) {
                delay(intervalMs)
                postValue(block(lastEmitted))
            }
        }
    }

    override fun onInactive() {
        super.onInactive()
        intervalJob?.cancel()
        intervalJob = null
    }
}
