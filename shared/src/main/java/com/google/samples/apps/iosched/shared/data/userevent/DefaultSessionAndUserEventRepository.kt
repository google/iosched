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

package com.google.samples.apps.iosched.shared.data.userevent

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.support.annotation.WorkerThread
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.domain.internal.DefaultScheduler
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCaseResult
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCaseResult
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.UserSession
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.shared.util.toEpochMilli
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single point of access to user events data associated with a user for the presentation layer.
 */
@Singleton
open class DefaultSessionAndUserEventRepository @Inject constructor(
        private val userEventDataSource: UserEventDataSource,
        private val sessionRepository: SessionRepository
) : SessionAndUserEventRepository {
    private val sessionsByDayResult = MediatorLiveData<Result<LoadUserSessionsByDayUseCaseResult>>()
    private val sessionResult = MediatorLiveData<Result<LoadUserSessionUseCaseResult>>()

    override fun getObservableUserEvents(userId: String?):
            LiveData<Result<LoadUserSessionsByDayUseCaseResult>> {

        // If there is no logged-in user, return the map with null UserEvents
        if (userId == null) {
            val userSessionsPerDay = mapUserDataAndSessions(null, sessionRepository.getSessions())
            sessionsByDayResult.postValue(Result.Success(
                    LoadUserSessionsByDayUseCaseResult(
                            userSessionsPerDay = userSessionsPerDay,
                            userMessage = null)
            ))
            return sessionsByDayResult
        }
        // Observes the user events and merges them with session data.
        val observableUserEvents = userEventDataSource.getObservableUserEvents(userId)
        sessionsByDayResult.removeSource(observableUserEvents)
        sessionsByDayResult.addSource(observableUserEvents) { userEvents ->
            userEvents ?: return@addSource

            DefaultScheduler.execute {
                try {
                    // Get the sessions, synchronously
                    val allSessions = sessionRepository.getSessions()
                    // Merges sessions with user data and emits the result
                    sessionsByDayResult.postValue(Result.Success(
                            LoadUserSessionsByDayUseCaseResult(
                                    userSessionsPerDay =  mapUserDataAndSessions(
                                            userEvents, allSessions),
                                    userMessage = userEvents.userEventsMessage)
                    ))

                } catch (e: Exception) {
                    sessionsByDayResult.postValue(Result.Error(e))
                }
            }
        }
        return sessionsByDayResult
    }

    override fun getObservableUserEvent(
            userId: String?,
            eventId: String
    ): LiveData<Result<LoadUserSessionUseCaseResult>> {

        // If there is no logged-in user, return the session with a null UserEvent
        if (userId == null) {
            val session = sessionRepository.getSession(eventId)
            sessionResult.postValue(Result.Success(
                    LoadUserSessionUseCaseResult(
                            userSession = UserSession(session, UserEvent(session.id,
                                            session.startTime.toEpochMilli(),
                                            session.endTime.toEpochMilli())),
                            userMessage = null)
            ))
            return sessionResult
        }

        // Observes the user events and merges them with session data.
        val observableUserEvent = userEventDataSource.getObservableUserEvent(userId, eventId)
        sessionResult.removeSource(observableUserEvent)
        sessionResult.addSource(observableUserEvent) { userEventResult ->
            userEventResult ?: return@addSource

            DefaultScheduler.execute {
                try {
                    // Get the session, synchronously
                    val event = sessionRepository.getSession(eventId)

                    // Merges session with user data and emits the result
                    val userSession = UserSession(event,
                            userEventResult.userEvent ?: createDefaultUserEvent(event))
                    sessionResult.postValue(Result.Success(
                            LoadUserSessionUseCaseResult(
                                    userSession = userSession,
                                    userMessage = userEventResult.userEventMessage
                            )
                    ))
                } catch (e: Exception) {
                    sessionResult.postValue(Result.Error(e))
                }
            }
        }
        return sessionResult
    }

    override fun starEvent(userId: String, userEvent: UserEvent):
            LiveData<Result<StarUpdatedStatus>> {
        return userEventDataSource.starEvent(userId, userEvent)
    }

    override fun changeReservation(
            userId: String,
            sessionId: String,
            action: ReservationRequestAction
    ): LiveData<Result<ReservationRequestAction>> {
        return userEventDataSource.requestReservation(userId,
                sessionRepository.getSession(sessionId),
                action)
    }

    private fun createDefaultUserEvent(session: Session): UserEvent {
        return UserEvent(session.id, session.startTime.toEpochMilli(),
                session.endTime.toEpochMilli())
    }

    /**
     * Merges user data with sessions.
     */
    @WorkerThread
    private fun mapUserDataAndSessions(
            userData: UserEventsResult?,
            allSessions: List<Session>
    ): Map<ConferenceDay, List<UserSession>> {

        // If there is no logged-in user, return the map with null UserEvents
        if (userData == null) {
            return ConferenceDay.values().map { day ->
                day to allSessions
                        .filter { day.contains(it) }
                        .map { session -> UserSession(session, UserEvent(session.id,
                                session.startTime.toEpochMilli(),
                                session.endTime.toEpochMilli())) }

            }.toMap()
        }

        val (userEvents, _) = userData

        val eventIdToUserEvent: Map<String, UserEvent?> = userEvents.map { it.id to it }.toMap()
        val allUserSessions = allSessions.map { UserSession(it,
                eventIdToUserEvent[it.id] ?: UserEvent(id = it.id,
                        startTime = it.startTime.toEpochMilli(),
                        endTime = it.endTime.toEpochMilli())) }


        // Note: hasPendingWrite field isn't used for the StarEvent use case because
        // the UI is updated optimistically regardless of the UserEvent is stored in the
        // backend. But keeping the updating hasPendingWrite field for a proof of concept
        // we need to use it for reservation
        return ConferenceDay.values().map { day ->
            day to allUserSessions
                    .filter { day.contains(it.session) }
                    .map { userSession ->
                        UserSession(userSession.session, userSession.userEvent)
                    }
                    .sortedBy { it.session.startTime }
        }.toMap()
    }
}

interface SessionAndUserEventRepository {

    fun getObservableUserEvents(userId: String?):
            LiveData<Result<LoadUserSessionsByDayUseCaseResult>>

    fun getObservableUserEvent(userId: String?, eventId: String):
            LiveData<Result<LoadUserSessionUseCaseResult>>

    fun changeReservation(
            userId: String, sessionId: String, action: ReservationRequestAction
    ): LiveData<Result<ReservationRequestAction>>

    fun starEvent(userId: String, userEvent: UserEvent): LiveData<Result<StarUpdatedStatus>>
}

