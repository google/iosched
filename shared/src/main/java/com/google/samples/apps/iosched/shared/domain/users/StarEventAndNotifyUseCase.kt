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

package com.google.samples.apps.iosched.shared.domain.users

import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.data.userevent.SessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.di.IoDispatcher
import com.google.samples.apps.iosched.shared.domain.CoroutinesUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.StarReserveNotificationAlarmUpdater
import com.google.samples.apps.iosched.shared.result.Result
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

open class StarEventAndNotifyUseCase @Inject constructor(
    private val repository: SessionAndUserEventRepository,
    private val alarmUpdater: StarReserveNotificationAlarmUpdater,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : CoroutinesUseCase<StarEventParameter, StarUpdatedStatus>(ioDispatcher) {

    override suspend fun execute(parameters: StarEventParameter): StarUpdatedStatus {
        return when (val result = repository.starEvent(
            parameters.userId, parameters.userSession.userEvent
        )) {
            is Result.Success -> {
                val updateResult = result.data
                alarmUpdater.updateSession(
                    parameters.userSession,
                    parameters.userSession.userEvent.isStarred
                )
                updateResult
            }
            is Result.Error -> throw result.exception
            else -> throw IllegalStateException()
        }
    }
}

data class StarEventParameter(
    val userId: String,
    val userSession: UserSession
)

enum class StarUpdatedStatus {
    STARRED,
    UNSTARRED
}
