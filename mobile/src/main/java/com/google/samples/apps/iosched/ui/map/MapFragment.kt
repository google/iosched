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

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.Marker
import com.google.samples.apps.iosched.databinding.FragmentMapBinding
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import com.google.samples.apps.iosched.widget.BottomSheetBehavior
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.BottomSheetCallback
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class MapFragment : DaggerFragment(), MainNavigationFragment, OnMarkerClickListener {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var analyticsHelper: AnalyticsHelper

    private lateinit var viewModel: MapViewModel
    private var mapViewBundle: Bundle? = null
    private lateinit var mapView: MapView

    private lateinit var binding: FragmentMapBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    companion object {
        val TAG: String = MapFragment::class.java.simpleName
        private const val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"

        private const val ARG_FEATURE_ID = "arg.FEATURE_ID"

        fun newInstance(featureId: String): MapFragment {
            return MapFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FEATURE_ID, featureId)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            mapViewBundle = savedInstanceState.getBundle(MAPVIEW_BUNDLE_KEY)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        viewModel = viewModelProvider(viewModelFactory)
        binding = FragmentMapBinding.inflate(inflater, container, false).apply {
            setLifecycleOwner(this@MapFragment)
            viewModel = this@MapFragment.viewModel
        }

        mapView = binding.map.apply {
            onCreate(mapViewBundle)
        }

        initializeMap()
        analyticsHelper.sendScreenView("Map", requireActivity())

        if (savedInstanceState == null) {
            arguments?.getString(ARG_FEATURE_ID)?.let {
                viewModel.requestHighlightFeature(it)
            }
        }

        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetCallback {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // Orient the expand/collapse icon accordingly
                val rotation = when (newState) {
                    BottomSheetBehavior.STATE_EXPANDED -> 0f
                    BottomSheetBehavior.STATE_COLLAPSED -> 180f
                    BottomSheetBehavior.STATE_HIDDEN -> 180f
                    else -> return
                }
                binding.expandIcon.animate().rotationX(rotation).start()

                // Analytics
                if (newState == BottomSheetBehavior.STATE_EXPANDED) {
                    viewModel.logViewedMarkerDetails()
                }
            }
        })

        // Make the header clickable.
        binding.clickable.setOnClickListener {
            if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            } else {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            }
        }
        // Mimic elevation change by activating header shadow.
        binding.descriptionScrollview
            .setOnScrollChangeListener { v: NestedScrollView, _: Int, _: Int, _: Int, _: Int ->
                binding.sheetHeaderShadow.isActivated = v.canScrollVertically(-1)
            }

        viewModel.selectedMarkerInfo.observe(this, Observer {
            updateInfoSheet(it ?: return@Observer)
        })

        return binding.root
    }

    private fun updateInfoSheet(markerInfo: MarkerInfo) {
        val iconRes = getDrawableResourceForIcon(binding.markerIcon.context, markerInfo.iconName)
        binding.markerIcon.apply {
            setImageResource(iconRes)
            visibility = if (iconRes == 0) View.GONE else View.VISIBLE
        }

        binding.markerTitle.text = markerInfo.title

        val desc = Html.fromHtml(markerInfo.description ?: "")
        val descVisibility = if (desc.isEmpty()) View.GONE else View.VISIBLE
        binding.markerDescription.apply {
            text = desc
            visibility = descVisibility
        }

        // Hide/disable expansion affordances when there is no description.
        binding.expandIcon.visibility = descVisibility
        binding.clickable.isClickable = !desc.isEmpty()
    }

    override fun onBackPressed(): Boolean {
        if (::bottomSheetBehavior.isInitialized &&
            bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED
        ) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            return true
        }
        return super.onBackPressed()
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
        viewModel.onMapDestroyed()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }

    private fun initializeMap() {
        mapView.getMapAsync {
            viewModel.onMapReady(it)

            it?.apply {
                setOnMarkerClickListener(this@MapFragment)
                setOnMapClickListener { viewModel.onMapClick() }
                // TODO: if user has enabled location permission, enable that on map.
            }
        }
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        // This is a hack. We set the geojson feature ID as the snippet since there is no other way
        // to add metadata and we need to look up the feature again by ID.
        val id = marker.snippet ?: return false
        // Marker IDs can be comma-separated list of rooms. Uses the first ID if there's a comma,
        // or the whole ID if there is no comma.
        viewModel.requestHighlightFeature(id.split(",")[0])
        return true
    }
}
