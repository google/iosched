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

package com.google.samples.apps.iosched.ui.filters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ItemFilterChipSelectableBinding
import com.google.samples.apps.iosched.databinding.ItemGenericSectionHeaderBinding
import com.google.samples.apps.iosched.shared.util.exceptionInDebug
import com.google.samples.apps.iosched.ui.SectionHeader

/** Adapter for selectable filters, e.g. ones shown in the filter sheet. */
class SelectableFilterChipAdapter(
    private val viewModelDelegate: FiltersViewModelDelegate
) : ListAdapter<Any, ViewHolder>(FilterChipAndHeadingDiff) {

    companion object {
        private const val VIEW_TYPE_HEADING = R.layout.item_generic_section_header
        private const val VIEW_TYPE_FILTER = R.layout.item_filter_chip_selectable

        /**
         * Inserts category headings in a list of [FilterChip]s to make a heterogeneous list.
         * Assumes the items are already grouped by [FilterChip.categoryLabel], beginning with
         * categoryLabel == '0'.
         */
        private fun insertCategoryHeadings(list: List<FilterChip>?): List<Any> {
            val newList = mutableListOf<Any>()
            var previousCategory = 0
            list?.forEach {
                val category = it.categoryLabel
                if (category != previousCategory && category != 0) {
                    newList += SectionHeader(
                        titleId = category,
                        useHorizontalPadding = false
                    )
                }
                newList.add(it)
                previousCategory = category
            }
            return newList
        }
    }

    override fun submitList(list: MutableList<Any>?) {
        exceptionInDebug(
            RuntimeException("call `submitEventFilterList()` instead to add category headings.")
        )
        super.submitList(list)
    }

    /** Prefer this method over [submitList] to add category headings. */
    fun submitFilterList(list: List<FilterChip>?) {
        super.submitList(insertCategoryHeadings(list))
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SectionHeader -> VIEW_TYPE_HEADING
            is FilterChip -> VIEW_TYPE_FILTER
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADING -> createHeadingViewHolder(parent)
            VIEW_TYPE_FILTER -> createFilterViewHolder(parent)
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }

    private fun createHeadingViewHolder(parent: ViewGroup): HeadingViewHolder {
        return HeadingViewHolder(
            ItemGenericSectionHeaderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    private fun createFilterViewHolder(parent: ViewGroup): FilterViewHolder {
        val binding = ItemFilterChipSelectableBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ).apply {
            viewModel = viewModelDelegate
        }
        return FilterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is HeadingViewHolder -> holder.bind(getItem(position) as SectionHeader)
            is FilterViewHolder -> holder.bind(getItem(position) as FilterChip)
        }
    }

    /** ViewHolder for category heading items. */
    class HeadingViewHolder(
        private val binding: ItemGenericSectionHeaderBinding
    ) : ViewHolder(binding.root) {

        internal fun bind(item: SectionHeader) {
            binding.sectionHeader = item
            binding.executePendingBindings()
        }
    }

    /** ViewHolder for [FilterChip] items. */
    class FilterViewHolder(private val binding: ItemFilterChipSelectableBinding) :
        ViewHolder(binding.root) {

        internal fun bind(item: FilterChip) {
            binding.filterChip = item
            binding.executePendingBindings()
        }
    }
}

private object FilterChipAndHeadingDiff : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when (oldItem) {
            is FilterChip -> newItem is FilterChip && newItem.filter == oldItem.filter
            else -> oldItem == newItem // SectionHeader
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when (oldItem) {
            is FilterChip -> oldItem.isSelected == (newItem as FilterChip).isSelected
            else -> true
        }
    }
}
