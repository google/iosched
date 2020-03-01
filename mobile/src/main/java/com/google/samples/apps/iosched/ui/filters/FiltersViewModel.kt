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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import com.google.samples.apps.iosched.model.filters.Filter
import com.google.samples.apps.iosched.util.compatRemoveIf

/**
 * Interface to add filters functionality to a screen through a ViewModel.
 */
interface FiltersViewModelDelegate {
    /** The full list of filter chips. */
    val filterChips: LiveData<List<FilterChip>>
    /** The list of selected filters. */
    val selectedFilters: LiveData<List<Filter>>
    /** The list of selected filter chips. */
    val selectedFilterChips: LiveData<List<FilterChip>>
    /** True if there are any selected filters. */
    val hasAnyFilters: LiveData<Boolean>
    /** Number of results from applying filters. Can be set by implementers. */
    val resultCount: MutableLiveData<Int>
    /** Whether to show the result count instead of the "Filters" header. */
    val showResultCount: LiveData<Boolean>

    /** Set the list of filters. */
    fun setSupportedFilters(filters: List<Filter>)

    /** Set the selected state of the filter. Must be one of the supported filters. */
    fun toggleFilter(filter: Filter, enabled: Boolean)

    /** Clear all selected filters. */
    fun clearFilters()
}

class FiltersViewModelDelegateImpl : FiltersViewModelDelegate {

    override val filterChips = MutableLiveData<List<FilterChip>>(emptyList())

    override val selectedFilters = MutableLiveData<List<Filter>>(emptyList())

    override val selectedFilterChips = MutableLiveData<List<FilterChip>>(emptyList())

    override val hasAnyFilters = selectedFilterChips.map { it.isNotEmpty() }

    override val resultCount = MutableLiveData(0)

    // Default behavior: show count when there are active filters.
    override val showResultCount = hasAnyFilters

    // State for internal logic
    private var _filters = mutableListOf<Filter>()
    private val _selectedFilters = mutableSetOf<Filter>()
    private var _filterChips = mutableListOf<FilterChip>()
    private var _selectedFilterChips = mutableListOf<FilterChip>()

    override fun setSupportedFilters(filters: List<Filter>) {
        // Remove orphaned filters
        val selectedChanged = _selectedFilters.compatRemoveIf { it !in filters }
        _filters = filters.toMutableList()
        _filterChips = _filters.mapTo(mutableListOf()) {
            it.asChip(it in _selectedFilters)
        }

        if (selectedChanged) {
            _selectedFilterChips = _filterChips.filterTo(mutableListOf()) { it.isSelected }
        }
        publish(selectedChanged)
    }

    private fun publish(selectedChanged: Boolean) {
        filterChips.value = _filterChips
        if (selectedChanged) {
            selectedFilters.value = _selectedFilters.toList()
            selectedFilterChips.value = _selectedFilterChips
        }
    }

    override fun toggleFilter(filter: Filter, enabled: Boolean) {
        if (filter !in _filters) {
            throw IllegalArgumentException("Unsupported filter: $filter")
        }
        val changed = if (enabled) _selectedFilters.add(filter) else _selectedFilters.remove(filter)
        if (changed) {
            _selectedFilterChips = _selectedFilters.mapTo(mutableListOf()) { it.asChip(true) }
            val index = _filterChips.indexOfFirst { it.filter == filter }
            _filterChips[index] = filter.asChip(enabled)

            publish(true)
        }
    }

    override fun clearFilters() {
        if (_selectedFilters.isNotEmpty()) {
            _selectedFilters.clear()
            _selectedFilterChips.clear()
            _filterChips = _filterChips.mapTo(mutableListOf()) {
                if (it.isSelected) it.copy(isSelected = false) else it
            }

            publish(true)
        }
    }
}
