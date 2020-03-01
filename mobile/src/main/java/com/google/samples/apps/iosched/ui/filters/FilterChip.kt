/*
 * Copyright 2020 Google LLC
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

package com.google.samples.apps.iosched.ui.filters

import android.graphics.Color
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.Tag
import com.google.samples.apps.iosched.model.filters.Filter
import com.google.samples.apps.iosched.model.filters.Filter.DateFilter
import com.google.samples.apps.iosched.model.filters.Filter.MyScheduleFilter
import com.google.samples.apps.iosched.model.filters.Filter.TagFilter
import com.google.samples.apps.iosched.shared.util.TimeUtils

/** Wrapper model for showing [Filter] as a chip in the UI. */
data class FilterChip(
    val filter: Filter,
    val isSelected: Boolean,
    val categoryLabel: Int = 0,
    val color: Int = Color.parseColor("#4768fd"), // @color/indigo
    val selectedTextColor: Int = Color.WHITE,
    val textResId: Int = 0,
    val text: String = ""
)

fun Filter.asChip(isSelected: Boolean): FilterChip = when (this) {
    is TagFilter -> FilterChip(
        filter = this,
        isSelected = isSelected,
        color = tag.color,
        text = tag.displayName,
        selectedTextColor = tag.fontColor ?: Color.TRANSPARENT,
        categoryLabel = tag.filterCategoryLabel()
    )
    is DateFilter -> FilterChip(
        filter = this,
        isSelected = isSelected,
        textResId = TimeUtils.getShortLabelResForDay(day),
        categoryLabel = R.string.category_heading_dates
    )
    MyScheduleFilter -> FilterChip(
        filter = this,
        isSelected = isSelected,
        textResId = R.string.my_events,
        categoryLabel = R.string.category_heading_dates
    )
}

private fun Tag.filterCategoryLabel(): Int = when (this.category) {
    Tag.CATEGORY_TYPE -> R.string.category_heading_types
    Tag.CATEGORY_TOPIC -> R.string.category_heading_tracks
    Tag.CATEGORY_LEVEL -> R.string.category_heading_levels
    else -> 0
}
