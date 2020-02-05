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

import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.data.userevent.UserEventMessage
import com.google.samples.apps.iosched.shared.di.IoDispatcher
import com.google.samples.apps.iosched.shared.domain.FlowUseCase
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDays
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.threeten.bp.ZonedDateTime
import timber.log.Timber

/**
 * Loads filtered sessions according to the provided parameters.
 */
open class LoadFilteredUserSessionsUseCase @Inject constructor(
    private val userEventRepository: DefaultSessionAndUserEventRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : FlowUseCase<LoadFilteredUserSessionsParameters, LoadFilteredUserSessionsResult>(dispatcher) {

    override fun execute(
        parameters: LoadFilteredUserSessionsParameters
    ): Flow<Result<LoadFilteredUserSessionsResult>> {
        val (sessionMatcher, userId) = parameters

        Timber.d("LoadFilteredUserSessionsUseCase: Refreshing sessions with user data")
        return userEventRepository.getObservableUserEvents(userId).map { observableResult ->
            when (observableResult) {
                is Result.Success -> {
                    val filteredSessions = observableResult.data.userSessions
                        .filter { sessionMatcher.matches(it) }
                        .sortedWith(compareBy({ it.session.startTime }, { it.session.type }))

                    // Compute type from tags now so it's done in the background
                    filteredSessions.forEach { it.session.type }

                    val usecaseResult = LoadFilteredUserSessionsResult(
                        userSessions = filteredSessions,
                        // TODO(b/122306429) expose user events messages separately
                        userMessage = observableResult.data.userMessage,
                        userMessageSession = observableResult.data.userMessageSession,
                        userSessionCount = filteredSessions.size,
                        firstUnfinishedSessionIndex = findFirstUnfinishedSession(
                            filteredSessions, parameters.now
                        ),
                        dayIndexer = buildConferenceDayIndexer(filteredSessions)
                    )
                    Result.Success(usecaseResult)
                }
                is Result.Error -> {
                    Result.Error(observableResult.exception)
                }
                is Result.Loading -> Result.Loading
            }
        }
    }

    /**
     * During the conference, find the first session which has not finished so that the UI can
     * scroll to it.
     */
    private fun findFirstUnfinishedSession(
        userSessions: List<UserSession>,
        now: ZonedDateTime
    ): Int {
        if (now.isAfter(ConferenceDays.first().start) && now.isBefore(ConferenceDays.last().end)) {
            return userSessions.indexOfFirst { it.session.endTime.isAfter(now) }
        }
        return -1
    }

    /**
     * Finds indices in [sessions] where each ConferenceDay begins. This method assumes [sessions]
     * is sorted by start time.
     */
    private fun buildConferenceDayIndexer(sessions: List<UserSession>): ConferenceDayIndexer {
        val mapping = ConferenceDays
            .associateWith { day ->
                sessions.indexOfFirst {
                    day.contains(it.session)
                }
            }
            .filterValues { it >= 0 } // exclude days with no matching sessions

        return ConferenceDayIndexer(mapping)
    }
}

data class LoadFilteredUserSessionsParameters(
    val userSessionMatcher: UserSessionMatcher,

    val userId: String?,

    val now: ZonedDateTime = ZonedDateTime.now()
)

data class LoadFilteredUserSessionsResult(

    val userSessions: List<UserSession>,

    /** A message to show to the user with important changes like reservation confirmations */
    val userMessage: UserEventMessage? = null,

    /** The session the user message is about, if any. */
    val userMessageSession: Session? = null,

    /** The total number of sessions. */
    val userSessionCount: Int = userSessions.size,

    /** The location of the first session which has not finished. */
    val firstUnfinishedSessionIndex: Int = -1,

    val dayIndexer: ConferenceDayIndexer
)
