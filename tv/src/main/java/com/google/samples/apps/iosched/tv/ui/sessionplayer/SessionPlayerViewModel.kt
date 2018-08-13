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

package com.google.samples.apps.iosched.tv.ui.sessionplayer

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.shared.domain.sessions.LoadSessionUseCase
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.setValueIfNew
import javax.inject.Inject

/**
 * Loads [Session] data and exposes it to the session player view.
 */
class SessionPlayerViewModel @Inject constructor(
    private val loadSessionUseCase: LoadSessionUseCase
) : ViewModel() {

    private val useCaseResult: MutableLiveData<Result<Session>>
    val session = MediatorLiveData<Event<Session>>()

    init {
        useCaseResult = loadSessionUseCase.observe()

        session.addSource(useCaseResult) {
            if (useCaseResult.value is Result.Success) {
                val newSession = (useCaseResult.value as Result.Success<Session>).data
                session.setValueIfNew(Event(newSession))
            }
        }
    }

    fun loadSessionById(sessionId: String) {
        loadSessionUseCase.execute(sessionId)
    }
}
