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
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCaseResult
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus
import com.google.samples.apps.iosched.shared.firestore.entity.LastReservationRequested
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.UserSession
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
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

    val result = MediatorLiveData<Result<LoadUserSessionsByDayUseCaseResult>>()

    override fun getObservableUserEvents(userId: String?):
            LiveData<Result<LoadUserSessionsByDayUseCaseResult>> {

        // If there is no logged-in user, return the map with null UserEvents
        if (userId == null) {
            val userSessionsPerDay = mapUserDataAndSessions(null, sessionRepository.getSessions())
            result.postValue(Result.Success(
                    LoadUserSessionsByDayUseCaseResult(
                            userSessionsPerDay = userSessionsPerDay,
                            userMessage = null)
            ))
            return result
        }
        // Observes the user events and merges them with session data.
        val observableUserEvents = userEventDataSource.getObservableUserEvents(userId)

        result.removeSource(observableUserEvents)
        result.addSource(observableUserEvents) { userEvents ->
            userEvents ?: return@addSource

            DefaultScheduler.execute {
                try {
                    // Get the sessions, synchronously
                    val allSessions = sessionRepository.getSessions()
                    // Merges sessions with user data and emits the result
                    result.postValue(Result.Success(
                            LoadUserSessionsByDayUseCaseResult(
                                    userSessionsPerDay =  mapUserDataAndSessions(
                                            userEvents, allSessions),
                                    userMessage = userEvents.userEventsMessage)
                    ))

                } catch (e: Exception) {
                    result.postValue(Result.Error(e))
                }
            }
        }
        return result
    }

    override fun updateIsStarred(userId: String, session: Session, isStarred: Boolean):
            LiveData<Result<StarUpdatedStatus>> {
        return userEventDataSource.updateStarred(userId, session, isStarred)
    }

    override fun changeReservation(
            userId: String,
            session: Session,
            action: ReservationRequestAction
    ): LiveData<Result<LastReservationRequested>> {
        return userEventDataSource.requestReservation(userId, session, action)
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
                        .map { session -> UserSession(session, null) }

            }.toMap()
        }

        val (allDataSynced, userEvents, _) = userData

        val eventIdToUserEvent: Map<String, UserEvent?> = userEvents.map { it.id to it }.toMap()
        val allUserSessions = allSessions.map { UserSession(it, eventIdToUserEvent[it.id]) }

        // If there are already entities that are marked as hasPendingWrite,
        // we merge the hasPendingWrite states unless all data is synced to the backend
        val alreadyPendingWriteIds = if (result.value is Result.Success) {
            if (allDataSynced) {
                emptySet()
            } else {
                (result.value as Result.Success).data.userSessionsPerDay.flatMap { it.value }
                        .filter { it.userEvent?.hasPendingWrite == true }
                        .map { it.userEvent?.id }
                        .toSet()
            }
        } else {
            emptySet()
        }

        // Note: hasPendingWrite field isn't used for the StarEvent use case because
        // the UI is updated optimistically regardless of the UserEvent is stored in the
        // backend. But keeping the updating hasPendingWrite field for a proof of concept
        // we need to use it for reservation
        return ConferenceDay.values().map { day ->
            day to allUserSessions
                    .filter { day.contains(it.session) }
                    .map { userSession ->
                        UserSession(userSession.session, userSession.userEvent?.apply {
                            if (alreadyPendingWriteIds.contains(id)) {
                                hasPendingWrite = true
                            }
                        })
                    }
                    .sortedBy { it.session.startTime }
        }.toMap()
    }
}

interface SessionAndUserEventRepository {

    fun getObservableUserEvents(userId: String?):
            LiveData<Result<LoadUserSessionsByDayUseCaseResult>>

    fun updateIsStarred(userId: String, session: Session, isStarred: Boolean):
            LiveData<Result<StarUpdatedStatus>>

    fun changeReservation(
            userId: String, session: Session, action: ReservationRequestAction
    ): LiveData<Result<LastReservationRequested>>
}

