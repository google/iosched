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

package com.google.samples.apps.iosched.ui.schedule

import com.google.samples.apps.iosched.shared.model.Tag

class SessionFilters {

    private val filteredTags = HashSet<Tag>()

    fun remove(tag: Tag) = filteredTags.remove(tag)

    fun add(vararg tags: Tag) {
        filteredTags.addAll(tags)
    }

    fun clearAll() = filteredTags.clear()

    fun getCategoryCount() = filteredTags.distinctBy { it.category }.size

    fun hasAnyFilters() = filteredTags.isNotEmpty()

    /**
     * There are different types of tag categories. Only filter if there are tags in each
     * category.
     */
    fun matchesSessionTags(sessionTags: List<Tag>): Boolean {
        var match = true
        filteredTags.groupBy { it.category }.forEach { category, tagsInCategory ->
            if (sessionTags.intersect(tagsInCategory).isEmpty()) {
                match = false
                return@forEach
            }
        }
        return match
    }
}