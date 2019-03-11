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

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.MapView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentMapBinding
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.util.viewModelProvider
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import com.google.samples.apps.iosched.widget.BottomSheetBehavior
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.BottomSheetCallback
import org.threeten.bp.Instant
import javax.inject.Inject

class MapFragment : MainNavigationFragment() {

    @Inject lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject lateinit var analyticsHelper: AnalyticsHelper

    private lateinit var viewModel: MapViewModel
    private var mapViewBundle: Bundle? = null
    private lateinit var mapView: MapView

    private lateinit var binding: FragmentMapBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    companion object {
        private const val FRAGMENT_MY_LOCATION_RATIONALE = "my_location_rationale"

        private const val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"

        private const val ARG_FEATURE_ID = "arg.FEATURE_ID"
        private const val ARG_FEATURE_START_TIME = "arg.FEATURE_START_TIME"

        private const val REQUEST_LOCATION_PERMISSION = 1

        fun newInstance(featureId: String, featureStartTime: Long): MapFragment {
            return MapFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_FEATURE_ID, featureId)
                    putLong(ARG_FEATURE_START_TIME, featureStartTime)
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
            lifecycleOwner = viewLifecycleOwner
            viewModel = this@MapFragment.viewModel
        }

        mapView = binding.map.apply {
            onCreate(mapViewBundle)
        }

        initializeMap()
        analyticsHelper.sendScreenView("Map", requireActivity())

        if (savedInstanceState == null) {
            val featureId = arguments?.getString(ARG_FEATURE_ID)
            if (featureId.isNullOrEmpty()) {
                viewModel.setMapVariant(MapVariant.forTime())
            } else {
                viewModel.requestHighlightFeature(featureId)
                // Choose map variant based on feature's time
                val time = Instant.ofEpochMilli(arguments?.getLong(ARG_FEATURE_START_TIME) ?: 0L)
                viewModel.setMapVariant(MapVariant.forTime(time))
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

        viewModel.geoJsonLayer.observe(this, Observer {
            updateMarkers(it ?: return@Observer)
        })

        viewModel.selectedMarkerInfo.observe(this, Observer {
            updateInfoSheet(it ?: return@Observer)
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // This Fragment can appear in a standalone activity, so set up the toolbar accordingly.
        if (navigationHost != null) {
            val toolbar: Toolbar = view.findViewById(R.id.toolbar) ?: return
            toolbar.run {
                setNavigationIcon(R.drawable.ic_menu)
                setNavigationContentDescription(R.string.a11y_show_navigation)
                inflateMenu(R.menu.map_menu)
                menu.findItem(R.id.action_my_location)?.let { item ->
                    viewModel.showMyLocationOption.observe(viewLifecycleOwner, Observer { option ->
                        item.isVisible = (option == true)
                    })
                }
                setOnMenuItemClickListener { item ->
                    if (item.itemId == R.id.action_my_location) {
                        enableMyLocation(true)
                        true
                    } else {
                        false
                    }
                }
            }
        }
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
                setOnMapClickListener { viewModel.onMapClick() }
                enableMyLocation(false)
            }
        }
    }

    private fun updateMarkers(geoJsonLayer: GeoJsonLayer) {
        geoJsonLayer.addLayerToMap()
        geoJsonLayer.setOnFeatureClickListener { feature ->
            // Feature ID can be a comma-separated list. In that case use only the first ID.
            viewModel.requestHighlightFeature(feature.id.split(",")[0])
        }
    }

    private fun requestLocationPermission() {
        val context = context ?: return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED) {
            return
        }
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            MyLocationRationaleFragment()
                .show(childFragmentManager, FRAGMENT_MY_LOCATION_RATIONALE)
            return
        }
        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION_PERMISSION)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableMyLocation()
            } else {
                MyLocationRationaleFragment()
                    .show(childFragmentManager, FRAGMENT_MY_LOCATION_RATIONALE)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun enableMyLocation(requestPermission: Boolean = false) {
        val context = context ?: return
        when {
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED -> {
                mapView.getMapAsync {
                    it.isMyLocationEnabled = true
                }
                viewModel.optIntoMyLocation()
            }
            requestPermission -> requestLocationPermission()
            else -> viewModel.optIntoMyLocation(false)
        }
    }

    class MyLocationRationaleFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return MaterialAlertDialogBuilder(context)
                .setMessage(R.string.my_location_rationale)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    parentFragment!!.requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_LOCATION_PERMISSION)
                }
                .setNegativeButton(android.R.string.cancel, null) // Give up
                .create()
        }
    }
}
