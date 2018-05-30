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

import com.google.samples.apps.iosched.model.ConferenceDay
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.data.userevent.UserEventMessage
import com.google.samples.apps.iosched.shared.domain.MediatorUseCase
import com.google.samples.apps.iosched.shared.domain.internal.DefaultScheduler
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDays
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import javax.inject.Inject

/**
 * Loads sessions into lists keyed by [ConferenceDay].
 */
open class LoadUserSessionsByDayUseCase @Inject constructor(
    private val userEventRepository: DefaultSessionAndUserEventRepository
) : MediatorUseCase<LoadUserSessionsByDayUseCaseParameters, LoadUserSessionsByDayUseCaseResult>() {

    override fun execute(parameters: LoadUserSessionsByDayUseCaseParameters) {
        val (sessionMatcher, userId) = parameters

        Timber.d("LoadUserSessionsByDayUseCase: Refreshing sessions with user data")
        result.postValue(Result.Loading)

        val userSessionsObservable = userEventRepository.getObservableUserEvents(userId)

        // Avoid duplicating sources and trigger an update on the LiveData from the base class.
        result.removeSource(userSessionsObservable)
        result.addSource(userSessionsObservable) {
            DefaultScheduler.execute {
                when (it) {
                    is Result.Success -> {
                        val userSessions = it.data.userSessionsPerDay.mapValues { (_, sessions) ->
                            sessions.filter { sessionMatcher.matches(it) }
                                .sortedBy { it.session.startTime }
                        }

                        // Compute type from tags now so it's done in the background
                        userSessions.forEach { it.value.forEach { it.session.type } }

                        val usecaseResult = LoadUserSessionsByDayUseCaseResult(
                            userSessionsPerDay = userSessions,
                            userMessage = it.data.userMessage,
                            userMessageSession = it.data.userMessageSession,
                            userSessionCount = userSessions.values.map { it.size }.sum(),
                            firstUnfinishedSession = findFirstUnfinishedSession(
                                userSessions, parameters.now
                            )
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

    /**
     * During the conference, find the first session which has not finished so that the UI can
     * scroll to it.
     */
    private fun findFirstUnfinishedSession(
        userSessions: Map<ConferenceDay, List<UserSession>>,
        now: ZonedDateTime
    ): EventLocation? {
        if (now.isAfter(ConferenceDays.first().start) && now.isBefore(ConferenceDays.last().end)) {
            var unfinishedDay: ConferenceDay? = null
            var unfinishedSessionIndex = -1
            run loop@{
                ConferenceDays.filter { now.isBefore(it.end) }
                    .forEach { day ->
                        userSessions[day]?.forEachIndexed { sessionIndex, userSession ->
                            if (userSession.session.endTime.isAfter(now)) {
                                unfinishedDay = day
                                unfinishedSessionIndex = sessionIndex
                                return@loop
                            }
                        }
                    }
            }
            val day = unfinishedDay
            if (day != null && unfinishedSessionIndex != -1) {
                return EventLocation(ConferenceDays.indexOf(day), unfinishedSessionIndex)
            }
        }
        return null
    }
}

data class LoadUserSessionsByDayUseCaseParameters(
    val userSessionMatcher: UserSessionMatcher,

    val userId: String?,

    val now: ZonedDateTime = ZonedDateTime.now()
)

data class LoadUserSessionsByDayUseCaseResult(

    val userSessionsPerDay: Map<ConferenceDay, List<UserSession>>,

    /** A message to show to the user with important changes like reservation confirmations */
    val userMessage: UserEventMessage? = null,

    /** The session the user message is about, if any. */
    val userMessageSession: Session? = null,

    /** The total number of sessions. */
    val userSessionCount: Int,

    /** The location of the first session which has not finished or null. */
    val firstUnfinishedSession: EventLocation? = null
)

data class EventLocation(val day: Int, val sessionIndex: Int)
