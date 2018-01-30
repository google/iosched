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

package com.google.samples.apps.iosched.ui.schedule

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.usecases.repository.LoadSessionsUseCase
import javax.inject.Inject

/**
 * Loads data and exposes it to the view.
 * By annotating the constructor with [@Inject], Dagger will use that constructor when needing to
 * create the object, so defining a [@Provides] method for this class won't be needed.
 */
class ScheduleViewModel @Inject constructor(loadSessionsUseCase: LoadSessionsUseCase)
    : ViewModel() {

    // TODO: Example LiveData holders
    val sessions: LiveData<List<Session>>
    val isLoading: LiveData<Boolean>
    val numberOfSessions: LiveData<Int>

    init {
        // TODO: replace. Dummy async task
        val liveResult: LiveData<Result<List<Session>>> = loadSessionsUseCase.executeAsync("dummy")

        sessions = Transformations.map(liveResult, { result ->
            (result as? Result.Success)?.data ?: emptyList()
        })
        isLoading = Transformations.map(liveResult, { result -> result == Result.Loading })
        numberOfSessions = Transformations.map(liveResult, { result ->
            (result as? Result.Success)?.data?.size ?: 0
        })
    }
}

