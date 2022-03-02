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

package com.google.samples.apps.iosched.ui.filters

import com.google.samples.apps.iosched.model.Tag
import com.google.samples.apps.iosched.model.filters.Filter
import com.google.samples.apps.iosched.model.filters.Filter.MyScheduleFilter
import com.google.samples.apps.iosched.model.filters.Filter.TagFilter
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FiltersViewModelTest {

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val tagFilters = TestData.tagsList.map { TagFilter(it) }
    private val androidFilter = TagFilter(TestData.androidTag)
    private val sessionsFilter = TagFilter(TestData.sessionsTag)

    private lateinit var viewModel: FiltersViewModelDelegate

    @Before
    fun setup() {
        viewModel = FiltersViewModelDelegateImpl(CoroutineScope(coroutineRule.testDispatcher))
        viewModel.setSupportedFilters(tagFilters)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `toggle unsupported filter throws exception`() {
        // Not one of the tag filters.
        viewModel.toggleFilter(MyScheduleFilter, true)
    }

    // Helper method for subsequent tests.
    private suspend fun verifyFlowEmissions(
        viewModel: FiltersViewModelDelegate,
        selectedFilters: List<Filter>,
        allFilters: List<Filter> = tagFilters
    ) {
        // Verify selected filters.
        val selectedFiltersResult = viewModel.selectedFilters.first()
        assertEquals(selectedFiltersResult.size, selectedFilters.size)
        selectedFiltersResult.forEach {
            assertTrue(it in selectedFilters)
        }

        // Verify selected filter chips.
        val selectedFilterChipsResult = viewModel.selectedFilterChips.first()
        assertEquals(selectedFilterChipsResult.size, selectedFilters.size)
        selectedFilterChipsResult.forEach {
            assertTrue(it.filter in selectedFilters)
        }

        // Verify all filter chips.
        val filterChipsResult = viewModel.filterChips.first()
        assertEquals(filterChipsResult.size, allFilters.size)
        filterChipsResult.forEachIndexed { index, filterChip ->
            assertEquals(filterChip.filter, allFilters[index])
            assertEquals(filterChip.isSelected, filterChip.filter in selectedFilters)
        }
    }

    @Test
    fun `setSupportedFilters() emits default values`() = runTest {
        verifyFlowEmissions(viewModel, emptyList())
    }

    @Test
    fun `activate and deactivate filters updates flows`() = runTest {
        // Activate filters.
        viewModel.toggleFilter(androidFilter, true)
        viewModel.toggleFilter(sessionsFilter, true)
        verifyFlowEmissions(viewModel, listOf(androidFilter, sessionsFilter))

        // Deactivate a filter.
        viewModel.toggleFilter(androidFilter, false)
        verifyFlowEmissions(viewModel, listOf(sessionsFilter))
    }

    @Test
    fun `activate same filter does not emit a change`() = runTest(UnconfinedTestDispatcher()) {
        // Activate filter.
        viewModel.toggleFilter(androidFilter, true)

        var calls = 0
        val collectionJob = launch {
            viewModel.selectedFilters.collect {
                calls++
            }
        }
        assertEquals(calls, 1)

        // Activate same filter.
        viewModel.toggleFilter(androidFilter, true)

        // Verify collector not called again.
        assertEquals(calls, 1)

        collectionJob.cancel()
    }

    @Test
    fun `deactivate filter that isn't selected does not emit a change`() =
        runTest(UnconfinedTestDispatcher()) {
            // Activate filter.
            viewModel.toggleFilter(androidFilter, true)

            var calls = 0
            val collectionJob = launch {
                viewModel.selectedFilters.collect {
                    calls++
                }
            }
            // Verify collector called for current value.
            assertEquals(calls, 1)

            // Deactivate some other filter.
            viewModel.toggleFilter(sessionsFilter, false)

            // Verify collector not called again.
            assertEquals(calls, 1)

            collectionJob.cancel()
        }

    @Test
    fun `setSupportedFilters() removes orphaned selected filters`() =
        runTest {
            // Activate a topic filter and a type filter.
            viewModel.toggleFilter(androidFilter, true)
            viewModel.toggleFilter(sessionsFilter, true)
            verifyFlowEmissions(viewModel, listOf(androidFilter, sessionsFilter))

            // Set supported filters to only topic tags.
            val topicTagFilters = tagFilters.filter { it.tag.category == Tag.CATEGORY_TOPIC }
            viewModel.setSupportedFilters(topicTagFilters)
            verifyFlowEmissions(viewModel, listOf(androidFilter), allFilters = topicTagFilters)
        }
}
