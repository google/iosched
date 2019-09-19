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

package com.google.samples.apps.iosched.test.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.broadcastIn

@FlowPreview
@ExperimentalCoroutinesApi
/**
 * Gets the current value of a Flow backed by a ConflatedBroadcastChannel.
 */
fun <T> Flow<T>.getValueFromConflatedBroadcastChannel(scope: CoroutineScope): T? {
    val channel = this.broadcastIn(scope).openSubscription()
    try {
        return channel.poll()
    } finally {
        channel.cancel()
    }
}