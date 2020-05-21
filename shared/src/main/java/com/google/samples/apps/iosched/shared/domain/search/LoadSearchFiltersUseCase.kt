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

import com.google.samples.apps.iosched.model.Tag
import com.google.samples.apps.iosched.model.filters.Filter
import com.google.samples.apps.iosched.model.filters.Filter.DateFilter
import com.google.samples.apps.iosched.model.filters.Filter.TagFilter
import com.google.samples.apps.iosched.shared.data.ConferenceDataRepository
import com.google.samples.apps.iosched.shared.data.tag.TagRepository
import com.google.samples.apps.iosched.shared.di.IoDispatcher
import com.google.samples.apps.iosched.shared.domain.UseCase
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

private val FILTER_CATEGORIES = listOf(
    Tag.CATEGORY_TYPE,
    Tag.CATEGORY_TOPIC,
    Tag.CATEGORY_LEVEL
)

/** Loads filters for the Search screen. */
class LoadSearchFiltersUseCase @Inject constructor(
    private val conferenceRepository: ConferenceDataRepository,
    private val tagRepository: TagRepository,
    @IoDispatcher dispatcher: CoroutineDispatcher
) : UseCase<Unit, List<Filter>>(dispatcher) {

    override suspend fun execute(parameters: Unit): List<Filter> {
        val filters = mutableListOf<Filter>()
        filters.addAll(conferenceRepository.getConferenceDays().map { DateFilter(it) })
        filters.addAll(tagRepository.getTags()
            .filter { it.category in FILTER_CATEGORIES }
            .sortedWith(
                compareBy({ FILTER_CATEGORIES.indexOf(it.category) }, { it.orderInCategory })
            )
            .map { TagFilter(it) }
        )
        return filters
    }
}
