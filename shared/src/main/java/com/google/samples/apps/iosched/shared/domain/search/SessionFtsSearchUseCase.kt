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

import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.data.db.AppDatabase
import com.google.samples.apps.iosched.shared.data.userevent.SessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.di.IoDispatcher
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Error
import com.google.samples.apps.iosched.shared.result.Result.Loading
import com.google.samples.apps.iosched.shared.result.Result.Success
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

/**
 * Searches sessions using FTS.
 */
class SessionFtsSearchUseCase @Inject constructor(
    private val repository: SessionAndUserEventRepository,
    private val appDatabase: AppDatabase,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : SessionSearchUseCase(dispatcher) {

    override fun execute(parameters: SessionSearchUseCaseParams): Flow<Result<List<UserSession>>> {
        val query = parameters.query.toLowerCase()
        Timber.d("Searching for query using Room: $query")
        return repository.getObservableUserEvents(parameters.userId).map { result ->
            when (result) {
                is Success -> {
                    val sessionIds =
                        appDatabase.sessionFtsDao().searchAllSessions(query).toSet()
                    val matches = result.data.userSessions.filter {
                        it.session.id in sessionIds
                    }
                        // Keynotes come first, then sessions, app reviews, game reviews ...
                        .sortedBy { it.session.type }
                    Success(matches)
                }
                is Loading -> result
                is Error -> result
            }
        }
    }
}
