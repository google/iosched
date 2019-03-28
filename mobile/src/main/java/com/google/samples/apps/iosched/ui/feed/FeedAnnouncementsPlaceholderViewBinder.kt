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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ItemFeedAnnouncementsPlaceholderBinding

/** Feed item which acts as a placeholder for announcements. */
data class AnnouncementsPlaceholder(
    val isLoading: Boolean = false,
    val notAvailable: Boolean = false
)

class FeedAnnouncementsPlaceholderViewBinder :
    FeedListItemViewBinder<AnnouncementsPlaceholder, AnnouncementsPlaceholderViewHolder>(
        AnnouncementsPlaceholder::class.java
    ) {

    override fun createViewHolder(parent: ViewGroup): AnnouncementsPlaceholderViewHolder {
        return AnnouncementsPlaceholderViewHolder(
            ItemFeedAnnouncementsPlaceholderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun bindViewHolder(
        model: AnnouncementsPlaceholder,
        viewHolder: AnnouncementsPlaceholderViewHolder
    ) =
        viewHolder.bind(model)

    override fun getFeedItemType(): Int = R.layout.item_feed_announcements_placeholder

    override fun areItemsTheSame(
        oldItem: AnnouncementsPlaceholder,
        newItem: AnnouncementsPlaceholder
    ): Boolean = true

    override fun areContentsTheSame(
        oldItem: AnnouncementsPlaceholder,
        newItem: AnnouncementsPlaceholder
    ) =
        oldItem == newItem
}

class AnnouncementsPlaceholderViewHolder(
    private val binding: ItemFeedAnnouncementsPlaceholderBinding
) : ViewHolder(binding.root) {

    fun bind(announcementsPlaceholder: AnnouncementsPlaceholder) {
        binding.announcementPlaceholder = announcementsPlaceholder
        binding.executePendingBindings()
    }
}
