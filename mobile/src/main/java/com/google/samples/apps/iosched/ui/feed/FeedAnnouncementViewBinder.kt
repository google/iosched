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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ItemFeedAnnouncementBinding
import com.google.samples.apps.iosched.model.Announcement
import org.threeten.bp.ZoneId

class FeedAnnouncementViewBinder(
    private val timeZoneId: LiveData<ZoneId>,
    private val lifecycleOwner: LifecycleOwner
) :
    FeedListItemViewBinder<Announcement, FeedAnnouncementViewHolder>(
        Announcement::class.java
    ) {

    override fun createViewHolder(parent: ViewGroup): FeedAnnouncementViewHolder =
        FeedAnnouncementViewHolder(
            ItemFeedAnnouncementBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            lifecycleOwner,
            timeZoneId
        )

    override fun bindViewHolder(model: Announcement, viewHolder: FeedAnnouncementViewHolder) {
        viewHolder.bind(model)
    }

    override fun getFeedItemType(): Int = R.layout.item_feed_announcement

    override fun areItemsTheSame(oldItem: Announcement, newItem: Announcement): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Announcement, newItem: Announcement) =
        oldItem == newItem
}

class FeedAnnouncementViewHolder(
    private val binding: ItemFeedAnnouncementBinding,
    private val lifecycleOwner: LifecycleOwner,
    private val timeZoneId: LiveData<ZoneId>
) :
    ViewHolder(binding.root) {

    fun bind(announcement: Announcement) {
        binding.announcement = announcement
        binding.lifecycleOwner = lifecycleOwner
        binding.timeZoneId = timeZoneId
        binding.executePendingBindings()
    }
}
