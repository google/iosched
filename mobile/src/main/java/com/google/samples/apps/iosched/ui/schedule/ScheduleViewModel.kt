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
import android.arch.lifecycle.ViewModel
import com.google.samples.apps.iosched.shared.model.Block
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.Tag
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.usecases.invoke
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_1
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_2
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_3
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.ui.schedule.agenda.LoadAgendaUseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Loads data and exposes it to the view.
 * By annotating the constructor with [@Inject], Dagger will use that constructor when needing to
 * create the object, so defining a [@Provides] method for this class won't be needed.
 */
class ScheduleViewModel @Inject constructor(
        private val loadSessionsByDayUseCase: LoadSessionsByDayUseCase,
        loadAgendaUseCase: LoadAgendaUseCase,
        loadTagsByCategoryUseCase: LoadTagsByCategoryUseCase
) : ViewModel(), ScheduleEventListener {

    private var filters = SessionFilters()

    val isLoading: LiveData<Boolean>

    val tags: LiveData<List<Tag>>

    val errorMessage: LiveData<String>
    val errorMessageShown = MutableLiveData<Boolean>()

    private val loadSessionsResult = MutableLiveData<Result<Map<ConferenceDay, List<Session>>>>()
    private val loadAgendaResult = MutableLiveData<Result<List<Block>>>()
    private val loadTagsResult = MutableLiveData<Result<List<Tag>>>()

    private val day1Sessions: LiveData<List<Session>>
    private val day2Sessions: LiveData<List<Session>>
    private val day3Sessions: LiveData<List<Session>>

    val agenda: LiveData<List<Block>>

    init {
        // Load sessions and tags and store the result in `LiveData`s
        loadSessionsByDayUseCase(filters, loadSessionsResult)
        loadAgendaUseCase(loadAgendaResult)
        loadTagsByCategoryUseCase(loadTagsResult)

        // map LiveData results from UseCase to each day's individual LiveData
        day1Sessions = loadSessionsResult.map {
            (it as? Result.Success)?.data?.get(DAY_1) ?: emptyList()
        }
        day2Sessions = loadSessionsResult.map {
            (it as? Result.Success)?.data?.get(DAY_2) ?: emptyList()
        }
        day3Sessions = loadSessionsResult.map {
            (it as? Result.Success)?.data?.get(DAY_3) ?: emptyList()
        }

        isLoading = loadSessionsResult.map { it == Result.Loading}

        errorMessage = loadSessionsResult.map { result ->
            errorMessageShown.value = false
            (result as? Result.Error)?.exception?.message ?: ""
        }

        agenda = loadAgendaResult.map {
            (it as? Result.Success)?.data ?: emptyList()
        }
        // TODO handle agenda errors

        tags = loadTagsResult.map { result ->
            (result as? Result.Success)?.data ?: emptyList()
        }
    }

    fun wasErrorMessageShown() : Boolean = errorMessageShown.value ?: false

    fun onErrorMessageShown() { errorMessageShown.value = true }

    fun getSessionsForDay(day: ConferenceDay): LiveData<List<Session>> = when (day) {
        DAY_1 -> day1Sessions
        DAY_2 -> day2Sessions
        DAY_3 -> day3Sessions
    }

    override fun openSessionDetail(id: String) {
        Timber.d("TODO: Open session detail for id: $id")
    }

    override fun toggleFilter(tag: Tag, enabled: Boolean) {
        if (enabled) {
            filters.add(tag)
        } else {
            filters.remove(tag)
        }
        loadSessionsByDayUseCase(filters, loadSessionsResult)
    }

    override fun clearFilters() {
        filters.clearAll()
        loadSessionsByDayUseCase(filters, loadSessionsResult)
    }
}

interface ScheduleEventListener {
    fun openSessionDetail(id: String)
    fun toggleFilter(tag: Tag, enabled: Boolean)
    fun clearFilters()
}
