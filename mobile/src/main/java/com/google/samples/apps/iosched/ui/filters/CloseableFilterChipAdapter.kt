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
import com.google.samples.apps.iosched.databinding.ItemFilterChipCloseableBinding
import com.google.samples.apps.iosched.ui.filters.CloseableFilterChipAdapter.FilterChipViewHolder
import com.google.samples.apps.iosched.util.executeAfter

// TODO(jdkoren): Maybe combine this with SelectableFilterChipAdapter
/** Adapter for closeable filters, e.g. those shown above search results. */
class CloseableFilterChipAdapter(
    private val viewModelDelegate: FiltersViewModelDelegate
) : ListAdapter<FilterChip, FilterChipViewHolder>(FilterChipDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterChipViewHolder {
        return FilterChipViewHolder(
            ItemFilterChipCloseableBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ).apply {
                viewModel = viewModelDelegate
            }
        )
    }

    override fun onBindViewHolder(holder: FilterChipViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class FilterChipViewHolder(
        private val binding: ItemFilterChipCloseableBinding
    ) : ViewHolder(binding.root) {
        fun bind(item: FilterChip) {
            binding.executeAfter {
                filterChip = item
            }
        }
    }
}

private object FilterChipDiff : DiffUtil.ItemCallback<FilterChip>() {
    override fun areItemsTheSame(oldItem: FilterChip, newItem: FilterChip) =
        oldItem.filter == newItem.filter

    override fun areContentsTheSame(oldItem: FilterChip, newItem: FilterChip) =
        oldItem.isSelected == newItem.isSelected
}
