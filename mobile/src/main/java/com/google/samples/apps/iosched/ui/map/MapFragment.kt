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

package com.google.samples.apps.iosched.ui.map

import android.arch.lifecycle.ViewModelProvider
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.MapView
import com.google.samples.apps.iosched.databinding.FragmentMapBinding
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class MapFragment : DaggerFragment(), MainNavigationFragment {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    private lateinit var viewModel: MapViewModel
    private var mapViewBundle: Bundle? = null
    private lateinit var mapView: MapView

    companion object {
        val TAG: String = MapFragment::class.java.simpleName
        fun newInstance() = MapFragment()

        const val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?): View? {
        viewModel = viewModelProvider(viewModelFactory)
        val binding = FragmentMapBinding.inflate(inflater, container, false).apply {
            setLifecycleOwner(this@MapFragment)
            viewModel = this@MapFragment.viewModel
        }

        mapView = binding.map.apply {
            onCreate(mapViewBundle)
        }

        initializeMap()

        return binding.root
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val mapViewBundle = outState.getBundle(MAPVIEW_BUNDLE_KEY)
                ?: Bundle().apply { putBundle(MAPVIEW_BUNDLE_KEY, this) }
        mapView.onSaveInstanceState(mapViewBundle)
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView.onStop()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    private fun initializeMap() {
        // TODO: Move this completely to BindingAdapter.

        mapView.getMapAsync {
            // TODO: Initialize marker icon bitmaps (active and normal).

            it?.apply {
                // TODO set maps style.

                setOnMarkerClickListener {
                    // TODO fwd to VM
                    false
                }
                setOnMapClickListener {
                    // TODO fwd to VM
                }
                // TODO: if user has enabled location permission, enable that on map.
            }
        }
    }
}
