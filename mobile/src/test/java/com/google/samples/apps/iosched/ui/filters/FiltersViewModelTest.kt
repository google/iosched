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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.Observer
import com.google.samples.apps.iosched.androidtest.util.observeForTesting
import com.google.samples.apps.iosched.model.Tag
import com.google.samples.apps.iosched.model.filters.Filter
import com.google.samples.apps.iosched.model.filters.Filter.MyScheduleFilter
import com.google.samples.apps.iosched.model.filters.Filter.TagFilter
import com.google.samples.apps.iosched.test.data.TestData
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class FiltersViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    private val tagFilters = TestData.tagsList.map { TagFilter(it) }
    private val androidFilter = TagFilter(TestData.androidTag)
    private val sessionsFilter = TagFilter(TestData.sessionsTag)

    private lateinit var viewModel: FiltersViewModelDelegate

    @Before
    fun setup() {
        viewModel = FiltersViewModelDelegateImpl()
        viewModel.setSupportedFilters(tagFilters)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `toggle unsupported filter throws exception`() {
        // Not one of the tag filters.
        viewModel.toggleFilter(MyScheduleFilter, true)
    }

    // Helper method for subsequent tests.
    private fun verifyLiveData(
        viewModel: FiltersViewModelDelegate,
        selectedFilters: List<Filter>,
        allFilters: List<Filter> = tagFilters
    ) {
        // Verify selected filters.
        viewModel.selectedFilters.observeForTesting {
            val selected = viewModel.selectedFilters.value!!
            assertThat(selected.size, `is`(selectedFilters.size))
            selected.forEach {
                assertThat(it in selectedFilters, `is`(true))
            }
        }
        // Verify selected filter chips.
        viewModel.selectedFilterChips.observeForTesting {
            val chips = viewModel.selectedFilterChips.value!!
            assertThat(chips.size, `is`(selectedFilters.size))
            // Verify each chip's filter is in the list.
            chips.forEach {
                assertThat(it.filter in selectedFilters, `is`(true))
            }
        }
        // Verify all filter chips.
        viewModel.filterChips.observeForTesting {
            val chips = viewModel.filterChips.value!!
            assertThat(chips.size, `is`(allFilters.size))
            // Verify chips are in order with correct selection state.
            chips.forEachIndexed { index, filterChip ->
                assertThat(filterChip.filter, `is`(equalTo(allFilters[index])))
                assertThat(filterChip.isSelected, `is`(filterChip.filter in selectedFilters))
            }
        }
    }

    @Test
    fun `setSupportedFilters() emits initial live data`() {
        verifyLiveData(viewModel, emptyList())
    }

    @Test
    fun `activate and deactivate filters updates live data`() {
        // Activate filters.
        viewModel.toggleFilter(androidFilter, true)
        viewModel.toggleFilter(sessionsFilter, true)
        verifyLiveData(viewModel, listOf(androidFilter, sessionsFilter))

        // Deactivate a filter.
        viewModel.toggleFilter(androidFilter, false)
        verifyLiveData(viewModel, listOf(sessionsFilter))
    }

    @Test
    fun `activate same filter does not emit a change`() {
        // Activate filter.
        viewModel.toggleFilter(androidFilter, true)

        var calls = 0
        val observer = Observer<List<Filter>> {
            calls++
        }
        viewModel.selectedFilters.observeForever(observer)
        // Verify observer called for current value.
        assertThat(calls, `is`(1))

        // Activate same filter.
        viewModel.toggleFilter(androidFilter, true)
        // Verify observer not called again.
        assertThat(calls, `is`(1))

        // Clean up.
        viewModel.selectedFilters.removeObserver(observer)
    }

    @Test
    fun `deactivate filter that isn't selected does not emit a change`() {
        // Activate filter.
        viewModel.toggleFilter(androidFilter, true)

        var calls = 0
        val observer = Observer<List<Filter>> {
            calls++
        }
        viewModel.selectedFilters.observeForever(observer)
        // Verify observer called for current value.
        assertThat(calls, `is`(1))

        // Deactivate some other filter.
        viewModel.toggleFilter(sessionsFilter, false)
        // Verify observer not called again.
        assertThat(calls, `is`(1))

        // Clean up.
        viewModel.selectedFilters.removeObserver(observer)
    }

    @Test
    fun `setSupportedFilters() removes orphaned selected filters`() {
        // Activate a topic filter and a type filter.
        viewModel.toggleFilter(androidFilter, true)
        viewModel.toggleFilter(sessionsFilter, true)
        verifyLiveData(viewModel, listOf(androidFilter, sessionsFilter))

        // Set supported filters to only topic tags.
        val topicTagFilters = tagFilters.filter { it.tag.category == Tag.CATEGORY_TOPIC }
        viewModel.setSupportedFilters(topicTagFilters)
        verifyLiveData(viewModel, listOf(androidFilter), allFilters = topicTagFilters)
    }
}
