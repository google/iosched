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

package com.google.samples.apps.iosched.shared.domain.repository

import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.userdata.UserEvent
import com.google.samples.apps.iosched.shared.data.userevent.UserEventDataSource
import com.google.samples.apps.iosched.shared.data.userevent.UserEventResult
import com.google.samples.apps.iosched.shared.data.userevent.UserEventsResult
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.CancelAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.RequestAction
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus.STARRED
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus.UNSTARRED
import com.google.samples.apps.iosched.shared.domain.users.SwapRequestAction
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.test.data.TestData
import kotlinx.coroutines.flow.flow

class TestUserEventDataSource : UserEventDataSource {

    override fun getObservableUserEvents(userId: String) = flow {
        emit(UserEventsResult(TestData.userEvents))
    }

    override fun getObservableUserEvent(userId: String, eventId: SessionId) = flow {
        emit(UserEventResult(TestData.userEvents.find { it.id == eventId }))
    }

    override suspend fun starEvent(
        userId: String,
        userEvent: UserEvent
    ): Result<StarUpdatedStatus> = Result.Success(if (userEvent.isStarred) STARRED else UNSTARRED)

    override suspend fun recordFeedbackSent(userId: String, userEvent: UserEvent): Result<Unit> {
        return Result.Success(Unit)
    }

    override suspend fun requestReservation(
        userId: String,
        session: Session,
        action: ReservationRequestAction
    ): Result<ReservationRequestAction> =
        Result.Success(
            if (action is RequestAction) RequestAction() else CancelAction()
        )

    override fun getUserEvents(userId: String): List<UserEvent> = TestData.userEvents

    override suspend fun swapReservation(
        userId: String,
        fromSession: Session,
        toSession: Session
    ): Result<SwapRequestAction> = Result.Success(SwapRequestAction())

    override fun clearSingleEventSubscriptions() {}

    override fun getUserEvent(userId: String, eventId: SessionId): UserEvent? {
        throw NotImplementedError()
    }
}
