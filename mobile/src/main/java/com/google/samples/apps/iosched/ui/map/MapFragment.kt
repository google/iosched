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
import android.view.ViewGroup.MarginLayoutParams
import androidx.activity.addCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.MapView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.maps.android.ktx.awaitMap
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.databinding.FragmentMapBinding
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.ui.MainActivityViewModel
import com.google.samples.apps.iosched.ui.MainNavigationFragment
import com.google.samples.apps.iosched.ui.signin.setupProfileMenuItem
import com.google.samples.apps.iosched.util.doOnApplyWindowInsets
import com.google.samples.apps.iosched.util.launchAndRepeatWithViewLifecycle
import com.google.samples.apps.iosched.util.slideOffsetToAlpha
import com.google.samples.apps.iosched.widget.BottomSheetBehavior
import com.google.samples.apps.iosched.widget.BottomSheetBehavior.BottomSheetCallback
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.threeten.bp.Instant
import javax.inject.Inject

@AndroidEntryPoint
class MapFragment : MainNavigationFragment() {

    @Inject lateinit var analyticsHelper: AnalyticsHelper

    private val viewModel: MapViewModel by viewModels()
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()

    private var mapViewBundle: Bundle? = null
    private lateinit var mapView: MapView

    private lateinit var binding: FragmentMapBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<*>

    private var fabBaseMarginBottom = 0

    companion object {
        private const val FRAGMENT_MY_LOCATION_RATIONALE = "my_location_rationale"

        private const val MAPVIEW_BUNDLE_KEY = "MapViewBundleKey"

        private const val ARG_FEATURE_ID = "arg.FEATURE_ID"
        private const val ARG_FEATURE_START_TIME = "arg.FEATURE_START_TIME"

        private const val REQUEST_LOCATION_PERMISSION = 1

        // Threshold for when the marker description reaches maximum alpha. Should be a value
        // between 0 and 1, inclusive, coinciding with a point between the bottom sheet's
        // collapsed (0) and expanded (1) states.
        private const val ALPHA_TRANSITION_END = 0.5f

        // Threshold for when the marker description reaches minimum alpha. Should be a value
        // between 0 and 1, inclusive, coinciding with a point between the bottom sheet's
        // collapsed (0) and expanded (1) states.
        private const val ALPHA_TRANSITION_START = 0.1f

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

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            onBackPressed()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMapBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = this@MapFragment.viewModel
        }

        mapView = binding.map.apply {
            onCreate(mapViewBundle)
        }

        if (savedInstanceState == null) {
            MapFragmentArgs.fromBundle(arguments ?: Bundle.EMPTY).let { it ->
                if (!it.featureId.isNullOrEmpty()) {
                    viewModel.requestHighlightFeature(it.featureId)
                }

                val variant = when {
                    it.mapVariant != null -> MapVariant.valueOf(it.mapVariant)
                    it.startTime > 0L -> MapVariant.forTime(Instant.ofEpochMilli(it.startTime))
                    else -> MapVariant.forTime()
                }
                viewModel.setMapVariant(variant)
            }
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fabBaseMarginBottom = binding.mapModeFab.marginBottom

        binding.toolbar.run {
            setupProfileMenuItem(mainActivityViewModel, viewLifecycleOwner)

            menu.findItem(R.id.action_my_location)?.let { item ->
                launchAndRepeatWithViewLifecycle {
                    viewModel.showMyLocationOption.collect { option ->
                        item.isVisible = option
                    }
                }
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

        bottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet)
        val bottomSheetCallback = object : BottomSheetCallback {
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

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Due to content being visible beneath the navigation bar, apply a short alpha
                // transition
                binding.descriptionScrollview.alpha =
                    slideOffsetToAlpha(slideOffset, ALPHA_TRANSITION_START, ALPHA_TRANSITION_END)

                if (slideOffset > 0f) {
                    binding.mapModeFab.hide()
                } else {
                    binding.mapModeFab.show()
                    // Translate FAB to make room for the peeking sheet.
                    val ty = (bottomSheet.top - fabBaseMarginBottom - binding.mapModeFab.bottom)
                        .coerceAtMost(0)
                    binding.mapModeFab.translationY = ty.toFloat()
                }
            }
        }
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallback)
        // Trigger the callbacks once on layout to set the state of the sheet content & FAB.
        binding.bottomSheet.post {
            val state = bottomSheetBehavior.state
            val slideOffset = when (state) {
                BottomSheetBehavior.STATE_EXPANDED -> 1f
                BottomSheetBehavior.STATE_COLLAPSED -> 0f
                else -> -1f // BottomSheetBehavior.STATE_HIDDEN
            }
            bottomSheetCallback.onStateChanged(binding.bottomSheet, state)
            bottomSheetCallback.onSlide(binding.bottomSheet, slideOffset)
        }

        val originalPeekHeight = bottomSheetBehavior.peekHeight
        binding.root.doOnApplyWindowInsets { _, insets, _ ->
            binding.statusBar.updateLayoutParams<ConstraintLayout.LayoutParams> {
                height = insets.systemWindowInsetTop
            }
            binding.statusBar.isVisible = true
            binding.statusBar.requestLayout()

            // Update the Map padding so that the copyright, etc is not displayed in nav bar
            viewLifecycleOwner.lifecycleScope.launch {
                val map = binding.map.awaitMap()
                map.setPadding(0, 0, 0, insets.systemWindowInsetBottom)
            }

            binding.mapModeFab.updateLayoutParams<MarginLayoutParams> {
                bottomMargin = fabBaseMarginBottom + insets.systemWindowInsetBottom
            }

            binding.descriptionScrollview.updatePaddingRelative(
                bottom = insets.systemWindowInsetBottom
            )

            // The peek height should use the bottom system gesture inset since it is a scrolling
            // widget
            val gestureInsets = insets.systemGestureInsets
            bottomSheetBehavior.peekHeight = gestureInsets.bottom + originalPeekHeight
        }

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

        binding.mapModeFab.setOnClickListener {
            MapVariantSelectionDialogFragment().show(childFragmentManager, "MAP_MODE_DIALOG")
        }

        // Initialize MapView
        viewLifecycleOwner.lifecycleScope.launch {
            mapView.awaitMap().apply {
                setOnMapClickListener { viewModel.dismissFeatureDetails() }
                setOnCameraMoveListener {
                    viewModel.onZoomChanged(cameraPosition.zoom)
                }
                enableMyLocation()
            }
        }

        launchAndRepeatWithViewLifecycle {
            launch {
                // Observe ViewModel data
                viewModel.mapVariant.collect {
                    mapView.awaitMap().apply {
                        clear()
                        viewModel.loadMapFeatures(this)
                    }
                }
            }

            // Set the center of the map's camera. Call this every time the user selects a marker.
            launch {
                viewModel.mapCenterEvent.collect { update ->
                    mapView.getMapAsync {
                        it.animateCamera(update)
                    }
                }
            }

            launch {
                viewModel.bottomSheetStateEvent.collect { event ->
                    BottomSheetBehavior.from(binding.bottomSheet).state = event
                }
            }

            launch {
                viewModel.geoJsonLayer.collect {
                    it?.let {
                        updateMarkers(it)
                    }
                }
            }
            launch {
                viewModel.selectedMarkerInfo.collect {
                    it?.let {
                        updateInfoSheet(it)
                    }
                }
            }
        }

        analyticsHelper.sendScreenView("Map", requireActivity())
    }

    private fun updateInfoSheet(markerInfo: MarkerInfo) {
        val iconRes = getDrawableResourceForIcon(binding.markerIcon.context, markerInfo.iconName)
        binding.markerIcon.apply {
            setImageResource(iconRes)
            visibility = if (iconRes == 0) View.GONE else View.VISIBLE
        }

        binding.markerTitle.text = markerInfo.title
        binding.markerSubtitle.apply {
            text = markerInfo.subtitle
            isVisible = !markerInfo.subtitle.isNullOrEmpty()
        }

        val description = Html.fromHtml(markerInfo.description ?: "")
        val hasDescription = description.isNotEmpty()
        binding.markerDescription.apply {
            text = description
            isVisible = hasDescription
        }

        // Hide/disable expansion affordances when there is no description.
        binding.expandIcon.isVisible = hasDescription
        binding.clickable.isVisible = hasDescription
    }

    private fun onBackPressed(): Boolean {
        if (::bottomSheetBehavior.isInitialized &&
            bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED
        ) {
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            return true
        }
        return false
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
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            MyLocationRationaleFragment()
                .show(childFragmentManager, FRAGMENT_MY_LOCATION_RATIONALE)
            return
        }
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_LOCATION_PERMISSION
        )
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
                lifecycleScope.launch {
                    val map = mapView.awaitMap()
                    map.isMyLocationEnabled = true
                }
                viewModel.optIntoMyLocation()
            }
            requestPermission -> requestLocationPermission()
            else -> viewModel.optIntoMyLocation(false)
        }
    }

    class MyLocationRationaleFragment : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
            return MaterialAlertDialogBuilder(requireContext())
                .setMessage(R.string.my_location_rationale)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    requireParentFragment().requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_LOCATION_PERMISSION
                    )
                }
                .setNegativeButton(android.R.string.cancel, null) // Give up
                .create()
        }
    }
}
