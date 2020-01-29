/*
 * Copyright 2019 Google LLC
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

package com.google.samples.apps.iosched.ui.search

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.samples.apps.iosched.databinding.ItemSearchResultBinding

class SearchAdapter(
    private val searchViewModel: SearchViewModel
) : ListAdapter<SearchResult, SearchViewHolder>(SearchDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        return SearchViewHolder(
            ItemSearchResultBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            searchViewModel
        )
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class SearchViewHolder(
    private val binding: ItemSearchResultBinding,
    private val searchViewModel: SearchViewModel
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(searchResult: SearchResult) {
        binding.eventListener = searchViewModel
        binding.searchResult = searchResult
        binding.executePendingBindings()
    }
}

object SearchDiff : DiffUtil.ItemCallback<SearchResult>() {
    override fun areItemsTheSame(oldItem: SearchResult, newItem: SearchResult) =
        oldItem.objectId == newItem.objectId

    override fun areContentsTheSame(oldItem: SearchResult, newItem: SearchResult) =
        oldItem == newItem
}
