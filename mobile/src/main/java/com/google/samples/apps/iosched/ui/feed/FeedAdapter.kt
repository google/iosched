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

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.common.collect.ImmutableMap
import com.google.samples.apps.iosched.model.feed.FeedItem

class FeedAdapter(
    callback: DiffUtil.ItemCallback<FeedItem>,
    val viewBinders: ImmutableMap<FeedItemClass, FeedBinder>
) : ListAdapter<FeedItem, ViewHolder>(callback) {

    private val viewTypeToBinders = viewBinders.mapKeys { it.value.getFeedItemType() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return getViewBinder(viewType).createViewHolder(parent)
    }

    fun getViewBinder(viewType: Int): FeedBinder = viewTypeToBinders[viewType]!!

    override fun getItemViewType(position: Int): Int =
        viewBinders[super.getItem(position).javaClass]!!.getFeedItemType()

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        return getViewBinder(getItemViewType(position)).bindViewHolder(getItem(position), holder)
    }
}
