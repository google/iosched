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

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.support.annotation.StringRes
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.domain.RefreshConferenceDataUseCase
import com.google.samples.apps.iosched.shared.domain.agenda.LoadAgendaUseCase
import com.google.samples.apps.iosched.shared.domain.invoke
import com.google.samples.apps.iosched.shared.domain.prefs.ScheduleUiHintsShownUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCaseResult
import com.google.samples.apps.iosched.shared.domain.sessions.ObserveConferenceDataUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.domain.users.StarEventParameter
import com.google.samples.apps.iosched.shared.domain.users.StarEventUseCase
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus
import com.google.samples.apps.iosched.shared.fcm.TopicSubscriber
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent
import com.google.samples.apps.iosched.shared.model.Block
import com.google.samples.apps.iosched.shared.model.SessionId
import com.google.samples.apps.iosched.shared.model.Tag
import com.google.samples.apps.iosched.shared.model.UserSession
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher.PinnedEventMatcher
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher.TagFilterMatcher
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_1
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_2
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_3
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.schedule.filters.LoadTagFiltersUseCase
import com.google.samples.apps.iosched.ui.schedule.filters.TagFilter
import com.google.samples.apps.iosched.ui.sessioncommon.EventActions
import com.google.samples.apps.iosched.ui.sessioncommon.stringRes
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import org.threeten.bp.ZoneId
import timber.log.Timber
import java.util.*
import javax.inject.Inject

/**
 * Loads data and exposes it to the view.
 * By annotating the constructor with [@Inject], Dagger will use that constructor when needing to
 * create the object, so defining a [@Provides] method for this class won't be needed.
 */
class ScheduleViewModel @Inject constructor(
    private val loadUserSessionsByDayUseCase: LoadUserSessionsByDayUseCase,
    loadAgendaUseCase: LoadAgendaUseCase,
    loadTagFiltersUseCase: LoadTagFiltersUseCase,
    signInViewModelDelegate: SignInViewModelDelegate,
    private val starEventUseCase: StarEventUseCase,
    scheduleUiHintsShownUseCase: ScheduleUiHintsShownUseCase,
    topicSubscriber: TopicSubscriber,
    private val snackbarMessageManager: SnackbarMessageManager,
    getTimeZoneUseCase: GetTimeZoneUseCase,
    private val refreshConferenceDataUseCase: RefreshConferenceDataUseCase,
    observeConferenceDataUseCase: ObserveConferenceDataUseCase
) : ViewModel(), ScheduleEventListener, SignInViewModelDelegate by signInViewModelDelegate {

    val isLoading: LiveData<Boolean>

    val swipeRefreshing: LiveData<Boolean>

    // The current UserSessionMatcher, used to filter the events that are shown
    private var currentSessionMatcher: UserSessionMatcher

    private val preferConferenceTimeZoneResult = MutableLiveData<Result<Boolean>>()

    /**
     * Gets the label to display for each conference date. When using time zones other than the
     * conference zone, conference days and calendar dates may not align. To minimize confusion,
     * we show actual dates when using conference zone time; otherwise, we show the day number.
     */
    val labelsForDays: LiveData<List<Int>>
    val timeZoneId: LiveData<ZoneId>

    private val _sessionTimeDataDay1 = MediatorLiveData<SessionTimeData>()
    val sessionTimeDataDay1: LiveData<SessionTimeData>
        get() = _sessionTimeDataDay1
    private val _sessionTimeDataDay2 = MediatorLiveData<SessionTimeData>()
    val sessionTimeDataDay2: LiveData<SessionTimeData>
        get() = _sessionTimeDataDay2
    private val _sessionTimeDataDay3 = MediatorLiveData<SessionTimeData>()
    val sessionTimeDataDay3: LiveData<SessionTimeData>
        get() = _sessionTimeDataDay3

    private val tagFilterMatcher = TagFilterMatcher()
    // Cached list of TagFilters returned by the use case. Only Result.Success modifies it.
    private var cachedTagFilters = emptyList<TagFilter>()

    val tagFilters: LiveData<List<TagFilter>>
    private val _selectedFilterTags = MutableLiveData<List<Tag>>()
    val selectedFilterTags: LiveData<List<Tag>>
        get() = _selectedFilterTags
    private val _hasTagFilters = MutableLiveData<Boolean>()
    val hasTagFilters: LiveData<Boolean>
        get() = _hasTagFilters
    private val _showPinnedEvents = MutableLiveData<Boolean>()
    val showPinnedEvents: LiveData<Boolean>
        get() = _showPinnedEvents
    private val _isAgendaPage = MutableLiveData<Boolean>()
    val isAgendaPage: LiveData<Boolean>
        get() = _isAgendaPage
    private val _emptyMessage = MutableLiveData<Int>()
    val emptyMessage: LiveData<Int>
        get() = _emptyMessage

    private var _transientUiStateVar = TransientUiState(false, false, false)
    private val _transientUiState = MutableLiveData<TransientUiState>()
    val transientUiState: LiveData<TransientUiState>
        get() = _transientUiState

    private val loadSessionsResult: MediatorLiveData<Result<LoadUserSessionsByDayUseCaseResult>>
    private val loadAgendaResult = MutableLiveData<Result<List<Block>>>()
    private val loadTagsResult = MutableLiveData<Result<List<TagFilter>>>()
    private val swipeRefreshResult = MutableLiveData<Result<Boolean>>()

    private val day1Sessions: LiveData<List<UserSession>>
    private val day2Sessions: LiveData<List<UserSession>>
    private val day3Sessions: LiveData<List<UserSession>>

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

    init {
        currentSessionMatcher = tagFilterMatcher
        _emptyMessage.value = R.string.schedule_tag_filters_empty

        // Load sessions and tags and store the result in `LiveData`s
        loadSessionsResult = loadUserSessionsByDayUseCase.observe()

        loadSessionsResult.addSource(observeConferenceDataUseCase.observe()) {
            Timber.d("Detected new data in conference data repository")
            refreshUserSessions()
        }

        loadAgendaUseCase(loadAgendaResult)
        loadTagFiltersUseCase(tagFilterMatcher, loadTagsResult)

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

        eventCount = loadSessionsResult.map {
            (it as? Result.Success)?.data?.userSessionCount ?: 0
        }

        isLoading = loadSessionsResult.map { it == Result.Loading }

        _errorMessage.addSource(loadSessionsResult, { result ->
            if (result is Result.Error) {
                _errorMessage.value = Event(content = result.exception.message ?: "Error")
            }
        })
        _errorMessage.addSource(loadTagsResult, { result ->
            if (result is Result.Error) {
                _errorMessage.value = Event(content = result.exception.message ?: "Error")
            }
        })

        agenda = loadAgendaResult.map {
            (it as? Result.Success)?.data ?: emptyList()
        }
        // TODO handle agenda errors

        tagFilters = loadTagsResult.map {
            if (it is Success) {
                cachedTagFilters = it.data
            }
            // TODO handle Error result
            cachedTagFilters
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

        getTimeZoneUseCase(Unit, preferConferenceTimeZoneResult)

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
        _sessionTimeDataDay1.addSource(day1Sessions, {
            _sessionTimeDataDay1.value = _sessionTimeDataDay1.value?.apply {
                list = it
            } ?: SessionTimeData(list = it)
        })

        _sessionTimeDataDay2.addSource(timeZoneId, {
            _sessionTimeDataDay2.value = _sessionTimeDataDay2.value?.apply {
                timeZoneId = it
            } ?: SessionTimeData(timeZoneId = it)
        })
        _sessionTimeDataDay2.addSource(day2Sessions, {
            _sessionTimeDataDay2.value = _sessionTimeDataDay2.value?.apply {
                list = it
            } ?: SessionTimeData(list = it)
        })

        _sessionTimeDataDay3.addSource(timeZoneId, {
            _sessionTimeDataDay3.value = _sessionTimeDataDay3.value?.apply {
                timeZoneId = it
            } ?: SessionTimeData(timeZoneId = it)
        })
        _sessionTimeDataDay3.addSource(day3Sessions, {
            _sessionTimeDataDay3.value = _sessionTimeDataDay3.value?.apply {
                list = it
            } ?: SessionTimeData(list = it)
        })

        swipeRefreshing = swipeRefreshResult.map {
            // Whenever refresh finishes, stop the indicator, whatever the result
            false
        }
        _transientUiState.value = _transientUiStateVar

        // Subscribe user to schedule updates
        topicSubscriber.subscribeToScheduleUpdates()

        // Observe updates in conference data
        observeConferenceDataUseCase.execute(Any())
    }

    /**
     * Called from each schedule day fragment to load data.
     */
    fun getSessionTimeDataForDay(day: ConferenceDay):
            LiveData<SessionTimeData> = when (day) {
        DAY_1 -> sessionTimeDataDay1
        DAY_2 -> sessionTimeDataDay2
        DAY_3 -> sessionTimeDataDay3
    }

    /*
     * Note: only for testing.
     */
    fun getSessionsForDay(day: ConferenceDay): LiveData<List<UserSession>> = when(day) {
        DAY_1 -> day1Sessions
        DAY_2 -> day2Sessions
        DAY_3 -> day3Sessions
    }

    override fun openEventDetail(id: SessionId) {
        _navigateToSessionAction.value = Event(id)
    }

    override fun toggleFilter(filter: TagFilter, enabled: Boolean) {
        var changed = false
        if (enabled && tagFilterMatcher.add(filter.tag)) {
            filter.isChecked.set(true)
            changed = true
        } else if (!enabled && tagFilterMatcher.remove(filter.tag)) {
            filter.isChecked.set(false)
            changed = true
        }
        // If tagFilterMatcher.add or .remove returns false, we do nothing.
        if (changed) {
            val hasTagFilters = !tagFilterMatcher.isEmpty()
            _hasTagFilters.value = hasTagFilters
            _selectedFilterTags.value = tagFilterMatcher.getSelectedTags()
            setTransientUiState(_transientUiStateVar.copy(hasTagFilters = hasTagFilters))
            refreshUserSessions()
        }
    }

    override fun clearTagFilters() {
        if (tagFilterMatcher.clearAll()) {
            tagFilters.value?.forEach { it.isChecked.set(false) }
            _hasTagFilters.value = false
            _selectedFilterTags.value = emptyList()
            setTransientUiState(_transientUiStateVar.copy(hasTagFilters = false))
            refreshUserSessions()
        }
    }

    override fun togglePinnedEvents(pinned: Boolean) {
        // TODO check if logged in first
        if (_showPinnedEvents.value != pinned) {
            _showPinnedEvents.value = pinned
            if (pinned) {
                currentSessionMatcher = PinnedEventMatcher
                _showPinnedEvents.value = true
                _emptyMessage.value = R.string.schedule_pinned_events_empty
                setTransientUiState(_transientUiStateVar.copy(showPinnedEvents = true))
            } else {
                currentSessionMatcher = tagFilterMatcher
                _showPinnedEvents.value = false
                _emptyMessage.value = R.string.schedule_tag_filters_empty
                setTransientUiState(_transientUiStateVar.copy(showPinnedEvents = false))
            }
            refreshUserSessions()
        }
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
        loadUserSessionsByDayUseCase.execute(currentSessionMatcher to getUserId())
    }

    override fun onStarClicked(userEvent: UserEvent) {
        if (!isSignedIn()) {
            Timber.d("Showing Sign-in dialog after star click")
            _navigateToSignInDialogAction.value = Event(Unit)
            return
        }
        val newIsStarredState = !userEvent.isStarred

        // Update the snackbar message optimistically.
        val stringResId = if (newIsStarredState) {
            R.string.event_starred
        } else {
            R.string.event_unstarred
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
                    userEvent.copy(isStarred = newIsStarredState)
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
}

data class SessionTimeData(var list: List<UserSession>? = null, var timeZoneId: ZoneId? = null)

interface ScheduleEventListener : EventActions {
    /** Called from the UI to enable or disable a particular filter. */
    fun toggleFilter(filter: TagFilter, enabled: Boolean)

    /** Called from the UI to remove all filters. */
    fun clearTagFilters()

    /** Called from the UI to toggle showing starred or reserved events only. */
    fun togglePinnedEvents(pinned: Boolean)
}

data class TransientUiState(
    val isAgendaPage: Boolean,
    val showPinnedEvents: Boolean,
    val hasTagFilters: Boolean
)
