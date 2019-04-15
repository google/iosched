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
import com.google.android.material.button.MaterialButton
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ItemFeedHeaderBinding
import com.google.samples.apps.iosched.model.Moment
import com.google.samples.apps.iosched.model.Moment.Companion.CTA_LIVE_STREAM
import com.google.samples.apps.iosched.model.Moment.Companion.CTA_MAP_LOCATION
import com.google.samples.apps.iosched.model.Moment.Companion.CTA_SIGNIN
import org.threeten.bp.ZoneId

/** Feed item for the Feed's header. */
data class FeedHeader(
    val timerVisible: Boolean = false,
    val moment: Moment?,
    val userSignedIn: Boolean = false,
    val userRegistered: Boolean = false,
    val timeZoneId: ZoneId
)

class FeedHeaderViewBinder(private val eventListener: FeedEventListener) :
    FeedListItemViewBinder<FeedHeader, FeedHeaderViewHolder>(FeedHeader::class.java) {

    override fun createViewHolder(parent: ViewGroup): FeedHeaderViewHolder =
        FeedHeaderViewHolder(
            ItemFeedHeaderBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            ),
            eventListener
        )

    override fun bindViewHolder(model: FeedHeader, viewHolder: FeedHeaderViewHolder) {
        viewHolder.bind(model)
    }

    override fun getFeedItemType(): Int = R.layout.item_feed_header

    override fun areItemsTheSame(oldItem: FeedHeader, newItem: FeedHeader): Boolean = true

    override fun areContentsTheSame(oldItem: FeedHeader, newItem: FeedHeader) = oldItem == newItem
}

class FeedHeaderViewHolder(
    private val binding: ItemFeedHeaderBinding,
    private val eventListener: FeedEventListener
) :
    ViewHolder(binding.root) {

    fun bind(feedHeader: FeedHeader) {
        binding.feedHeader = feedHeader
        binding.executePendingBindings()

        feedHeader.moment?.apply {

            val button = binding.actionButton as MaterialButton
            when (ctaType) {
                CTA_LIVE_STREAM -> {
                    val url = streamUrl
                    if (url.isNullOrEmpty()) {
                        button.isVisible = false
                    } else {
                        button.apply {
                            isVisible = true
                            setText(R.string.feed_watch_live_stream)
                            setOnClickListener {
                                eventListener.openLiveStream(url)
                            }
                            setIconResource(R.drawable.ic_play_circle_outline)
                        }
                    }
                }
                CTA_MAP_LOCATION -> {
                    button.apply {
                        isVisible = true
                        text = featureName
                        setOnClickListener {
                            eventListener.openMap(feedHeader.moment)
                        }
                        setIconResource(R.drawable.ic_nav_map)
                    }
                }
                CTA_SIGNIN -> {
                    if (feedHeader.userSignedIn) {
                        button.isVisible = false
                    } else {
                        button.apply {
                            isVisible = true
                            setText(R.string.sign_in)
                            setOnClickListener { eventListener.signIn() }
                            setIconResource(R.drawable.ic_default_profile_avatar)
                        }
                    }
                }
                else /* CTA_NO_ACTION */ -> button.isVisible = false
            }
        }
    }
}
