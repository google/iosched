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

package com.google.samples.apps.iosched.ui.schedule

import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.samples.apps.iosched.databinding.ItemSessionBinding
import com.google.samples.apps.iosched.shared.model.Session

class ScheduleDayAdapter(
    private val eventListener: ScheduleEventListener
) : ListAdapter<Session, SessionViewHolder>(SessionDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        return SessionViewHolder(
            ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            eventListener
        )
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class SessionViewHolder(
    private val binding: ItemSessionBinding,
    private val eventListener: ScheduleEventListener
) : ViewHolder(binding.root) {

    fun bind(session: Session) {
        binding.session = session
        binding.eventListener = eventListener
        binding.executePendingBindings()
    }
}

object SessionDiff : DiffUtil.ItemCallback<Session>() {

    override fun areItemsTheSame(oldItem: Session?, newItem: Session?): Boolean {
        return oldItem?.id == newItem?.id
    }

    override fun areContentsTheSame(oldItem: Session?, newItem: Session?) = (oldItem == newItem)
}
