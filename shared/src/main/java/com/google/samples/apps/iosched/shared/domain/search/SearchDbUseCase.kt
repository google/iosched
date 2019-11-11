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

import com.google.samples.apps.iosched.shared.data.ConferenceDataRepository
import com.google.samples.apps.iosched.shared.data.db.AppDatabase
import com.google.samples.apps.iosched.shared.data.prefs.PreferenceStorage
import com.google.samples.apps.iosched.shared.data.prefs.UserIsAttendee.REMOTE
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.di.IoDispatcher
import com.google.samples.apps.iosched.shared.domain.UseCase
import com.google.samples.apps.iosched.shared.domain.search.Searchable.SearchedSession
import com.google.samples.apps.iosched.shared.domain.search.Searchable.SearchedSpeaker
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

private const val MAX_WORD_COUNT_QUERY = 5
/**
 * Performs a search in the database from a query string.
 */
class SearchDbUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val conferenceRepository: ConferenceDataRepository,
    private val appDatabase: AppDatabase,
    private val preferenceStorage: PreferenceStorage,
    @IoDispatcher ioDispatcher: CoroutineDispatcher
) : UseCase<String, List<Searchable>>(ioDispatcher) {

    override fun execute(parameters: String): List<Searchable> {
        // Add '*' the query for FTS with multiple tokens
        val query = if (parameters.isNotEmpty()) {
            parameters.split(", ", " ", ",").take(MAX_WORD_COUNT_QUERY).map {
                "${it.toLowerCase()}*"
            }.joinToString(" AND ")
        } else {
            parameters
        }

        val sessionResults = appDatabase.sessionFtsDao().searchAllSessions(query).toSet()
        val speakerResults = appDatabase.speakerFtsDao().searchAllSpeakers(query).toSet()
        // Show all events if userIsAttendee is IN_PERSON or NO_ANSWER (means dialog dismissed)
        val isOnsiteAttendee = preferenceStorage.userIsAttendee != REMOTE
        val searchedSessions = sessionRepository.getSessions(isOnsiteAttendee)
            .filter { session -> session.id in sessionResults }
            // Keynotes come first, followed by sessions, app reviews, game reviews ...
            .sortedBy { it.type }
            .map { SearchedSession(it) }
        val conferenceData = conferenceRepository.getOfflineConferenceData()
        val searchedSpeakers = conferenceData.speakers.filter {
            it.id in speakerResults
        }.map { SearchedSpeaker(it) }

        return searchedSessions.plus(searchedSpeakers)
    }
}
