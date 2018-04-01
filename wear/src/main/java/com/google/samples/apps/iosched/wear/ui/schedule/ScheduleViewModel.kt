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

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCaseResult
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.UserSession
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher.TagFilterMatcher
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_1
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_2
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_3
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
    // TODO: Swap out empty TagFilterMatcher() with PinnedEventMatcher once sign-in is enabled.
    private val userSessionMatcher: UserSessionMatcher = TagFilterMatcher()

    private val loadSessionsResult: MediatorLiveData<Result<LoadUserSessionsByDayUseCaseResult>>

    private val day1Sessions: LiveData<List<UserSession>>
    private val day2Sessions: LiveData<List<UserSession>>
    private val day3Sessions: LiveData<List<UserSession>>

    private val _errorMessage = MediatorLiveData<Event<String>>()
    val errorMessage : LiveData<Event<String>>
        get() = _errorMessage

    private val _navigateToSessionAction = MutableLiveData<Event<String>>()
    val navigateToSessionAction : LiveData<Event<String>>
        get() = _navigateToSessionAction

    init {

        // Load sessions and tags and store the result in `LiveData`s
        loadSessionsResult = loadUserSessionsByDayUseCase.observe()

        // map LiveData results from UseCase to each day's individual LiveData
        day1Sessions = loadSessionsResult.map {
            (it as? Result.Success)?.data?.userSessionsPerDay?.get(DAY_1) ?: emptyList()
        }
        day2Sessions = loadSessionsResult.map {
            (it as? Result.Success)?.data?.userSessionsPerDay?.get(DAY_2) ?: emptyList()
        }
        day3Sessions = loadSessionsResult.map {
            (it as? Result.Success)?.data?.userSessionsPerDay?.get(DAY_3) ?: emptyList()
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
    fun getSessionsForDay(day: ConferenceDay):
            LiveData<List<UserSession>> = when (day) {
        DAY_1 -> day1Sessions
        DAY_2 -> day2Sessions
        DAY_3 -> day3Sessions
    }

    override fun openSessionDetail(id: String) {
        _navigateToSessionAction.value = Event(id)
    }

    private fun refreshUserSessions() {
        Timber.d("ViewModel refreshing user sessions")
        loadUserSessionsByDayUseCase.execute(userSessionMatcher to ("tempUser"))
    }
}

interface ScheduleEventListener {
    /** Called from UI to start a navigation action to the detail screen. */
    fun openSessionDetail(id: String)
}
