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

import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.Tag
import com.google.samples.apps.iosched.shared.model.UserSession

/**
 * Discerns UserSessions that have [Tag]s satisfying a selected set of Tags as follows:
 * - selected Tags are grouped by their [category][Tag.category]
 * - for each category, the UserSession must contain at least one of those Tags
 */
class TagFilterMatcher : UserSessionMatcher {

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
     * For each category among selected [Tag]s, the [Session] must have at least one of those tags.
     */
    override fun matches(userSession: UserSession): Boolean {
        var match = true
        selectedTags.groupBy { it.category }.forEach { (_, tagsInCategory) ->
            if (userSession.session.tags.intersect(tagsInCategory).isEmpty()) {
                match = false
                return@forEach
            }
        }
        return match
    }
}
