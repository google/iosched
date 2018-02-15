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
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.Tag
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_1
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_2
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_3
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.PRECONFERENCE_DAY
import timber.log.Timber
import javax.inject.Inject


/**
 * Loads data and exposes it to the view.
 * By annotating the constructor with [@Inject], Dagger will use that constructor when needing to
 * create the object, so defining a [@Provides] method for this class won't be needed.
 */
class ScheduleViewModel @Inject constructor(
        private val loadSessionsByDayUseCase: LoadSessionsByDayUseCase,
        private val loadTagsByCategoryUseCase: LoadTagsByCategoryUseCase
) : ViewModel(), ScheduleEventListener {

    private var filters = SessionFilters()

    lateinit var isLoading: LiveData<Boolean>

    val errorMessage: LiveData<String>
    val errorMessageShown = MutableLiveData<Boolean>()

    private val loadSessionsResult = MutableLiveData<Result<Map<ConferenceDay, List<Session>>>>()
    private val loadTagsResult = MutableLiveData<Result<List<Tag>>>()

    private val preconferenceSessions: LiveData<List<Session>>
    private val day1Sessions: LiveData<List<Session>>
    private val day2Sessions: LiveData<List<Session>>
    private val day3Sessions: LiveData<List<Session>>

    val tags: LiveData<List<Tag>>

    init {
        loadSessionsByDayUseCase.executeAsync(filters, loadSessionsResult)
        loadTagsByCategoryUseCase.executeAsync(Unit, loadTagsResult)

        // map LiveData results from UseCase to each day's individual LiveData
        preconferenceSessions = Transformations.map(loadSessionsResult) { result ->
            (result as? Result.Success)?.data?.get(PRECONFERENCE_DAY) ?: emptyList()
        }
        day1Sessions = Transformations.map(loadSessionsResult) { result ->
            (result as? Result.Success)?.data?.get(DAY_1) ?: emptyList()
        }
        day2Sessions = Transformations.map(loadSessionsResult) { result ->
            (result as? Result.Success)?.data?.get(DAY_2) ?: emptyList()
        }
        day3Sessions = Transformations.map(loadSessionsResult) { result ->
            (result as? Result.Success)?.data?.get(DAY_3) ?: emptyList()
        }

        isLoading = Transformations.map(loadSessionsResult) { result -> result == Result.Loading }

        errorMessage = Transformations.map(loadSessionsResult) { result ->
            errorMessageShown.value = false
            (result as? Result.Error)?.exception?.message ?: ""
        }

        tags = Transformations.map(loadTagsResult) { result ->
            (result as? Result.Success)?.data ?: emptyList()
        }
    }

    fun wasErrorMessageShown() : Boolean = errorMessageShown.value ?: false

    fun onErrorMessageShown() { errorMessageShown.value = true }

    fun getSessionsForDay(day: ConferenceDay): LiveData<List<Session>> = when (day) {
        PRECONFERENCE_DAY -> preconferenceSessions
        DAY_1 -> day1Sessions
        DAY_2 -> day2Sessions
        DAY_3 -> day3Sessions
    }

    fun applyFilters(filters: SessionFilters) {
        this.filters = filters
        loadSessionsByDayUseCase.executeAsync(filters, loadSessionsResult)
    }

    override fun openSessionDetail(id: String) {
        Timber.d("TODO: Open session detail for id: $id")
    }
}

interface ScheduleEventListener {
    fun openSessionDetail(id: String)
}
