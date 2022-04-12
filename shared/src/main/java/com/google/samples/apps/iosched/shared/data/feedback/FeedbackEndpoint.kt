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

package com.google.samples.apps.iosched.shared.data.feedback

import com.google.firebase.functions.FirebaseFunctions
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.shared.result.Result
import javax.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface FeedbackEndpoint {
    suspend fun sendFeedback(sessionId: SessionId, responses: Map<String, Int>): Result<Unit>
}

class DefaultFeedbackEndpoint @Inject constructor(
    private val functions: FirebaseFunctions
) : FeedbackEndpoint {

    override suspend fun sendFeedback(
        sessionId: SessionId,
        responses: Map<String, Int>
    ): Result<Unit> {
        return suspendCancellableCoroutine { continuation ->
                functions
                    .getHttpsCallable("sendFeedback")
                    .call(
                        hashMapOf(
                            "sessionId" to sessionId,
                            "responses" to responses,
                            "client" to "ANDROID"
                        )
                    )
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            continuation.resume(Result.Success(Unit))
                        } else {
                            continuation.resume(Result.Error(RuntimeException(task.exception)))
                        }
                    }
            }
        }
}
