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

package com.google.samples.apps.iosched.ui.schedule.day

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.model.Session
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

class TestUserEventDataSource(
    private val userEventsResult: MutableLiveData<UserEventsResult> = MutableLiveData(),
    private val userEventResult: MutableLiveData<UserEventResult> = MutableLiveData()
) : UserEventDataSource {

    override fun getObservableUserEvents(userId: String): LiveData<UserEventsResult> {
        userEventsResult.postValue(UserEventsResult(TestData.userEvents))
        return userEventsResult
    }

    override fun getObservableUserEvent(
        userId: String,
        eventId: String
    ): LiveData<UserEventResult> {
        userEventResult.postValue(UserEventResult(TestData.userEvents
            .find { it.id == eventId } ?: TestData.userEvents[0]))
        return userEventResult
    }

    override fun starEvent(
        userId: String,
        userEvent: UserEvent
    ): LiveData<Result<StarUpdatedStatus>> {
        val result = MutableLiveData<Result<StarUpdatedStatus>>()
        result.postValue(
            Result.Success(
                if (userEvent.isStarred) STARRED else UNSTARRED
            )
        )
        return result
    }

    override fun requestReservation(
        userId: String,
        session: Session,
        action: ReservationRequestAction
    ): LiveData<Result<ReservationRequestAction>> {
        val result = MutableLiveData<Result<ReservationRequestAction>>()
        result.postValue(
            Result.Success(
                if (action is RequestAction) RequestAction() else CancelAction()
            )
        )
        return result
    }

    override fun getUserEvents(userId: String): List<UserEvent> = TestData.userEvents

    override fun swapReservation(
        userId: String,
        fromSession: Session,
        toSession: Session
    ): LiveData<Result<SwapRequestAction>> {
        val result = MutableLiveData<Result<SwapRequestAction>>()
        result.postValue(Result.Success(SwapRequestAction()))
        return result
    }

    override fun clearSingleEventSubscriptions() {}
}
