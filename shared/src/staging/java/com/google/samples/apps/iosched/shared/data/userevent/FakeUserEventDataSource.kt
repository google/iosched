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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.reservations.ReservationRequestResult
import com.google.samples.apps.iosched.model.reservations.ReservationRequestResult.ReservationRequestStatus.RESERVE_SUCCEEDED
import com.google.samples.apps.iosched.model.userdata.UserEvent
import com.google.samples.apps.iosched.shared.data.BootstrapConferenceDataSource
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.CancelAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.RequestAction
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus
import com.google.samples.apps.iosched.shared.domain.users.SwapRequestAction
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Success
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Returns data loaded from a local JSON file for development and testing.
 */
object FakeUserEventDataSource : UserEventDataSource {

    private val conferenceData = BootstrapConferenceDataSource.getOfflineConferenceData()!!
    private val userEvents = ArrayList<UserEvent>()

    init {
        conferenceData.sessions.forEachIndexed { i, session ->
            val reservation = ReservationRequestResult(
                RESERVE_SUCCEEDED, "123",
                System.currentTimeMillis()
            )
            if (i in 1..50) {
                userEvents.add(
                    UserEvent(
                        session.id,
                        isStarred = i % 2 == 0,
                        reservationRequestResult = reservation
                    )
                )
            }
        }
    }

    override fun getObservableUserEvents(userId: String): Flow<UserEventsResult> {
        return flow { emit(UserEventsResult(userEvents)) }
    }

    override fun getObservableUserEvent(
        userId: String,
        eventId: SessionId
    ) = flow {
        emit(UserEventResult(userEvents[0]))
    }

    override suspend fun starEvent(
        userId: SessionId,
        userEvent: UserEvent
    ) = Result.Success(
        if (userEvent.isStarred) StarUpdatedStatus.STARRED
        else StarUpdatedStatus.UNSTARRED
    )

    override suspend fun recordFeedbackSent(
        userId: String,
        userEvent: UserEvent
    ): Result<Unit> {
        return Result.Success(Unit)
    }

    override fun requestReservation(
        userId: String,
        session: Session,
        action: ReservationRequestAction
    ): LiveData<Result<ReservationRequestAction>> {
        val result = MutableLiveData<Result<ReservationRequestAction>>()
        result.postValue(
            Result.Success(
                if (action is RequestAction) RequestAction()
                else CancelAction()
            )
        )
        return result
    }

    override fun getUserEvents(userId: String): List<UserEvent> {
        return userEvents
    }

    override fun swapReservation(
        userId: String,
        fromSession: Session,
        toSession: Session
    ): LiveData<Result<SwapRequestAction>> {
        val result = MutableLiveData<Result<SwapRequestAction>>()
        result.postValue(Result.Success(SwapRequestAction()))
        return result
    }

    override fun getUserEvent(userId: String, eventId: SessionId): UserEvent? {
        return userEvents.firstOrNull { it.id == eventId }
    }

    override fun clearSingleEventSubscriptions() {}
}
