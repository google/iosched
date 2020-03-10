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

package com.google.samples.apps.iosched.shared.domain.sessions

import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.di.IoDispatcher
import com.google.samples.apps.iosched.shared.domain.FlowUseCase
import com.google.samples.apps.iosched.shared.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Load starred and reserved [UserSession]s for a given user.
 */
@ExperimentalCoroutinesApi
class LoadStarredAndReservedSessionsUseCase @Inject constructor(
    private val userEventRepository: DefaultSessionAndUserEventRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : FlowUseCase<String?, List<UserSession>>(dispatcher) {

    override fun execute(parameters: String?): Flow<Result<List<UserSession>>> {
        // Shortcut: if there's no userId, there won't be any starred or reserved sessions.
        val userId = parameters ?: return flowOf(Result.Success(emptyList()))
        // Observe *all* user events
        return userEventRepository.getObservableUserEvents(userId).map { result ->
            when (result) {
                is Result.Success -> {
                    val relevantUserSessions = result.data.userSessions.filter {
                        it.userEvent.isStarredOrReserved()
                    }
                    if (relevantUserSessions.isNotEmpty()) {
                        Result.Success(relevantUserSessions)
                    } else {
                        Result.Success(emptyList())
                    }
                }
                is Result.Loading -> result
                is Result.Error -> result
            }
        }
    }
}
