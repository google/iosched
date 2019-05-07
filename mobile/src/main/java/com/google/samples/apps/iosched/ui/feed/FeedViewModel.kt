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
import com.google.samples.apps.iosched.model.Announcement
import com.google.samples.apps.iosched.model.Moment
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.domain.feed.LoadAnnouncementsUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadFilteredUserSessionsParameters
import com.google.samples.apps.iosched.shared.domain.sessions.LoadFilteredUserSessionsResult
import com.google.samples.apps.iosched.shared.domain.sessions.LoadFilteredUserSessionsUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.successOr
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.ui.SectionHeader
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.sessioncommon.EventActions
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.google.samples.apps.iosched.ui.theme.ThemedActivityDelegate
import com.google.samples.apps.iosched.util.combine
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject

/**
 * Loads data and exposes it to the view.
 * By annotating the constructor with [@Inject], Dagger will use that constructor when needing to
 * create the object, so defining a [@Provides] method for this class won't be needed.
 */
class FeedViewModel @Inject constructor(
    loadAnnouncementsUseCase: LoadAnnouncementsUseCase,
    private val loadFilteredUserSessionsUseCase: LoadFilteredUserSessionsUseCase,
    private val signInViewModelDelegate: SignInViewModelDelegate,
    private val analyticsHelper: AnalyticsHelper,
    getTimeZoneUseCase: GetTimeZoneUseCase,
    themedActivityDelegate: ThemedActivityDelegate,
    feedHeaderLiveData: FeedHeaderLiveData
) : ViewModel(), FeedEventListener, ThemedActivityDelegate by themedActivityDelegate {

    companion object {
        // Show at max 10 sessions in the horizontal sessions list as user can click on
        // View All sessions and go to schedule to view the full list
        private const val MAX_SESSIONS = 10
    }

    val errorMessage: LiveData<Event<String>>

    val feed: LiveData<List<Any>>

    val snackBarMessage: LiveData<Event<SnackbarMessage>>

    private val loadSessionsResult: MediatorLiveData<Result<LoadFilteredUserSessionsResult>>

    private val loadFeedResult = MutableLiveData<Result<List<Announcement>>>()

    private val _navigateToSessionAction = MutableLiveData<Event<String>>()
    val navigateToSessionAction: LiveData<Event<String>>
        get() = _navigateToSessionAction

    private val _navigateToMapAction = MutableLiveData<Event<Moment>>()
    val navigateToMapAction: LiveData<Event<Moment>>
        get() = _navigateToMapAction

    private val _openSignInDialogAction = MutableLiveData<Event<Unit>>()
    val openSignInDialogAction: LiveData<Event<Unit>>
        get() = _openSignInDialogAction

    private val _openLiveStreamAction = MutableLiveData<Event<String>>()
    val openLiveStreamAction: LiveData<Event<String>>
        get() = _openLiveStreamAction

    private val _navigateToScheduleAction = MutableLiveData<Event<Boolean>>()
    val navigateToScheduleAction: LiveData<Event<Boolean>>
        get() = _navigateToScheduleAction

    private val preferConferenceTimeZoneResult = MutableLiveData<Result<Boolean>>()
    val timeZoneId: LiveData<ZoneId>

    init {
        timeZoneId = preferConferenceTimeZoneResult.map {
            val preferConferenceTimeZone = it.successOr(true)
            if (preferConferenceTimeZone) {
                TimeUtils.CONFERENCE_TIMEZONE
            } else {
                ZoneId.systemDefault()
            }
        }

        loadSessionsResult = loadFilteredUserSessionsUseCase.observe()
        loadSessionsResult.addSource(signInViewModelDelegate.currentUserInfo) {
            refreshSessions()
        }

        val sessionContainerLiveData = signInViewModelDelegate.currentUserInfo.combine(
            loadSessionsResult
        ) { userInfo, sessions -> createFeedSessionsContainer(userInfo, sessions) }
            .combine(timeZoneId) { sessionContainer, timeZoneId ->
                sessionContainer.copy(timeZoneId = timeZoneId)
            }

        loadAnnouncementsUseCase(Unit, loadFeedResult)
        val announcementsLiveData: LiveData<List<Any>> = loadFeedResult.map {
            val announcementsPlaceholder = createAnnouncementsPlaceholder(announcementsResult = it)
            val announcementList = (it as? Result.Success)?.data ?: emptyList()
            if (announcementsPlaceholder.isLoading || announcementsPlaceholder.notAvailable)
                listOf(announcementsPlaceholder)
            else
                announcementList
        }

        val feedHeaderWithTimezoneAndTheme =
            feedHeaderLiveData.combine(timeZoneId, theme) { feedHeader, timeZoneId, theme ->
                feedHeader.copy(timeZoneId = timeZoneId, theme = theme)
            }

        // Generate feed
        feed = sessionContainerLiveData
            .combine(announcementsLiveData) { sessionContainer, announcements ->
                arrayListOf(sessionContainer, SectionHeader(string.feed_announcement_title))
                    .plus(announcements)
            }.combine(feedHeaderWithTimezoneAndTheme) { otherItems, feedHeader ->
                arrayListOf(
                    removeMomentIfNotRegistered(injectUserInfo(feedHeader))
                ).plus(otherItems)
            }

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

        getTimeZoneUseCase(Unit, preferConferenceTimeZoneResult)
    }

    private fun injectUserInfo(feedHeader: FeedHeader): FeedHeader {
        val userInfo = signInViewModelDelegate.currentUserInfo.value ?: return feedHeader.copy(
            userSignedIn = false, userRegistered = false
        )
        return feedHeader.copy(
            userSignedIn = userInfo.isSignedIn(), userRegistered = userInfo.isRegistered()
        )
    }

    private fun removeMomentIfNotRegistered(feedHeader: FeedHeader): FeedHeader =
        feedHeader.let {
            return@removeMomentIfNotRegistered it.copy(
                moment = if (!it.userRegistered &&
                    (it.moment?.attendeeRequired == true)
                ) null
                else it.moment
            )
        }

    private fun createAnnouncementsPlaceholder(
        announcementsResult: Result<List<Any>>?
    ): AnnouncementsPlaceholder {
        return AnnouncementsPlaceholder(
            isLoading = announcementsResult is Result.Loading,
            notAvailable = announcementsResult !is Result.Loading &&
                ((announcementsResult as? Result.Success)?.data?.isEmpty() ?: false ||
                    announcementsResult is Result.Error)
        )
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
                .let { it.subList(0, Math.min(MAX_SESSIONS, it.size)) },
            isLoading = sessions is Result.Loading
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

    override fun openEventDetail(id: SessionId) {
        analyticsHelper.logUiEvent("Home to event detail",
            AnalyticsActions.HOME_TO_SESSION_DETAIL)
        _navigateToSessionAction.value = Event(id)
    }

    override fun openSchedule(showOnlyPinnedSessions: Boolean) {
        analyticsHelper.logUiEvent("Home to Schedule", AnalyticsActions.HOME_TO_SCHEDULE)
        _navigateToScheduleAction.value = Event(showOnlyPinnedSessions)
    }

    override fun onStarClicked(userSession: UserSession) {
        TODO("not implemented")
    }

    override fun signIn() {
        analyticsHelper.logUiEvent("Home to Sign In", AnalyticsActions.HOME_TO_SIGN_IN)
        _openSignInDialogAction.value = Event(Unit)
    }

    override fun openMap(moment: Moment) {
        analyticsHelper.logUiEvent(moment.title.toString(), AnalyticsActions.HOME_TO_MAP)
        _navigateToMapAction.value = Event(moment)
    }

    override fun openLiveStream(liveStreamUrl: String) {
        analyticsHelper.logUiEvent(liveStreamUrl, AnalyticsActions.HOME_TO_LIVESTREAM)
        _openLiveStreamAction.value = Event(liveStreamUrl)
    }
}

interface FeedEventListener : EventActions {
    fun openSchedule(showOnlyPinnedSessions: Boolean)
    fun signIn()
    fun openMap(moment: Moment)
    fun openLiveStream(liveStreamUrl: String)
}
