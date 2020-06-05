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
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.data.db.AppDatabase
import javax.inject.Inject

interface SessionTextMatchStrategy {
    suspend fun searchSessions(userSessions: List<UserSession>, query: String): List<UserSession>
}

/** Searches sessions by simple string comparison against title and description. */
object SimpleMatchStrategy : SessionTextMatchStrategy {

    override suspend fun searchSessions(
        userSessions: List<UserSession>,
        query: String
    ): List<UserSession> {
        trace("search-path-simplematchstrategy") {
            if (query.isEmpty()) {
                return userSessions
            }
            val lowercaseQuery = query.toLowerCase()
            return userSessions.filter {
                it.session.title.toLowerCase().contains(lowercaseQuery) ||
                    it.session.description.toLowerCase().contains(lowercaseQuery)
            }
        }
    }
}

/** Searches sessions using FTS. */
class FtsMatchStrategy @Inject constructor(
    private val appDatabase: AppDatabase
) : SessionTextMatchStrategy {

    override suspend fun searchSessions(
        userSessions: List<UserSession>,
        query: String
    ): List<UserSession> {
        trace("search-path-ftsmatchstrategy") {
            if (query.isEmpty()) {
                return userSessions
            }
            val sessionIds = trace("search-path-roomquery") {
                appDatabase.sessionFtsDao().searchAllSessions(query.toLowerCase()).toSet()
            }
            return userSessions.filter { it.session.id in sessionIds }
        }
    }
}
