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

package com.google.samples.apps.adssched.shared.domain.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.samples.apps.adssched.model.SessionId
import com.google.samples.apps.adssched.model.userdata.UserEvent
import com.google.samples.apps.adssched.shared.data.userevent.UserEventDataSource
import com.google.samples.apps.adssched.shared.data.userevent.UserEventResult
import com.google.samples.apps.adssched.shared.data.userevent.UserEventsResult
import com.google.samples.apps.adssched.shared.domain.users.StarUpdatedStatus
import com.google.samples.apps.adssched.shared.domain.users.StarUpdatedStatus.STARRED
import com.google.samples.apps.adssched.shared.domain.users.StarUpdatedStatus.UNSTARRED
import com.google.samples.apps.adssched.shared.result.Result
import com.google.samples.apps.adssched.test.data.TestData

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
        eventId: SessionId
    ): LiveData<UserEventResult> {
        userEventResult.postValue(UserEventResult(TestData.userEvents[0]))
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

    override fun getUserEvents(userId: String): List<UserEvent> = TestData.userEvents

    override fun clearSingleEventSubscriptions() {}

    override fun getUserEvent(userId: String, eventId: SessionId): UserEvent? {
        throw NotImplementedError()
    }
}
