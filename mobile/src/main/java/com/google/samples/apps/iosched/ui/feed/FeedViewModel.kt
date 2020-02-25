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
import androidx.lifecycle.asLiveData
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
import com.google.samples.apps.iosched.shared.domain.sessions.LoadFilteredUserSessionsParameters
import com.google.samples.apps.iosched.shared.domain.sessions.LoadFilteredUserSessionsResult
import com.google.samples.apps.iosched.shared.domain.sessions.LoadFilteredUserSessionsUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCaseLegacy
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Loading
import com.google.samples.apps.iosched.shared.result.successOr
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.shared.time.TimeProvider
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDays
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.shared.util.toEpochMilli
import com.google.samples.apps.iosched.ui.SectionHeader
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.sessioncommon.EventActions
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailFragmentDirections
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.google.samples.apps.iosched.ui.theme.ThemedActivityDelegate
import com.google.samples.apps.iosched.util.combine
import javax.inject.Inject
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

/**
 * Loads data and exposes it to the view.
 * By annotating the constructor with [@Inject], Dagger will use that constructor when needing to
 * create the object, so defining a [@Provides] method for this class won't be needed.
 */
class FeedViewModel @Inject constructor(
    private val loadCurrentMomentUseCase: LoadCurrentMomentUseCase,
    loadAnnouncementsUseCase: LoadAnnouncementsUseCase,
    private val loadFilteredUserSessionsUseCase: LoadFilteredUserSessionsUseCase,
    getTimeZoneUseCaseLegacy: GetTimeZoneUseCaseLegacy, // TODO(COROUTINES): Migrate to non-legacy
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

    val errorMessage: LiveData<Event<String>>

    val feed: LiveData<List<Any>>

    val snackBarMessage: LiveData<Event<SnackbarMessage>>

    private val loadSessionsResult = signInViewModelDelegate.currentUserInfo.switchMap {
        // TODO: side effects, check if we can remove them.
        val sessionMatcher = UserSessionMatcher()
        if (signInViewModelDelegate.isSignedIn()) {
            sessionMatcher.setShowPinnedEventsOnly(true)
        }
        loadFilteredUserSessionsUseCase(
            LoadFilteredUserSessionsParameters(
                sessionMatcher,
                signInViewModelDelegate.getUserId()
            )
        ).asLiveData()
    }
    private val conferenceStateLiveData = MutableLiveData<ConferenceState>()

    private val loadAnnouncementsResult = MutableLiveData<Result<List<Announcement>>>()

    private val currentMomentResult = MediatorLiveData<Result<Moment?>>()

    private val _navigateToSessionAction = MutableLiveData<Event<String>>()
    val navigateToSessionAction: LiveData<Event<String>>
        get() = _navigateToSessionAction

    private val _navigateToMapAction = MutableLiveData<Event<NavDirections>>()
    val navigateToMapAction: LiveData<Event<NavDirections>>
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
        viewModelScope.launch {
            getConferenceStateUseCase(Unit)
                .map { it.successOr(UPCOMING) }
                .collect { conferenceStateLiveData.value = it }
        }

        timeZoneId = preferConferenceTimeZoneResult.map {
            if (it.successOr(true)) TimeUtils.CONFERENCE_TIMEZONE else ZoneId.systemDefault()
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

        loadAnnouncementsUseCase(timeProvider.now(), loadAnnouncementsResult)
        val announcements: LiveData<List<Any>> = loadAnnouncementsResult.map {
            if (it is Loading) {
                listOf(LoadingIndicator)
            } else {
                val items = it.successOr(emptyList())
                if (items.isNotEmpty()) items else listOf(AnnouncementsEmpty)
            }
        }

        currentMomentResult.addSource(conferenceStateLiveData) {
            // This will change to the first moment when the Keynote starts
            loadCurrentMomentUseCase(timeProvider.now(), currentMomentResult)
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
            announcements
        ) { feedHeader, sessionContainer, announcementItems ->
            val feedItems = mutableListOf<Any>()
            if (feedHeader != NoHeader) {
                feedItems.add(feedHeader)
            }
            if (sessionContainer != NoSessionsContainer) {
                feedItems.add(sessionContainer)
            }
            feedItems.plus(SectionHeader(R.string.feed_announcement_title))
                .plus(announcementItems)
                .plus(FeedSocialChannelsSection)
        }

        errorMessage = loadAnnouncementsResult.map {
            Event(content = (it as? Result.Error)?.exception?.message ?: "")
        }

        // Show an error message if the feed could not be loaded.
        snackBarMessage = MediatorLiveData()
        snackBarMessage.addSource(loadAnnouncementsResult) {
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
        // TODO(COROUTINES): Migrate to GetTimeZoneUseCase
        getTimeZoneUseCaseLegacy(Unit, preferConferenceTimeZoneResult)
    }

    private fun createFeedSessionsContainer(
        sessionsResult: Result<LoadFilteredUserSessionsResult>,
        timeZoneId: ZoneId
    ): FeedSessions {
        val sessions = sessionsResult.successOr(null)?.userSessions ?: emptyList()
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
        _navigateToMapAction.value = Event(
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
            featureId = session?.room?.id,
            startTime = session?.startTime?.toEpochMilli() ?: 0L
        )
        _navigateToMapAction.value = Event(directions)
    }
}

interface FeedEventListener : EventActions {
    fun openSchedule(showOnlyPinnedSessions: Boolean)
    fun signIn()
    fun openMap(moment: Moment)
    fun openLiveStream(liveStreamUrl: String)
    fun openMapForSession(session: Session)
}
