/*
 * Copyright 2018 Google LLC
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
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.domain.UseCase
import timber.log.Timber
import javax.inject.Inject

/**
 * Performs a search from a query string.
 *
 * A session is returned in the results if the title, abstract, or tag matches the query parameter.
 */
class SearchUseCase @Inject constructor(
    private val repository: SessionRepository
) : UseCase<String, List<Session>>() {

    override fun execute(parameters: String): List<Session> {
        Timber.d("Performing a search for any sessions that contain `$parameters`")
        val query = parameters.toLowerCase()
        return repository.getSessions()
            .filter { session ->
                session.title.toLowerCase().contains(query) ||
                    session.abstract.toLowerCase().contains(query) ||
                    session.tags.any { tag -> query.contains(tag.name.toLowerCase()) }
            }
    }
}
