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

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.model.TestData
import com.google.samples.apps.iosched.shared.data.userevent.UserEventDataSource
import com.google.samples.apps.iosched.shared.data.userevent.UserEventsResult
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus
import com.google.samples.apps.iosched.shared.firestore.entity.LastReservationRequested
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.result.Result

class TestUserEventDataSource(
        val userEventsResult: MutableLiveData<UserEventsResult>
            = MutableLiveData<UserEventsResult>()
) : UserEventDataSource {

    override fun getObservableUserEvents(userId: String): LiveData<UserEventsResult> {
        userEventsResult.postValue(UserEventsResult(true, TestData.userEvents))
        return userEventsResult
    }

    override fun updateStarred(userId: String, session: Session, isStarred: Boolean):
            LiveData<Result<StarUpdatedStatus>> {

        val result = MutableLiveData<Result<StarUpdatedStatus>>()
        result.postValue(Result.Success(
                if (isStarred) StarUpdatedStatus.STARRED else StarUpdatedStatus.UNSTARRED))
        return result
    }

    override fun requestReservation(
            userId: String, session: Session, action: ReservationRequestAction
    ): LiveData<Result<LastReservationRequested>> {

        val result = MutableLiveData<Result<LastReservationRequested>>()
        result.postValue(Result.Success(
                if (action == ReservationRequestAction.REQUEST) LastReservationRequested.RESERVATION
                else LastReservationRequested.CANCEL))
        return result
    }

    override fun getUserEvents(userId: String) = TestData.userEvents
}