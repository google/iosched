/*
 * Copyright 2019 Google LLC
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

package com.google.samples.apps.iosched.shared.domain.sessions

import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.domain.MediatorUseCase
import com.google.samples.apps.iosched.shared.domain.internal.DefaultScheduler
import com.google.samples.apps.iosched.shared.result.Result
import javax.inject.Inject

/**
 * Load a list of pinned (starred or reserved) [UserSession]s for a given user.
 */
open class LoadPinnedSessionsUseCase @Inject constructor(
    private val userEventRepository: DefaultSessionAndUserEventRepository
) : MediatorUseCase<String, List<UserSession>>() {

    override fun execute(parameters: String) {
        val userSessionsObservable = userEventRepository.getObservableUserEvents(parameters)

        result.removeSource(userSessionsObservable)
        result.value = null
        result.addSource(userSessionsObservable) { observableResult ->
            DefaultScheduler.execute {
                when (observableResult) {
                    is Result.Success -> {
                        val useCaseResult = observableResult.data.userSessions.filter {
                            it.userEvent.isPinned()
                        }
                        result.postValue(Result.Success(useCaseResult))
                    }
                    is Result.Error -> {
                        result.postValue(observableResult)
                    }
                }
            }
        }
    }
}
