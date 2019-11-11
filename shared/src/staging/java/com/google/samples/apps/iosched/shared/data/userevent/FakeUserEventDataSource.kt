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

import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.userdata.UserEvent
import com.google.samples.apps.iosched.shared.data.FakeConferenceDataSource
import com.google.samples.apps.iosched.shared.data.FakeConferenceDataSource.ALARM_SESSION_ID
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus
import com.google.samples.apps.iosched.shared.result.Result
import kotlinx.coroutines.flow.flow

/**
 * Returns data loaded from a local JSON file for development and testing.
 */
object FakeUserEventDataSource : UserEventDataSource {
    private val conferenceData = FakeConferenceDataSource.getOfflineConferenceData()!!
    private val userEvents = ArrayList<UserEvent>()

    init {
        conferenceData.sessions.forEachIndexed { i, session ->
            if (i in 1..50) {
                userEvents.add(
                    UserEvent(
                        session.id,
                        isStarred = i % 2 == 0
                    )
                )
            }
        }
        conferenceData.sessions.find { it.id == ALARM_SESSION_ID }?.let { session ->
            userEvents.add(
                UserEvent(
                    session.id,
                    isStarred = true
                )
            )
        }
    }

    override fun getObservableUserEvents(userId: String) = flow {
        emit(UserEventsResult(userEvents))
    }

    override fun getObservableUserEvent(userId: String, eventId: SessionId) = flow {
        emit(UserEventResult(userEvents[0]))
    }

    override suspend fun starEvent(userId: SessionId, userEvent: UserEvent) =
        Result.Success(
            if (userEvent.isStarred) StarUpdatedStatus.STARRED
            else StarUpdatedStatus.UNSTARRED
        )

    override fun getUserEvents(userId: String): List<UserEvent> {
        return userEvents
    }

    override fun getUserEvent(userId: String, eventId: SessionId): UserEvent? {
        return userEvents.firstOrNull { it.id == eventId }
    }
}
