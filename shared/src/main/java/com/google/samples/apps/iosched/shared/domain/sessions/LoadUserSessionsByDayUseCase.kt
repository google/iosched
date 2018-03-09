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
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import javax.inject.Inject

/**
 * Loads sessions into lists keyed by [ConferenceDay].
 */
open class LoadUserSessionsByDayUseCase @Inject constructor(
        private val userEventRepository: DefaultSessionAndUserEventRepository
): MediatorUseCase<Pair<UserSessionMatcher, String>, Map<ConferenceDay, List<UserSession>>>() {

    override fun execute(parameters: Pair<UserSessionMatcher, String>) {
        val (sessionMatcher, userId) = parameters
        val userSessions = userEventRepository.getObservableUserEvents(userId)

        // Avoid duplicating sources and trigger an update on the LiveData from the base class.
        result.removeSource(userSessions)
        result.addSource(userSessions) {
            DefaultScheduler.execute {
                when (it) {
                    is Result.Success -> {
                        val res = it.data.mapValues { (_, sessions) ->
                            sessions.filter { sessionMatcher.matches(it) }
                        }
                        result.postValue(Result.Success(res))
                    }
                    is Result.Error -> {
                        result.postValue(it)
                    }
                }
            }
        }
    }
}
