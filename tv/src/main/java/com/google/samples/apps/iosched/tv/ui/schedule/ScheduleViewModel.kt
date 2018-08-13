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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.samples.apps.iosched.model.ConferenceDay
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCaseParameters
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCaseResult
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDays
import com.google.samples.apps.iosched.shared.util.map

/**
 * Loads sessions data and exposes each day's sessions to the view.
 */
class ScheduleViewModel(loadSessionsByDayUseCase: LoadUserSessionsByDayUseCase) : ViewModel() {

    // The current UserSessionMatcher, used to filter the events that are shown
    private var userSessionMatcher = UserSessionMatcher()

    // TODO: Remove it once the FirebaseUser is available when the app is launched
    val tempUser = null

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
        loadSessionsByDayUseCase.execute(
            LoadUserSessionsByDayUseCaseParameters(userSessionMatcher, tempUser)
        )

        // Map LiveData results from UseCase to each day's individual LiveData
        day1Sessions = groupSessionsByTimeSlot(loadSessionsResult, ConferenceDays[0])
        day2Sessions = groupSessionsByTimeSlot(loadSessionsResult, ConferenceDays[1])
        day3Sessions = groupSessionsByTimeSlot(loadSessionsResult, ConferenceDays[2])

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
            // TODO: grab time zone from preferences
            val finalTimeZoneId = TimeUtils.CONFERENCE_TIMEZONE
            sessions.groupBy({
                val localStartTime = TimeUtils.zonedTime(it.session.startTime, finalTimeZoneId)
                val localEndTime = TimeUtils.zonedTime(it.session.endTime, finalTimeZoneId)

                TimeUtils.timeString(localStartTime, localEndTime)
            })
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
            ConferenceDays[0] -> day1Sessions
            ConferenceDays[1] -> day2Sessions
            ConferenceDays[2] -> day3Sessions
            else -> throw Exception("Day not found")
        }
}
