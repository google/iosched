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

package com.google.samples.apps.iosched.tv.ui.schedule

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCaseResult
import com.google.samples.apps.iosched.shared.model.UserSession
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher.TagFilterMatcher
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.shared.util.map

/**
 * Loads sessions data and exposes each day's sessions to the view.
 */
class ScheduleViewModel(loadSessionsByDayUseCase: LoadUserSessionsByDayUseCase) : ViewModel() {

    // The current UserSessionMatcher, used to filter the events that are shown
    private var userSessionMatcher = TagFilterMatcher()

    // TODO: Remove it once the FirebaseUser is available when the app is launched
    val tempUser = "user1"

    val isLoading: LiveData<Boolean>

    val errorMessage: LiveData<String>
    private val errorMessageShown = MutableLiveData<Boolean>()

    private val loadSessionsResult: LiveData<Result<LoadUserSessionsByDayUseCaseResult>>

    // Each day is represented by a map of time slot labels to a list of sessions.
    private val day1Sessions: LiveData<Map<String, List<UserSession>>>
    private val day2Sessions: LiveData<Map<String, List<UserSession>>>
    private val day3Sessions: LiveData<Map<String, List<UserSession>>>

    init {
        // Load sessions and tags and store the result in `LiveData`s
        loadSessionsResult = loadSessionsByDayUseCase.observe()
        loadSessionsByDayUseCase.execute(parameters = userSessionMatcher to tempUser)

        // Map LiveData results from UseCase to each day's individual LiveData
        day1Sessions = groupSessionsByTimeSlot(loadSessionsResult, ConferenceDay.DAY_1)
        day2Sessions = groupSessionsByTimeSlot(loadSessionsResult, ConferenceDay.DAY_2)
        day3Sessions = groupSessionsByTimeSlot(loadSessionsResult, ConferenceDay.DAY_3)

        isLoading = loadSessionsResult.map { it == Result.Loading }

        errorMessage = loadSessionsResult.map { result ->
            errorMessageShown.value = false
            (result as? Result.Error)?.exception?.message ?: ""
        }
    }

    private fun groupSessionsByTimeSlot(
            result: MutableLiveData<Result<LoadUserSessionsByDayUseCaseResult>>,
            day: ConferenceDay
    ): LiveData<Map<String, List<UserSession>>> {
        return result.map {
            val sessions = (it as? Result.Success)?.data?.userSessionsPerDay?.get(day)
                    ?: emptyList()

            // Groups sessions by formatted header string.
            sessions.groupBy({ TimeUtils.timeString(it.session.startTime, it.session.endTime) })
        }
    }

    fun wasErrorMessageShown(): Boolean = errorMessageShown.value ?: false

    fun onErrorMessageShown() {
        errorMessageShown.value = true
    }

    fun getSessionsGroupedByTimeForDay(
            day: ConferenceDay
    ): LiveData<Map<String, List<UserSession>>> =
            when (day) {
                ConferenceDay.DAY_1 -> day1Sessions
                ConferenceDay.DAY_2 -> day2Sessions
                ConferenceDay.DAY_3 -> day3Sessions
            }
}

