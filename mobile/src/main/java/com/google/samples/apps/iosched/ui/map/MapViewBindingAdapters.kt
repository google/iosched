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

import androidx.annotation.DimenRes
import androidx.annotation.RawRes
import androidx.databinding.BindingAdapter
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.TileOverlayOptions
import com.google.samples.apps.iosched.util.getFloatUsingCompat

@BindingAdapter("mapStyle")
fun mapStyle(mapView: MapView, @RawRes resId: Int) {
    if (resId != 0) {
        mapView.getMapAsync { map ->
            map.setMapStyle(MapStyleOptions.loadRawResourceStyle(mapView.context, resId))
        }
    }
}

/**
 * Sets the map viewport to a specific rectangle specified by two Latitude/Longitude points.
 */
@BindingAdapter("mapViewport")
fun mapViewport(mapView: MapView, bounds: LatLngBounds?) {
    if (bounds != null) {
        mapView.getMapAsync {
            it.setLatLngBoundsForCameraTarget(bounds)
        }
    }
}

/**
 * Sets the minimum zoom level of the map (how far out the user is allowed to zoom).
 */
@BindingAdapter("mapMinZoom", "mapMaxZoom", requireAll = true)
fun mapZoomLevels(mapView: MapView, @DimenRes minZoomResId: Int, @DimenRes maxZoomResId: Int) {
    val minZoom = mapView.resources.getFloatUsingCompat(minZoomResId)
    val maxZoom = mapView.resources.getFloatUsingCompat(maxZoomResId)
    mapView.getMapAsync {
        it.setMinZoomPreference(minZoom)
        it.setMaxZoomPreference(maxZoom)
    }
}

@BindingAdapter("isIndoorEnabled")
fun isIndoorEnabled(mapView: MapView, isIndoorEnabled: Boolean?) {
    if (isIndoorEnabled != null) {
        mapView.getMapAsync {
            it.isIndoorEnabled = isIndoorEnabled
        }
    }
}

@BindingAdapter("isMapToolbarEnabled")
fun isMapToolbarEnabled(mapView: MapView, isMapToolbarEnabled: Boolean?) {
    if (isMapToolbarEnabled != null) {
        mapView.getMapAsync {
            it.uiSettings.isMapToolbarEnabled = isMapToolbarEnabled
        }
    }
}

@BindingAdapter("mapTileProvider")
fun mapTileProvider(mapView: MapView, mapVariant: MapVariant?) {
    mapVariant?.run {
        val tileProvider =
            MapTileProvider.forDensity(mapView.resources.displayMetrics.density, mapVariant)
        mapView.getMapAsync { map ->
            map.addTileOverlay(
                TileOverlayOptions()
                    .tileProvider(tileProvider)
                    .visible(true)
            )
        }
    }
}
