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
import com.google.samples.apps.iosched.ui.schedule.ScheduleFilterAdapter.FilterViewHolder

/**
 * Adapter for the filters drawer
 */
class ScheduleFilterAdapter : Adapter<FilterViewHolder>() {

    // TODO(jdkoren) temporary just to see it render
    private var tags = listOf(
            Tag("1", "TRACK", 0, "Ads", 0xFFB0BEC5.toInt()),
            Tag("2", "TRACK", 1, "Android", 0xFFAED581.toInt()),
            Tag("3", "TRACK", 2, "Assistant", 0xFF1ce8b5.toInt()),
            Tag("4", "TRACK", 3, "Cloud", 0xFF80CBC4.toInt()),
            Tag("5", "TRACK", 4, "Design", 0xFFF8BBD0.toInt()),
            Tag("6", "TRACK", 5, "Firebase", 0xFFFFD54F.toInt()),
            Tag("7", "TRACK", 6, "IoT", 0xFFBCAAA4.toInt()),
            Tag("8", "TRACK", 7, "Location & Maps", 0xFFEF9A9A.toInt()),
            Tag("9", "TRACK", 8, "Machine Learning", 0xFFbcc8fb.toInt()),
            Tag("10", "TRACK", 9, "Misc", 0xFFC5C9E9.toInt()),
            Tag("11", "TRACK", 10, "Mobile Web", 0xFFFFF176.toInt()),
            Tag("12", "TRACK", 11, "Search", 0xFF90CAF9.toInt()),
            Tag("13", "TRACK", 12, "VR", 0xFFFF8A65.toInt())
    )

    override fun getItemCount() = tags.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            FilterViewHolder(parent.inflate(R.layout.list_item_filter_drawer, false))

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        holder.bind(tags[position])
    }

    fun clearFilters() {
        // TODO(jdkoren) uncheck all items
    }

    class FilterViewHolder(itemView: View) : ViewHolder(itemView) {

        private val label: TextView = itemView.findViewById(R.id.filter_label)
        private val checkbox: CheckBox = itemView.findViewById(R.id.filter_checkbox)

        private var tag: Tag? = null

        init {
            itemView.setOnClickListener { checkbox.performClick() }
        }

        // TODO(jdkoren): add databinding
        internal fun bind(tag: Tag) {
            this.tag = tag
            label.text = tag.name
            ViewCompat.setBackgroundTintList(label, ColorStateList.valueOf(tag.color))
        }
    }
}
