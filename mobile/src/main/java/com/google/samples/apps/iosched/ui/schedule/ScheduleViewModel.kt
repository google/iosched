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
import android.databinding.ObservableBoolean
import android.support.annotation.StringRes
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.data.login.AuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.domain.agenda.LoadAgendaUseCase
import com.google.samples.apps.iosched.shared.domain.invoke
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCaseResult
import com.google.samples.apps.iosched.shared.domain.sessions.UserEventsMessage
import com.google.samples.apps.iosched.shared.domain.tags.LoadTagsByCategoryUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationActionUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestParameters
import com.google.samples.apps.iosched.shared.domain.users.StarEventParameter
import com.google.samples.apps.iosched.shared.domain.users.StarEventUseCase
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus
import com.google.samples.apps.iosched.shared.firestore.entity.LastReservationRequested
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent
import com.google.samples.apps.iosched.shared.model.Block
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.Tag
import com.google.samples.apps.iosched.shared.model.UserSession
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.shared.schedule.PinnedEventMatcher
import com.google.samples.apps.iosched.shared.schedule.TagFilterMatcher
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_1
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_2
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_3
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.login.LoginViewModelPlugin
import com.google.samples.apps.iosched.util.hasSameValue
import timber.log.Timber
import javax.inject.Inject

/**
 * Loads data and exposes it to the view.
 * By annotating the constructor with [@Inject], Dagger will use that constructor when needing to
 * create the object, so defining a [@Provides] method for this class won't be needed.
 */
class ScheduleViewModel @Inject constructor(
    private val loadUserSessionsByDayUseCase: LoadUserSessionsByDayUseCase,
    loadAgendaUseCase: LoadAgendaUseCase,
    loadTagsByCategoryUseCase: LoadTagsByCategoryUseCase,
    loginViewModelPlugin: LoginViewModelPlugin,
    private val starEventUseCase: StarEventUseCase,
    private val reservationActionUseCase: ReservationActionUseCase
) : ViewModel(), ScheduleEventListener, LoginViewModelPlugin by loginViewModelPlugin {

    val isLoading: LiveData<Boolean>

    // The current UserSessionMatcher, used to filter the events that are shown
    private var userSessionMatcher: UserSessionMatcher

    private val tagFilterMatcher = TagFilterMatcher()
    // List of TagFilters returned by the LiveData transformation. Only Result.Success modifies it.
    private var cachedTagFilters = emptyList<TagFilter>()

    val tagFilters: LiveData<List<TagFilter>>
    val hasAnyFilters = ObservableBoolean(false)
    val showPinnedEvents = ObservableBoolean(false)

    private val loadSessionsResult: MediatorLiveData<Result<LoadUserSessionsByDayUseCaseResult>>
    private val loadAgendaResult = MutableLiveData<Result<List<Block>>>()
    private val loadTagsResult = MutableLiveData<Result<List<Tag>>>()

    private val day1Sessions: LiveData<List<UserSession>>
    private val day2Sessions: LiveData<List<UserSession>>
    private val day3Sessions: LiveData<List<UserSession>>

    val agenda: LiveData<List<Block>>

    /** LiveData for Actions and Events **/

    private val _errorMessage = MediatorLiveData<Event<String>>()
    val errorMessage : LiveData<Event<String>>
        get() = _errorMessage

    private val _navigateToSessionAction = MutableLiveData<Event<String>>()
    val navigateToSessionAction : LiveData<Event<String>>
        get() = _navigateToSessionAction

    private val _snackBarMessage = MediatorLiveData<Event<SnackbarMessage>>()
    val snackBarMessage : LiveData<Event<SnackbarMessage>>
        get() = _snackBarMessage


    /** Resource id of the profile button's content description; changes based on login state */
    val profileContentDesc: LiveData<Int>

    /**
     * Event to navigate to the sign in Dialog. We only want to consume the event, so the
     * Boolean value isn't used actually.
     */
    private val _navigateToSignInDialogAction = MutableLiveData<Event<Boolean>>()
    val navigateToSignInDialogAction: LiveData<Event<Boolean>>
        get() = _navigateToSignInDialogAction

    init {
        userSessionMatcher = tagFilterMatcher

        // Load sessions and tags and store the result in `LiveData`s
        loadSessionsResult = loadUserSessionsByDayUseCase.observe()
        loadSessionsResult.addSource(currentFirebaseUser) {
            refreshUserSessions()
        }

        loadAgendaUseCase(loadAgendaResult)
        loadTagsByCategoryUseCase(loadTagsResult)

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
                cachedTagFilters = processTags(it.data)
            }
            // TODO handle Error result
            cachedTagFilters
        }

        profileContentDesc = currentFirebaseUser.map(::getProfileContentDescription)

        // Show an error message if a reservation request fails
        _snackBarMessage.addSource(reservationActionUseCase.observe()) {
            if (it is Result.Error) {
                _snackBarMessage.postValue(Event(SnackbarMessage(R.string.reservation_error)))
            }
        }

        // Show an error message if a reservation request fails
        _snackBarMessage.addSource(starEventUseCase.observe()) { it: Result<StarUpdatedStatus>? ->
            // Show a snackbar message on error.
            if (it is Result.Error)  {
                _snackBarMessage.postValue(Event(SnackbarMessage(R.string.event_star_error)))
            }
        }

        // Show a message with the result of a reservation
        _snackBarMessage.addSource(loadUserSessionsByDayUseCase.observe()) {
            val message = when (it) {
                is Result.Success ->
                    when (it.data.userMessage) {
                        UserEventsMessage.CHANGES_IN_WAITLIST -> R.string.waitlist_new
                        UserEventsMessage.CHANGES_IN_RESERVATIONS -> R.string.reservation_new
                        else -> null
                    }
                else -> null
            }

            message?.let {
                // Snackbar messages about changes in reservations last longer and have an action.
                _snackBarMessage.postValue(Event(
                        SnackbarMessage(message, actionId = R.string.got_it, longDuration = true)))
            }
        }
    }

    private fun processTags(tags: List<Tag>): List<TagFilter> {
        tagFilterMatcher.removeOrphanedTags(tags)
        // Convert to list of TagFilters, checking the ones that are selected in TagFilterMatcher.
        return tags.map { TagFilter(it, it in tagFilterMatcher) }
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

    override fun toggleFilter(filter: TagFilter, enabled: Boolean) {
        // If tagFilterMatcher.add or .remove returns false, we do nothing.
        if (enabled && tagFilterMatcher.add(filter.tag)) {
            filter.isChecked.set(true)
            hasAnyFilters.set(true)
            refreshUserSessions()
        } else if (!enabled && tagFilterMatcher.remove(filter.tag)) {
            filter.isChecked.set(false)
            hasAnyFilters.set(!tagFilterMatcher.isEmpty())
            refreshUserSessions()
        }
    }

    override fun clearFilters() {
        if (tagFilterMatcher.clearAll()) {
            tagFilters.value?.forEach { it.isChecked.set(false) }
            hasAnyFilters.set(false)
            refreshUserSessions()
        }
    }

    override fun togglePinnedEvents(pinned: Boolean) {
        // TODO check if logged in first
        if (showPinnedEvents.get() != pinned) {
            showPinnedEvents.set(pinned)
            userSessionMatcher = if (pinned) { PinnedEventMatcher } else { tagFilterMatcher }
            refreshUserSessions()
        }
    }

    fun onProfileClicked() {
        if (isLoggedIn()) {
            emitLogoutRequest()
        } else {
            emitLoginRequest()
        }
    }

    @StringRes
    private fun getProfileContentDescription(userResult: Result<AuthenticatedUserInfo>?): Int {
        return if (userResult is Success && userResult.data.isLoggedIn()) {
            R.string.a11y_logout
        } else {
            R.string.a11y_login
        }
    }

    private fun refreshUserSessions() {
        val uid = (currentFirebaseUser.value as? Success)?.data?.getUid()
        loadUserSessionsByDayUseCase.execute(userSessionMatcher to (uid ?: ""))
    }

    override fun onStarClicked(session: Session, userEvent: UserEvent?) {
        if (!isLoggedIn()) {
            Timber.d("Showing Sign-in dialog after star click")
            _navigateToSignInDialogAction.value = Event(true)
            return
        }
        val newIsStarredState = !(userEvent?.isStarred ?: false)

        // Update the snackbar message optimistically.
        val snackbarMessage = if(newIsStarredState) {
            SnackbarMessage(R.string.event_starred, R.string.got_it)
        } else {
            SnackbarMessage(R.string.event_unstarred)
        }
        _snackBarMessage.postValue(Event(snackbarMessage))

        // uid should not be null at this moment because the user is logged in
        val uid = (currentFirebaseUser.value as Success).data.getUid()!!
        starEventUseCase.execute(StarEventParameter(uid, session, newIsStarredState))
    }

    override fun onReservationClicked(session: Session, userEvent: UserEvent?) {
        if (!isLoggedIn()) {
            // TODO: Show a dialog saying "Sign in to customize your schedule"
            // https://docs.google.com/presentation/d/1VtsO3f-FfaigP1dErhIYJaqRXMCIYMXvHBa3y5cUTb0/edit?ts=5aa15da0#slide=id.g34bc00dc0a_0_7
            Timber.d("You need to sign in to star an event")
            _errorMessage.value = Event("Sign in to star events")
            return
        }

        val action = if (userEvent?.isReserved() == true
                || userEvent?.isWaitlisted() == true
                || userEvent?.reservationRequested == LastReservationRequested.RESERVATION) {
            // Cancel the reservation if it's reserved, waitlisted or pending
            ReservationRequestAction.CANCEL
        } else {
            ReservationRequestAction.REQUEST
        }

        // Update the snackbar message optimistically.
        val snackbarMessage = when(action) {
            ReservationRequestAction.REQUEST ->
                SnackbarMessage(R.string.reservation_request_succeeded, R.string.got_it)
            ReservationRequestAction.CANCEL ->
                SnackbarMessage(R.string.reservation_cancel_succeeded, R.string.got_it)
        }
        _snackBarMessage.postValue(Event(snackbarMessage))

        // uid should not be null at this moment because the user is logged in
        val uid = (currentFirebaseUser.value as Success).data.getUid()!!
        reservationActionUseCase.execute(ReservationRequestParameters(uid, session, action))
    }
}

class TagFilter(val tag: Tag, isChecked: Boolean) {
    val isChecked = ObservableBoolean(isChecked)

    /** Only the tag is used for equality. */
    override fun equals(other: Any?) = this === other || (other is TagFilter && other.tag == tag)

    /** Only the tag is used for equality. */
    override fun hashCode() = tag.hashCode()

    fun isUiContentEqual(other: TagFilter) =
        tag.isUiContentEqual(other.tag) && isChecked.hasSameValue(other.isChecked)
}

interface ScheduleEventListener {
    /** Called from UI to start a navigation action to the detail screen. */
    fun openSessionDetail(id: String)

    /** Called from the UI to enable or disable a particular filter. */
    fun toggleFilter(filter: TagFilter, enabled: Boolean)

    /** Called from the UI to remove all filters. */
    fun clearFilters()

    /** Called from the UI to toggle showing starred or reserved events only. */
    fun togglePinnedEvents(pinned: Boolean)

    // TODO: replace session with sessionId once userEvent in [UserSession] isn't nullable
    fun onStarClicked(session: Session, userEvent: UserEvent?)

    fun onReservationClicked(session: Session, userEvent: UserEvent?)
}

//TODO(jalc) move somewhere else (b/74113562)
/**
 * Used as a wrapper for data that is exposed via a LiveData that represents an event.
 */
open class Event<out T>(private val content: T, private var hasBeenHandled: Boolean = false) {
    /**
     * Returns the content and prevents its use again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content, even if it's already been handled.
     */
    fun peekContent(): T = content
}
