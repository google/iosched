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

import android.support.annotation.StringRes
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.GridLayoutManager.SpanSizeLookup
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ItemTagFilterBinding
import com.google.samples.apps.iosched.shared.model.Tag
import com.google.samples.apps.iosched.shared.util.exceptionInDebug
import com.google.samples.apps.iosched.shared.util.inflate
import com.google.samples.apps.iosched.ui.schedule.ScheduleViewModel
import com.google.samples.apps.iosched.ui.schedule.filters.ScheduleFilterAdapter.FilterCategoryHeading

/**
 * Adapter for the filters drawer
 */
class ScheduleFilterAdapter(val viewModel: ScheduleViewModel) :
    ListAdapter<Any, ViewHolder>(TagFilterDiff) {

    companion object {
        private const val VIEW_TYPE_HEADING = R.layout.item_filter_heading
        private const val VIEW_TYPE_TAGFILTER = R.layout.item_tag_filter

        /**
         * Insert [FilterCategoryHeading]s in a list of [TagFilter]s to make a heterogeneous list.
         */
        private fun insertCategoryHeadings(list: List<TagFilter>?): List<Any> {
            val newList = mutableListOf<Any>()
            var previousCategory: String? = null
            list?.forEach {
                if (it.tag.category != previousCategory) {
                    newList.add(createCategoryHeading(it.tag.category))
                }
                newList.add(it)
                previousCategory = it.tag.category
            }
            return newList
        }

        /** Convert a [Tag.category] to a heading with text. */
        private fun createCategoryHeading(category: String): FilterCategoryHeading {
            val titleRes = when (category) {
                Tag.CATEGORY_TRACK -> R.string.category_heading_tracks
                Tag.CATEGORY_TYPE -> R.string.category_heading_types
                else -> throw IllegalArgumentException("Unsupported category")
            }
            return FilterCategoryHeading(titleRes)
        }
    }

    /** Item that represents a filter category heading. */
    internal data class FilterCategoryHeading(@StringRes val stringRes: Int)

    override fun submitList(list: MutableList<Any>?) {
        exceptionInDebug(
            RuntimeException("call `submitTagFilterList()` instead to add category headings.")
        )
        super.submitList(list)
    }

    /** Prefer this method over [submitList] to add category headings. */
    fun submitTagFilterList(list: List<TagFilter>?) {
        super.submitList(insertCategoryHeadings(list))
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is FilterCategoryHeading -> VIEW_TYPE_HEADING
            is TagFilter -> VIEW_TYPE_TAGFILTER
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }

    fun getSpanSize(position: Int): Int {
        return if (getItemViewType(position) == VIEW_TYPE_HEADING) { 2 } else { 1 }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            VIEW_TYPE_HEADING -> createHeadingViewHolder(parent)
            VIEW_TYPE_TAGFILTER -> createFilterViewHolder(parent)
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }

    private fun createHeadingViewHolder(parent: ViewGroup): HeadingViewHolder {
        return HeadingViewHolder(parent.inflate(VIEW_TYPE_HEADING, false))
    }

    private fun createFilterViewHolder(parent: ViewGroup): FilterViewHolder {
        val binding = ItemTagFilterBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false
        ).apply {
            viewModel = this@ScheduleFilterAdapter.viewModel
        }
        return FilterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder) {
            is HeadingViewHolder -> holder.bind(getItem(position) as FilterCategoryHeading)
            is FilterViewHolder -> holder.bind(getItem(position) as TagFilter)
        }
    }

    /** ViewHolder for category heading items. */
    class HeadingViewHolder(itemView: View) : ViewHolder(itemView) {
        private val textView = itemView as TextView

        internal fun bind(item: FilterCategoryHeading) {
            textView.setText(item.stringRes)
        }
    }

    /** ViewHolder for [TagFilter] items. */
    class FilterViewHolder(private val binding: ItemTagFilterBinding) :
        ViewHolder(binding.root) {

        internal fun bind(item: TagFilter) {
            binding.tagFilter = item
            binding.executePendingBindings()
        }
    }
}

internal object TagFilterDiff : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any) = oldItem == newItem

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        // This method is only called if areItemsTheSame() returns true. For FilterCategoryHeading
        // items, that check suffices for this one as well.
        return when (oldItem) {
            is FilterCategoryHeading -> true
            is TagFilter -> oldItem.isUiContentEqual(newItem as TagFilter)
            else -> throw IllegalArgumentException("Unknown item type")
        }
    }
}

internal class ScheduleFilterSpanSizeLookup(private val adapter: ScheduleFilterAdapter) :
    SpanSizeLookup() {

    override fun getSpanSize(position: Int) = adapter.getSpanSize(position)
}
