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

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.google.android.gms.maps.model.MarkerOptions
import com.google.samples.apps.iosched.shared.data.map.MapMetadataRepository
import javax.inject.Inject

class MapViewModel @Inject constructor(
    mapMetadataRepository: MapMetadataRepository
) : ViewModel() {

    /**
     * Markers for key locations on map (i.e. sessions, code labs, food, bathrooms, etc.)
     */
    val markers: LiveData<List<MarkerOptions?>>

    /**
     * Area covered by the venue. Determines the viewport of the map.
     */
    val conferenceLocationBounds = mapMetadataRepository.getConferenceLocationBounds()

    /**
     * Min zoom level for map.
     */
    val minZoom = mapMetadataRepository.getMapViewportMinZoom()

    /**
     * Default camera position
     */
    val defaultCameraPosition = mapMetadataRepository.getDefaultCameraPosition()

    /**
     * True if any errors occur in fetching the data.
     */
    val errorMessageShown = MutableLiveData<Boolean>().apply { value = false }

    // TODO: add cameraTarget and zoomLevel to VM and wire it from domain layer to databinding.

    init {

        // TODO fetch markers.
        markers = MutableLiveData()

        // TODO fetch tile
    }
}
