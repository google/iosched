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

import androidx.annotation.WorkerThread
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import com.google.samples.apps.iosched.model.ConferenceDay
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.userdata.UserEvent
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.domain.internal.DefaultScheduler
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCaseResult
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCaseResult
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus
import com.google.samples.apps.iosched.shared.result.Result
import timber.log.Timber
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

    // Keep a reference to the observable for a single event (from the details screen) so we can
    // stop observing when done.
    private var observableUserEvent: LiveData<UserEventResult>? = null

    override fun getObservableUserEvents(
        userId: String?
    ): LiveData<Result<LoadUserSessionsByDayUseCaseResult>> {

        // If there is no logged-in user, return the map with null UserEvents
        if (userId == null) {
            DefaultScheduler.execute {
                Timber.d(
                    "EventRepository: No user logged in, returning sessions without user events."
                )
                val allSessions = sessionRepository.getSessions()
                val userSessionsPerDay = mapUserDataAndSessions(null, allSessions)
                sessionsByDayResult.postValue(
                    Result.Success(
                        LoadUserSessionsByDayUseCaseResult(
                            userSessionsPerDay = userSessionsPerDay,
                            userSessionCount = allSessions.size
                        )
                    )
                )
            }
            return sessionsByDayResult
        }

        // Observes the user events and merges them with session data.
        val observableUserEvents = userEventDataSource.getObservableUserEvents(userId)
        sessionsByDayResult.removeSource(observableUserEvents)
        sessionsByDayResult.addSource(observableUserEvents) { userEvents ->
            DefaultScheduler.execute {
                try {
                    // Don't update the result when userEvents is null, otherwise the count of the
                    // filtered results in the use case is going to be 0.
                    userEvents ?: return@execute

                    Timber.d(
                        """EventRepository: Received ${userEvents.userEvents.size}
                            |user events changes""".trimMargin()
                    )

                    // Get the sessions, synchronously
                    val allSessions = sessionRepository.getSessions()

                    sessionsByDayResult.postValue(
                        Result.Success(
                            LoadUserSessionsByDayUseCaseResult(
                                userSessionsPerDay = mapUserDataAndSessions(
                                    userEvents, allSessions
                                ),
                                userSessionCount = allSessions.size
                            )
                        )
                    )
                } catch (e: Exception) {
                    sessionsByDayResult.postValue(Result.Error(e))
                }
            }
        }
        return sessionsByDayResult
    }

    override fun getObservableUserEvent(
        userId: String?,
        eventId: SessionId
    ): LiveData<Result<LoadUserSessionUseCaseResult>> {
        val sessionResult = MediatorLiveData<Result<LoadUserSessionUseCaseResult>>()

        // If there is no logged-in user, return the session with a null UserEvent
        if (userId == null) {
            Timber.d("EventRepository: No user logged in, returning session without user event.")
            val session = sessionRepository.getSession(eventId)
            sessionResult.postValue(
                Result.Success(
                    LoadUserSessionUseCaseResult(
                        userSession = UserSession(session, createDefaultUserEvent(session))
                    )
                )
            )
            return sessionResult
        }

        // Observes the user events and merges them with session data.
        val newObservableUserEvent = userEventDataSource.getObservableUserEvent(userId, eventId)
        sessionResult.removeSource(newObservableUserEvent) // Avoid multiple subscriptions
        sessionResult.value = null // Prevent old data from being emitted
        sessionResult.addSource(newObservableUserEvent) { userEventResult ->

            DefaultScheduler.execute {
                try {
                    Timber.d("EventRepository: Received user event changes")
                    // Get the session, synchronously
                    val event = sessionRepository.getSession(eventId)

                    // Merges session with user data and emits the result
                    val userSession = UserSession(
                        event,
                        userEventResult?.userEvent ?: createDefaultUserEvent(event)
                    )
                    sessionResult.postValue(
                        Result.Success(
                            LoadUserSessionUseCaseResult(
                                userSession = userSession
                            )
                        )
                    )
                } catch (e: Exception) {
                    sessionResult.postValue(Result.Error(e))
                }
            }
        }
        this.observableUserEvent = newObservableUserEvent
        return sessionResult
    }

    override fun getUserEvents(userId: String?): List<UserEvent> {
        return userEventDataSource.getUserEvents(userId ?: "")
    }

    override fun getUserSession(userId: String, sessionId: SessionId): UserSession {
        val session = sessionRepository.getSession(sessionId)
        val userEvent = userEventDataSource.getUserEvent(userId, sessionId)
                ?: throw Exception("UserEvent not found")

        return UserSession(
                session = session,
                userEvent = userEvent
        )
    }

    override fun starEvent(
        userId: String,
        userEvent: UserEvent
    ): LiveData<Result<StarUpdatedStatus>> {
        return userEventDataSource.starEvent(userId, userEvent)
    }

    private fun createDefaultUserEvent(session: Session): UserEvent {
        return UserEvent(id = session.id)
    }

    /**
     * Merges user data with sessions.
     */
    @WorkerThread
    private fun mapUserDataAndSessions(
        userData: UserEventsResult?,
        allSessions: List<Session>
    ): Map<ConferenceDay, List<UserSession>> {

        val conferenceDays = sessionRepository.getConferenceDays()
        // If there is no logged-in user, return the map with null UserEvents
        if (userData == null) {
            return conferenceDays.map { day ->
                day to allSessions
                    .filter { day.contains(it) }
                    .map { session -> UserSession(session, createDefaultUserEvent(session)) }
            }.toMap()
        }

        val userEvents = userData.userEvents

        val allUserSessions = mergeUserEventsAndSessions(userEvents, allSessions)

        return conferenceDays.map { day ->
            day to allUserSessions
                .filter { day.contains(it.session) }
                .map { userSession ->
                    UserSession(userSession.session, userSession.userEvent)
                }
                .sortedBy { it.session.startTime }
        }.toMap()
    }

    private fun mergeUserEventsAndSessions(
        userEvents: List<UserEvent>,
        allSessions: List<Session>
    ): List<UserSession> {
        val eventIdToUserEvent: Map<String, UserEvent?> = userEvents.map { it.id to it }.toMap()
        return allSessions.map {
            UserSession(
                    it,
                    eventIdToUserEvent[it.id] ?: createDefaultUserEvent(it)
            )
        }
    }

    override fun clearSingleEventSubscriptions() {
        // The UserEvent data source can stop observing user data
        userEventDataSource.clearSingleEventSubscriptions()
    }

    override fun getConferenceDays(): List<ConferenceDay> = sessionRepository.getConferenceDays()
}

interface SessionAndUserEventRepository {

    fun getObservableUserEvents(
        userId: String?
    ): LiveData<Result<LoadUserSessionsByDayUseCaseResult>>

    fun getObservableUserEvent(
        userId: String?,
        eventId: SessionId
    ): LiveData<Result<LoadUserSessionUseCaseResult>>

    fun getUserEvents(userId: String?): List<UserEvent>

    fun getUserSession(userId: String, sessionId: SessionId): UserSession

    fun starEvent(userId: String, userEvent: UserEvent): LiveData<Result<StarUpdatedStatus>>

    fun clearSingleEventSubscriptions()

    fun getConferenceDays(): List<ConferenceDay>
}
