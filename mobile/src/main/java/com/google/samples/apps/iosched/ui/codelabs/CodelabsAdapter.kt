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

package com.google.samples.apps.iosched.ui.codelabs

import android.os.Bundle
import android.transition.TransitionInflater
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ItemCodelabBinding
import com.google.samples.apps.iosched.model.Codelab
import com.google.samples.apps.iosched.util.compatRemoveIf
import com.google.samples.apps.iosched.util.executeAfter

internal class CodelabsAdapter(
    savedState: Bundle?
) : ListAdapter<Codelab, CodelabViewHolder>(CodelabsDiffCallback) {

    companion object {
        private const val STATE_KEY_EXPANDED_IDS = "CodelabsAdapter:expandedIds"
    }

    private var expandedIds = mutableSetOf<String>()

    init {
        savedState?.getStringArray(STATE_KEY_EXPANDED_IDS)?.let {
            expandedIds.addAll(it)
        }
    }

    fun onSaveInstanceState(state: Bundle) {
        state.putStringArray(STATE_KEY_EXPANDED_IDS, expandedIds.toTypedArray())
    }

    override fun submitList(list: List<Codelab>?) {
        // Clear out any invalid IDs
        if (list == null) {
            expandedIds.clear()
        } else {
            val ids = list.map { it.id }
            expandedIds.compatRemoveIf { it !in ids }
        }
        super.submitList(list)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodelabViewHolder {
        return CodelabViewHolder(
            ItemCodelabBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: CodelabViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.executeAfter {
            codelab = item
            isExpanded = expandedIds.contains(item.id)
        }
        holder.itemView.setOnClickListener {
            val parent = holder.itemView.parent as? ViewGroup ?: return@setOnClickListener
            val expanded = holder.binding.isExpanded ?: false
            if (expanded) {
                expandedIds.remove(item.id)
            } else {
                expandedIds.add(item.id)
            }
            val transition = TransitionInflater.from(holder.itemView.context)
                .inflateTransition(R.transition.codelab_toggle)
            TransitionManager.beginDelayedTransition(parent, transition)
            holder.binding.executeAfter {
                isExpanded = !expanded
            }
        }
    }
}

internal class CodelabViewHolder(
    val binding: ItemCodelabBinding
) : ViewHolder(binding.root)

internal object CodelabsDiffCallback : DiffUtil.ItemCallback<Codelab>() {
    override fun areItemsTheSame(oldItem: Codelab, newItem: Codelab): Boolean =
        oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Codelab, newItem: Codelab): Boolean =
        oldItem == newItem
}

@BindingAdapter("codelabDuration")
fun codelabDuration(view: TextView, durationMinutes: Int) {
    view.text = view.resources.getString(R.string.codelab_duration, durationMinutes)
}
