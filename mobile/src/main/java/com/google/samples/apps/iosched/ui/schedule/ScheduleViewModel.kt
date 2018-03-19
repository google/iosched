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
import com.google.samples.apps.iosched.shared.domain.tags.LoadTagsByCategoryUseCase
import com.google.samples.apps.iosched.shared.domain.users.StarEventParameter
import com.google.samples.apps.iosched.shared.domain.users.StarEventUseCase
import com.google.samples.apps.iosched.shared.domain.users.UpdatedStatus
import com.google.samples.apps.iosched.shared.domain.users.UpdatedStatus.STARRED
import com.google.samples.apps.iosched.shared.domain.users.UpdatedStatus.UNSTARRED
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
    private val starEventUseCase: StarEventUseCase
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

    private val loadSessionsResult: LiveData<Result<Map<ConferenceDay, List<UserSession>>>>
    private val loadAgendaResult = MutableLiveData<Result<List<Block>>>()
    private val loadTagsResult = MutableLiveData<Result<List<Tag>>>()

    private val day1Sessions: LiveData<List<UserSession>>
    private val day2Sessions: LiveData<List<UserSession>>
    private val day3Sessions: LiveData<List<UserSession>>

    val agenda: LiveData<List<Block>>

    /** LiveData for Actions and Events **/
    val errorMessage: MediatorLiveData<Event<String>>
    val navigateToSessionAction = MutableLiveData<Event<String>>()

    private val _starEvent = MutableLiveData<Event<UpdatedStatus>>()
    val starEvent: LiveData<Event<UpdatedStatus>>
        get() = _starEvent

    // TODO: Remove it once the FirebaseUser is available when the app is launched
    val tempUser = "user1"

    /** Resource id of the profile button's content description; changes based on login state**/
    val profileContentDesc: LiveData<Int>

    init {
        userSessionMatcher = tagFilterMatcher

        // Load sessions and tags and store the result in `LiveData`s
        loadSessionsResult = loadUserSessionsByDayUseCase.observe()

        refreshUserSessions()

        loadAgendaUseCase(loadAgendaResult)
        loadTagsByCategoryUseCase(loadTagsResult)

        // map LiveData results from UseCase to each day's individual LiveData
        day1Sessions = loadSessionsResult.map {
            (it as? Result.Success)?.data?.get(DAY_1) ?: emptyList()
        }
        day2Sessions = loadSessionsResult.map {
            (it as? Result.Success)?.data?.get(DAY_2) ?: emptyList()
        }
        day3Sessions = loadSessionsResult.map {
            (it as? Result.Success)?.data?.get(DAY_3) ?: emptyList()
        }

        isLoading = loadSessionsResult.map { it == Result.Loading }

        errorMessage = MediatorLiveData()
        errorMessage.addSource(loadSessionsResult, { result ->
            if (result is Result.Error) {
                errorMessage.value = Event(content = result.exception.message ?: "Error")
            }
        })
        errorMessage.addSource(loadTagsResult, { result ->
            if (result is Result.Error) {
                errorMessage.value = Event(content = result.exception.message ?: "Error")
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
        navigateToSessionAction.value = Event(id)
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
        loadUserSessionsByDayUseCase.execute(userSessionMatcher to tempUser)
    }

    override fun onStarClicked(session: Session, userEvent: UserEvent?) {
        if (!isLoggedIn()) {
            // TODO: Show a dialog saying "Sign in to customize your schedule"
            // https://docs.google.com/presentation/d/1VtsO3f-FfaigP1dErhIYJaqRXMCIYMXvHBa3y5cUTb0/edit?ts=5aa15da0#slide=id.g34bc00dc0a_0_7
            Timber.d("You need to sign in to star an event")
            errorMessage.value = Event("Sign in to star events")
            return
        }
        val newIsStarredState = if (userEvent?.isStarred != null) !userEvent.isStarred else true
        starEventUseCase.observe()

        starEventUseCase.execute(StarEventParameter(tempUser, session, newIsStarredState))

        _starEvent.value = if (newIsStarredState) Event(STARRED) else Event(UNSTARRED)
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
}

//TODO(jalc) move somewhere else (b/74113562)
/**
 * Used as a wrapper for data that is exposed via a LiveData that represents an event.
 */
open class Event<T>(private val content: T, private var hasBeenHandled: Boolean = false) {
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
