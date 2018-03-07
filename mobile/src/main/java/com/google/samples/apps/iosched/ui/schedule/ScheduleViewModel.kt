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
import android.support.annotation.VisibleForTesting
import com.google.samples.apps.iosched.shared.domain.agenda.LoadAgendaUseCase
import com.google.samples.apps.iosched.shared.domain.invoke
import com.google.samples.apps.iosched.shared.domain.sessions.LoadSessionsByDayUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadTagsByCategoryUseCase
import com.google.samples.apps.iosched.shared.model.Block
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.Tag
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.shared.schedule.SessionMatcher
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_1
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_2
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_3
import com.google.samples.apps.iosched.shared.util.checkAllSealed
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.util.hasSameValue
import com.google.samples.apps.iosched.util.login.LoginFailed
import com.google.samples.apps.iosched.util.login.LoginResult
import com.google.samples.apps.iosched.util.login.LoginSuccess
import javax.inject.Inject

/**
 * Loads data and exposes it to the view.
 * By annotating the constructor with [@Inject], Dagger will use that constructor when needing to
 * create the object, so defining a [@Provides] method for this class won't be needed.
 */
class ScheduleViewModel @Inject constructor(
    private val loadSessionsByDayUseCase: LoadSessionsByDayUseCase,
    loadAgendaUseCase: LoadAgendaUseCase,
    loadTagsByCategoryUseCase: LoadTagsByCategoryUseCase
) : ViewModel(), ScheduleEventListener {

    val isLoading: LiveData<Boolean>

    private val sessionMatcher = SessionMatcher()
    // List of TagFilters returned by the LiveData transformation. Only Result.Success modifies it.
    private var cachedTagFilters = emptyList<TagFilter>()

    val tagFilters: LiveData<List<TagFilter>>
    val hasAnyFilters = ObservableBoolean(false)

    private val loadSessionsResult = MutableLiveData<Result<Map<ConferenceDay, List<Session>>>>()
    private val loadAgendaResult = MutableLiveData<Result<List<Block>>>()
    private val loadTagsResult = MutableLiveData<Result<List<Tag>>>()

    private val day1Sessions: LiveData<List<Session>>
    private val day2Sessions: LiveData<List<Session>>
    private val day3Sessions: LiveData<List<Session>>

    val agenda: LiveData<List<Block>>

    /** LiveData for Actions and Events **/
    val errorMessage: MediatorLiveData<Event<String>>
    val navigateToSessionAction = MutableLiveData<Event<String>>()

    init {
        // Load sessions and tags and store the result in `LiveData`s
        loadSessionsByDayUseCase(sessionMatcher, loadSessionsResult)
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
    }

    @VisibleForTesting
    internal fun processTags(tags: List<Tag>): List<TagFilter> {
        sessionMatcher.removeOrphanedTags(tags)
        // Convert to list of TagFilters, checking the ones that are selected in SessionMatcher.
        return tags.map { TagFilter(it, it in sessionMatcher) }
    }

    /**
     * Called from each schedule day fragment to load data.
     */
    fun getSessionsForDay(day: ConferenceDay): LiveData<List<Session>> = when (day) {
        DAY_1 -> day1Sessions
        DAY_2 -> day2Sessions
        DAY_3 -> day3Sessions
    }

    /**
     * Called from UI to start a navigation action to the detail screen.
     */
    override fun openSessionDetail(id: String) {
        navigateToSessionAction.value = Event(id)
    }

    /**
     * Called from the UI to enable or disable the filters.
     */
    override fun toggleFilter(filter: TagFilter, enabled: Boolean) {
        // If sessionMatcher.add or .remove returns false, we do nothing.
        if (enabled && sessionMatcher.add(filter.tag)) {
            filter.isChecked.set(true)
            hasAnyFilters.set(true)
            loadSessionsByDayUseCase(sessionMatcher, loadSessionsResult)

        } else if (!enabled && sessionMatcher.remove(filter.tag)) {
            filter.isChecked.set(false)
            hasAnyFilters.set(!sessionMatcher.isEmpty())
            loadSessionsByDayUseCase(sessionMatcher, loadSessionsResult)
        }
    }

    /**
     * Called from the UI to reset the filters.
     */
    override fun clearFilters() {
        if (sessionMatcher.clearAll()) {
            tagFilters.value?.forEach { it.isChecked.set(false) }
            hasAnyFilters.set(false)
            loadSessionsByDayUseCase(sessionMatcher, loadSessionsResult)
        }
    }

    /**
     * Handle the a login result.
     */
    fun handleLoginResult(result: LoginResult) {
        when(result) {
            is LoginSuccess -> {
                // success
                Unit
            }
            is LoginFailed -> {
                // failed
                Unit
            }
        }.checkAllSealed
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
    fun openSessionDetail(id: String)
    fun toggleFilter(filter: TagFilter, enabled: Boolean)
    fun clearFilters()
}

//TODO(jalc) move somewhere else (b/74113562)
/**
 * Used as a wrapper for data that is exposed via a LiveData that represents an event.
 */
class Event<T>(private val content: T, private var hasBeenHandled: Boolean = false) {
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
