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

package com.google.samples.apps.iosched.ui.schedule

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.samples.apps.iosched.databinding.ItemScheduleDayIndicatorBinding
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.util.executeAfter

class DayIndicatorAdapter(
    private val scheduleViewModel: ScheduleViewModel,
    private val lifecycleOwner: LifecycleOwner
) : ListAdapter<DayIndicator, DayIndicatorViewHolder>(IndicatorDiff) {

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return getItem(position).day.start.toEpochSecond()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayIndicatorViewHolder {
        val binding = ItemScheduleDayIndicatorBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return DayIndicatorViewHolder(binding, scheduleViewModel, lifecycleOwner)
    }

    override fun onBindViewHolder(holder: DayIndicatorViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class DayIndicatorViewHolder(
    private val binding: ItemScheduleDayIndicatorBinding,
    private val scheduleViewModel: ScheduleViewModel,
    private val lifecycleOwner: LifecycleOwner
) : ViewHolder(binding.root) {

    fun bind(item: DayIndicator) {
        binding.executeAfter {
            indicator = item
            viewModel = scheduleViewModel
            lifecycleOwner = this@DayIndicatorViewHolder.lifecycleOwner
        }
    }
}

object IndicatorDiff : ItemCallback<DayIndicator>() {
    override fun areItemsTheSame(oldItem: DayIndicator, newItem: DayIndicator) =
        oldItem == newItem

    override fun areContentsTheSame(oldItem: DayIndicator, newItem: DayIndicator) =
        oldItem.areUiContentsTheSame(newItem)
}

@BindingAdapter("indicatorText", "inConferenceTimeZone", requireAll = true)
fun setIndicatorText(
    view: TextView,
    dayIndicator: DayIndicator,
    inConferenceTimeZone: Boolean
) {
    view.setText(TimeUtils.getShortLabelResForDay(dayIndicator.day, inConferenceTimeZone))
}
