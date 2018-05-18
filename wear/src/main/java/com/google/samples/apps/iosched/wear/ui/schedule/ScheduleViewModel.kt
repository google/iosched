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

package com.google.samples.apps.iosched.wear.ui.schedule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCaseParameters
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCaseResult
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDays
import com.google.samples.apps.iosched.shared.util.map
import timber.log.Timber
import javax.inject.Inject

/**
 * Loads sessions data and exposes each day's sessions to the view.
 */
class ScheduleViewModel @Inject constructor(
    private val loadUserSessionsByDayUseCase: LoadUserSessionsByDayUseCase
) : ViewModel(), ScheduleEventListener {

    val isLoading: LiveData<Boolean>

    // Filters by pinned events, since users should only see their pinned events on Wear.
    // TODO: call userSessionMatcher.setShowPinnedEventsOnly(true) once sign-in is enabled.
    private val userSessionMatcher = UserSessionMatcher()

    private val loadSessionsResult: MediatorLiveData<Result<LoadUserSessionsByDayUseCaseResult>>

    private val day1Sessions: LiveData<List<UserSession>>
    private val day2Sessions: LiveData<List<UserSession>>
    private val day3Sessions: LiveData<List<UserSession>>

    private val _errorMessage = MediatorLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>>
        get() = _errorMessage

    private val _navigateToSessionAction = MutableLiveData<Event<String>>()
    val navigateToSessionAction: LiveData<Event<String>>
        get() = _navigateToSessionAction

    init {

        // Load sessions and tags and store the result in `LiveData`s
        loadSessionsResult = loadUserSessionsByDayUseCase.observe()

        // map LiveData results from UseCase to each day's individual LiveData
        day1Sessions = loadSessionsResult.map {
            (it as? Result.Success)?.data?.userSessionsPerDay?.get(ConferenceDays[0]) ?: emptyList()
        }
        day2Sessions = loadSessionsResult.map {
            (it as? Result.Success)?.data?.userSessionsPerDay?.get(ConferenceDays[1]) ?: emptyList()
        }
        day3Sessions = loadSessionsResult.map {
            (it as? Result.Success)?.data?.userSessionsPerDay?.get(ConferenceDays[2]) ?: emptyList()
        }

        isLoading = loadSessionsResult.map { it == Result.Loading }

        _errorMessage.addSource(loadSessionsResult, { result ->
            if (result is Result.Error) {
                _errorMessage.value = Event(content = result.exception.message ?: "Error")
            }
        })
        refreshUserSessions()
    }

    /**
     * Called from each schedule day fragment to load data.
     */
    fun getSessionsForDay(day: Int):
        LiveData<List<UserSession>> = when (day) {
        0 -> day1Sessions
        1 -> day2Sessions
        2 -> day3Sessions
        else -> {
            val exception = Exception("Invalid day: $day")
            Timber.e(exception)
            throw exception
        }
    }

    override fun openSessionDetail(id: SessionId) {
        _navigateToSessionAction.value = Event(id)
    }

    private fun refreshUserSessions() {
        Timber.d("ViewModel refreshing user sessions")
        loadUserSessionsByDayUseCase.execute(
            LoadUserSessionsByDayUseCaseParameters(userSessionMatcher, ("tempUser"))
        )
    }
}

interface ScheduleEventListener {
    /** Called from UI to start a navigation action to the detail screen. */
    fun openSessionDetail(id: SessionId)
}
