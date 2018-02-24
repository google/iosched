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

package com.google.samples.apps.iosched.shared.schedule

import com.google.samples.apps.iosched.shared.model.Tag

class SessionMatcher {

    private val selectedTags = HashSet<Tag>()

    fun add(tag: Tag) = selectedTags.add(tag)

    fun remove(tag: Tag) = selectedTags.remove(tag)

    /** Returns true if the set of filtered tags changed. */
    fun addAll(vararg tags: Tag): Boolean {
        var changed = false
        tags.forEach {
            changed = changed or add(it)
        }
        return changed
    }

    /** Returns true if the set of filtered tags changed. */
    fun clearAll(): Boolean {
        if (selectedTags.isEmpty()) {
            return false
        }
        selectedTags.clear()
        return true
    }

    fun isEmpty() = selectedTags.isEmpty()

    operator fun contains(tag: Tag) = selectedTags.contains(tag)

    /**
     * Remove any tags that aren't in [newTags]. Call this whenever a new list of Tags is loaded in
     * order to reconcile the currently selected filters.
     */
    fun removeOrphanedTags(newTags: List<Tag>) {
        if (newTags.isEmpty()) {
            selectedTags.clear()
        } else {
            selectedTags.removeAll(selectedTags.subtract(newTags))
        }
    }

    /**
     * There are different types of tag categories. Only filter if there are tags in each
     * category.
     */
    fun matchesSessionTags(sessionTags: List<Tag>): Boolean {
        var match = true
        selectedTags.groupBy { it.category }.forEach { (_, tagsInCategory) ->
            if (sessionTags.intersect(tagsInCategory).isEmpty()) {
                match = false
                return@forEach
            }
        }
        return match
    }
}
