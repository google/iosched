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

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ItemFeedSustainabilityBinding

object FeedSustainabilitySection

class FeedSustainabilitySectionViewBinder :
    FeedItemViewBinder<FeedSustainabilitySection, FeedSustainabilitySectionViewHolder>(
        FeedSustainabilitySection::class.java
    ) {

    override fun createViewHolder(parent: ViewGroup): ViewHolder =
        FeedSustainabilitySectionViewHolder(
            ItemFeedSustainabilityBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun bindViewHolder(
        model: FeedSustainabilitySection,
        viewHolder: FeedSustainabilitySectionViewHolder
    ) = Unit

    override fun getFeedItemType() = R.layout.item_feed_sustainability

    override fun areItemsTheSame(
        oldItem: FeedSustainabilitySection,
        newItem: FeedSustainabilitySection
    ) = true

    override fun areContentsTheSame(
        oldItem: FeedSustainabilitySection,
        newItem: FeedSustainabilitySection
    ) = true
}

class FeedSustainabilitySectionViewHolder(
    binding: ItemFeedSustainabilityBinding
) : ViewHolder(binding.root)
