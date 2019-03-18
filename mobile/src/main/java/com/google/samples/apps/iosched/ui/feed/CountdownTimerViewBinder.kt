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
import com.google.samples.apps.iosched.databinding.ItemFeedCountdownTimerBinding

/** Feed item for the countdown timer. */
data class CountdownTimer(
    val id: String = "countdown_timer"
)

class CountdownTimerViewBinder :
    FeedListItemViewBinder<CountdownTimer, CountdownTimerViewHolder>(CountdownTimer::class.java) {

    override fun createViewHolder(parent: ViewGroup): CountdownTimerViewHolder =
        CountdownTimerViewHolder(
            ItemFeedCountdownTimerBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )

    override fun bindViewHolder(model: CountdownTimer, viewHolder: CountdownTimerViewHolder) =
        viewHolder.bind()

    override fun getFeedItemType(): Int = R.layout.item_feed_countdown_timer

    override fun areItemsTheSame(oldItem: CountdownTimer, newItem: CountdownTimer): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: CountdownTimer, newItem: CountdownTimer) =
        oldItem == newItem
}

class CountdownTimerViewHolder(private val binding: ItemFeedCountdownTimerBinding) :
    ViewHolder(binding.root) {

    fun bind() = binding.executePendingBindings()
}
