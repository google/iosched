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
): MediatorUseCase<Pair<UserSessionMatcher, String?>, LoadUserSessionsByDayUseCaseResult>() {

    override fun execute(parameters: Pair<UserSessionMatcher, String?>) {
        val (sessionMatcher, userId) = parameters

        val userSessionsObservable = userEventRepository.getObservableUserEvents(userId)

        // Avoid duplicating sources and trigger an update on the LiveData from the base class.
        result.removeSource(userSessionsObservable)
        result.addSource(userSessionsObservable) {
            DefaultScheduler.execute {
                when (it) {
                    is Result.Success -> {
                        val userSessions = it.data.userSessionsPerDay.mapValues { (_, sessions) ->
                            sessions.filter { sessionMatcher.matches(it) }
                        }
                        // Compute type from tags now so it's done in the background
                        userSessions.forEach { _, value -> value.forEach { it.session.type } }
                        val usecaseResult = LoadUserSessionsByDayUseCaseResult(
                                userSessionsPerDay = userSessions,
                                userMessage = it.data.userMessage
                        )
                        result.postValue(Result.Success(usecaseResult))
                    }
                    is Result.Error -> {
                        result.postValue(it)
                    }
                }
            }
        }
    }
}

data class LoadUserSessionsByDayUseCaseResult(
    val userSessionsPerDay: Map<ConferenceDay, List<UserSession>>,
    val userMessage: UserEventsMessage?
)

enum class UserEventsMessage {
    CHANGES_IN_RESERVATIONS,
    CHANGES_IN_WAITLIST,
    RESERVATION_CANCELED,
    WAITLIST_CANCELED,
    RESERVATION_DENIED_CUTOFF,
    RESERVATION_DENIED_CLASH,
    RESERVATION_DENIED_UNKNOWN,
    CANCELLATION_DENIED_CUTOFF,
    CANCELLATION_DENIED_UNKNOWN,
    DATA_NOT_SYNCED
}
