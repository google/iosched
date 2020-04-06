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

package com.google.samples.apps.iosched.ui.search

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.SpeakerId
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.di.SearchUsingRoomEnabledFlag
import com.google.samples.apps.iosched.shared.domain.search.SessionFtsSearchUseCase
import com.google.samples.apps.iosched.shared.domain.search.SessionSearchUseCaseParams
import com.google.samples.apps.iosched.shared.domain.search.SessionSimpleSearchUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.successOr
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.ui.filters.FiltersViewModelDelegate
import com.google.samples.apps.iosched.ui.sessioncommon.EventActions
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.threeten.bp.ZoneId
import javax.inject.Inject

class SearchViewModel @Inject constructor(
    private val analyticsHelper: AnalyticsHelper,
    simpleSearchUseCase: SessionSimpleSearchUseCase,
    ftsSearchUseCase: SessionFtsSearchUseCase,
    getTimeZoneUseCase: GetTimeZoneUseCase,
    signInViewModelDelegate: SignInViewModelDelegate,
    filtersViewModelDelegate: FiltersViewModelDelegate,
    @SearchUsingRoomEnabledFlag val searchUsingRoomFeatureEnabled: Boolean
) : ViewModel(),
    EventActions,
    SignInViewModelDelegate by signInViewModelDelegate,
    FiltersViewModelDelegate by filtersViewModelDelegate {

    private val _navigateToSessionAction = MutableLiveData<Event<SessionId>>()
    val navigateToSessionAction: LiveData<Event<SessionId>> = _navigateToSessionAction

    private val _navigateToSpeakerAction = MutableLiveData<Event<SpeakerId>>()
    val navigateToSpeakerAction: LiveData<Event<SpeakerId>> = _navigateToSpeakerAction

    // Has codelabUrl as String
    private val _navigateToCodelabAction = MutableLiveData<Event<String>>()
    val navigateToCodelabAction: LiveData<Event<String>> = _navigateToCodelabAction

    private val _searchResults = MediatorLiveData<List<UserSession>>()
    val searchResults: LiveData<List<UserSession>> = _searchResults

    private val _isEmpty = MediatorLiveData<Boolean>()
    val isEmpty: LiveData<Boolean> = _isEmpty

    private val searchUseCase = if (searchUsingRoomFeatureEnabled) {
        ftsSearchUseCase
    } else {
        simpleSearchUseCase
    }
    private var searchJob: Job? = null

    private val _timeZoneId = MutableLiveData<ZoneId>()
    val timeZoneId: LiveData<ZoneId>
        get() = _timeZoneId

    init {
        // Load timezone
        viewModelScope.launch {
            _timeZoneId.value = if (getTimeZoneUseCase(Unit).successOr(true)) {
                TimeUtils.CONFERENCE_TIMEZONE
            } else {
                ZoneId.systemDefault()
            }
        }
    }

    fun onSearchQueryChanged(newQuery: String) {
        if (newQuery.length < 2) {
            onQueryCleared()
            return
        }
        analyticsHelper.logUiEvent("Query: $newQuery", AnalyticsActions.SEARCH_QUERY_SUBMIT)
        executeSearch(newQuery)
    }

    private fun executeSearch(query: String) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            searchUseCase(SessionSearchUseCaseParams(getUserId(), query)).collect {
                processSearchResult(it)
            }
        }
    }

    private fun onQueryCleared() {
        _searchResults.value = emptyList()
        // Explicitly set false to not show the "No results" state
        _isEmpty.value = false
    }

    private fun processSearchResult(searchResult: Result<List<UserSession>>) {
        val sessions = searchResult.successOr(emptyList())
        _searchResults.value = sessions
        _isEmpty.value = sessions.isEmpty()
    }

    override fun openEventDetail(id: SessionId) {
        _navigateToSessionAction.value = Event(id)
    }

    override fun onStarClicked(userSession: UserSession) {
        // TODO(jdkoren) make an EventActionsViewModelDelegate that handles this for everyone
    }
}
