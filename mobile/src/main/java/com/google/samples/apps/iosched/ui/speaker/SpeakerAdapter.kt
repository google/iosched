/*
 * Copyright 2018 Google LLC
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

package com.google.samples.apps.iosched.ui.speaker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ItemSessionBinding
import com.google.samples.apps.iosched.databinding.ItemSpeakerInfoBinding
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.ui.speaker.SpeakerViewHolder.HeaderViewHolder
import com.google.samples.apps.iosched.ui.speaker.SpeakerViewHolder.SpeakerInfoViewHolder
import com.google.samples.apps.iosched.ui.speaker.SpeakerViewHolder.SpeakerSessionViewHolder
import java.util.Collections.emptyList

/**
 * [RecyclerView.Adapter] for presenting a speaker details, composed of information about the
 * speaker and any sessions that the speaker presents.
 */
class SpeakerAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val speakerViewModel: SpeakerViewModel,
    private val imageLoadListener: ImageLoadListener,
    private val tagRecycledViewPool: RecycledViewPool
) : RecyclerView.Adapter<SpeakerViewHolder>() {

    var speakerSessions: List<UserSession> = emptyList()
        set(value) {
            field = value
            differ.submitList(buildMergedList(sessions = value))
        }

    private val differ = AsyncListDiffer<Any>(this, DiffCallback)

    init {
        differ.submitList(buildMergedList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeakerViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.item_speaker_info -> SpeakerInfoViewHolder(
                ItemSpeakerInfoBinding.inflate(inflater, parent, false)
            )
            R.layout.item_speaker_events_header -> HeaderViewHolder(
                inflater.inflate(viewType, parent, false)
            )
            R.layout.item_session -> SpeakerSessionViewHolder(
                ItemSessionBinding.inflate(inflater, parent, false).apply {
                    tags.setRecycledViewPool(tagRecycledViewPool)
                }
            )
            else -> throw IllegalStateException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: SpeakerViewHolder, position: Int) {
        when (holder) {
            is SpeakerInfoViewHolder -> holder.binding.apply {
                viewModel = speakerViewModel
                headshotLoadListener = imageLoadListener
                setLifecycleOwner(lifecycleOwner)
                executePendingBindings()
            }
            is SpeakerSessionViewHolder -> holder.binding.apply {
                userSession = differ.currentList[position] as UserSession
                eventListener = speakerViewModel
                setLifecycleOwner(lifecycleOwner)
                executePendingBindings()
            }
            is HeaderViewHolder -> Unit // no-op
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (differ.currentList[position]) {
            SpeakerItem -> R.layout.item_speaker_info
            SpeakerEventsHeaderItem -> R.layout.item_speaker_events_header
            is UserSession -> R.layout.item_session
            else -> throw IllegalStateException("Unknown view type at position $position")
        }
    }

    override fun getItemCount() = differ.currentList.size

    /**
     * This adapter displays heterogeneous data types but `RecyclerView` & `AsyncListDiffer` deal in
     * a single list of items. We therefore combine them into a merged list, using marker objects
     * for static items. We still hold separate lists of [speakerSessions] so that
     * we can provide them individually, as they're loaded.
     */
    private fun buildMergedList(
        sessions: List<UserSession> = speakerSessions
    ): List<Any> {
        val merged = mutableListOf<Any>(SpeakerItem)
        if (sessions.isNotEmpty()) {
            merged += SpeakerEventsHeaderItem
            merged.addAll(sessions)
        }
        return merged
    }
}

// Marker objects for use in our merged representation.

object SpeakerItem

object SpeakerEventsHeaderItem

/**
 * Diff items presented by this adapter.
 */
object DiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem === SpeakerItem && newItem === SpeakerItem -> true
            oldItem === SpeakerEventsHeaderItem && newItem === SpeakerEventsHeaderItem -> true
            oldItem is UserSession && newItem is UserSession ->
                oldItem.session.id == newItem.session.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is UserSession && newItem is UserSession -> oldItem == newItem
            else -> true
        }
    }
}

/**
 * [RecyclerView.ViewHolder] types used by this adapter.
 */
sealed class SpeakerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    class SpeakerInfoViewHolder(
        val binding: ItemSpeakerInfoBinding
    ) : SpeakerViewHolder(binding.root)

    class SpeakerSessionViewHolder(
        val binding: ItemSessionBinding
    ) : SpeakerViewHolder(binding.root)

    class HeaderViewHolder(
        itemView: View
    ) : SpeakerViewHolder(itemView)
}
