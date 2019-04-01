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
import com.google.samples.apps.iosched.databinding.ItemFeedHeaderBinding
import org.threeten.bp.ZonedDateTime

/** Feed item for the countdown timer. */
data class FeedHeader(
    val timerVisible: Boolean = false,
    val moment: Moment?
)

data class Moment(
    val title: String,
    val startTime: ZonedDateTime,
    val endTime: ZonedDateTime,
    val contentActionUrl: String?,
    val contentActionScreen: String?,
    val callToAction: String?,
    val imageUrl: String,
    val signInRequiredForCta: Boolean
)

class FeedHeaderViewBinder :
    FeedListItemViewBinder<FeedHeader, FeedHeaderViewHolder>(FeedHeader::class.java) {

    override fun createViewHolder(parent: ViewGroup): FeedHeaderViewHolder =
        FeedHeaderViewHolder(
            ItemFeedHeaderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun bindViewHolder(model: FeedHeader, viewHolder: FeedHeaderViewHolder) {
        viewHolder.bind(model)
    }

    override fun getFeedItemType(): Int = R.layout.item_feed_header

    override fun areItemsTheSame(oldItem: FeedHeader, newItem: FeedHeader): Boolean = true

    override fun areContentsTheSame(oldItem: FeedHeader, newItem: FeedHeader) = oldItem == newItem
}

class FeedHeaderViewHolder(private val binding: ItemFeedHeaderBinding) :
    ViewHolder(binding.root) {

    fun bind(feedHeader: FeedHeader) {
        binding.feedHeader = feedHeader
        binding.executePendingBindings()
    }
}
