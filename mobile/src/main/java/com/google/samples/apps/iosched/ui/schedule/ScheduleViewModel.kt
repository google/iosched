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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.ConferenceDay
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.domain.RefreshConferenceDataUseCaseLegacy
import com.google.samples.apps.iosched.shared.domain.prefs.ScheduleUiHintsShownUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.ConferenceDayIndexer
import com.google.samples.apps.iosched.shared.domain.sessions.LoadFilteredUserSessionsParameters
import com.google.samples.apps.iosched.shared.domain.sessions.LoadFilteredUserSessionsUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.ObserveConferenceDataUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCaseLegacy
import com.google.samples.apps.iosched.shared.domain.users.StarEventAndNotifyUseCase
import com.google.samples.apps.iosched.shared.domain.users.StarEventParameter
import com.google.samples.apps.iosched.shared.fcm.TopicSubscriber
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.shared.result.successOr
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.sessioncommon.EventActions
import com.google.samples.apps.iosched.ui.sessioncommon.stringRes
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import kotlinx.coroutines.launch
import org.threeten.bp.ZoneId
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * Loads data and exposes it to the view.
 * By annotating the constructor with [@Inject], Dagger will use that constructor when needing to
 * create the object, so defining a [@Provides] method for this class won't be needed.
 */
class ScheduleViewModel @Inject constructor(
    private val loadFilteredUserSessionsUseCase: LoadFilteredUserSessionsUseCase,
    signInViewModelDelegate: SignInViewModelDelegate,
    private val starEventUseCase: StarEventAndNotifyUseCase,
    scheduleUiHintsShownUseCase: ScheduleUiHintsShownUseCase,
    topicSubscriber: TopicSubscriber,
    private val snackbarMessageManager: SnackbarMessageManager,
    // TODO(COROUTINES): Migrate to non-legacy
    getTimeZoneUseCase: GetTimeZoneUseCaseLegacy,
    // TODO(COROUTINES): Migrate to non-legacy
    private val refreshConferenceDataUseCase: RefreshConferenceDataUseCaseLegacy,
    observeConferenceDataUseCase: ObserveConferenceDataUseCase,
    private val analyticsHelper: AnalyticsHelper
) : ViewModel(),
    EventActions,
    SignInViewModelDelegate by signInViewModelDelegate {

    val isLoading: LiveData<Boolean>

    val swipeRefreshing: LiveData<Boolean>

    // TODO(jdkoren): remove when use case is updated
    private val userSessionMatcher = UserSessionMatcher()

    private val preferConferenceTimeZoneResult = MutableLiveData<Result<Boolean>>()
    val isConferenceTimeZone: LiveData<Boolean>
    val timeZoneId: LiveData<ZoneId> = preferConferenceTimeZoneResult.map {
        val preferConferenceTimeZone = it.successOr(true)
        if (preferConferenceTimeZone) {
            TimeUtils.CONFERENCE_TIMEZONE
        } else {
            ZoneId.systemDefault()
        }
    }

    private lateinit var dayIndexer: ConferenceDayIndexer

    private val userSessionsNeedRefresh = MediatorLiveData<Unit>()

    // Refresh sessions when needed
    private val loadSessionsResult = userSessionsNeedRefresh.switchMap {
        loadFilteredUserSessionsUseCase(
            LoadFilteredUserSessionsParameters(userSessionMatcher, getUserId())
        ).asLiveData()
    }

    // Expose new UI data when loadSessionsResult changes
    val scheduleUiData = loadSessionsResult.switchMap { sessions ->
        liveData {
            val timeZoneIdValue = timeZoneId.value
            sessions.data?.let { data ->
                dayIndexer = data.dayIndexer
                emit(ScheduleUiData(
                    list = data.userSessions,
                    dayIndexer = data.dayIndexer,
                    timeZoneId = timeZoneIdValue
                ))
            }
        }
    }

    private val swipeRefreshResult = MutableLiveData<Result<Boolean>>()

    /** LiveData for Actions and Events **/

    private val _errorMessage = MediatorLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>>
        get() = _errorMessage

    private val _navigateToSessionAction = MutableLiveData<Event<String>>()
    val navigateToSessionAction: LiveData<Event<String>>
        get() = _navigateToSessionAction

    private val _snackBarMessage = MediatorLiveData<Event<SnackbarMessage>>()
    val snackBarMessage: LiveData<Event<SnackbarMessage>>
        get() = _snackBarMessage

    private val _navigateToSignInDialogAction = MutableLiveData<Event<Unit>>()
    val navigateToSignInDialogAction: LiveData<Event<Unit>>
        get() = _navigateToSignInDialogAction

    private val _navigateToSignOutDialogAction = MutableLiveData<Event<Unit>>()
    val navigateToSignOutDialogAction: LiveData<Event<Unit>>
        get() = _navigateToSignOutDialogAction

    private val scheduleUiHintsShownResult = MutableLiveData<Result<Boolean>>()
    /** Indicates if the UI hints for the schedule have been shown */
    val scheduleUiHintsShown: LiveData<Event<Boolean>>

    // Flags used to indicate if the "scroll to now" feature has been used already.
    var userHasInteracted = false

    // LiveData describing which item to scroll to automatically. We use an Event because on
    // rotation RecyclerView takes care of restoring its scroll position.
    private val _scrollToEvent = MediatorLiveData<Event<ScheduleScrollEvent>>()
    val scrollToEvent: LiveData<Event<ScheduleScrollEvent>>
        get() = _scrollToEvent

    init {
        val conferenceDataAvailable = observeConferenceDataUseCase.observe()

        // Load sessions when there's new conference data
        userSessionsNeedRefresh.addSource(conferenceDataAvailable) {
            Timber.d("Detected new data in conference data repository")
            refreshUserSessions()
        }
        userSessionsNeedRefresh.addSource(timeZoneId) {
            Timber.d("Timezone changed")
            refreshUserSessions()
        }
        // Refresh the list of user sessions if the user is updated.
        userSessionsNeedRefresh.addSource(currentUserInfo) {
            Timber.d("User change: Loading user session with user ${it?.getUid()}")
            refreshUserSessions()
        }

        isLoading = loadSessionsResult.map { it == Result.Loading }

        _errorMessage.addSource(loadSessionsResult) { result ->
            if (result is Result.Error) {
                _errorMessage.value = Event(content = result.exception.message ?: "Error")
            }
        }

        // Show a message with the result of a reservation
        _snackBarMessage.addSource(loadSessionsResult) {
            if (it is Success) {
                it.data.userMessage?.type?.stringRes()?.let { messageId ->
                    // There is a message to display:
                    snackbarMessageManager.addMessage(
                        SnackbarMessage(
                            messageId = messageId,
                            longDuration = true,
                            session = it.data.userMessageSession,
                            requestChangeId = it.data.userMessage?.changeRequestId
                        )
                    )
                }
            }
        }

        scheduleUiHintsShownUseCase(Unit, scheduleUiHintsShownResult)
        scheduleUiHintsShown = scheduleUiHintsShownResult.map {
            Event((it as? Success)?.data == true)
        }

        isConferenceTimeZone = timeZoneId.map { zoneId ->
            TimeUtils.isConferenceTimeZone(zoneId)
        }
        swipeRefreshing = swipeRefreshResult.map {
            // Whenever refresh finishes, stop the indicator, whatever the result
            false
        }

        // Load time zone preference
        getTimeZoneUseCase(Unit, preferConferenceTimeZoneResult)

        // Subscribe user to schedule updates
        topicSubscriber.subscribeToScheduleUpdates()

        // Observe updates in conference data
        observeConferenceDataUseCase.execute(Any())

        _scrollToEvent.addSource(loadSessionsResult) { result ->
            if (!userHasInteracted) {
                val index =
                    (result as? Success)?.data?.firstUnfinishedSessionIndex ?: return@addSource
                if (index != -1) {
                    _scrollToEvent.value = Event(ScheduleScrollEvent(index))
                }
            }
        }
    }

    // TODO(jdkoren) support showing all events or just the user's starred/reserved events
    fun showMySchedule() {}

    fun showAllEvents() {}

    override fun openEventDetail(id: SessionId) {
        _navigateToSessionAction.value = Event(id)
    }

    fun onSwipeRefresh() {
        refreshConferenceDataUseCase(Any(), swipeRefreshResult)
    }

    fun onSignInRequired() {
        _navigateToSignInDialogAction.value = Event(Unit)
    }

    private fun refreshUserSessions() {
        userSessionsNeedRefresh.value = Unit
    }

    override fun onStarClicked(userSession: UserSession) {
        if (!isSignedIn()) {
            Timber.d("Showing Sign-in dialog after star click")
            _navigateToSignInDialogAction.value = Event(Unit)
            return
        }
        val newIsStarredState = !userSession.userEvent.isStarred

        // Update the snackbar message optimistically.
        val stringResId = if (newIsStarredState) {
            R.string.event_starred
        } else {
            R.string.event_unstarred
        }

        if (newIsStarredState) {
            analyticsHelper.logUiEvent(userSession.session.title, AnalyticsActions.STARRED)
        }

        snackbarMessageManager.addMessage(
            SnackbarMessage(
                messageId = stringResId,
                actionId = R.string.dont_show,
                requestChangeId = UUID.randomUUID().toString()
            )
        )

        viewModelScope.launch {
            getUserId()?.let {
                val result = starEventUseCase(
                    StarEventParameter(
                        it, userSession.copy(
                            userEvent = userSession.userEvent.copy(isStarred = newIsStarredState)
                        )
                    )
                )
                // Show an error message if a star request fails
                if (result is Result.Error) {
                    _snackBarMessage.value = Event(SnackbarMessage(R.string.event_star_error))
                }
            }
        }
    }

    fun scrollToStartOfDay(day: ConferenceDay) {
        val index = dayIndexer.positionForDay(day)
        // We don't check userHasInteracted because the user explicitly requested this scroll.
        _scrollToEvent.value = Event(ScheduleScrollEvent(index, true))
    }
}

data class ScheduleUiData(
    val list: List<UserSession>? = null,
    val timeZoneId: ZoneId? = null,
    val dayIndexer: ConferenceDayIndexer? = null
)

data class ScheduleScrollEvent(val targetPosition: Int, val smoothScroll: Boolean = false)
