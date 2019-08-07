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

import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.userdata.UserEvent
import com.google.samples.apps.iosched.shared.data.userevent.UserEventDataSource
import com.google.samples.apps.iosched.shared.data.userevent.UserEventResult
import com.google.samples.apps.iosched.shared.data.userevent.UserEventsResult
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus.STARRED
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus.UNSTARRED
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.test.data.TestData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TestUserEventDataSource : UserEventDataSource {

    override fun getObservableUserEvents(userId: String): Flow<UserEventsResult> = flow {
        emit(UserEventsResult(TestData.userEvents))
    }

    override fun getObservableUserEvent(userId: String, eventId: String) = flow {
        emit(UserEventResult(TestData.userEvents
            .find { it.id == eventId } ?: TestData.userEvents[0]))
    }

    override suspend fun starEvent(
        userId: String,
        userEvent: UserEvent
    ): Result<StarUpdatedStatus> = Result.Success(if (userEvent.isStarred) STARRED else UNSTARRED)

    override fun getUserEvents(userId: String): List<UserEvent> = TestData.userEvents

    override fun getUserEvent(userId: String, eventId: SessionId): UserEvent? {
        TODO("not implemented")
    }
}
