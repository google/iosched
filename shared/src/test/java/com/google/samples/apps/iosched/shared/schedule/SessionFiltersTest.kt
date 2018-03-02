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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [SessionFilters].
 */
class SessionFiltersTest {

    private val androidTag = Tag("1", "TRACK", 0, "Android", 0xFFAED581.toInt())
    private val webTag = Tag("2", "TRACK", 1, "Web", 0xFFFFF176.toInt())
    private val sessionsTag = Tag("101", "TYPE", 0, "Sessions", 0)
    private val codelabsTag = Tag("102", "TYPE", 1, "Codelabs", 0)

    private var sessionFilters = SessionFilters()

    @Before
    fun createSessionFilters() {
        // Reset filters
        sessionFilters = SessionFilters()
    }

    @Test
    fun oneCategory_sameFilters_match() {
        // When the user is requesting two tags
        sessionFilters.add(androidTag, webTag)

        // And the session contains those two tags
        val sessionTags = listOf(androidTag, webTag)

        // There's a match
        assertTrue(sessionFilters.matchesSessionTags(sessionTags))
    }

    @Test
    fun oneCategory_subset_match() {
        // When the user is requesting one tag
        sessionFilters.add(androidTag)

        // And the session contains that tag
        val sessionTags = listOf(androidTag, webTag)

        // There's a match
        assertTrue(sessionFilters.matchesSessionTags(sessionTags))
    }

    @Test
    fun oneCategory_superset_match() {
        // When the user is requesting two tags from the same category
        sessionFilters.add(androidTag, webTag)

        // And the session only contains one
        val sessionTags = listOf(androidTag)

        // There's a match
        assertTrue(sessionFilters.matchesSessionTags(sessionTags))
    }

    @Test
    fun multipleCategories_partialMatch_noMatch() {
        // When the user is requesting two tags from different categories
        sessionFilters.add(androidTag, codelabsTag)

        // And the session only contains one of them
        val sessionTags = listOf(androidTag, sessionsTag)

        // There's no match
        assertFalse(sessionFilters.matchesSessionTags(sessionTags))
    }

    @Test
    fun multipleCategories_extraCategoryNotPresentInSession_noMatch() {
        // When the user is requesting two tags from different categories
        sessionFilters.add(androidTag, codelabsTag)

        // And the session only contains one of them and no category for the other
        val sessionTags = listOf(androidTag)

        // There's no match
        assertFalse(sessionFilters.matchesSessionTags(sessionTags))
    }

    @Test
    fun emptyfilters_match() {
        // When the user has not chosen any filters
        sessionFilters.clearAll()

        // Given all combinations
        val noTags = emptyList<Tag>()
        val oneTag = listOf(androidTag)
        val twoTagsSameCat = listOf(androidTag, webTag)
        val twoTagsDiffCat = listOf(androidTag, codelabsTag)

        // They all match
        assertTrue(sessionFilters.matchesSessionTags(noTags))
        assertTrue(sessionFilters.matchesSessionTags(oneTag))
        assertTrue(sessionFilters.matchesSessionTags(twoTagsSameCat))
        assertTrue(sessionFilters.matchesSessionTags(twoTagsDiffCat))
    }
}