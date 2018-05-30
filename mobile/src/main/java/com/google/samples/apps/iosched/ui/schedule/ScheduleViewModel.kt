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

import androidx.annotation.StringRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.Block
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.domain.RefreshConferenceDataUseCase
import com.google.samples.apps.iosched.shared.domain.agenda.LoadAgendaUseCase
import com.google.samples.apps.iosched.shared.domain.invoke
import com.google.samples.apps.iosched.shared.domain.prefs.LoadSelectedFiltersUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.SaveSelectedFiltersUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.ScheduleUiHintsShownUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.EventLocation
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCaseParameters
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCaseResult
import com.google.samples.apps.iosched.shared.domain.sessions.ObserveConferenceDataUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.domain.users.StarEventParameter
import com.google.samples.apps.iosched.shared.domain.users.StarEventUseCase
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus
import com.google.samples.apps.iosched.shared.fcm.TopicSubscriber
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDays
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
    private val loadUserSessionsByDayUseCase: LoadUserSessionsByDayUseCase,
    loadAgendaUseCase: LoadAgendaUseCase,
    loadEventFiltersUseCase: LoadEventFiltersUseCase,
    signInViewModelDelegate: SignInViewModelDelegate,
    private val starEventUseCase: StarEventUseCase,
    scheduleUiHintsShownUseCase: ScheduleUiHintsShownUseCase,
    topicSubscriber: TopicSubscriber,
    private val snackbarMessageManager: SnackbarMessageManager,
    private val getTimeZoneUseCase: GetTimeZoneUseCase,
    private val refreshConferenceDataUseCase: RefreshConferenceDataUseCase,
    observeConferenceDataUseCase: ObserveConferenceDataUseCase,
    loadSelectedFiltersUseCase: LoadSelectedFiltersUseCase,
    private val saveSelectedFiltersUseCase: SaveSelectedFiltersUseCase,
    private val analyticsHelper: AnalyticsHelper
) : ViewModel(), ScheduleEventListener, SignInViewModelDelegate by signInViewModelDelegate {

    val isLoading: LiveData<Boolean>

    val swipeRefreshing: LiveData<Boolean>

    // The current UserSessionMatcher, used to filter the events that are shown
    private val userSessionMatcher = UserSessionMatcher()
    private val loadSelectedFiltersResult = MutableLiveData<Result<Unit>>()

    private val preferConferenceTimeZoneResult = MutableLiveData<Result<Boolean>>()

    /**
     * Gets the label to display for each conference date. When using time zones other than the
     * conference zone, conference days and calendar dates may not align. To minimize confusion,
     * we show actual dates when using conference zone time; otherwise, we show the day number.
     */
    val labelsForDays: LiveData<List<Int>>
    val timeZoneId: LiveData<ZoneId>

    private val _sessionTimeDataDay1 = MediatorLiveData<SessionTimeData>()
    private val sessionTimeDataDay1: LiveData<SessionTimeData>
        get() = _sessionTimeDataDay1
    private val _sessionTimeDataDay2 = MediatorLiveData<SessionTimeData>()
    private val sessionTimeDataDay2: LiveData<SessionTimeData>
        get() = _sessionTimeDataDay2
    private val _sessionTimeDataDay3 = MediatorLiveData<SessionTimeData>()
    private val sessionTimeDataDay3: LiveData<SessionTimeData>
        get() = _sessionTimeDataDay3

    // Cached list of TagFilters returned by the use case. Only Result.Success modifies it.
    private var cachedEventFilters = emptyList<EventFilter>()

    val eventFilters: LiveData<List<EventFilter>>
    private val _selectedFilters = MutableLiveData<List<EventFilter>>()
    val selectedFilters: LiveData<List<EventFilter>>
        get() = _selectedFilters
    private val _hasAnyFilters = MutableLiveData<Boolean>()
    val hasAnyFilters: LiveData<Boolean>
        get() = _hasAnyFilters
    private val _isAgendaPage = MutableLiveData<Boolean>()
    val isAgendaPage: LiveData<Boolean>
        get() = _isAgendaPage

    private var _transientUiStateVar = TransientUiState(false, false)
    private val _transientUiState = MutableLiveData<TransientUiState>()
    val transientUiState: LiveData<TransientUiState>
        get() = _transientUiState

    private val loadSessionsResult: MediatorLiveData<Result<LoadUserSessionsByDayUseCaseResult>>
    private val loadAgendaResult = MutableLiveData<Result<List<Block>>>()
    private val loadEventFiltersResult = MediatorLiveData<Result<List<EventFilter>>>()
    private val swipeRefreshResult = MutableLiveData<Result<Boolean>>()

    val eventCount: LiveData<Int>

    val agenda: LiveData<List<Block>>

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

    /** Resource id of the profile button's content description; changes based on sign in state**/
    private val _profileContentDesc = MediatorLiveData<Int>().apply { value = R.string.sign_in }

    val profileContentDesc: LiveData<Int>
        get() = _profileContentDesc

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

    // The currently happening event
    val currentEvent: LiveData<EventLocation?>

    init {
        // Load sessions and tags and store the result in `LiveData`s
        loadSessionsResult = loadUserSessionsByDayUseCase.observe()

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

        loadAgendaUseCase(loadAgendaResult)

        eventCount = loadSessionsResult.map {
            (it as? Result.Success)?.data?.userSessionCount ?: 0
        }

        isLoading = loadSessionsResult.map { it == Result.Loading }

        _errorMessage.addSource(loadSessionsResult, { result ->
            if (result is Result.Error) {
                _errorMessage.value = Event(content = result.exception.message ?: "Error")
            }
        })
        _errorMessage.addSource(loadEventFiltersResult, { result ->
            if (result is Result.Error) {
                _errorMessage.value = Event(content = result.exception.message ?: "Error")
            }
        })

        agenda = loadAgendaResult.map {
            (it as? Result.Success)?.data ?: emptyList()
        }
        // TODO handle agenda errors

        eventFilters = loadEventFiltersResult.map {
            if (it is Success) {
                cachedEventFilters = it.data
                updateFilterStateObservables()
            }
            // TODO handle Error result
            cachedEventFilters
        }

        _profileContentDesc.addSource(currentFirebaseUser) {
            _profileContentDesc.value = getProfileContentDescription(it)
        }

        // Show an error message if a star request fails
        _snackBarMessage.addSource(starEventUseCase.observe()) { it: Result<StarUpdatedStatus>? ->
            // Show a snackbar message on error.
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
        _snackBarMessage.addSource(loadUserSessionsByDayUseCase.observe()) {
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
        loadSessionsResult.addSource(currentFirebaseUser) {
            Timber.d("Loading user session with user ${(it as? Result.Success)?.data?.getUid()}")
            refreshUserSessions()
        }

        // Show reservation button if not logged in or (logged in && registered)
        showReservations = currentFirebaseUser.map {
            isRegistered() || !isSignedIn()
        }

        scheduleUiHintsShownUseCase(Unit, scheduleUiHintsShownResult)
        scheduleUiHintsShown = scheduleUiHintsShownResult.map {
            Event((it as? Result.Success)?.data == true)
        }

        val showInConferenceTimeZone = preferConferenceTimeZoneResult.map {
            (it as? Result.Success<Boolean>)?.data ?: true
        }

        timeZoneId = showInConferenceTimeZone.map { inConferenceTimeZone ->
            if (inConferenceTimeZone) {
                TimeUtils.CONFERENCE_TIMEZONE
            } else {
                ZoneId.systemDefault()
            }
        }

        labelsForDays = showInConferenceTimeZone.map { inConferenceTimeZone ->
            if (TimeUtils.physicallyInConferenceTimeZone() || inConferenceTimeZone) {
                return@map listOf(R.string.day1_date, R.string.day2_date, R.string.day3_date)
            } else {
                return@map listOf(R.string.day1, R.string.day2, R.string.day3)
            }
        }

        _sessionTimeDataDay1.addSource(timeZoneId, {
            _sessionTimeDataDay1.value = _sessionTimeDataDay1.value?.apply {
                timeZoneId = it
            } ?: SessionTimeData(timeZoneId = it)
        })
        _sessionTimeDataDay1.addSource(loadSessionsResult, {
            val userSessions =
                (it as? Result.Success)?.data?.userSessionsPerDay?.get(ConferenceDays[0])
                    ?: return@addSource
            _sessionTimeDataDay1.value = _sessionTimeDataDay1.value?.apply {
                list = userSessions
            } ?: SessionTimeData(list = userSessions)
        })

        _sessionTimeDataDay2.addSource(timeZoneId, {
            _sessionTimeDataDay2.value = _sessionTimeDataDay2.value?.apply {
                timeZoneId = it
            } ?: SessionTimeData(timeZoneId = it)
        })
        _sessionTimeDataDay2.addSource(loadSessionsResult, {
            val userSessions =
                (it as? Result.Success)?.data?.userSessionsPerDay?.get(ConferenceDays[1])
                    ?: return@addSource
            _sessionTimeDataDay2.value = _sessionTimeDataDay2.value?.apply {
                list = userSessions
            } ?: SessionTimeData(list = userSessions)
        })

        _sessionTimeDataDay3.addSource(timeZoneId, {
            _sessionTimeDataDay3.value = _sessionTimeDataDay3.value?.apply {
                timeZoneId = it
            } ?: SessionTimeData(timeZoneId = it)
        })
        _sessionTimeDataDay3.addSource(loadSessionsResult, {
            val userSessions =
                (it as? Result.Success)?.data?.userSessionsPerDay?.get(ConferenceDays[2])
                    ?: return@addSource
            _sessionTimeDataDay3.value = _sessionTimeDataDay3.value?.apply {
                list = userSessions
            } ?: SessionTimeData(list = userSessions)
        })

        swipeRefreshing = swipeRefreshResult.map {
            // Whenever refresh finishes, stop the indicator, whatever the result
            false
        }

        // Subscribe user to schedule updates
        topicSubscriber.subscribeToScheduleUpdates()

        // Observe updates in conference data
        observeConferenceDataUseCase.execute(Any())

        currentEvent = loadSessionsResult.map { result ->
            (result as? Success)?.data?.firstUnfinishedSession
        }
    }

    /**
     * Called from each schedule day fragment to load data.
     */
    fun getSessionTimeDataForDay(day: Int): LiveData<SessionTimeData> = when (day) {
        0 -> sessionTimeDataDay1
        1 -> sessionTimeDataDay2
        2 -> sessionTimeDataDay3
        else -> {
            val exception = Exception("Invalid day: $day")
            Timber.e(exception)
            throw exception
        }
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
        setTransientUiState(_transientUiStateVar.copy(hasAnyFilters = hasAnyFilters))
    }

    fun onSwipeRefresh() {
        refreshConferenceDataUseCase(Any(), swipeRefreshResult)
    }

    fun onProfileClicked() {
        if (isSignedIn()) {
            _navigateToSignOutDialogAction.value = Event(Unit)
        } else {
            _navigateToSignInDialogAction.value = Event(Unit)
        }
    }

    fun onSignInRequired() {
        _navigateToSignInDialogAction.value = Event(Unit)
    }

    @StringRes
    private fun getProfileContentDescription(userResult: Result<AuthenticatedUserInfo>?): Int {
        return if (userResult is Success && userResult.data.isSignedIn()) {
            R.string.sign_out
        } else {
            R.string.sign_in
        }
    }

    private fun refreshUserSessions() {
        Timber.d("ViewModel refreshing user sessions")
        loadUserSessionsByDayUseCase.execute(
            LoadUserSessionsByDayUseCaseParameters(userSessionMatcher, getUserId())
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
                    userSession.userEvent.copy(isStarred = newIsStarredState)
                )
            )
        }
    }

    fun setIsAgendaPage(isAgendaPage: Boolean) {
        if (_isAgendaPage.value != isAgendaPage) {
            _isAgendaPage.value = isAgendaPage
            setTransientUiState(_transientUiStateVar.copy(isAgendaPage = isAgendaPage))
        }
    }

    private fun setTransientUiState(state: TransientUiState) {
        _transientUiStateVar = state
        _transientUiState.value = state
    }

    fun initializeTimeZone() {
        getTimeZoneUseCase(Unit, preferConferenceTimeZoneResult)
    }
}

data class SessionTimeData(var list: List<UserSession>? = null, var timeZoneId: ZoneId? = null)

interface ScheduleEventListener : EventActions {
    /** Called from the UI to enable or disable a particular filter. */
    fun toggleFilter(filter: EventFilter, enabled: Boolean)

    /** Called from the UI to remove all filters. */
    fun clearFilters()
}

data class TransientUiState(
    val isAgendaPage: Boolean,
    val hasAnyFilters: Boolean
)
