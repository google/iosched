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

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.view.WindowManager
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.RecyclerView
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.util.collectLifecycleFlow
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MapVariantSelectionDialogFragment : AppCompatDialogFragment() {

    private val mapViewModel: MapViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    private lateinit var adapter: MapVariantAdapter

    // Normally we would implement onCreateDialog using a MaterialAlertDialogBuilder, but that
    // doesn't allow the dialog width to wrap its contents, and also doesn't allow positioning
    // of the dialog. Instead we implement onCreateView and handle the rest later.
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

        collectLifecycleFlow(mapViewModel.mapVariant) {
            adapter.currentSelection = it
        }
    }

    @SuppressLint("RtlHardcoded")
    override fun onStart() {
        super.onStart()
        requireDialog().window?.apply {
            // Don't dim the screen
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            // Position the window
            val isRtl = resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
            attributes.gravity = Gravity.BOTTOM or if (isRtl) Gravity.LEFT else Gravity.RIGHT
            // The window decor view's background shows behind the card, so remove it
            setBackgroundDrawable(null)
        }
        // We can't set margins in XML because when shown as a dialog, onCreateView is passed a null
        // container. LayoutParams are only generated when the view is added to a Dialog later.
        val margin = resources.getDimensionPixelSize(R.dimen.margin_normal)
        view?.updateLayoutParams<MarginLayoutParams> {
            setMargins(margin, margin, margin, margin)
        }
    }

    private fun selectMapVariant(mapVariant: MapVariant) {
        mapViewModel.setMapVariant(mapVariant)
        dismiss()
    }
}
