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

package com.google.samples.apps.iosched.shared.domain.search

import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.shared.data.db.AppDatabase
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.domain.UseCase
import javax.inject.Inject

/**
 * Performs a search in the database from a query string.
 */
class SearchDbUseCase @Inject constructor(
    private val repository: SessionRepository,
    private val appDatabase: AppDatabase
) : UseCase<String, List<Session>>() {

    override fun execute(parameters: String): List<Session> {
        val query = parameters.toLowerCase()
        val results = appDatabase.sessionFtsDao().searchAllSessions(query).toSet()
        return repository.getSessions().filter { session -> session.id in results }
            // Keynotes come first, followed by sessions, app reviews, game reviews ...
            .sortedBy { it.type }
    }
}
