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

package com.google.samples.apps.iosched.wear.ui.sessiondetail

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import com.google.samples.apps.iosched.shared.domain.sessions.LoadSessionUseCase
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.result.Result
import javax.inject.Inject

/**
 * Loads [Session] data and exposes it to the session detail view.
 */
class SessionDetailViewModel @Inject constructor(
    private val loadSessionUseCase: LoadSessionUseCase
) : ViewModel() {

    val useCaseResult = MutableLiveData<Result<Session>>()
    val session: LiveData<Session?>

    // TODO: Add error message if session doesn't load (w/ timeout)

    init {
        session = Transformations.map(useCaseResult) { result ->
            (result as? Result.Success)?.data
        }
    }

    fun loadSessionById(sessionId: String) {
        loadSessionUseCase(sessionId, useCaseResult)
    }
}
