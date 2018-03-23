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
import com.google.samples.apps.iosched.shared.model.TestData
import com.google.samples.apps.iosched.shared.model.TestData.androidTag
import com.google.samples.apps.iosched.shared.model.TestData.codelabsTag
import com.google.samples.apps.iosched.shared.model.TestData.session0
import com.google.samples.apps.iosched.shared.model.TestData.sessionsTag
import com.google.samples.apps.iosched.shared.model.TestData.webTag
import com.google.samples.apps.iosched.shared.model.UserSession
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TagFilterMatcher].
 */
class TagFilterMatcherTest {

    private var sessionMatcher = TagFilterMatcher()

    private fun createTestUserSession(vararg tags: Tag): UserSession {
        return UserSession(session0.copy(tags = tags.asList()), TestData.userEvents[0])
    }

    @Before
    fun createSessionFilters() {
        // Reset filters
        sessionMatcher = TagFilterMatcher()
    }

    @Test
    fun oneCategory_sameFilters_match() {
        // When the user is requesting two tags
        sessionMatcher.addAll(androidTag, webTag)

        // And the session contains those two tags
        val session = createTestUserSession(androidTag, webTag)

        // There's a match
        assertTrue(sessionMatcher.matches(session))
    }

    @Test
    fun oneCategory_subset_match() {
        // When the user is requesting one tag
        sessionMatcher.add(androidTag)

        // And the session contains that tag
        val session = createTestUserSession(androidTag, webTag)

        // There's a match
        assertTrue(sessionMatcher.matches(session))
    }

    @Test
    fun oneCategory_superset_match() {
        // When the user is requesting two tags from the same category
        sessionMatcher.addAll(androidTag, webTag)

        // And the session only contains one
        val session = createTestUserSession(androidTag)

        // There's a match
        assertTrue(sessionMatcher.matches(session))
    }

    @Test
    fun multipleCategories_partialMatch_noMatch() {
        // When the user is requesting two tags from different categories
        sessionMatcher.addAll(androidTag, codelabsTag)

        // And the session only contains one of them
        val session = createTestUserSession(androidTag, sessionsTag)

        // There's no match
        assertFalse(sessionMatcher.matches(session))
    }

    @Test
    fun multipleCategories_extraCategoryNotPresentInSession_noMatch() {
        // When the user is requesting two tags from different categories
        sessionMatcher.addAll(androidTag, codelabsTag)

        // And the session only contains one of them and no category for the other
        val session = createTestUserSession(androidTag)

        // There's no match
        assertFalse(sessionMatcher.matches(session))
    }

    @Test
    fun emptyfilters_match() {
        // When the user has not chosen any filters
        sessionMatcher.clearAll()

        // Given all combinations
        val noTags = createTestUserSession()
        val oneTag = createTestUserSession(androidTag)
        val twoTagsSameCat = createTestUserSession(androidTag, webTag)
        val twoTagsDiffCat = createTestUserSession(androidTag, codelabsTag)

        // They all match
        assertTrue(sessionMatcher.matches(noTags))
        assertTrue(sessionMatcher.matches(oneTag))
        assertTrue(sessionMatcher.matches(twoTagsSameCat))
        assertTrue(sessionMatcher.matches(twoTagsDiffCat))
    }

    @Test
    fun newInstance_isEmpty() {
        // New instance should be empty
        assertTrue(sessionMatcher.isEmpty())
    }

    @Test
    fun add() {
        // Given empty filters
        assertTrue(sessionMatcher.isEmpty())
        // Adding a tag -> no longer empty
        assertTrue(sessionMatcher.add(androidTag))
        assertFalse(sessionMatcher.isEmpty())
        // Adding same tag -> no change
        assertFalse(sessionMatcher.add(androidTag))
        assertFalse(sessionMatcher.isEmpty())
    }

    @Test
    fun remove() {
        // Given empty filters
        assertTrue(sessionMatcher.isEmpty())
        // Remove non-existing tag -> no change
        assertFalse(sessionMatcher.remove(webTag))
        assertTrue(sessionMatcher.isEmpty())

        // Add a tag -> no longer empty
        assertTrue(sessionMatcher.add(androidTag))
        assertFalse(sessionMatcher.isEmpty())
        // Remove non-existing tag -> no change
        assertFalse(sessionMatcher.remove(webTag))
        assertFalse(sessionMatcher.isEmpty())
        // Remove existing tag -> empty
        assertTrue(sessionMatcher.remove(androidTag))
        assertTrue(sessionMatcher.isEmpty())
    }

    @Test
    fun addAll() {
        // Given empty filters
        assertTrue(sessionMatcher.isEmpty())
        // Add multiple tags -> no longer empty
        sessionMatcher.addAll(androidTag, webTag)
        assertFalse(sessionMatcher.isEmpty())

        // Remove one -> filters changed, but not empty
        assertTrue(sessionMatcher.remove(androidTag))
        assertFalse(sessionMatcher.isEmpty())
        // Remove the other -> empty
        assertTrue(sessionMatcher.remove(webTag))
        assertTrue(sessionMatcher.isEmpty())
    }

    @Test
    fun clearAll() {
        // Given empty filters
        assertTrue(sessionMatcher.isEmpty())
        // Add multiple tags -> no longer empty
        sessionMatcher.addAll(androidTag, webTag)
        assertFalse(sessionMatcher.isEmpty())
        // Clear all -> empty
        sessionMatcher.clearAll()
        assertTrue(sessionMatcher.isEmpty())
    }

    @Test
    fun contains() {
        // Given empty filters
        assertTrue(sessionMatcher.isEmpty())
        // Does not contain
        assertFalse(sessionMatcher.contains(androidTag))
        // Add tag
        assertTrue(sessionMatcher.add(androidTag))
        // Contains the tag
        assertTrue(sessionMatcher.contains(androidTag))
        // Does not contain some other tag
        assertFalse(sessionMatcher.contains(webTag))
    }

    @Test
    fun removeOrphanedTags_noChangeIfEmpty() {
        // Given empty filters
        assertTrue(sessionMatcher.isEmpty())
        // removeOrphanedTags -> no change
        sessionMatcher.removeOrphanedTags(listOf(androidTag, webTag, codelabsTag, sessionsTag))
        assertTrue(sessionMatcher.isEmpty())
    }

    @Test
    fun removeOrphanedTags_removesMissingTagsOnly() {
        // Given empty filters
        assertTrue(sessionMatcher.isEmpty())
        // Add tags
        sessionMatcher.addAll(androidTag, sessionsTag)
        assertTrue(sessionMatcher.contains(androidTag))
        assertTrue(sessionMatcher.contains(sessionsTag))

        // removeOrphanedTags, androidTag excluded from list
        sessionMatcher.removeOrphanedTags(listOf(webTag, codelabsTag, sessionsTag))

        // This tag was removed
        assertFalse(sessionMatcher.contains(androidTag))
        // But this tag remains
        assertTrue(sessionMatcher.contains(sessionsTag))
    }
}
