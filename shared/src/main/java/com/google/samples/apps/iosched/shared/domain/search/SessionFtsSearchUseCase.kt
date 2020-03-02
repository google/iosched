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

import com.google.samples.apps.iosched.shared.data.db.AppDatabase
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.di.IoDispatcher
import com.google.samples.apps.iosched.shared.domain.CoroutinesUseCase
import com.google.samples.apps.iosched.shared.domain.search.Searchable.SearchedSession
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Searches sessions using FTS.
 */
class SessionFtsSearchUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val appDatabase: AppDatabase,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : CoroutinesUseCase<String, List<Searchable>>(dispatcher) {

    override suspend fun execute(parameters: String): List<Searchable> {
        val query = parameters.toLowerCase()
        val sessionResults = appDatabase.sessionFtsDao().searchAllSessions(query).toSet()
        return sessionRepository.getSessions()
            .filter { session -> session.id in sessionResults }
            // Keynotes come first, followed by sessions, app reviews, game reviews ...
            .sortedBy { it.type }
            .map { SearchedSession(it) }
    }
}
