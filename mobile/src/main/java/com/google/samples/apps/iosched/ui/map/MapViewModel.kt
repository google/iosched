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
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.google.samples.apps.iosched.util.combine
import com.google.samples.apps.iosched.widget.BottomSheetBehavior
import javax.inject.Inject

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
        BuildConfig.MAP_VIEWPORT_BOUND_NW,
        BuildConfig.MAP_VIEWPORT_BOUND_SE
    )

    private val _mapVariant = MutableLiveData<MapVariant>()
    val mapVariant = Transformations.distinctUntilChanged(_mapVariant)

    /**
     * True if any errors occur in fetching the data.
     */
    val errorMessageShown = MutableLiveData<Boolean>().apply { value = false }

    private val _mapCenterEvent = MutableLiveData<Event<CameraUpdate>>()
    val mapCenterEvent: LiveData<Event<CameraUpdate>>
        get() = _mapCenterEvent

    private val loadGeoJsonResult = MutableLiveData<Result<GeoJsonData>>()

    private val _geoJsonLayer = MediatorLiveData<GeoJsonLayer>()
    val geoJsonLayer: LiveData<GeoJsonLayer>
        get() = _geoJsonLayer

    private val featureLookup: MutableMap<String, GeoJsonFeature> = mutableMapOf()
    private var hasLoadedFeatures = false
    private var requestedFeatureId: String? = null

    private val focusZoomLevel = BuildConfig.MAP_CAMERA_FOCUS_ZOOM

    private val _bottomSheetStateEvent = MediatorLiveData<Event<Int>>()
    val bottomSheetStateEvent: LiveData<Event<Int>>
        get() = _bottomSheetStateEvent
    private val _selectedMarkerInfo = MutableLiveData<MarkerInfo>()
    val selectedMarkerInfo: LiveData<MarkerInfo>
        get() = _selectedMarkerInfo

    private val myLocationOptedIn = MutableLiveData<Result<Boolean>>()

    val showMyLocationOption = currentUserInfo.combine(myLocationOptedIn) { info, optedIn ->
        // Show the button to enable "My Location" when the user is an on-site attendee and he/she
        // is not opted into the feature yet.
        info != null &&
            optedIn != null &&
            info.isRegistered() &&
            optedIn is Success &&
            !optedIn.data
    }

    init {
        myLocationOptedInUseCase(Unit, myLocationOptedIn)

        _geoJsonLayer.addSource(loadGeoJsonResult) { result ->
            if (result is Success) {
                hasLoadedFeatures = true
                _geoJsonLayer.value = result.data.geoJsonLayer
                setMapFeatures(result.data.featureMap)
            }
        }

        // When the map variant changes, the selected feature might not be present in the new
        // variant, so hide the feature detail.
        _bottomSheetStateEvent.addSource(mapVariant) {
            dismissFeatureDetails()
        }
    }

    fun optIntoMyLocation(optIn: Boolean = true) {
        optIntoMyLocationUseCase(optIn, myLocationOptedIn)
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
        loadGeoJsonFeaturesUseCase(
            LoadGeoJsonParams(googleMap, variant.markersResId),
            loadGeoJsonResult
        )
    }

    private fun setMapFeatures(features: Map<String, GeoJsonFeature>) {
        featureLookup.clear()
        featureLookup.putAll(features)
        // if we have a pending request to highlight a feature, resolve it now
        val featureId = requestedFeatureId ?: return
        requestedFeatureId = null
        highlightFeature(featureId)
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
            title,
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
    }

    fun logViewedMarkerDetails() {
        val title = _selectedMarkerInfo.value?.title ?: return
        analyticsHelper.logUiEvent(title, AnalyticsActions.MAP_MARKER_DETAILS)
    }
}

data class MarkerInfo(
    val title: String,
    val description: String?,
    val iconName: String?
)
