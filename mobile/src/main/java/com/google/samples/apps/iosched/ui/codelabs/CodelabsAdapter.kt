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

import android.annotation.SuppressLint
import android.os.Bundle
import android.transition.TransitionInflater
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.google.android.flexbox.FlexboxLayoutManager
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.ItemCodelabBinding
import com.google.samples.apps.iosched.databinding.ItemCodelabsInformationCardBinding
import com.google.samples.apps.iosched.model.Codelab
import com.google.samples.apps.iosched.ui.codelabs.CodelabsViewHolder.CodelabItemHolder
import com.google.samples.apps.iosched.ui.codelabs.CodelabsViewHolder.CodelabsInformationCardHolder
import com.google.samples.apps.iosched.util.compatRemoveIf
import com.google.samples.apps.iosched.util.executeAfter

internal class CodelabsAdapter(
    private val codelabsActionsHandler: CodelabsActionsHandler,
    private val tagViewPool: RecycledViewPool,
    savedState: Bundle?
) : ListAdapter<Any, CodelabsViewHolder>(CodelabsDiffCallback) {

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

    override fun submitList(list: List<Any>?) {
        // Clear out any invalid IDs
        if (list == null) {
            expandedIds.clear()
        } else {
            val ids = list.filterIsInstance<Codelab>().map { it.id }
            expandedIds.compatRemoveIf { it !in ids }
        }
        super.submitList(list)
    }

    override fun getItemViewType(position: Int): Int {
        return when (val item = getItem(position)) {
            is Codelab -> R.layout.item_codelab
            is CodelabsInformationCard -> R.layout.item_codelabs_information_card
            else -> throw IllegalStateException("Unknown type: ${item::class.java.simpleName}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CodelabsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            R.layout.item_codelab -> CodelabItemHolder(
                ItemCodelabBinding.inflate(inflater, parent, false).apply {
                    actionHandler = codelabsActionsHandler
                    codelabTags.apply {
                        setRecycledViewPool(tagViewPool)
                        layoutManager = FlexboxLayoutManager(parent.context).apply {
                            recycleChildrenOnDetach = true
                        }
                    }
                }
            )
            R.layout.item_codelabs_information_card -> CodelabsInformationCardHolder(
                ItemCodelabsInformationCardBinding.inflate(inflater, parent, false).apply {
                    actionHandler = codelabsActionsHandler
                }
            )
            else -> throw IllegalArgumentException("Invalid viewType")
        }
    }

    override fun onBindViewHolder(holder: CodelabsViewHolder, position: Int) {
        if (holder is CodelabItemHolder) {
            bindCodelabItemHolder(holder, getItem(position) as Codelab)
        }
        // Other types don't need additional binding
    }

    private fun bindCodelabItemHolder(holder: CodelabItemHolder, item: Codelab) {
        holder.binding.executeAfter {
            codelab = item
            isExpanded = expandedIds.contains(item.id)
        }
        // In certain configurations the view already has a click listener to start the codelab.
        if (!holder.itemView.hasOnClickListeners()) {
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
}

// Marker objects for singleton items
object CodelabsInformationCard

internal sealed class CodelabsViewHolder(itemView: View) : ViewHolder(itemView) {

    class CodelabItemHolder(
        val binding: ItemCodelabBinding
    ) : CodelabsViewHolder(binding.root)

    class CodelabsInformationCardHolder(
        val binding: ItemCodelabsInformationCardBinding
    ) : CodelabsViewHolder(binding.root)
}

internal object CodelabsDiffCallback : DiffUtil.ItemCallback<Any>() {

    override fun areItemsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem === CodelabsInformationCard && newItem === CodelabsInformationCard -> true
            oldItem is Codelab && newItem is Codelab -> oldItem.id == newItem.id
            else -> false
        }
    }

    @SuppressLint("DiffUtilEquals")
    // Workaround of https://issuetracker.google.com/issues/122928037
    override fun areContentsTheSame(oldItem: Any, newItem: Any): Boolean {
        return when {
            oldItem is Codelab && newItem is Codelab -> oldItem == newItem
            else -> true
        }
    }
}

@BindingAdapter("codelabDuration")
fun codelabDuration(view: TextView, durationMinutes: Int) {
    view.text = view.resources.getString(R.string.codelab_duration, durationMinutes)
}
