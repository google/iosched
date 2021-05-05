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

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ItemGenericSectionHeaderBinding
import com.google.samples.apps.iosched.databinding.ItemSessionBinding
import com.google.samples.apps.iosched.databinding.ItemSpeakerInfoBinding
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.ui.SectionHeader
import com.google.samples.apps.iosched.ui.sessioncommon.EventActions
import com.google.samples.apps.iosched.ui.speaker.SpeakerViewHolder.HeaderViewHolder
import com.google.samples.apps.iosched.ui.speaker.SpeakerViewHolder.SpeakerInfoViewHolder
import com.google.samples.apps.iosched.ui.speaker.SpeakerViewHolder.SpeakerSessionViewHolder
import com.google.samples.apps.iosched.util.executeAfter
import java.util.Collections.emptyList

/**
 * [RecyclerView.Adapter] for presenting a speaker details, composed of information about the
 * speaker and any sessions that the speaker presents.
 */
class SpeakerAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val speakerViewModel: SpeakerViewModel,
    private val tagRecycledViewPool: RecycledViewPool,
    private val eventListener: EventActions
) : RecyclerView.Adapter<SpeakerViewHolder>() {

    private val differ = AsyncListDiffer<Any>(this, DiffCallback)

    var speakerSessions: List<UserSession> = emptyList()
        set(value) {
            field = value
            differ.submitList(buildMergedList(sessions = value))
        }

    init {
        differ.submitList(buildMergedList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SpeakerViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.item_speaker_info -> SpeakerInfoViewHolder(
                ItemSpeakerInfoBinding.inflate(inflater, parent, false)
            )
            R.layout.item_session -> SpeakerSessionViewHolder(
                ItemSessionBinding.inflate(inflater, parent, false).apply {
                    tags.setRecycledViewPool(tagRecycledViewPool)
                }
            )
            R.layout.item_generic_section_header -> HeaderViewHolder(
                ItemGenericSectionHeaderBinding.inflate(inflater, parent, false)
            )
            else -> throw IllegalStateException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: SpeakerViewHolder, position: Int) {
        when (holder) {
            is SpeakerInfoViewHolder -> holder.binding.executeAfter {
                viewModel = speakerViewModel
                lifecycleOwner = this@SpeakerAdapter.lifecycleOwner
            }
            is SpeakerSessionViewHolder -> holder.binding.executeAfter {
                userSession = differ.currentList[position] as UserSession
                eventListener = this@SpeakerAdapter.eventListener
                timeZoneId = speakerViewModel.timeZoneIdFlow
                showTime = true
                lifecycleOwner = this@SpeakerAdapter.lifecycleOwner
            }
            is HeaderViewHolder -> holder.binding.executeAfter {
                sectionHeader = differ.currentList[position] as SectionHeader
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (differ.currentList[position]) {
            SpeakerItem -> R.layout.item_speaker_info
            is UserSession -> R.layout.item_session
            is SectionHeader -> R.layout.item_generic_section_header
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
            merged += SectionHeader(R.string.speaker_events_subhead)
            merged.addAll(sessions)
        }
        return merged
    }
}

// Marker object for use in our merged representation.
object SpeakerItem

/**
 * Diff items presented by this adapter.
 */
object DiffCallback : DiffUtil.ItemCallback<Any>() {

    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem === SpeakerItem && newItem === SpeakerItem -> true
            oldItem is SectionHeader && newItem is SectionHeader -> oldItem == newItem
            oldItem is UserSession && newItem is UserSession ->
                oldItem.session.id == newItem.session.id
            else -> false
        }
    }

    @SuppressLint("DiffUtilEquals")
    // Workaround of https://issuetracker.google.com/issues/122928037
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
        val binding: ItemGenericSectionHeaderBinding
    ) : SpeakerViewHolder(binding.root)
}
