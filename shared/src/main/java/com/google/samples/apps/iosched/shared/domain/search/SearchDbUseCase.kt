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
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.domain.UseCase
import com.google.samples.apps.iosched.shared.domain.search.Searchable.SearchedCodelab
import com.google.samples.apps.iosched.shared.domain.search.Searchable.SearchedSession
import com.google.samples.apps.iosched.shared.domain.search.Searchable.SearchedSpeaker
import javax.inject.Inject

/**
 * Performs a search in the database from a query string.
 */
class SearchDbUseCase @Inject constructor(
    private val sessionRepository: SessionRepository,
    private val conferenceRepository: ConferenceDataRepository,
    private val appDatabase: AppDatabase
) : UseCase<String, List<Searchable>>() {

    override fun execute(parameters: String): List<Searchable> {
        val query = parameters.toLowerCase()
        val sessionResults = appDatabase.sessionFtsDao().searchAllSessions(query).toSet()
        val speakerResults = appDatabase.speakerFtsDao().searchAllSpeakers(query).toSet()
        val codelabResults = appDatabase.codelabFtsDao().searchAllCodelabs(query).toSet()
        val searchedSessions = sessionRepository.getSessions()
            .filter { session -> session.id in sessionResults }
            // Keynotes come first, followed by sessions, app reviews, game reviews ...
            .sortedBy { it.type }
            .map { SearchedSession(it) }
        val conferenceData = conferenceRepository.getOfflineConferenceData()
        val searchedSpeakers = conferenceData.speakers.filter {
            it.id in speakerResults
        }.map { SearchedSpeaker(it) }
        val searchedCodelabs = conferenceData.codelabs.filter {
            it.id in codelabResults
        }.map { SearchedCodelab(it) }
        return searchedSessions.plus(searchedSpeakers).plus(searchedCodelabs)
    }
}
