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

package com.google.samples.apps.iosched.ui.agenda

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.samples.apps.iosched.BR
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.Block
import org.threeten.bp.ZoneId

class AgendaAdapter(var timeZoneId: ZoneId = ZoneId.systemDefault()) :
    ListAdapter<Block, AgendaViewHolder>(BlockDiff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AgendaViewHolder {
        return AgendaViewHolder(
            DataBindingUtil.inflate(LayoutInflater.from(parent.context), viewType, parent, false)
        )
    }

    override fun onBindViewHolder(holder: AgendaViewHolder, position: Int) {
        holder.bind(getItem(position), timeZoneId)
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isDark) {
            R.layout.item_agenda_dark
        } else {
            R.layout.item_agenda_light
        }
    }
}

class AgendaViewHolder(
    private val binding: ViewDataBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(block: Block, timeZoneId: ZoneId) {
        binding.setVariable(BR.agenda, block)
        binding.setVariable(BR.timeZoneId, timeZoneId)
        binding.executePendingBindings()
    }
}

object BlockDiff : DiffUtil.ItemCallback<Block>() {
    override fun areItemsTheSame(oldItem: Block, newItem: Block): Boolean {
        return oldItem.title == newItem.title &&
            oldItem.startTime == newItem.startTime &&
            oldItem.endTime == newItem.endTime
    }

    override fun areContentsTheSame(oldItem: Block, newItem: Block) = oldItem == newItem
}
