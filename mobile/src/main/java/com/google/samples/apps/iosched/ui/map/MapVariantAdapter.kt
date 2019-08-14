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

package com.google.samples.apps.iosched.ui.map

import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ItemMapVariantBinding
import com.google.samples.apps.iosched.util.executeAfter

internal class MapVariantAdapter(
    private val callback: (MapVariant) -> Unit
) : Adapter<MapVariantViewHolder>() {

    var currentSelection: MapVariant? = null
        set(value) {
            if (field == value) {
                return
            }
            val previous = field
            if (previous != null) {
                notifyItemChanged(items.indexOf(previous)) // deselect previous selection
            }
            field = value
            if (value != null) {
                notifyItemChanged(items.indexOf(value)) // select new selection
            }
        }

    private val items = MapVariant.values().toMutableList().apply {
        sortBy { it.start }
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MapVariantViewHolder {
        return MapVariantViewHolder(
            ItemMapVariantBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: MapVariantViewHolder, position: Int) {
        val mapVariant = items[position]
        holder.bind(mapVariant, mapVariant == currentSelection, callback)
    }
}

internal class MapVariantViewHolder(
    val binding: ItemMapVariantBinding
) : ViewHolder(binding.root) {

    fun bind(mapVariant: MapVariant, isSelected: Boolean, callback: (MapVariant) -> Unit) {
        binding.executeAfter {
            variant = mapVariant
            isChecked = isSelected
        }
        itemView.setOnClickListener {
            callback(mapVariant)
        }
    }
}

// This is used instead of drawableStart="@{int_value}" because Databinding interprets the int as a
// color instead of a drawable resource ID.
@BindingAdapter("variantIcon")
fun variantIcon(view: TextView, @DrawableRes iconResId: Int) {
    val drawable = AppCompatResources.getDrawable(view.context, iconResId)
    // Below API 23 we need to apply the drawableTint manually.
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
        drawable?.setTintList(
            AppCompatResources.getColorStateList(view.context, R.color.map_variant_icon)
        )
    }
    view.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
}
