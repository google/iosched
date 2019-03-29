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

package com.google.samples.apps.iosched.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.R.string
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.domain.feed.LoadAnnouncementsUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadFilteredUserSessionsParameters
import com.google.samples.apps.iosched.shared.domain.sessions.LoadFilteredUserSessionsResult
import com.google.samples.apps.iosched.shared.domain.sessions.LoadFilteredUserSessionsUseCase
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.ui.SectionHeader
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.sessioncommon.EventActions
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.google.samples.apps.iosched.util.combine
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject

/**
 * Loads data and exposes it to the view.
 * By annotating the constructor with [@Inject], Dagger will use that constructor when needing to
 * create the object, so defining a [@Provides] method for this class won't be needed.
 */
class FeedViewModel @Inject constructor(
    private val loadAnnouncementsUseCase: LoadAnnouncementsUseCase,
    private val loadFilteredUserSessionsUseCase: LoadFilteredUserSessionsUseCase,
    private val signInViewModelDelegate: SignInViewModelDelegate
) : ViewModel(), FeedEventListener {
    companion object {
        // Show at max 10 sessions in the horizontal sessions list as user can click on
        // View All sessions and go to schedule to view the full list
        const val MAX_SESSIONS = 10
    }

    val errorMessage: LiveData<Event<String>>

    val feed: LiveData<List<Any>>

    val isLoading: LiveData<Boolean>

    val snackBarMessage: LiveData<Event<SnackbarMessage>>

    private val loadSessionsResult: MediatorLiveData<Result<LoadFilteredUserSessionsResult>>

    private val loadFeedResult = loadAnnouncementsUseCase.observe()

    private val _navigateToSessionAction = MutableLiveData<Event<String>>()
    val navigateToSessionAction: LiveData<Event<String>>
        get() = _navigateToSessionAction

    private val _navigateToScheduleAction = MutableLiveData<Event<Boolean>>()
    val navigateToScheduleAction: LiveData<Event<Boolean>>
        get() = _navigateToScheduleAction

    init {
        loadSessionsResult = loadFilteredUserSessionsUseCase.observe()
        loadSessionsResult.addSource(signInViewModelDelegate.currentUserInfo) {
            refreshSessions()
        }
        val sessionContainerLiveData = signInViewModelDelegate.currentUserInfo.combine(
            loadSessionsResult
        ) { userInfo, sessions -> createFeedSessionsContainer(userInfo, sessions) }

        val announcements: LiveData<List<Any>> = loadFeedResult.map {
            (it as? Result.Success)?.data ?: emptyList()
        }

        // Generate feed
        feed = sessionContainerLiveData.combine(announcements) { sessionContainer, announcements ->
            arrayListOf(
                CountdownTimer(),
                sessionContainer,
                SectionHeader(string.feed_announcement_title)
            ).plus(announcements)
        }

        isLoading = loadFeedResult.map { it == Result.Loading }

        errorMessage = loadFeedResult.map {
            Event(content = (it as? Result.Error)?.exception?.message ?: "")
        }

        // Show an error message if the feed could not be loaded.
        snackBarMessage = MediatorLiveData()
        snackBarMessage.addSource(loadFeedResult) {
            if (it is Result.Error) {
                snackBarMessage.value =
                    Event(
                        SnackbarMessage(
                            messageId = R.string.feed_loading_error,
                            longDuration = true
                        )
                    )
            }
        }

        loadAnnouncementsUseCase.execute(Unit)
    }

    private fun createFeedSessionsContainer(
        userInfo: AuthenticatedUserInfo?,
        sessions: Result<LoadFilteredUserSessionsResult>
    ): FeedSessions =
        FeedSessions(
            username = userInfo?.getDisplayName()?.split(" ")?.get(0),
            titleId =
            if (userInfo?.isSignedIn() == true) {
                if (sessions as? Result.Success != null && sessions.data.userSessionCount == 0) {
                    string.feed_no_saved_events
                } else if (userInfo.isRegistered()) {
                    string.feed_upcoming_events
                } else {
                    string.feed_saved_events
                }
            } else
                string.title_schedule,
            actionTextId =
            when (userInfo?.isSignedIn() == true) {
                true -> {
                    if (sessions as? Result.Success != null && sessions.data.userSessionCount == 0)
                        string.feed_view_all_events
                    else
                        string.feed_view_your_schedule
                }
                false -> string.feed_view_all_events
            },
            userSessions = ((sessions as? Result.Success)?.data?.userSessions ?: emptyList())
                .filter { it.session.endTime >= ZonedDateTime.now() }
                .let { it.subList(0, Math.min(MAX_SESSIONS, it.size)) }

        )

    private fun refreshSessions() {
        val sessionMatcher = UserSessionMatcher()
        if (signInViewModelDelegate.isSignedIn()) {
            sessionMatcher.setShowPinnedEventsOnly(true)
        }
        loadFilteredUserSessionsUseCase.execute(
            LoadFilteredUserSessionsParameters(
                sessionMatcher,
                signInViewModelDelegate.getUserId()
            )
        )
    }

    override fun onCleared() {
        // Clear subscriptions that might be leaked or that will not be used in the future.
        loadAnnouncementsUseCase.onCleared()
    }

    override fun openEventDetail(id: SessionId) {
        _navigateToSessionAction.value = Event(id)
    }

    override fun openSchedule(showOnlyPinnedSessions: Boolean) {
        _navigateToScheduleAction.value = Event(showOnlyPinnedSessions)
    }

    override fun onStarClicked(userSession: UserSession) {
        TODO("not implemented")
    }
}

interface FeedEventListener : EventActions {
    fun openSchedule(showOnlyPinnedSessions: Boolean)
}
