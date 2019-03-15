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
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.samples.apps.iosched.model.feed.FeedItem

typealias FeedBinder = FeedListItemViewBinder<FeedItem, ViewHolder>

abstract class FeedListItemViewBinder<M : FeedItem, in VH : ViewHolder>(
    val modelClass: Class<out M>,
    val context: Context
) : DiffUtil.ItemCallback<M>() {

    abstract fun createViewHolder(parent: ViewGroup): ViewHolder
    abstract fun bindViewHolder(model: M, viewHolder: VH)
    abstract fun getFeedItemType(): Int
}
