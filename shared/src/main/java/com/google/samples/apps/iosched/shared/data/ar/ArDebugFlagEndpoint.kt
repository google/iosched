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

package com.google.samples.apps.iosched.shared.data.ar

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Interface that checkes if a signed user is able to demo the AR feature
 */
interface ArDebugFlagEndpoint {

    /**
     * Asks if the signed in user can demo the AR feature (bypass the teaser page)
     */
    suspend fun canDemoAr(): Boolean
}

class DefaultArDebugFlagEndpoint @Inject constructor(private val functions: FirebaseFunctions) :
    ArDebugFlagEndpoint {

    override suspend fun canDemoAr(): Boolean = suspendCancellableCoroutine { cont ->
        functions
            .getHttpsCallable("canDemoAr")
            .call()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val taskResult = task.result?.data as Map<*, *>
                    if (taskResult["whitelisted"] == true) {
                        cont.resume(true)
                    } else {
                        cont.resume(false)
                    }
                } else {
                    cont.resumeWithException(RuntimeException(task.exception))
                }
            }
    }
}
