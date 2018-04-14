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

import com.google.samples.apps.iosched.model.TestData
import com.google.samples.apps.iosched.model.TestData.androidTag
import com.google.samples.apps.iosched.model.TestData.cloudTag
import com.google.samples.apps.iosched.model.TestData.webTag
import com.google.samples.apps.iosched.model.TestDataRepository
import com.google.samples.apps.iosched.shared.data.tag.TagRepository
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.ui.schedule.filters.EventFilter.MyEventsFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoadTagFiltersUseCaseTest {

    @Test
    fun interleaveSort() {
        // Given unordered tags with same category
        val testList = listOf(webTag, cloudTag, androidTag)
        val expected = listOf(androidTag, webTag, cloudTag)

        val useCase = LoadTagFiltersUseCase(TagRepository(TestDataRepository))

        // Items are sorted and interleaved
        assertEquals(expected, useCase.interleaveSort(testList))
    }

    @Test
    fun loadsTagFilters() {
        val useCase = LoadTagFiltersUseCase(TagRepository(TestDataRepository))
        val result = useCase.executeNow(UserSessionMatcher()) as Success

        assertTrue(result.data[0] is MyEventsFilter)
        assertTrue(result.data.containsAll(TestData.tagFiltersList))
    }
}
