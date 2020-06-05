/*
 * Copyright 2020 Google LLC
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

package com.google.samples.apps.iosched.shared.domain.search

import androidx.core.os.trace
import com.google.samples.apps.iosched.model.filters.Filter
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.data.userevent.SessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.di.IoDispatcher
import com.google.samples.apps.iosched.shared.domain.FlowUseCase
import com.google.samples.apps.iosched.shared.domain.filters.UserSessionFilterMatcher
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Error
import com.google.samples.apps.iosched.shared.result.Result.Loading
import com.google.samples.apps.iosched.shared.result.Result.Success
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

data class SessionSearchUseCaseParams(
    val userId: String?,
    val query: String,
    val filters: List<Filter>
)

class SessionSearchUseCase @Inject constructor(
    private val repository: SessionAndUserEventRepository,
    private val textMatchStrategy: SessionTextMatchStrategy,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : FlowUseCase<SessionSearchUseCaseParams, List<UserSession>>(dispatcher) {

    override fun execute(parameters: SessionSearchUseCaseParams): Flow<Result<List<UserSession>>> {
        val (userId, query, filters) = parameters
        trace("search-path-usecase") {
            val filterMatcher = UserSessionFilterMatcher(filters)
            return repository.getObservableUserEvents(userId).map { result ->
                when (result) {
                    is Success -> {
                        val searchResults = textMatchStrategy.searchSessions(
                            result.data.userSessions, query
                        ).filter { filterMatcher.matches(it) }
                        Success(searchResults)
                    }
                    is Loading -> result
                    is Error -> result
                }
            }
        }
    }
}
