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

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLngBounds
import com.google.samples.apps.iosched.shared.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

interface MapMetadataRepository {
    fun getConferenceLocationBounds(): LatLngBounds
    fun getMapViewportMinZoom(): Float
    fun getDefaultCameraPosition(): CameraPosition
}

@Singleton
class DefaultMapMetadataRepository @Inject constructor() : MapMetadataRepository {
    companion object {
        private val EVENT_LOCATION_BOUNDS = LatLngBounds(
            BuildConfig.MAP_VIEWPORT_BOUND_NW,
            BuildConfig.MAP_VIEWPORT_BOUND_SE
        )

        private const val MAP_VIEWPORT_MIN_ZOOM = BuildConfig.MAP_VIEWPORT_MIN_ZOOM

        private val DEFAULT_CAMERA = CameraPosition.Builder()
            .bearing(BuildConfig.MAP_DEFAULT_CAMERA_BEARING)
            .target(BuildConfig.MAP_DEFAULT_CAMERA_TARGET)
            .zoom(BuildConfig.MAP_DEFAULT_CAMERA_ZOOM)
            .tilt(BuildConfig.MAP_DEFAULT_CAMERA_TILT)
            .build()
    }

    override fun getConferenceLocationBounds() = EVENT_LOCATION_BOUNDS

    override fun getMapViewportMinZoom() = MAP_VIEWPORT_MIN_ZOOM

    override fun getDefaultCameraPosition() = DEFAULT_CAMERA
}
