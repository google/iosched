/*
 * Copyright 2020 Google LLC
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

import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ItemFeedSocialChannelsBinding
import com.google.samples.apps.iosched.util.getColorFromTheme

object FeedSocialChannelsSection

class FeedSocialChannelsSectionViewBinder :
    FeedItemViewBinder<FeedSocialChannelsSection, FeedSocialChannelsSectionViewHolder>(
        FeedSocialChannelsSection::class.java
    ) {

    override fun createViewHolder(parent: ViewGroup): ViewHolder =
        FeedSocialChannelsSectionViewHolder(
            ItemFeedSocialChannelsBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun bindViewHolder(
        model: FeedSocialChannelsSection,
        viewHolder: FeedSocialChannelsSectionViewHolder
    ) = viewHolder.bind()

    override fun getFeedItemType() = R.layout.item_feed_social_channels

    override fun areItemsTheSame(
        oldItem: FeedSocialChannelsSection,
        newItem: FeedSocialChannelsSection
    ) = true

    override fun areContentsTheSame(
        oldItem: FeedSocialChannelsSection,
        newItem: FeedSocialChannelsSection
    ) = true
}

class FeedSocialChannelsSectionViewHolder(val binding: ItemFeedSocialChannelsBinding) :
    ViewHolder(binding.root) {

    fun bind() {
        val titleText = SpannableStringBuilder()
            .append(SpannableString("#Google"))
            .append(
                SpannableString("IO")
                    .apply {
                        setSpan(
                            ForegroundColorSpan(
                                binding.root.context.getColorFromTheme(R.attr.colorPrimary)
                            ),
                            0, length, Spanned.SPAN_INCLUSIVE_INCLUSIVE
                        )
                    }
            )
        binding.title.text = titleText
    }
}
