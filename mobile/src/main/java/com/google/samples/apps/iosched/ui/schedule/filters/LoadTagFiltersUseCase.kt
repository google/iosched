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

package com.google.samples.apps.iosched.ui.schedule.filters

import com.google.samples.apps.iosched.shared.data.tag.TagRepository
import com.google.samples.apps.iosched.shared.domain.UseCase
import com.google.samples.apps.iosched.shared.model.Tag
import com.google.samples.apps.iosched.shared.schedule.TagFilterMatcher
import com.google.samples.apps.iosched.util.isEven
import javax.inject.Inject

/**
 * Use case that loads a list of [TagFilter]s for the schedule filters UI.
 */
open class LoadTagFiltersUseCase @Inject constructor(
    private val tagRepository: TagRepository
) : UseCase<TagFilterMatcher, List<TagFilter>>() {

    companion object {
        // Used to filter out Tag categories and define which order to display them.
        private val FILTER_CATEGORIES = arrayOf(Tag.CATEGORY_TRACK, Tag.CATEGORY_TYPE)
    }

    override fun execute(parameters: TagFilterMatcher): List<TagFilter> {
        val tags = tagRepository.getTags()
        parameters.removeOrphanedTags(tags)

        return tags.filter { it.category in FILTER_CATEGORIES }
            // Only tags in these categories appear in the filters list
            // Map category -> List<TagFilter>
            .groupBy { it.category }
            // Sort entries in desired order
            .toSortedMap(compareBy { key -> FILTER_CATEGORIES.indexOf(key) })
            // Interleave items in each category. This makes them appear ordered vertically when
            // displayed in the 2-column grid.
            .mapValues { entry -> interleaveSort(entry.value) }
            // Flatten to a single list
            .flatMap { entry -> entry.value }
            // Convert to TagFilters, checking ones that are currently selected
            .map { TagFilter(it, it in parameters) }
    }

    fun interleaveSort(tags: List<Tag>): List<Tag> {
        // tags should all be the same category
        val sorted = tags.sortedBy { it.orderInCategory }
        val split = when {
            sorted.size.isEven() -> sorted.size / 2
            else -> sorted.size / 2 + 1
        }
        val newList = ArrayList<Tag>(sorted.size)
        for (i in 0 until split) {
            newList.add(sorted[i])
            if (i + split < sorted.size) {
                newList.add(sorted[i + split])
            }
        }
        return newList
    }
}
