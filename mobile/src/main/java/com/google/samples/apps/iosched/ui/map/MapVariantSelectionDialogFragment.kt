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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.updatePaddingRelative
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.util.parentViewModelProvider
import com.google.samples.apps.iosched.util.doOnApplyWindowInsets
import com.google.samples.apps.iosched.widget.DaggerBottomSheetDialogFragment
import javax.inject.Inject

class MapVariantSelectionDialogFragment : DaggerBottomSheetDialogFragment() {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var mapViewModel: MapViewModel
    private lateinit var adapter: MapVariantAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map_variant_select, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = MapVariantAdapter(::selectMapVariant)
        view.findViewById<RecyclerView>(R.id.map_variant_list).adapter = adapter
        view.doOnApplyWindowInsets { v, insets, padding ->
            v.updatePaddingRelative(bottom = padding.bottom + insets.systemWindowInsetBottom)
        }

        mapViewModel = parentViewModelProvider(viewModelFactory)
        mapViewModel.mapVariant.observe(this, Observer {
            adapter.currentSelection = it
        })
    }

    override fun onStart() {
        super.onStart()
        val window = dialog?.window ?: return
        // Minor hack to make the bottom sheet draw behind the navigation bar.
        window.findViewById<View>(com.google.android.material.R.id.container)
            .fitsSystemWindows = false
        // Minor hack to not show the sheet collapsed in landscape
        (dialog as BottomSheetDialog).behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }

    private fun selectMapVariant(mapVariant: MapVariant) {
        mapViewModel.setMapVariant(mapVariant)
        dismiss()
    }
}
