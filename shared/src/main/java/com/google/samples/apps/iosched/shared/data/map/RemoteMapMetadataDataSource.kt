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

package com.google.samples.apps.iosched.shared.data.map

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

/**
 * TODO: Placeholder
 */
class RemoteMapMetadataDataSource : MapMetadataDataSource {

    private val EVENT_LOCATION_BOUNDS: LatLngBounds by lazy {
        LatLngBounds(LatLng(DEFAULT_VIEWPORT_WEST, DEFAULT_VIEWPORT_NORTH),
                LatLng(DEFAULT_VIEWPORT_EAST, DEFAULT_VIEWPORT_SOUTH))
    }

    private val MAP_VIEWPORT_MIN_ZOOM = 16f

    override fun getConferenceLocationBounds() = EVENT_LOCATION_BOUNDS

    override fun getMapViewportMinZoom() = MAP_VIEWPORT_MIN_ZOOM

    companion object {
        const val DEFAULT_VIEWPORT_WEST = 37.423205
        const val DEFAULT_VIEWPORT_NORTH = -122.081757
        const val DEFAULT_VIEWPORT_EAST = 37.428479
        const val DEFAULT_VIEWPORT_SOUTH = -122.078109
    }
}
