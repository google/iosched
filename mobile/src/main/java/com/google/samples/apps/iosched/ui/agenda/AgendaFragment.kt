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
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePaddingRelative
import androidx.databinding.BindingAdapter
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.RecyclerView
import com.google.samples.apps.iosched.databinding.FragmentAgendaBinding
import com.google.samples.apps.iosched.model.Block
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.ui.MainActivityViewModel
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import com.google.samples.apps.iosched.ui.signin.setupProfileMenuItem
import com.google.samples.apps.iosched.util.clearDecorations
import com.google.samples.apps.iosched.util.doOnApplyWindowInsets
import dagger.hilt.android.AndroidEntryPoint
import org.threeten.bp.ZoneId

@AndroidEntryPoint
class AgendaFragment : MainNavigationFragment() {

    private val viewModel: AgendaViewModel by viewModels()
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()
    private lateinit var binding: FragmentAgendaBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAgendaBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }
        // Pad the bottom of the RecyclerView so that the content scrolls up above the nav bar
        binding.recyclerView.doOnApplyWindowInsets { v, insets, padding ->
            v.updatePaddingRelative(
                bottom =
                padding.bottom + insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            )
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel
        binding.toolbar.setupProfileMenuItem(mainActivityViewModel, viewLifecycleOwner)
    }
}

@BindingAdapter(value = ["agendaItems", "timeZoneId"])
fun agendaItems(recyclerView: RecyclerView, list: List<Block>?, zoneId: ZoneId?) {
    list ?: return
    zoneId ?: return
    val isInConferenceTimeZone = TimeUtils.isConferenceTimeZone(zoneId)
    if (recyclerView.adapter == null) {
        recyclerView.adapter = AgendaAdapter()
    }
    (recyclerView.adapter as AgendaAdapter).apply {
        submitList(list)
        timeZoneId = zoneId
    }
    // Recreate the decoration used for the sticky date headers
    recyclerView.clearDecorations()
    if (list.isNotEmpty()) {
        recyclerView.addItemDecoration(
            AgendaHeadersDecoration(recyclerView.context, list, isInConferenceTimeZone)
        )
    }
}
