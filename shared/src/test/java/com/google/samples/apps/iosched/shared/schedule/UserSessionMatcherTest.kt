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

import com.google.samples.apps.iosched.model.Tag
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.data.prefs.PreferenceStorage
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.data.TestData.androidTag
import com.google.samples.apps.iosched.test.data.TestData.codelabsTag
import com.google.samples.apps.iosched.test.data.TestData.session0
import com.google.samples.apps.iosched.test.data.TestData.sessionsTag
import com.google.samples.apps.iosched.test.data.TestData.webTag
import com.google.samples.apps.iosched.test.util.FakePreferenceStorage
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [UserSessionMatcher].
 */
class UserSessionMatcherTest {

    private val testJson = "{\"showPinnedEventsOnly\":true,\"tagsAndCategories\":[{\"id\":\"1\"," +
        "\"category\":\"topic\"},{\"id\":\"3\",\"category\":\"topic\"}]}"

    private var sessionMatcher = UserSessionMatcher()

    private fun createTestUserSession(vararg tags: Tag): UserSession {
        return UserSession(session0.copy(tags = tags.asList()), TestData.userEvents[0])
    }

    @Before
    fun createSessionFilters() {
        // Reset filters
        sessionMatcher = UserSessionMatcher()
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
        assertFalse(sessionMatcher.hasAnyFilters())
    }

    @Test
    fun add() {
        // Given empty filters
        assertFalse(sessionMatcher.hasAnyFilters())
        // Adding a tag -> no longer empty
        assertTrue(sessionMatcher.add(androidTag))
        assertTrue(sessionMatcher.hasAnyFilters())
        // Adding same tag -> no change
        assertFalse(sessionMatcher.add(androidTag))
        assertTrue(sessionMatcher.hasAnyFilters())
    }

    @Test
    fun remove() {
        // Given empty filters
        assertFalse(sessionMatcher.hasAnyFilters())
        // Remove non-existing tag -> no change
        assertFalse(sessionMatcher.remove(webTag))
        assertFalse(sessionMatcher.hasAnyFilters())

        // Add a tag -> no longer empty
        assertTrue(sessionMatcher.add(androidTag))
        assertTrue(sessionMatcher.hasAnyFilters())
        // Remove non-existing tag -> no change
        assertFalse(sessionMatcher.remove(webTag))
        assertTrue(sessionMatcher.hasAnyFilters())
        // Remove existing tag -> empty
        assertTrue(sessionMatcher.remove(androidTag))
        assertFalse(sessionMatcher.hasAnyFilters())
    }

    @Test
    fun addAll() {
        // Given empty filters
        assertFalse(sessionMatcher.hasAnyFilters())
        // Add multiple tags -> no longer empty
        sessionMatcher.addAll(androidTag, webTag)
        assertTrue(sessionMatcher.hasAnyFilters())

        // Remove one -> filters changed, but not empty
        assertTrue(sessionMatcher.remove(androidTag))
        assertTrue(sessionMatcher.hasAnyFilters())
        // Remove the other -> empty
        assertTrue(sessionMatcher.remove(webTag))
        assertFalse(sessionMatcher.hasAnyFilters())
    }

    @Test
    fun clearAll() {
        // Given empty filters
        assertFalse(sessionMatcher.hasAnyFilters())
        // Add multiple tags -> no longer empty
        sessionMatcher.addAll(androidTag, webTag)
        assertTrue(sessionMatcher.hasAnyFilters())
        // Clear all -> empty
        sessionMatcher.clearAll()
        assertFalse(sessionMatcher.hasAnyFilters())
    }

    @Test
    fun contains() {
        // Given empty filters
        assertFalse(sessionMatcher.hasAnyFilters())
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
        assertFalse(sessionMatcher.hasAnyFilters())
        // removeOrphanedTags -> no change
        sessionMatcher.removeOrphanedTags(listOf(androidTag, webTag, codelabsTag, sessionsTag))
        assertFalse(sessionMatcher.hasAnyFilters())
    }

    @Test
    fun removeOrphanedTags_removesMissingTagsOnly() {
        // Given empty filters
        assertFalse(sessionMatcher.hasAnyFilters())
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

    @Test
    fun userEventIsPinned_matches() {
        sessionMatcher.setShowPinnedEventsOnly(true)

        TestData.userEvents.forEach {
            val userSession = UserSession(session0, it)
            Assert.assertEquals(it.isStarredOrReserved(), sessionMatcher.matches(userSession))
        }
    }

    @Test
    fun load_emptyValue_noChange() {
        // Given empty filters
        assertFalse(sessionMatcher.hasAnyFilters())
        // Load from an empty PreferenceStorage
        sessionMatcher.load(FakePreferenceStorage())
        // No change
        assertFalse(sessionMatcher.hasAnyFilters())
    }

    @Test
    fun load_invalidJson_noChange() {
        // Given empty filters
        assertFalse(sessionMatcher.hasAnyFilters())
        // Load malformed json
        val storage = mock<PreferenceStorage> {
            on { selectedFilters }.doReturn("{foobar[")
        }
        sessionMatcher.load(storage)
        // No change
        assertFalse(sessionMatcher.hasAnyFilters())
    }

    @Test
    fun loadSavedFilters() {
        // Given empty filters
        assertFalse(sessionMatcher.hasAnyFilters())
        // Load from an non-empty PreferenceStorage
        val preferenceStorage = FakePreferenceStorage().apply {
            selectedFilters = testJson
        }
        sessionMatcher.load(preferenceStorage)
        // Filters were loaded
        assertTrue(sessionMatcher.getShowPinnedEventsOnly())
        assertTrue(sessionMatcher.contains(androidTag))
        assertTrue(sessionMatcher.contains(webTag))
    }

    @Test
    fun saveFilters() {
        // Apply some filters
        sessionMatcher.addAll(androidTag, webTag)
        sessionMatcher.setShowPinnedEventsOnly(true)
        // Save to preferences
        val preferenceStorage = FakePreferenceStorage()
        sessionMatcher.save(preferenceStorage)
        // Storage contains expected JSON
        assertEquals(testJson, preferenceStorage.selectedFilters)
    }
}
