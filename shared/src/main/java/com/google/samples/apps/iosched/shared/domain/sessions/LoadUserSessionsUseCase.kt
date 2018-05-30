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
import com.google.samples.apps.iosched.shared.domain.MediatorUseCase
import com.google.samples.apps.iosched.shared.domain.internal.DefaultScheduler
import com.google.samples.apps.iosched.shared.result.Result
import javax.inject.Inject

/**
 * Load [UserSession]s for a given list of sessions.
 */
open class LoadUserSessionsUseCase @Inject constructor(
    private val userEventRepository: DefaultSessionAndUserEventRepository
) : MediatorUseCase<Pair<String?, Set<SessionId>>, LoadUserSessionsUseCaseResult>() {

    override fun execute(parameters: Pair<String?, Set<String>>) {
        val (userId, eventIds) = parameters
        // Observe *all* user events
        val userSessions = userEventRepository.getObservableUserEvents(userId)

        result.removeSource(userSessions)
        result.value = null
        result.addSource(userSessions) {
            DefaultScheduler.execute {
                when (it) {
                    is Result.Success -> {
                        // Filter down to events for sessions we're interested in
                        val relevantUserSessions = it.data.userSessionsPerDay
                            .flatMap { it.value.filter { it.session.id in eventIds } }
                            .sortedBy { it.session.startTime }
                        if (relevantUserSessions.isNotEmpty()) {
                            val useCaseResult = LoadUserSessionsUseCaseResult(relevantUserSessions)
                            result.postValue(Result.Success(useCaseResult))
                        }
                    }
                    is Result.Error -> {
                        result.postValue(it)
                    }
                }
            }
        }
    }

    fun onCleared() {
        // This use case is no longer going to be used so remove subscriptions
        userEventRepository.clearSingleEventSubscriptions()
    }
}

data class LoadUserSessionsUseCaseResult(val userSessions: List<UserSession>)
