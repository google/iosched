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

package com.google.samples.apps.iosched.tv.ui.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.samples.apps.iosched.model.Tag
import com.google.samples.apps.iosched.tv.R

class TagAdapter : RecyclerView.Adapter<TagViewHolder>() {

    var tags = emptyList<Tag>()

    override fun getItemCount() = tags.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_session_tag, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = tags[position]

        holder.tagView?.apply {
            text = tag.name
            compoundDrawablesRelative[0]?.setTint(
                tagTintOrDefault(tag.color, context)
            )
        }
    }

    private fun tagTintOrDefault(color: Int, context: Context): Int {
        return if (color != Color.TRANSPARENT) {
            color
        } else {
            ContextCompat.getColor(context, R.color.default_tag_color)
        }
    }
}

class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tagView = itemView.findViewById<TextView>(R.id.tag_name)
}
