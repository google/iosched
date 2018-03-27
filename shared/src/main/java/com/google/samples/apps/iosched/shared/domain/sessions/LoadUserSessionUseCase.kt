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

import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.domain.MediatorUseCase
import com.google.samples.apps.iosched.shared.domain.internal.DefaultScheduler
import com.google.samples.apps.iosched.shared.model.UserSession
import com.google.samples.apps.iosched.shared.result.Result
import javax.inject.Inject

open class LoadUserSessionUseCase @Inject constructor(
        private val userEventRepository: DefaultSessionAndUserEventRepository
) : MediatorUseCase<Pair<String,String>, LoadUserSessionUseCaseResult>() {

    override fun execute(parameters: Pair<String, String>) {
        val (userId, eventId) = parameters
        val userSession = userEventRepository.getObservableUserEvent(userId, eventId)

        result.removeSource(userSession)
        result.addSource(userSession) {
            DefaultScheduler.execute {
                when (it) {
                    is Result.Success -> {
                        val useCaseResult = LoadUserSessionUseCaseResult(
                                userSession = it.data.userSession,
                                userMessage = it.data.userMessage
                        )
                        result.postValue(Result.Success(useCaseResult))
                    }
                    is Result.Error -> {
                        result.postValue(it)
                    }
                }
            }
        }
    }
}

data class LoadUserSessionUseCaseResult(
        val userSession: UserSession,
        val userMessage: UserEventMessage?
)

// TODO-76284 Do we need to make one of these
enum class UserEventMessage {
    CHANGE_IN_RESERVATION, CHANGE_IN_WAITLIST
}