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
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ItemFeedHeaderBinding
import com.google.samples.apps.iosched.model.Moment
import com.google.samples.apps.iosched.model.Moment.Companion.CTA_LIVE_STREAM
import com.google.samples.apps.iosched.model.Moment.Companion.CTA_MAP_LOCATION
import com.google.samples.apps.iosched.model.Moment.Companion.CTA_NO_ACTION
import com.google.samples.apps.iosched.model.Moment.Companion.CTA_SIGNIN

import com.google.samples.apps.iosched.shared.util.TimeUtils
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

/** Feed item for the Feed's header. */
data class FeedHeader(
    val timerVisible: Boolean = false,
    val moment: Moment?,
    val userSignedIn: Boolean = false,
    val userRegistered: Boolean = false,
    val timeZoneId: ZoneId
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

        feedHeader.moment?.apply {
            binding.time.text =
                (DateTimeFormatter.ofPattern("h:mm").format(
                    TimeUtils.zonedTime(this.startTime, feedHeader.timeZoneId)
                ) +
                    " - " +
                    DateTimeFormatter.ofPattern("h:mm a").format(
                        TimeUtils.zonedTime(this.endTime, feedHeader.timeZoneId)
                    ))

            when (feedHeader.moment.ctaType) {
                // TODO: set icons programatically
                // TODO: set click listeners programatically
                CTA_LIVE_STREAM -> {
                    binding.actionButton.isVisible = true
                    binding.actionButton.setText(R.string.feed_watch_live_stream)
                }
                CTA_MAP_LOCATION -> {
                    binding.actionButton.isVisible = true
                    binding.actionButton.text = feedHeader.moment.featureName
                }
                CTA_SIGNIN -> {
                    if (feedHeader.userSignedIn) {
                        binding.actionButton.isVisible = false
                    } else {
                        binding.actionButton.isVisible = true
                        binding.actionButton.setText(R.string.sign_in)
                    }
                }
                CTA_NO_ACTION -> {
                    binding.actionButton.isVisible = false
                }
            }
        }
    }
}
