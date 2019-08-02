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

package com.google.samples.apps.iosched.ui.sessiondetail

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
import com.google.samples.apps.iosched.databinding.ItemSessionInfoBinding
import com.google.samples.apps.iosched.databinding.ItemSpeakerBinding
import com.google.samples.apps.iosched.model.Speaker
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailViewHolder.HeaderViewHolder
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailViewHolder.RelatedViewHolder
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailViewHolder.SessionInfoViewHolder
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailViewHolder.SpeakerViewHolder

/**
 * [RecyclerView.Adapter] for presenting a session details, composed of information about the
 * session, any speakers plus any related events.
 */
class SessionDetailAdapter(
    private val lifecycleOwner: LifecycleOwner,
    private val sessionDetailViewModel: SessionDetailViewModel,
    private val tagRecycledViewPool: RecycledViewPool
) : RecyclerView.Adapter<SessionDetailViewHolder>() {

    var speakers: List<Speaker> = emptyList()
        set(value) {
            field = value
            differ.submitList(buildMergedList(sessionSpeakers = value))
        }

    var related: List<UserSession> = emptyList()
        set(value) {
            field = value
            differ.submitList(buildMergedList(relatedSessions = value))
        }

    private val differ = AsyncListDiffer<Any>(this, DiffCallback)

    init {
        differ.submitList(buildMergedList())
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionDetailViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.item_session_info -> SessionInfoViewHolder(
                ItemSessionInfoBinding.inflate(inflater, parent, false)
            )
            R.layout.item_speaker_header -> HeaderViewHolder(
                inflater.inflate(viewType, parent, false)
            )
            R.layout.item_speaker -> SpeakerViewHolder(
                ItemSpeakerBinding.inflate(inflater, parent, false)
            )
            R.layout.item_related_header -> HeaderViewHolder(
                inflater.inflate(viewType, parent, false)
            )
            R.layout.item_session -> RelatedViewHolder(
                ItemSessionBinding.inflate(inflater, parent, false).apply {
                    tags.setRecycledViewPool(tagRecycledViewPool)
                }
            )
            else -> throw IllegalStateException("Unknown viewType $viewType")
        }
    }

    override fun onBindViewHolder(holder: SessionDetailViewHolder, position: Int) {
        when (holder) {
            is SessionInfoViewHolder -> holder.binding.apply {
                viewModel = sessionDetailViewModel
                tagViewPool = tagRecycledViewPool
                setLifecycleOwner(lifecycleOwner)
                executePendingBindings()
            }
            is SpeakerViewHolder -> holder.binding.apply {
                val presenter = differ.currentList[position] as Speaker
                speaker = presenter
                eventListener = sessionDetailViewModel
                setLifecycleOwner(lifecycleOwner)
                root.setTag(R.id.tag_speaker_id, presenter.id) // Used to identify clicked view
                executePendingBindings()
            }
            is RelatedViewHolder -> holder.binding.apply {
                userSession = differ.currentList[position] as UserSession
                eventListener = sessionDetailViewModel
                setLifecycleOwner(lifecycleOwner)
                executePendingBindings()
            }
            is HeaderViewHolder -> Unit // no-op
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (differ.currentList[position]) {
            is SessionItem -> R.layout.item_session_info
            is SpeakerHeaderItem -> R.layout.item_speaker_header
            is Speaker -> R.layout.item_speaker
            is RelatedHeaderItem -> R.layout.item_related_header
            is UserSession -> R.layout.item_session
            else -> throw IllegalStateException("Unknown view type at position $position")
        }
    }

    override fun getItemCount() = differ.currentList.size

    /**
     * This adapter displays heterogeneous data types but `RecyclerView` & `AsyncListDiffer` deal in
     * a single list of items. We therefore combine them into a merged list, using marker objects
     * for static items. We still hold separate lists of [speakers] and [related] sessions so that
     * we can provide them individually, as they're loaded.
     */
    private fun buildMergedList(
        sessionSpeakers: List<Speaker> = speakers,
        relatedSessions: List<UserSession> = related
    ): List<Any> {
        val merged = mutableListOf<Any>(SessionItem)
        if (sessionSpeakers.isNotEmpty()) {
            merged += SpeakerHeaderItem
            merged.addAll(sessionSpeakers)
        }
        if (relatedSessions.isNotEmpty()) {
            merged += RelatedHeaderItem
            merged.addAll(relatedSessions)
        }
        return merged
    }
}

// Marker objects for use in our merged representation.

object SessionItem

object SpeakerHeaderItem

object RelatedHeaderItem

/**
 * Diff items presented by this adapter.
 */
object DiffCallback : DiffUtil.ItemCallback<Any>() {
    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem === SessionItem && newItem === SessionItem -> true
            oldItem === SpeakerHeaderItem && newItem === SpeakerHeaderItem -> true
            oldItem === RelatedHeaderItem && newItem === RelatedHeaderItem -> true
            oldItem is Speaker && newItem is Speaker -> oldItem.id == newItem.id
            oldItem is UserSession && newItem is UserSession ->
                oldItem.session.id == newItem.session.id
            else -> false
        }
    }

    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is Speaker && newItem is Speaker -> oldItem == newItem
            oldItem is UserSession && newItem is UserSession -> oldItem == newItem
            else -> true
        }
    }
}

/**
 * [RecyclerView.ViewHolder] types used by this adapter.
 */
sealed class SessionDetailViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

    class SessionInfoViewHolder(
        val binding: ItemSessionInfoBinding
    ) : SessionDetailViewHolder(binding.root)

    class SpeakerViewHolder(
        val binding: ItemSpeakerBinding
    ) : SessionDetailViewHolder(binding.root)

    class RelatedViewHolder(
        val binding: ItemSessionBinding
    ) : SessionDetailViewHolder(binding.root)

    class HeaderViewHolder(
        itemView: View
    ) : SessionDetailViewHolder(itemView)
}
