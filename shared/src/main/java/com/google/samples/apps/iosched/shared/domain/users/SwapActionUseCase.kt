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

package com.google.samples.apps.iosched.shared.domain.users

import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.shared.data.userevent.SessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.di.IoDispatcher
import com.google.samples.apps.iosched.shared.domain.UseCase
import com.google.samples.apps.iosched.shared.result.Result.Error
import com.google.samples.apps.iosched.shared.result.Result.Loading
import com.google.samples.apps.iosched.shared.result.Result.Success
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Sends a request to replace reservations.
 */
open class SwapActionUseCase @Inject constructor(
    private val repository: SessionAndUserEventRepository,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<SwapRequestParameters, SwapRequestAction>(ioDispatcher) {

    override suspend fun execute(parameters: SwapRequestParameters): SwapRequestAction {
        val (userId, sessionId, _, toId) = parameters
        return when (
            val updateResult =
                repository.swapReservation(userId, sessionId, toId)
        ) {
            is Success -> updateResult.data
            is Error -> throw updateResult.exception
            Loading -> throw IllegalStateException()
        }
    }
}

/**
 * Parameters required to process the swap reservations request.
 */
data class SwapRequestParameters(
    val userId: String,
    val fromId: SessionId,
    val fromTitle: String,
    val toId: SessionId,
    val toTitle: String
)

class SwapRequestAction
