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

package com.google.samples.apps.iosched.wear.ui.schedule

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.samples.apps.iosched.shared.model.UserSession
import com.google.samples.apps.iosched.wear.databinding.ItemSessionBinding

/**
 * Provides a binding from our ViewModel data to the views displayed within a WearableRecyclerView.
 */
class ScheduleAdapter(
    private val eventListener: ScheduleEventListener
) : ListAdapter<UserSession, SessionViewHolder>(SessionDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {

        val binding =
            ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SessionViewHolder(binding, eventListener)
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class SessionViewHolder(
    private val binding: ItemSessionBinding,
    private val eventListener: ScheduleEventListener
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(userSession: UserSession) {
        binding.session = userSession.session
        binding.userEvent = userSession.userEvent
        binding.eventListener = eventListener
        binding.executePendingBindings()
    }
}

object SessionDiff : DiffUtil.ItemCallback<UserSession>() {
    override fun areItemsTheSame(oldItem: UserSession, newItem: UserSession): Boolean {
        // Comparison based on the session from userEvent. If two session ids are the same,
        // we found a match.
        return oldItem.session.id == newItem.session.id
    }

    override fun areContentsTheSame(oldItem: UserSession, newItem: UserSession) = oldItem == newItem
}
