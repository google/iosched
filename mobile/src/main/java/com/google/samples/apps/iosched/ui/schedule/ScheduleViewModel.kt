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
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.ConferenceDay
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.domain.RefreshConferenceDataUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.LoadSelectedFiltersUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.SaveSelectedFiltersUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.ScheduleUiHintsShownUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.ConferenceDayIndexer
import com.google.samples.apps.iosched.shared.domain.sessions.LoadFilteredUserSessionsUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadFilteredUserSessionsParameters
import com.google.samples.apps.iosched.shared.domain.sessions.ObserveConferenceDataUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadFilteredUserSessionsResult
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.domain.users.StarEventAndNotifyUseCase
import com.google.samples.apps.iosched.shared.domain.users.StarEventParameter
import com.google.samples.apps.iosched.shared.fcm.TopicSubscriber
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.shared.result.successOr
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.schedule.filters.EventFilter
import com.google.samples.apps.iosched.ui.schedule.filters.EventFilter.MyEventsFilter
import com.google.samples.apps.iosched.ui.schedule.filters.EventFilter.TagFilter
import com.google.samples.apps.iosched.ui.schedule.filters.LoadEventFiltersUseCase
import com.google.samples.apps.iosched.ui.sessioncommon.EventActions
import com.google.samples.apps.iosched.ui.sessioncommon.stringRes
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
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
    loadEventFiltersUseCase: LoadEventFiltersUseCase,
    signInViewModelDelegate: SignInViewModelDelegate,
    private val starEventUseCase: StarEventAndNotifyUseCase,
    scheduleUiHintsShownUseCase: ScheduleUiHintsShownUseCase,
    topicSubscriber: TopicSubscriber,
    private val snackbarMessageManager: SnackbarMessageManager,
    getTimeZoneUseCase: GetTimeZoneUseCase,
    private val refreshConferenceDataUseCase: RefreshConferenceDataUseCase,
    observeConferenceDataUseCase: ObserveConferenceDataUseCase,
    loadSelectedFiltersUseCase: LoadSelectedFiltersUseCase,
    private val saveSelectedFiltersUseCase: SaveSelectedFiltersUseCase,
    private val analyticsHelper: AnalyticsHelper
) : ViewModel(),
    ScheduleEventListener,
    SignInViewModelDelegate by signInViewModelDelegate {

    val isLoading: LiveData<Boolean>

    val swipeRefreshing: LiveData<Boolean>

    // The current UserSessionMatcher, used to filter the events that are shown
    private val userSessionMatcher = UserSessionMatcher()
    private val loadSelectedFiltersResult = MutableLiveData<Result<Unit>>()

    private val preferConferenceTimeZoneResult = MutableLiveData<Result<Boolean>>()
    val isConferenceTimeZone: LiveData<Boolean>
    val timeZoneId: LiveData<ZoneId>

    private lateinit var dayIndexer: ConferenceDayIndexer

    private val _scheduleUiData = MediatorLiveData<ScheduleUiData>()
    val scheduleUiData: LiveData<ScheduleUiData>
        get() = _scheduleUiData

    // Cached list of TagFilters returned by the use case. Only Result.Success modifies it.
    private var cachedEventFilters = emptyList<EventFilter>()

    val eventFilters: LiveData<List<EventFilter>>
    private val _selectedFilters = MutableLiveData<List<EventFilter>>()
    val selectedFilters: LiveData<List<EventFilter>>
        get() = _selectedFilters
    private val _hasAnyFilters = MutableLiveData<Boolean>().apply { value = false }
    val hasAnyFilters: LiveData<Boolean>
        get() = _hasAnyFilters

    private val loadSessionsResult: MediatorLiveData<Result<LoadFilteredUserSessionsResult>> =
        loadFilteredUserSessionsUseCase.observe()
    private val loadEventFiltersResult = MediatorLiveData<Result<List<EventFilter>>>()
    private val swipeRefreshResult = MutableLiveData<Result<Boolean>>()

    val eventCount: LiveData<Int>

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

    val showReservations: LiveData<Boolean>

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

        // Load EventFilters when persisted filters are loaded and when there's new conference data
        loadEventFiltersResult.addSource(loadSelectedFiltersResult) {
            loadEventFiltersUseCase(userSessionMatcher, loadEventFiltersResult)
        }
        loadEventFiltersResult.addSource(conferenceDataAvailable) {
            loadEventFiltersUseCase(userSessionMatcher, loadEventFiltersResult)
        }

        // Load persisted filters to the matcher
        loadSelectedFiltersUseCase(userSessionMatcher, loadSelectedFiltersResult)

        // Load sessions when persisted filters are loaded and when there's new conference data
        loadSessionsResult.addSource(conferenceDataAvailable) {
            Timber.d("Detected new data in conference data repository")
            refreshUserSessions()
        }
        loadSessionsResult.addSource(loadEventFiltersResult) {
            Timber.d("Loaded filters from persistent storage")
            refreshUserSessions()
        }

        eventCount = loadSessionsResult.map {
            (it as? Result.Success)?.data?.userSessionCount ?: 0
        }

        isLoading = loadSessionsResult.map { it == Result.Loading }

        _errorMessage.addSource(loadSessionsResult) { result ->
            if (result is Result.Error) {
                _errorMessage.value = Event(content = result.exception.message ?: "Error")
            }
        }
        _errorMessage.addSource(loadEventFiltersResult) { result ->
            if (result is Result.Error) {
                _errorMessage.value = Event(content = result.exception.message ?: "Error")
            }
        }

        eventFilters = loadEventFiltersResult.map {
            if (it is Success) {
                cachedEventFilters = it.data
                updateFilterStateObservables()
            }
            // TODO handle Error result
            cachedEventFilters
        }

        // Show an error message if a star request fails
        _snackBarMessage.addSource(starEventUseCase.observe()) {
            if (it is Result.Error) {
                _snackBarMessage.postValue(
                        Event(
                                SnackbarMessage(
                                        messageId = R.string.event_star_error,
                                        longDuration = true
                                )
                        )
                )
            }
        }

        // Show a message with the result of a reservation
        _snackBarMessage.addSource(loadFilteredUserSessionsUseCase.observe()) {
            if (it is Result.Success) {
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

        // Refresh the list of user sessions if the user is updated.
        loadSessionsResult.addSource(currentUserInfo) {
            Timber.d("Loading user session with user ${it?.getUid()}")
            refreshUserSessions()
        }

        // Show reservation button if not logged in or (logged in && registered)
        showReservations = currentUserInfo.map {
            isRegistered() || !isSignedIn()
        }

        scheduleUiHintsShownUseCase(Unit, scheduleUiHintsShownResult)
        scheduleUiHintsShown = scheduleUiHintsShownResult.map {
            Event((it as? Result.Success)?.data == true)
        }

        timeZoneId = preferConferenceTimeZoneResult.map {
            val preferConferenceTimeZone = it.successOr(true)
            if (preferConferenceTimeZone) {
                TimeUtils.CONFERENCE_TIMEZONE
            } else {
                ZoneId.systemDefault()
            }
        }

        isConferenceTimeZone = timeZoneId.map { zoneId ->
            TimeUtils.isConferenceTimeZone(zoneId)
        }

        _scheduleUiData.addSource(timeZoneId) {
            _scheduleUiData.value = _scheduleUiData.value?.copy(
                    timeZoneId = it
            ) ?: ScheduleUiData(timeZoneId = it)
        }
        _scheduleUiData.addSource(loadSessionsResult) {
            val data = (it as? Result.Success)?.data ?: return@addSource
            dayIndexer = data.dayIndexer
            _scheduleUiData.value = _scheduleUiData.value?.copy(
                    list = data.userSessions,
                    dayIndexer = data.dayIndexer
            ) ?: ScheduleUiData(list = data.userSessions, dayIndexer = data.dayIndexer)
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

    fun showPinnedEvents() {
        toggleFilter(MyEventsFilter(true), true)
    }

    override fun openEventDetail(id: SessionId) {
        _navigateToSessionAction.value = Event(id)
    }

    override fun toggleFilter(filter: EventFilter, enabled: Boolean) {
        val changed = when (filter) {
            is MyEventsFilter -> userSessionMatcher.setShowPinnedEventsOnly(enabled)
            is TagFilter -> {
                if (enabled) {
                    userSessionMatcher.add(filter.tag)
                } else {
                    userSessionMatcher.remove(filter.tag)
                }
            }
        }
        if (changed) {
            // Actually toggle the filter
            filter.isChecked.set(enabled)
            // Persist the filters
            saveSelectedFiltersUseCase(userSessionMatcher)
            // Update observables
            updateFilterStateObservables()
            refreshUserSessions()

            // Analytics
            val filterName = if (filter is MyEventsFilter) {
                "Starred & Reserved"
            } else {
                filter.getText()
            }
            val action = if (enabled) AnalyticsActions.ENABLE else AnalyticsActions.DISABLE
            analyticsHelper.logUiEvent("Filter changed: $filterName", action)
        }
    }

    override fun clearFilters() {
        if (userSessionMatcher.clearAll()) {
            eventFilters.value?.forEach { it.isChecked.set(false) }
            saveSelectedFiltersUseCase(userSessionMatcher)
            updateFilterStateObservables()
            refreshUserSessions()

            analyticsHelper.logUiEvent("Clear filters", AnalyticsActions.CLICK)
        }
    }

    // Update all observables related to the filter state. Called from methods that modify
    // selected filters in the UserSessionMatcher.
    private fun updateFilterStateObservables() {
        val hasAnyFilters = userSessionMatcher.hasAnyFilters()
        _hasAnyFilters.value = hasAnyFilters
        _selectedFilters.value = cachedEventFilters.filter { it.isChecked.get() }
    }

    fun onSwipeRefresh() {
        refreshConferenceDataUseCase(Any(), swipeRefreshResult)
    }

    fun onSignInRequired() {
        _navigateToSignInDialogAction.value = Event(Unit)
    }

    private fun refreshUserSessions() {
        Timber.d("ViewModel refreshing user sessions")
        loadFilteredUserSessionsUseCase.execute(
            LoadFilteredUserSessionsParameters(userSessionMatcher, getUserId())
        )
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

        getUserId()?.let {
            starEventUseCase.execute(
                StarEventParameter(
                    it,
                    userSession.copy(
                        userEvent = userSession.userEvent.copy(isStarred = newIsStarredState))
                )
            )
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

interface ScheduleEventListener : EventActions {
    /** Called from the UI to enable or disable a particular filter. */
    fun toggleFilter(filter: EventFilter, enabled: Boolean)

    /** Called from the UI to remove all filters. */
    fun clearFilters()
}
