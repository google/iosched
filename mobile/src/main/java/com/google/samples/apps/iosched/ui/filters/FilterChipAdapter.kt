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
import com.google.samples.apps.iosched.databinding.ItemFilterChipBinding
import com.google.samples.apps.iosched.util.executeAfter

// TODO(jdkoren): Maybe combine this with SelectableFilterChipAdapter
class FilterChipAdapter : ListAdapter<FilterChip, FilterChipViewHolder>(FilterChipDiff2) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterChipViewHolder {
        return FilterChipViewHolder(
            ItemFilterChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: FilterChipViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class FilterChipViewHolder(private val binding: ItemFilterChipBinding) : ViewHolder(binding.root) {
    fun bind(item: FilterChip) {
        binding.executeAfter {
            filterChip = item
        }
    }
}

private object FilterChipDiff2 : DiffUtil.ItemCallback<FilterChip>() {
    override fun areItemsTheSame(oldItem: FilterChip, newItem: FilterChip) =
        oldItem.filter == newItem.filter

    override fun areContentsTheSame(oldItem: FilterChip, newItem: FilterChip): Boolean =
        oldItem.isSelected == newItem.isSelected
}
