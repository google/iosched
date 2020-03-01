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

import com.google.samples.apps.iosched.model.filters.Filter.DateFilter
import com.google.samples.apps.iosched.model.filters.Filter.TagFilter
import com.google.samples.apps.iosched.shared.data.tag.TagRepository
import com.google.samples.apps.iosched.shared.model.TestDataRepository
import com.google.samples.apps.iosched.shared.result.successOr
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.data.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [LoadSearchFiltersUseCase]
 */
class LoadSearchFiltersUseCaseTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun `returns ordered filters`() = coroutineRule.runBlockingTest {
        val useCase = LoadSearchFiltersUseCase(
            TestDataRepository,
            TagRepository(TestDataRepository),
            coroutineRule.testDispatcher)
        val filters = useCase(Unit).successOr(emptyList()) // empty list will fail assert

        // Expected values to assert
        val expected = listOf(
            DateFilter(TestData.TestConferenceDays[0]),
            DateFilter(TestData.TestConferenceDays[1]),
            DateFilter(TestData.TestConferenceDays[2]),
            TagFilter(TestData.sessionsTag),
            TagFilter(TestData.codelabsTag),
            TagFilter(TestData.androidTag),
            TagFilter(TestData.cloudTag),
            TagFilter(TestData.webTag),
            TagFilter(TestData.beginnerTag),
            TagFilter(TestData.intermediateTag),
            TagFilter(TestData.advancedTag)
            // no TestData.themeTag
        )

        assertThat(filters, `is`(equalTo(expected)))
    }
}
