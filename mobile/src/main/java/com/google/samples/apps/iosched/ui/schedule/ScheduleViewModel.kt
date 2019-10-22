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
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.domain.RefreshConferenceDataUseCase
import com.google.samples.apps.iosched.shared.domain.logistics.LoadAutoScrollFlagUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.LoadSelectedFiltersUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.SaveSelectedFiltersUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.EventLocation
import com.google.samples.apps.iosched.shared.domain.sessions.GetConferenceDaysUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCaseParameters
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCaseResult
import com.google.samples.apps.iosched.shared.domain.sessions.ObserveConferenceDataUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.domain.users.StarEventAndNotifyUseCase
import com.google.samples.apps.iosched.shared.domain.users.StarEventParameter
import com.google.samples.apps.iosched.shared.fcm.TopicSubscriber
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.shared.time.TimeProvider
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.shared.util.switchMap
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.schedule.filters.EventFilter
import com.google.samples.apps.iosched.ui.schedule.filters.EventFilter.MyEventsFilter
import com.google.samples.apps.iosched.ui.schedule.filters.EventFilter.TagFilter
import com.google.samples.apps.iosched.ui.schedule.filters.LoadEventFiltersUseCase
import com.google.samples.apps.iosched.ui.sessioncommon.EventActions
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.google.samples.apps.iosched.util.cancelIfActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
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
    private val getConferenceDaysUseCase: GetConferenceDaysUseCase,
    private val loadEventFiltersUseCase: LoadEventFiltersUseCase,
    signInViewModelDelegate: SignInViewModelDelegate,
    private val starEventUseCase: StarEventAndNotifyUseCase,
    topicSubscriber: TopicSubscriber,
    private val snackbarMessageManager: SnackbarMessageManager,
    private val getTimeZoneUseCase: GetTimeZoneUseCase,
    private val refreshConferenceDataUseCase: RefreshConferenceDataUseCase,
    observeConferenceDataUseCase: ObserveConferenceDataUseCase,
    loadSelectedFiltersUseCase: LoadSelectedFiltersUseCase,
    private val saveSelectedFiltersUseCase: SaveSelectedFiltersUseCase,
    private val analyticsHelper: AnalyticsHelper,
    isScrollFeatureEnabledUseCase: LoadAutoScrollFlagUseCase,
    private val timeProvider: TimeProvider
) : ViewModel(), ScheduleEventListener, SignInViewModelDelegate by signInViewModelDelegate {

    // Keeps track of the coroutine that listens for user sessions
    private var loadUserSessionsJob: Job? = null

    private val loadSessionsResult = MutableLiveData<Result<LoadUserSessionsByDayUseCaseResult>>()

    val isLoading: LiveData<Boolean> = loadSessionsResult.map { it == Result.Loading }

    // The current UserSessionMatcher, used to filter the events that are shown
    private val userSessionMatcher = UserSessionMatcher()

    private val preferConferenceTimeZoneResult = MutableLiveData<Result<Boolean>>()

    private val showInConferenceTimeZone = preferConferenceTimeZoneResult.map { it.data ?: true }

    /**
     * Gets the label to display for each conference date. When using time zones other than the
     * conference zone, conference days and calendar dates may not align. To minimize confusion,
     * we show actual dates when using conference zone time; otherwise, we show the day number.
     */
    val labelsForDays: LiveData<List<Int>> = showInConferenceTimeZone.map { inConferenceTimeZone ->
        if (TimeUtils.physicallyInConferenceTimeZone() || inConferenceTimeZone) {
            return@map listOf(R.string.day1_date, R.string.day2_date)
        } else {
            return@map listOf(R.string.day1, R.string.day2)
        }
    }

    val timeZoneId: LiveData<ZoneId> = showInConferenceTimeZone.map { inConferenceTimeZone ->
        if (inConferenceTimeZone) {
            TimeUtils.CONFERENCE_TIMEZONE
        } else {
            ZoneId.systemDefault()
        }
    }

    private val sessionTimeDataDay: List<MediatorLiveData<SessionTimeData>> =
        getConferenceDaysUseCase().map {
            MediatorLiveData<SessionTimeData>()
        }.apply {
            // Session data observes the time zone and the repository.
            forEachIndexed { _, sessionTimeDataDay ->
                sessionTimeDataDay.addSource(timeZoneId) {
                    sessionTimeDataDay.value = sessionTimeDataDay.value?.apply {
                        timeZoneId = it
                    } ?: SessionTimeData(timeZoneId = it)
                }
            }
        }

    // Cached list of TagFilters returned by the use case. Only Result.Success modifies it.
    private var cachedEventFilters = mutableListOf<EventFilter>()

    private val _eventFilters = MutableLiveData<List<EventFilter>>()
    val eventFilters: LiveData<List<EventFilter>>
        get() = _eventFilters

    private val _selectedFilters = MutableLiveData<List<EventFilter>>()
    val selectedFilters: LiveData<List<EventFilter>>
        get() = _selectedFilters
    private val _hasAnyFilters = MutableLiveData<Boolean>().apply { value = false }
    val hasAnyFilters: LiveData<Boolean>
        get() = _hasAnyFilters

    private val swipeRefreshResult = MutableLiveData<Result<Boolean>>()
    val swipeRefreshing: LiveData<Boolean> = swipeRefreshResult.map {
        // Whenever refresh finishes, stop the indicator, whatever the result
        false
    }

    val eventCount: LiveData<Int> = loadSessionsResult.map { it.data?.userSessionCount ?: 0 }

    /** LiveData for Actions and Events **/

    private val _errorMessage = MediatorLiveData<Event<String>>().apply {
        addSource(loadSessionsResult) { result ->
            if (result is Result.Error) {
                value = Event(content = result.exception.message ?: "Error")
            }
        }
    }
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

    private val _navigateToSignInDialogAction = MutableLiveData<Event<Unit>>()
    val navigateToSignInDialogAction: LiveData<Event<Unit>>
        get() = _navigateToSignInDialogAction

    private val _navigateToSignOutDialogAction = MutableLiveData<Event<Unit>>()
    val navigateToSignOutDialogAction: LiveData<Event<Unit>>
        get() = _navigateToSignOutDialogAction

    private val _navigateToSearchAction = MutableLiveData<Event<Unit>>()
    val navigateToSearchAction: LiveData<Event<Unit>> = _navigateToSearchAction

    // Flags used to disable the "scroll to now" feature
    var userHasInteracted = false

    // The currently happening event
    val currentEvent: LiveData<EventLocation?> = loadSessionsResult.switchMap {
        liveData {
            // Only emit if the feature is not remotely disabled
            if (isScrollFeatureEnabledUseCase(Unit).data == true) {
                emit(it.data?.firstUnfinishedSession)
            }
        }
    }

    init {
        // Load persisted filters to the matcher when the ViewModel starts
        viewModelScope.launch {

            // If these use cases fail, we don't want to cancel the execution of this coroutine
            supervisorScope {
                loadSelectedFiltersUseCase(userSessionMatcher)
                loadEventFilters()
            }

            // Creating a new coroutine to listen for user auth changes because we don't want
            // to suspend the calling coroutine on the collect call and we also want to
            // listen for changes in conference data
            launch {
                // Load user sessions when the user has changed and update profile content
                currentFirebaseUser.collect {
                    _profileContentDesc.value = getProfileContentDescription(it)
                    Timber.d("Loading user session with user ${it?.data?.getUid()}")
                    refreshUserSessions()
                }
            }

            launch {
                // Load filters and user sessions when there's new conference data
                observeConferenceDataUseCase(Any()).collect {
                    Timber.d("Detected new data in conference data repository")
                    loadEventFilters()
                    refreshUserSessions()
                }
            }
        }

        // Subscribe user to schedule updates
        topicSubscriber.subscribeToScheduleUpdates()

        initializeTimeZone()
    }

    // Load EventFilters when persisted filters are loaded and when there's new conference data
    private suspend fun loadEventFilters() {
        val loadEventFiltersResult = loadEventFiltersUseCase(userSessionMatcher)
        if (loadEventFiltersResult is Success) {
            cachedEventFilters = loadEventFiltersResult.data.toMutableList()
            updateFilterStateObservables()
        } else if (loadEventFiltersResult is Result.Error) {
            _errorMessage.value =
                Event(content = loadEventFiltersResult.exception.message ?: "Error")
        }

        initializeTimeZone()
    }

    /**
     * Called from each schedule day fragment to load data.
     */
    fun getSessionTimeDataForDay(day: Int): LiveData<SessionTimeData> {
        return sessionTimeDataDay.getOrElse(day) {
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
            val index = cachedEventFilters.indexOf(filter)
            cachedEventFilters[index] = filter.copy(enabled)
            // Persist the filters
            viewModelScope.launch {
                saveSelectedFiltersUseCase(userSessionMatcher)
            }
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
            cachedEventFilters = cachedEventFilters.mapTo(mutableListOf()) {
                if (it.isSelected) it.copy(false) else it
            }

            viewModelScope.launch {
                saveSelectedFiltersUseCase(userSessionMatcher)
            }
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
        _eventFilters.value = cachedEventFilters
        _selectedFilters.value = cachedEventFilters.filter { it.isSelected }
    }

    fun onSwipeRefresh() {
        viewModelScope.launch {
            swipeRefreshResult.value = refreshConferenceDataUseCase(Any())
        }
    }

    fun onProfileClicked() {
        if (isSignedIn()) {
            _navigateToSignOutDialogAction.value = Event(Unit)
        } else {
            _navigateToSignInDialogAction.value = Event(Unit)
        }
    }

    fun onSearchClicked() {
        _navigateToSearchAction.value = Event(Unit)
    }

    fun onSignInRequired() {
        _navigateToSignInDialogAction.value = Event(Unit)
    }

    @StringRes
    private fun getProfileContentDescription(userResult: Result<AuthenticatedUserInfo?>): Int {
        return if (userResult is Success && userResult.data?.isSignedIn() == true) {
            R.string.sign_out
        } else {
            R.string.sign_in
        }
    }

    private fun refreshUserSessions() {
        Timber.d("ViewModel refreshing user sessions")

        // when there's a new user, cancel listening for the old user's sessions
        loadUserSessionsJob.cancelIfActive()

        loadUserSessionsJob = viewModelScope.launch {
            loadUserSessionsByDayUseCase(
                LoadUserSessionsByDayUseCaseParameters(
                    userSessionMatcher, getUserId(), timeProvider.nowZoned()
                )
            ).collect {
                loadSessionsResult.value = it

                // Group sessions by day
                sessionTimeDataDay.forEachIndexed { index, sessionTimeDataDay ->
                    val userSessions =
                        it.data?.userSessionsPerDay?.get(getConferenceDaysUseCase()[index])
                            ?: return@forEachIndexed
                    sessionTimeDataDay.value = sessionTimeDataDay.value?.apply {
                        list = userSessions
                    } ?: SessionTimeData(list = userSessions)
                }
            }
        }
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
                        it,
                        userSession.copy(
                            userEvent = userSession.userEvent.copy(isStarred = newIsStarredState)
                        )
                    )
                )

                // Show an error message if a star request fails
                if (result is Result.Error) {
                    _snackBarMessage.value = Event(
                        SnackbarMessage(
                            messageId = R.string.event_star_error,
                            longDuration = true
                        )
                    )
                }
            }
        }
    }

    fun initializeTimeZone() {
        viewModelScope.launch {
            preferConferenceTimeZoneResult.value = getTimeZoneUseCase(Unit)
        }
    }
}

data class SessionTimeData(var list: List<UserSession>? = null, var timeZoneId: ZoneId? = null)

interface ScheduleEventListener : EventActions {
    /** Called from the UI to enable or disable a particular filter. */
    fun toggleFilter(filter: EventFilter, enabled: Boolean)

    /** Called from the UI to remove all filters. */
    fun clearFilters()
}
