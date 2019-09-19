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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.BindingAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.samples.apps.iosched.databinding.FragmentAgendaBinding
import com.google.samples.apps.iosched.model.Block
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import com.google.samples.apps.iosched.util.clearDecorations
import dagger.android.support.DaggerFragment
import org.threeten.bp.ZoneId
import javax.inject.Inject

class AgendaFragment : DaggerFragment(), MainNavigationFragment {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: AgendaViewModel
    private lateinit var binding: FragmentAgendaBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentAgendaBinding.inflate(inflater, container, false).apply {
            setLifecycleOwner(this@AgendaFragment)
        }
        return binding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = viewModelProvider(viewModelFactory)
        binding.viewModel = viewModel
    }

    override fun onStart() {
        super.onStart()
        viewModel.refreshAgenda()
    }
}

@BindingAdapter(value = ["agendaItems", "timeZoneId"])
fun agendaItems(recyclerView: RecyclerView, list: List<Block>?, timeZoneId: ZoneId?) {
    if (recyclerView.adapter == null) {
        recyclerView.adapter = AgendaAdapter()
    }
    (recyclerView.adapter as AgendaAdapter).apply {
        this.submitList(list ?: emptyList())
        this.timeZoneId = timeZoneId ?: ZoneId.systemDefault()
        // Force a redraw in case the time zone has changed
        this.notifyDataSetChanged()
    }

    // Recreate the decoration used for the sticky date headers
    recyclerView.clearDecorations()
    if (list != null && list.isNotEmpty()) {
        recyclerView.addItemDecoration(
            AgendaHeadersDecoration(recyclerView.context, list)
        )
    }
}
