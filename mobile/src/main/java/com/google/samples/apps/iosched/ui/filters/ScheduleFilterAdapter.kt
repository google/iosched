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
import androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ItemEventFilterBinding
import com.google.samples.apps.iosched.databinding.ItemGenericSectionHeaderBinding
import com.google.samples.apps.iosched.shared.util.exceptionInDebug
import com.google.samples.apps.iosched.ui.SectionHeader
import com.google.samples.apps.iosched.ui.filters.EventFilter.EventFilterCategory
import com.google.samples.apps.iosched.ui.filters.EventFilter.EventFilterCategory.NONE
import com.google.samples.apps.iosched.ui.filters.EventFilter.MyEventsFilter
import com.google.samples.apps.iosched.ui.filters.EventFilter.TagFilter

/**
 * Adapter for the filters drawer
 */
class ScheduleFilterAdapter(val viewModel: FiltersViewModelDelegate) :
    ListAdapter<Any, ViewHolder>(EventFilterDiff) {

    companion object {
        private const val VIEW_TYPE_HEADING = R.layout.item_generic_section_header
        private const val VIEW_TYPE_FILTER = R.layout.item_event_filter

        /**
         * Inserts category headings in a list of [EventFilter]s to make a heterogeneous list.
         * Assumes the items are already sorted by the value of [EventFilter.getFilterCategory],
         * with items belonging to [NONE] first.
         */
        private fun insertCategoryHeadings(list: List<EventFilter>?): List<Any> {
            val newList = mutableListOf<Any>()
            var previousCategory: EventFilterCategory = NONE
            list?.forEach {
                val category = it.getFilterCategory()
                if (category != previousCategory && category != NONE) {
                    newList += SectionHeader(
                            titleId = category.labelResId,
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
    fun submitEventFilterList(list: List<EventFilter>?) {
        super.submitList(insertCategoryHeadings(list))
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is SectionHeader -> VIEW_TYPE_HEADING
            is EventFilter -> VIEW_TYPE_FILTER
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }

    fun getSpanSize(position: Int): Int {
        return if (getItem(position) is TagFilter) 1 else 2
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
        val binding = ItemEventFilterBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ).apply {
            viewModel = this@ScheduleFilterAdapter.viewModel
        }
        return FilterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is HeadingViewHolder -> holder.bind(getItem(position) as SectionHeader)
            is FilterViewHolder -> holder.bind(getItem(position) as EventFilter)
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

    /** ViewHolder for [TagFilter] items. */
    class FilterViewHolder(private val binding: ItemEventFilterBinding) :
        ViewHolder(binding.root) {

        internal fun bind(item: EventFilter) {
            binding.eventFilter = item
            binding.executePendingBindings()
        }
    }
}

internal object EventFilterDiff : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any) = oldItem == newItem

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when (oldItem) {
            is MyEventsFilter -> oldItem.isUiContentEqual(newItem as MyEventsFilter)
            is TagFilter -> oldItem.isUiContentEqual(newItem as TagFilter)
            else -> true
        }
    }
}

internal class ScheduleFilterSpanSizeLookup(private val adapter: ScheduleFilterAdapter) :
    SpanSizeLookup() {

    override fun getSpanSize(position: Int) = adapter.getSpanSize(position)
}
