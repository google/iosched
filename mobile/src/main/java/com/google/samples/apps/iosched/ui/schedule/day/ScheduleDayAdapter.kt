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

package com.google.samples.apps.iosched.ui.schedule.day

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LiveData
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.samples.apps.iosched.databinding.ItemSessionBinding
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.ui.schedule.ScheduleEventListener
import com.google.samples.apps.iosched.widget.UnscrollableFlexboxLayoutManager
import org.threeten.bp.ZoneId

class ScheduleDayAdapter(
    private val eventListener: ScheduleEventListener,
    private val tagViewPool: RecycledViewPool,
    private val timeZoneId: LiveData<ZoneId>,
    private val lifecycleOwner: LifecycleOwner
) : ListAdapter<UserSession, SessionViewHolder>(SessionDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionViewHolder {
        val binding =
            ItemSessionBinding.inflate(LayoutInflater.from(parent.context), parent, false).apply {
                tags.apply {
                    setRecycledViewPool(tagViewPool)
                    // Use a customized FlexboxLayoutManager so that swiping the tags are doesn't
                    // trigger pull to refresh behavior.
                    layoutManager = UnscrollableFlexboxLayoutManager(parent.context).apply {
                        recycleChildrenOnDetach = true
                    }
                }
            }
        return SessionViewHolder(
            binding, eventListener, timeZoneId, lifecycleOwner
        )
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class SessionViewHolder(
    private val binding: ItemSessionBinding,
    private val eventListener: ScheduleEventListener,
    private val timeZoneId: LiveData<ZoneId>,
    private val lifecycleOwner: LifecycleOwner
) : ViewHolder(binding.root) {

    fun bind(userSession: UserSession) {
        binding.userSession = userSession
        binding.eventListener = eventListener
        binding.timeZoneId = timeZoneId
        binding.setLifecycleOwner(lifecycleOwner)
        binding.executePendingBindings()
    }
}

object SessionDiff : DiffUtil.ItemCallback<UserSession>() {
    override fun areItemsTheSame(
        oldItem: UserSession,
        newItem: UserSession
    ): Boolean {
        // We don't have to compare the #userEvent because the id of #session and #userEvent
        // should match
        return oldItem.session.id == newItem.session.id
    }

    override fun areContentsTheSame(oldItem: UserSession, newItem: UserSession): Boolean {
        return oldItem == newItem
    }
}
