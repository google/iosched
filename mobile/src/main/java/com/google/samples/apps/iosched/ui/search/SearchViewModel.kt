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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.domain.search.SearchUseCase
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.successOr
import com.google.samples.apps.iosched.shared.util.map
import timber.log.Timber
import javax.inject.Inject

class SearchViewModel @Inject constructor(
    private val analyticsHelper: AnalyticsHelper,
    private val loadSearchResultsUseCase: SearchUseCase
) : ViewModel(), SearchResultActionHandler {

    private val _navigateToSessionAction = MutableLiveData<Event<SessionId>>()
    val navigateToSessionAction: LiveData<Event<SessionId>>
        get() = _navigateToSessionAction

    private val loadSearchResults = MutableLiveData<Result<List<Session>>>()
    val searchResults: LiveData<List<SearchResult>>

    val isEmpty: LiveData<Boolean>

    init {
        searchResults = loadSearchResults.map {
            val result = it as? Result.Success ?: return@map emptyList<SearchResult>()
            result.data.map { session ->
                SearchResult(session.title, "Session", "session", session.id)
            }
        }

        isEmpty = loadSearchResults.map {
            it.successOr(null).isNullOrEmpty()
        }
    }

    override fun openSearchResult(searchResult: SearchResult) {
        if (searchResult.type == "session") {
            val sessionId = searchResult.objectId
            analyticsHelper.logUiEvent("Session: $sessionId", AnalyticsActions.SEARCH_RESULT_CLICK)
            _navigateToSessionAction.value = Event(sessionId)
        }
    }

    fun onScheduleSearchQuerySubmitted(query: String) {
        Timber.d("Searching for query: $query")
        analyticsHelper.logUiEvent("Query: $query", AnalyticsActions.SEARCH_QUERY_SUBMIT)
        loadSearchResultsUseCase(query, loadSearchResults)
    }
}
