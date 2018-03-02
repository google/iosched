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

package com.google.samples.apps.iosched.ui.schedule.day.filters

import android.content.res.ColorStateList
import android.support.v4.view.ViewCompat
import android.support.v7.widget.RecyclerView.Adapter
import android.support.v7.widget.RecyclerView.ViewHolder
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.model.Tag
import com.google.samples.apps.iosched.shared.util.inflate
import com.google.samples.apps.iosched.ui.schedule.ScheduleViewModel
import com.google.samples.apps.iosched.ui.schedule.day.filters.ScheduleFilterAdapter.FilterViewHolder

/**
 * Adapter for the filters drawer
 */
class ScheduleFilterAdapter(val viewModel: ScheduleViewModel) : Adapter<FilterViewHolder>() {

    private var tags: List<Tag> = emptyList()

    override fun getItemCount() = tags.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            FilterViewHolder(parent.inflate(R.layout.list_item_filter_drawer, false))

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.bind(tags[position])
    }

    fun clearFilters() {
        // TODO(jdkoren) uncheck all items
        viewModel.clearFilters()
    }

    inner class FilterViewHolder(itemView: View) : ViewHolder(itemView) {

        private val label: TextView = itemView.findViewById(R.id.filter_label)
        private val checkbox: CheckBox = itemView.findViewById(R.id.filter_checkbox)

        private var tag: Tag? = null

        init {
            itemView.setOnClickListener {
                //TODO move to Data Binding
                checkbox.performClick()
                if (tag != null) {
                    viewModel.toggleFilter(tag!!, checkbox.isChecked)
                }
            }
        }

        // TODO(jdkoren): add databinding
        internal fun bind(tag: Tag) {
            this.tag = tag
            label.text = tag.name
            ViewCompat.setBackgroundTintList(label, ColorStateList.valueOf(tag.color))
        }
    }

    fun setItems(list: List<Tag>) {
        // TODO(jdkoren) use DiffUtil
        // TODO(jdkoren) reconcile new tags with current filters
        tags = list
        notifyDataSetChanged()
    }
}
