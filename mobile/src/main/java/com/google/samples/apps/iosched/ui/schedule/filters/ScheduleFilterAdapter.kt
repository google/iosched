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

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.samples.apps.iosched.databinding.ListItemFilterDrawerBinding
import com.google.samples.apps.iosched.ui.schedule.ScheduleViewModel
import com.google.samples.apps.iosched.ui.schedule.TagFilter
import com.google.samples.apps.iosched.ui.schedule.filters.ScheduleFilterAdapter.FilterViewHolder

/**
 * Adapter for the filters drawer
 */
class ScheduleFilterAdapter(val viewModel: ScheduleViewModel) :
    ListAdapter<TagFilter, FilterViewHolder>(TagFilterDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val binding = ListItemFilterDrawerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent, false
        ).apply {
            viewModel = this@ScheduleFilterAdapter.viewModel
        }
        return FilterViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FilterViewHolder(private val binding: ListItemFilterDrawerBinding) :
        ViewHolder(binding.root) {

        internal fun bind(filter: TagFilter) {
            binding.tagFilter = filter
            binding.executePendingBindings()
        }
    }
}

object TagFilterDiff : DiffUtil.ItemCallback<TagFilter>() {
    override fun areItemsTheSame(oldItem: TagFilter, newItem: TagFilter) = oldItem == newItem

    override fun areContentsTheSame(oldItem: TagFilter, newItem: TagFilter) =
        oldItem.isUiContentEqual(newItem
    )
}
