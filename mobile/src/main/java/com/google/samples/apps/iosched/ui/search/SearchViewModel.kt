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

import androidx.core.os.trace
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.SpeakerId
import com.google.samples.apps.iosched.model.filters.Filter
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.domain.search.LoadSearchFiltersUseCase
import com.google.samples.apps.iosched.shared.domain.search.SessionSearchUseCase
import com.google.samples.apps.iosched.shared.domain.search.SessionSearchUseCaseParams
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Loading
import com.google.samples.apps.iosched.shared.result.successOr
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.ui.filters.FiltersViewModelDelegate
import com.google.samples.apps.iosched.ui.sessioncommon.EventActions
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.threeten.bp.ZoneId
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val analyticsHelper: AnalyticsHelper,
    private val searchUseCase: SessionSearchUseCase,
    getTimeZoneUseCase: GetTimeZoneUseCase,
    loadFiltersUseCase: LoadSearchFiltersUseCase,
    signInViewModelDelegate: SignInViewModelDelegate,
    filtersViewModelDelegate: FiltersViewModelDelegate
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

    private var searchJob: Job? = null

    private val _timeZoneId = MutableLiveData<ZoneId>()
    val timeZoneId: LiveData<ZoneId>
        get() = _timeZoneId

    private var textQuery = ""
    private val selectedFiltersObserver = Observer<List<Filter>> {
        executeSearch()
    }
    private val currentUserObserver = Observer<AuthenticatedUserInfo?> {
        executeSearch()
    }

    // Override because we also want to show result count when there's a text query.
    override val showResultCount = MutableLiveData(false)

    init {
        // Load timezone
        viewModelScope.launch {
            _timeZoneId.value = if (getTimeZoneUseCase(Unit).successOr(true)) {
                TimeUtils.CONFERENCE_TIMEZONE
            } else {
                ZoneId.systemDefault()
            }
        }
        // Load filters
        viewModelScope.launch {
            setSupportedFilters(loadFiltersUseCase(Unit).successOr(emptyList()))
        }
        // Re-execute search when selected filters change
        selectedFilters.observeForever(selectedFiltersObserver)
        // Re-execute search when signed in user changes.
        // Required because we show star / reservation status.
        currentUserInfo.observeForever(currentUserObserver)
    }

    override fun onCleared() {
        super.onCleared()
        selectedFilters.removeObserver(selectedFiltersObserver)
        currentUserInfo.removeObserver(currentUserObserver)
    }

    fun onSearchQueryChanged(query: String) {
        val newQuery = query.trim().takeIf { it.length >= 2 } ?: ""
        if (textQuery != newQuery) {
            textQuery = newQuery
            analyticsHelper.logUiEvent("Query: $newQuery", AnalyticsActions.SEARCH_QUERY_SUBMIT)
            executeSearch()
        }
    }

    private fun executeSearch() {
        // Cancel any in-flight searches
        searchJob?.cancel()

        val filters = selectedFilters.value ?: emptyList()
        if (textQuery.isEmpty() && filters.isEmpty()) {
            clearSearchResults()
            return
        }

        searchJob = viewModelScope.launch {
            // The user could be typing or toggling filters rapidly. Giving the search job
            // a slight delay and cancelling it on each call to this method effectively debounces.
            delay(500)
            trace("search-path-viewmodel") {
                searchUseCase(
                    SessionSearchUseCaseParams(getUserId(), textQuery, filters)
                ).collect {
                    processSearchResult(it)
                }
            }
        }
    }

    private fun clearSearchResults() {
        _searchResults.value = emptyList()
        // Explicitly set false to not show the "No results" state
        _isEmpty.value = false
        showResultCount.value = false
        resultCount.value = 0
    }

    private fun processSearchResult(searchResult: Result<List<UserSession>>) {
        if (searchResult is Loading) {
            return // avoids UI flickering
        }
        val sessions = searchResult.successOr(emptyList())
        _searchResults.value = sessions
        _isEmpty.value = sessions.isEmpty()
        showResultCount.value = true
        resultCount.value = sessions.size
    }

    override fun openEventDetail(id: SessionId) {
        _navigateToSessionAction.value = Event(id)
    }

    override fun onStarClicked(userSession: UserSession) {
        // TODO(jdkoren) make an EventActionsViewModelDelegate that handles this for everyone
    }
}
