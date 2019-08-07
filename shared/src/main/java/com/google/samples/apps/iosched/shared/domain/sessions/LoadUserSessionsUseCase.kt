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

package com.google.samples.apps.iosched.shared.domain.sessions

import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.di.DefaultDispatcher
import com.google.samples.apps.iosched.shared.domain.FlowUseCase
import com.google.samples.apps.iosched.shared.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Load [UserSession]s for a given list of sessions.
 */
open class LoadUserSessionsUseCase @Inject constructor(
    private val userEventRepository: DefaultSessionAndUserEventRepository,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
) : FlowUseCase<Pair<String?, Set<SessionId>>, LoadUserSessionsUseCaseResult>(defaultDispatcher) {

    override fun execute(
        parameters: Pair<String?, Set<String>>
    ): Flow<Result<LoadUserSessionsUseCaseResult>> {
        val (userId, eventIds) = parameters
        // Observe *all* user events
        return userEventRepository.getObservableUserEvents(userId).map { result ->
            when (result) {
                is Result.Success -> {
                    // Filter down to events for sessions we're interested in
                    val relevantUserSessions = result.data.userSessionsPerDay
                        .flatMap { it.value.filter { it.session.id in eventIds } }
                        .sortedBy { it.session.startTime }
                    if (relevantUserSessions.isNotEmpty()) {
                        val useCaseResult = LoadUserSessionsUseCaseResult(relevantUserSessions)
                        Result.Success(useCaseResult)
                    } else {
                        Result.Error(IllegalStateException("RelevantUserSessions is empty"))
                    }
                }
                is Result.Error -> result
                else -> Result.Error(IllegalStateException("Result must be Success or Error"))
            }
        }
    }
}

data class LoadUserSessionsUseCaseResult(val userSessions: List<UserSession>)
