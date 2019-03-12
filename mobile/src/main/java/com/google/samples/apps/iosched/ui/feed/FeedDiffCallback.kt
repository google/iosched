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

import androidx.recyclerview.widget.DiffUtil
import com.google.common.collect.ImmutableMap
import com.google.samples.apps.iosched.model.feed.FeedItem

class FeedDiffCallback(
    val viewBinders: ImmutableMap<FeedItemClass, FeedBinder>
) : DiffUtil.ItemCallback<FeedItem>() {

    override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean =
        if (oldItem::class != newItem::class) {
            false
        } else {
            viewBinders[oldItem::class.java]?.areItemsTheSame(oldItem, newItem) ?: false
        }

    override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean =
        if (oldItem::class != newItem::class) {
            false
        } else {
            viewBinders[oldItem::class.java]?.areContentsTheSame(oldItem, newItem) ?: false
        }
}