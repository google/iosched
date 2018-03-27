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

import android.support.v7.widget.RecyclerView
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.samples.apps.iosched.databinding.ItemTagChipBinding
import com.google.samples.apps.iosched.shared.model.Tag

class TagChipAdapter : RecyclerView.Adapter<TagChipViewHolder>() {

    var tags = emptyList<Tag>()

    override fun getItemCount() = tags.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagChipViewHolder {
        return TagChipViewHolder(
            ItemTagChipBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: TagChipViewHolder, position: Int) {
        holder.bind(tags[position])
    }
}

class TagChipViewHolder(private val binding: ItemTagChipBinding) : ViewHolder(binding.root) {
    fun bind(tag: Tag) {
        binding.tag = tag
        binding.executePendingBindings()
    }
}
