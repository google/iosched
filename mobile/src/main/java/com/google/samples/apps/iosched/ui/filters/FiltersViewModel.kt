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

import com.google.samples.apps.iosched.model.filters.Filter
import com.google.samples.apps.iosched.util.compatRemoveIf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Interface to add filters functionality to a screen through a ViewModel.
 */
interface FiltersViewModelDelegate {
    /** The full list of filter chips. */
    val filterChips: Flow<List<FilterChip>>
    /** The list of selected filters. */
    val selectedFilters: StateFlow<List<Filter>>
    /** The list of selected filter chips. */
    val selectedFilterChips: StateFlow<List<FilterChip>>
    /** True if there are any selected filters. */
    val hasAnyFilters: StateFlow<Boolean>
    /** Number of results from applying filters. Can be set by implementers. */
    val resultCount: MutableStateFlow<Int>
    /** Whether to show the result count instead of the "Filters" header. */
    val showResultCount: StateFlow<Boolean>

    /** Set the list of filters. */
    fun setSupportedFilters(filters: List<Filter>)

    /** Set the selected state of the filter. Must be one of the supported filters. */
    fun toggleFilter(filter: Filter, enabled: Boolean)

    /** Clear all selected filters. */
    fun clearFilters()
}

class FiltersViewModelDelegateImpl(
    externalScope: CoroutineScope
) : FiltersViewModelDelegate {

    private val _filterChips = MutableStateFlow<List<FilterChip>>(emptyList())
    override val filterChips: Flow<List<FilterChip>> = _filterChips

    private val _selectedFilters = MutableStateFlow<List<Filter>>(emptyList())
    override val selectedFilters: StateFlow<List<Filter>> = _selectedFilters

    private val _selectedFilterChips = MutableStateFlow<List<FilterChip>>(emptyList())
    override val selectedFilterChips: StateFlow<List<FilterChip>> = _selectedFilterChips

    override val hasAnyFilters = selectedFilterChips
        .map { it.isNotEmpty() }
        .stateIn(externalScope, SharingStarted.Lazily, false)

    override val resultCount = MutableStateFlow(0)

    // Default behavior: show count when there are active filters.
    override val showResultCount = hasAnyFilters

    // State for internal logic
    private var _filters = mutableListOf<Filter>()
    private val _selectedFiltersList = mutableSetOf<Filter>()
    private var _filterChipsList = mutableListOf<FilterChip>()
    private var _selectedFilterChipsList = mutableListOf<FilterChip>()

    override fun setSupportedFilters(filters: List<Filter>) {
        // Remove orphaned filters
        val selectedChanged = _selectedFiltersList.compatRemoveIf { it !in filters }
        _filters = filters.toMutableList()
        _filterChipsList = _filters.mapTo(mutableListOf()) {
            it.asChip(it in _selectedFiltersList)
        }

        if (selectedChanged) {
            _selectedFilterChipsList = _filterChipsList.filterTo(mutableListOf()) { it.isSelected }
        }
        publish(selectedChanged)
    }

    private fun publish(selectedChanged: Boolean) {
        _filterChips.value = _filterChipsList
        if (selectedChanged) {
            _selectedFilters.value = _selectedFiltersList.toList()
            _selectedFilterChips.value = _selectedFilterChipsList
        }
    }

    override fun toggleFilter(filter: Filter, enabled: Boolean) {
        if (filter !in _filters) {
            throw IllegalArgumentException("Unsupported filter: $filter")
        }
        val changed = if (enabled) {
            _selectedFiltersList.add(filter)
        } else {
            _selectedFiltersList.remove(filter)
        }
        if (changed) {
            _selectedFilterChipsList =
                _selectedFiltersList.mapTo(mutableListOf()) { it.asChip(true) }
            val index = _filterChipsList.indexOfFirst { it.filter == filter }
            _filterChipsList[index] = filter.asChip(enabled)

            publish(true)
        }
    }

    override fun clearFilters() {
        if (_selectedFiltersList.isNotEmpty()) {
            _selectedFiltersList.clear()
            _selectedFilterChipsList.clear()
            _filterChipsList = _filterChipsList.mapTo(mutableListOf()) {
                if (it.isSelected) it.copy(isSelected = false) else it
            }

            publish(true)
        }
    }
}
