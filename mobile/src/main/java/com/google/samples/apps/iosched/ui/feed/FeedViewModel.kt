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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavDirections
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.Announcement
import com.google.samples.apps.iosched.model.Moment
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.domain.feed.ConferenceState
import com.google.samples.apps.iosched.shared.domain.feed.ConferenceState.ENDED
import com.google.samples.apps.iosched.shared.domain.feed.ConferenceState.UPCOMING
import com.google.samples.apps.iosched.shared.domain.feed.GetConferenceStateUseCase
import com.google.samples.apps.iosched.shared.domain.feed.LoadAnnouncementsUseCase
import com.google.samples.apps.iosched.shared.domain.feed.LoadCurrentMomentUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadStarredAndReservedSessionsUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Loading
import com.google.samples.apps.iosched.shared.result.successOr
import com.google.samples.apps.iosched.shared.time.TimeProvider
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDays
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.shared.util.toEpochMilli
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.sessioncommon.EventActions
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailFragmentDirections
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.google.samples.apps.iosched.ui.theme.ThemedActivityDelegate
import com.google.samples.apps.iosched.util.combine
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject

/**
 * Loads data and exposes it to the view.
 * By annotating the constructor with [@Inject], Dagger will use that constructor when needing to
 * create the object, so defining a [@Provides] method for this class won't be needed.
 */
class FeedViewModel @Inject constructor(
    private val loadCurrentMomentUseCase: LoadCurrentMomentUseCase,
    loadAnnouncementsUseCase: LoadAnnouncementsUseCase,
    private val loadStarredAndReservedSessionsUseCase: LoadStarredAndReservedSessionsUseCase,
    getTimeZoneUseCase: GetTimeZoneUseCase,
    getConferenceStateUseCase: GetConferenceStateUseCase,
    private val timeProvider: TimeProvider,
    private val analyticsHelper: AnalyticsHelper,
    private val signInViewModelDelegate: SignInViewModelDelegate,
    themedActivityDelegate: ThemedActivityDelegate
) : ViewModel(),
    FeedEventListener,
    ThemedActivityDelegate by themedActivityDelegate,
    SignInViewModelDelegate by signInViewModelDelegate {

    companion object {
        // Show at max 10 sessions in the horizontal sessions list as user can click on
        // View All sessions and go to schedule to view the full list
        private const val MAX_SESSIONS = 10

        // Indicates there is no header to show at the current time. We need this sentinel value
        // because our LiveData.combine extension functions interpret null values to mean the
        // LiveData has not returned a result, but null for the current Moment is valid.
        private object NoHeader

        // Indicates there is no sessions related display on the home screen as the conference is
        // over.
        private object NoSessionsContainer
    }

    val feed: LiveData<List<Any>>

    val timeZoneId: LiveData<ZoneId> = liveData {
        val result = getTimeZoneUseCase(Unit)
        emit(if (result.successOr(true)) TimeUtils.CONFERENCE_TIMEZONE else ZoneId.systemDefault())
    }

    private val loadSessionsResult = signInViewModelDelegate.currentUserInfo.switchMap {
        // TODO(jdkoren): might need to show sessions for not signed in users too...
        loadStarredAndReservedSessionsUseCase(signInViewModelDelegate.getUserId()).asLiveData()
    }
    private val conferenceStateLiveData = MutableLiveData<ConferenceState>()

    private val _navigateToSessionAction = MutableLiveData<Event<String>>()
    val navigateToSessionAction: LiveData<Event<String>>
        get() = _navigateToSessionAction

    private val _navigateAction = MutableLiveData<Event<NavDirections>>()
    val navigateAction: LiveData<Event<NavDirections>>
        get() = _navigateAction

    private val _openSignInDialogAction = MutableLiveData<Event<Unit>>()
    val openSignInDialogAction: LiveData<Event<Unit>>
        get() = _openSignInDialogAction

    private val _openLiveStreamAction = MutableLiveData<Event<String>>()
    val openLiveStreamAction: LiveData<Event<String>>
        get() = _openLiveStreamAction

    private val _navigateToScheduleAction = MutableLiveData<Event<Boolean>>()
    val navigateToScheduleAction: LiveData<Event<Boolean>>
        get() = _navigateToScheduleAction

    private val currentMomentResult: LiveData<Result<Moment?>> = conferenceStateLiveData.switchMap {
        liveData {
            emit(loadCurrentMomentUseCase(timeProvider.now()))
        }
    }

    private val loadAnnouncementsResult: LiveData<Result<List<Announcement>>> = liveData {
        emit(loadAnnouncementsUseCase(timeProvider.now()))
    }

    private val announcementsPreviewLiveData: LiveData<List<Any>> = loadAnnouncementsResult.map {
        val announcementsHeader = AnnouncementsHeader(
            showPastNotificationsButton = it.successOr(emptyList()).size > 1
        )
        if (it is Loading) {
            listOf(announcementsHeader, LoadingIndicator)
        } else {
            listOf(
                announcementsHeader,
                it.successOr(emptyList()).firstOrNull() ?: AnnouncementsEmpty
            )
        }
    }

    val errorMessage: LiveData<Event<String>> = loadAnnouncementsResult.map {
        Event(content = (it as? Result.Error)?.exception?.message ?: "")
    }

    val snackBarMessage: LiveData<Event<SnackbarMessage>> = loadAnnouncementsResult.switchMap {
        liveData {
            // Show an error message if the feed could not be loaded.
            if (it is Result.Error) {
                emit(
                    Event(
                        SnackbarMessage(
                            messageId = R.string.feed_loading_error,
                            longDuration = true
                        )
                    )
                )
            }
        }
    }

    init {
        viewModelScope.launch {
            getConferenceStateUseCase(Unit)
                .map { it.successOr(UPCOMING) }
                .collect { conferenceStateLiveData.value = it }
        }

        val sessionContainerLiveData =
            loadSessionsResult
                .combine(
                    timeZoneId
                ) { sessions, timeZone ->
                    createFeedSessionsContainer(sessions, timeZone)
                }
                // Further combine with conferenceState and userInfo and decide if the
                // feedSessionsContainer should be shown or not.
                .combine(
                    conferenceStateLiveData, signInViewModelDelegate.currentUserInfo
                ) { sessionsContainer, conferenceState, userInfo ->
                    val isSignedIn = userInfo?.isSignedIn() ?: false
                    val isRegistered = userInfo?.isRegistered() ?: false
                    if (conferenceState != ENDED && isSignedIn && isRegistered)
                        sessionsContainer
                    else
                        NoSessionsContainer
                }

        val currentFeedHeader = conferenceStateLiveData.combine(
            currentMomentResult
        ) { conferenceStarted, momentResult ->
            if (conferenceStarted == UPCOMING) {
                CountdownItem
            } else {
                // Use case can return null even on success, so replace nulls with a sentinel
                momentResult.successOr(null) ?: NoHeader
            }
        }

        // Compose feed
        feed = currentFeedHeader.combine(
            sessionContainerLiveData,
            announcementsPreviewLiveData
        ) { feedHeader, sessionContainer, announcementsPreview ->
            val feedItems = mutableListOf<Any>()
            if (feedHeader != NoHeader) {
                feedItems.add(feedHeader)
            }
            if (sessionContainer != NoSessionsContainer) {
                feedItems.add(sessionContainer)
            }
            feedItems
                .plus(announcementsPreview)
                .plus(FeedSustainabilitySection)
                .plus(FeedSocialChannelsSection)
        }
    }

    private fun createFeedSessionsContainer(
        sessionsResult: Result<List<UserSession>>,
        timeZoneId: ZoneId
    ): FeedSessions {
        val sessions = sessionsResult.successOr(emptyList())
        val now = ZonedDateTime.ofInstant(timeProvider.now(), timeZoneId)

        // TODO: Making conferenceState a sealed class and moving currentDay in STARTED state might be a better option
        val currentDayEndTime = TimeUtils.getCurrentConferenceDay()?.end
        // Treat start of the conference as endTime as sessions shouldn't be shown if the
        // currentConferenceDay is null
            ?: ConferenceDays.first().start

        val upcomingReservedSessions = sessions
            .filter {
                it.userEvent.isReserved() &&
                    it.session.endTime.isAfter(now) &&
                    it.session.endTime.isBefore(currentDayEndTime)
            }
            .take(MAX_SESSIONS)

        val titleId = R.string.feed_sessions_title
        val actionId = R.string.feed_view_full_schedule

        return FeedSessions(
            titleId = titleId,
            actionTextId = actionId,
            userSessions = upcomingReservedSessions,
            timeZoneId = timeZoneId,
            isLoading = sessionsResult is Loading
        )
    }

    override fun openEventDetail(id: SessionId) {
        analyticsHelper.logUiEvent(
            "Home to event detail",
            AnalyticsActions.HOME_TO_SESSION_DETAIL
        )
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
        _navigateAction.value = Event(
            SessionDetailFragmentDirections.toMap(
                featureId = moment.featureId,
                startTime = moment.startTime.toEpochMilli()
            )
        )
    }

    override fun openLiveStream(liveStreamUrl: String) {
        analyticsHelper.logUiEvent(liveStreamUrl, AnalyticsActions.HOME_TO_LIVESTREAM)
        _openLiveStreamAction.value = Event(liveStreamUrl)
    }

    override fun openMapForSession(session: Session) {
        analyticsHelper.logUiEvent(session.id, AnalyticsActions.HOME_TO_MAP)
        val directions = SessionDetailFragmentDirections.toMap(
            featureId = session.room?.id,
            startTime = session.startTime.toEpochMilli()
        )
        _navigateAction.value = Event(directions)
    }

    override fun openPastAnnouncements() {
        analyticsHelper.logUiEvent("", AnalyticsActions.HOME_TO_ANNOUNCEMENTS)
        _navigateAction.value = Event(
            FeedFragmentDirections.toAnnouncementsFragment()
        )
    }
}

interface FeedEventListener : EventActions {
    fun openSchedule(showOnlyPinnedSessions: Boolean)
    fun signIn()
    fun openMap(moment: Moment)
    fun openLiveStream(liveStreamUrl: String)
    fun openMapForSession(session: Session)
    fun openPastAnnouncements()
}
