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

package com.google.samples.apps.adssched.shared.domain.sessions

import com.google.samples.apps.adssched.model.ConferenceDay
import com.google.samples.apps.adssched.model.userdata.UserSession
import com.google.samples.apps.adssched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.adssched.shared.domain.MediatorUseCase
import com.google.samples.apps.adssched.shared.domain.internal.DefaultScheduler
import com.google.samples.apps.adssched.shared.result.Result
import com.google.samples.apps.adssched.shared.schedule.UserSessionMatcher
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
                            userSessionCount = userSessions.values.map { it.size }.sum(),
                            firstUnfinishedSession = findFirstUnfinishedSession(
                                userSessions,
                                userEventRepository.getConferenceDays(),
                                parameters.now
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
        conferenceDays: List<ConferenceDay>,
        now: ZonedDateTime
    ): EventLocation? {
        if (now.isAfter(conferenceDays.first().start) && now.isBefore(conferenceDays.last().end)) {
            var unfinishedDay: ConferenceDay? = null
            var unfinishedSessionIndex = -1
            run loop@{
                conferenceDays.filter { now.isBefore(it.end) }
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
                return EventLocation(conferenceDays.indexOf(day), unfinishedSessionIndex)
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

    /** The total number of sessions. */
    val userSessionCount: Int,

    /** The location of the first session which has not finished or null. */
    val firstUnfinishedSession: EventLocation? = null
)

data class EventLocation(val day: Int, val sessionIndex: Int)
