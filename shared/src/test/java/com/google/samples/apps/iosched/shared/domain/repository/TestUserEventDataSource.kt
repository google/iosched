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

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.shared.data.userevent.UserEventDataSource
import com.google.samples.apps.iosched.shared.data.userevent.UserEventsResult
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.CANCEL
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.REQUEST
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus.STARRED
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus.UNSTARRED
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.TestData
import com.google.samples.apps.iosched.shared.result.Result

class TestUserEventDataSource(
        private val userEventsResult: MutableLiveData<UserEventsResult> = MutableLiveData()
) : UserEventDataSource {

    override fun getObservableUserEvents(userId: String): LiveData<UserEventsResult> {
        userEventsResult.postValue(UserEventsResult(TestData.userEvents))
        return userEventsResult
    }

    override fun starEvent(userId: String, userEvent: UserEvent):
            LiveData<Result<StarUpdatedStatus>> {

        val result = MutableLiveData<Result<StarUpdatedStatus>>()
        result.postValue(Result.Success(
                if (userEvent.isStarred) STARRED else UNSTARRED))
        return result
    }

    override fun requestReservation(
            userId: String, session: Session, action: ReservationRequestAction
    ): LiveData<Result<ReservationRequestAction>> {

        val result = MutableLiveData<Result<ReservationRequestAction>>()
        result.postValue(Result.Success(
                if (action == REQUEST) REQUEST else CANCEL))
        return result
    }
}
