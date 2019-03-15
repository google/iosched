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

package com.google.samples.apps.iosched.ui.feed

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ItemFeedSectionHeaderBinding

class FeedAnnouncementHeaderViewBinder(modelClass: Class<SectionHeader>, context: Context) :
    FeedListItemViewBinder<SectionHeader, AnnouncementViewHolder>(modelClass, context) {

    override fun createViewHolder(parent: ViewGroup): AnnouncementViewHolder =
        AnnouncementViewHolder(
            ItemFeedSectionHeaderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ),
            context
        )

    override fun bindViewHolder(model: SectionHeader, viewHolder: AnnouncementViewHolder) {
        viewHolder.bind(model)
    }

    override fun getFeedItemType(): Int = R.layout.item_feed_section_header

    override fun areItemsTheSame(oldItem: SectionHeader, newItem: SectionHeader): Boolean =
        oldItem.titleId == newItem.titleId

    override fun areContentsTheSame(oldItem: SectionHeader, newItem: SectionHeader) =
        oldItem == newItem
}

class AnnouncementViewHolder(
    private val binding: ItemFeedSectionHeaderBinding,
    val context: Context
) : ViewHolder(binding.root) {

    fun bind(model: SectionHeader) {
        binding.announcementHeaderText = context.getString(model.titleId)
        binding.executePendingBindings()
    }
}
