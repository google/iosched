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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdate
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.data.geojson.GeoJsonFeature
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.maps.android.data.geojson.GeoJsonPoint
import com.google.samples.apps.iosched.BuildConfig
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.domain.prefs.MyLocationOptedInUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.OptIntoMyLocationUseCase
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.successOr
import com.google.samples.apps.iosched.shared.result.updateOnSuccess
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.google.samples.apps.iosched.util.combine
import com.google.samples.apps.iosched.widget.BottomSheetBehavior
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val loadGeoJsonFeaturesUseCase: LoadGeoJsonFeaturesUseCase,
    private val analyticsHelper: AnalyticsHelper,
    private val signInViewModelDelegate: SignInViewModelDelegate,
    private val optIntoMyLocationUseCase: OptIntoMyLocationUseCase,
    myLocationOptedInUseCase: MyLocationOptedInUseCase
) : ViewModel(), SignInViewModelDelegate by signInViewModelDelegate {

    /**
     * Area covered by the venue. Determines the viewport of the map.
     */
    val conferenceLocationBounds = LatLngBounds(
        BuildConfig.MAP_VIEWPORT_BOUND_SW,
        BuildConfig.MAP_VIEWPORT_BOUND_NE
    )

    private val _mapVariant = MutableLiveData<MapVariant>()
    val mapVariant = Transformations.distinctUntilChanged(_mapVariant)

    private val _mapCenterEvent = MutableLiveData<Event<CameraUpdate>>()
    val mapCenterEvent: LiveData<Event<CameraUpdate>>
        get() = _mapCenterEvent

    private val loadGeoJsonResult = MutableLiveData<GeoJsonData>()

    private val _geoJsonLayer = MediatorLiveData<GeoJsonLayer?>()
    val geoJsonLayer: LiveData<GeoJsonLayer?>
        get() = _geoJsonLayer

    private val featureLookup: MutableMap<String, GeoJsonFeature> = mutableMapOf()
    private var hasLoadedFeatures = false
    private var requestedFeatureId: String? = null

    private val focusZoomLevel = BuildConfig.MAP_CAMERA_FOCUS_ZOOM
    private var currentZoomLevel = 16 // min zoom level supported

    private val _bottomSheetStateEvent = MediatorLiveData<Event<Int>>()
    val bottomSheetStateEvent: LiveData<Event<Int>>
        get() = _bottomSheetStateEvent
    private val _selectedMarkerInfo = MutableLiveData<MarkerInfo?>()
    val selectedMarkerInfo: LiveData<MarkerInfo?>
        get() = _selectedMarkerInfo

    private val myLocationOptedIn = MutableLiveData<Boolean>()

    val showMyLocationOption = currentUserInfo.combine(myLocationOptedIn) { info, optedIn ->
        // Show the button to enable "My Location" when the user is an on-site attendee and he/she
        // is not opted into the feature yet.
        info != null && info.isRegistered() && !optedIn
    }

    init {
        viewModelScope.launch {
            myLocationOptedIn.value = myLocationOptedInUseCase(Unit).successOr(false)
        }
        _geoJsonLayer.addSource(loadGeoJsonResult) { data ->
            hasLoadedFeatures = true
            setMapFeatures(data.featureMap)
            _geoJsonLayer.value = data.geoJsonLayer
        }

        // When the map variant changes, the selected feature might not be present in the new
        // variant, so hide the feature detail.
        _bottomSheetStateEvent.addSource(mapVariant) {
            dismissFeatureDetails()
        }
    }

    fun optIntoMyLocation(optIn: Boolean = true) {
        viewModelScope.launch {
            optIntoMyLocationUseCase(optIn).updateOnSuccess(myLocationOptedIn)
        }
    }

    fun setMapVariant(variant: MapVariant) {
        _mapVariant.value = variant
    }

    fun onMapDestroyed() {
        // The geo json layer is tied to the GoogleMap, so we should release it.
        hasLoadedFeatures = false
        featureLookup.clear()
        _geoJsonLayer.value = null
    }

    fun loadMapFeatures(googleMap: GoogleMap) {
        val variant = _mapVariant.value ?: return
        // Load markers
        viewModelScope.launch {
            loadGeoJsonFeaturesUseCase(
                LoadGeoJsonParams(googleMap, variant.markersResId)
            ).updateOnSuccess(loadGeoJsonResult)
        }
    }

    private fun setMapFeatures(features: Map<String, GeoJsonFeature>) {
        featureLookup.clear()
        featureLookup.putAll(features)
        updateFeaturesVisiblity(currentZoomLevel.toFloat())
        // if we have a pending request to highlight a feature, resolve it now
        val featureId = requestedFeatureId ?: return
        requestedFeatureId = null
        highlightFeature(featureId)
    }

    fun onZoomChanged(zoom: Float) {
        // Truncate the zoom and check if we're in the same level
        val zoomInt = zoom.toInt()
        if (currentZoomLevel != zoomInt) {
            currentZoomLevel = zoomInt
            updateFeaturesVisiblity(zoom)
        }
    }

    private fun updateFeaturesVisiblity(zoom: Float) {
        // Don't hide the marker if it's currently being focused on by the user
        val selectedId = selectedMarkerInfo.value?.id
        featureLookup.values.forEach { feature ->
            if (feature.id != selectedId) {
                val minZoom = feature.getProperty("minZoom")?.toFloatOrNull() ?: 0f
                feature.pointStyle.isVisible = zoom >= minZoom
            }
        }
    }

    fun requestHighlightFeature(featureId: String) {
        if (hasLoadedFeatures) {
            highlightFeature(featureId)
        } else {
            // save and re-evaluate when the map features are loaded
            requestedFeatureId = featureId
        }
    }

    private fun highlightFeature(featureId: String) {
        val feature = featureLookup[featureId] ?: return
        val geometry = feature.geometry as? GeoJsonPoint ?: return
        // center map on the requested feature.
        val update = CameraUpdateFactory.newLatLngZoom(geometry.coordinates, focusZoomLevel)
        _mapCenterEvent.value = Event(update)

        // publish feature data
        val title = feature.getProperty("title")
        _selectedMarkerInfo.value = MarkerInfo(
            featureId,
            title,
            feature.getProperty("subtitle"),
            feature.getProperty("description"),
            feature.getProperty("icon")
        )

        // bring bottom sheet into view
        _bottomSheetStateEvent.value = Event(BottomSheetBehavior.STATE_COLLAPSED)

        // Analytics
        analyticsHelper.logUiEvent(title, AnalyticsActions.MAP_MARKER_SELECT)
    }

    fun dismissFeatureDetails() {
        _bottomSheetStateEvent.value = Event(BottomSheetBehavior.STATE_HIDDEN)
        _selectedMarkerInfo.value = null
    }

    fun logViewedMarkerDetails() {
        val title = _selectedMarkerInfo.value?.title ?: return
        analyticsHelper.logUiEvent(title, AnalyticsActions.MAP_MARKER_DETAILS)
    }
}

data class MarkerInfo(
    val id: String,
    val title: String,
    val subtitle: String?,
    val description: String?,
    val iconName: String?
)
