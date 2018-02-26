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
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.usecases.invoke
import com.google.samples.apps.iosched.shared.usecases.repository.LoadConferenceLocationUseCase
import com.google.samples.apps.iosched.shared.usecases.repository.LoadConferenceMinZoomUseCase
import com.google.samples.apps.iosched.shared.util.map
import javax.inject.Inject

class MapViewModel @Inject constructor(
        private val loadConferenceLocationUseCase: LoadConferenceLocationUseCase,
        private val loadConferenceMinZoomUseCase: LoadConferenceMinZoomUseCase
) : ViewModel() {

    /**
     * Markers for key locations on map (i.e. sessions, code labs, food, bathrooms, etc.)
     */
    val markers: LiveData<List<MarkerOptions?>>

    /**
     * Area covered by the venue. Determines the viewport of the map.
     */
    val conferenceLocationBounds: LiveData<LatLngBounds?>

    /**
     * Min zoom level for map.
     */
    val minZoom: LiveData<Float?>

    /**
     * True if any errors occur in fetching the data.
     */
    val errorMessageShown = MutableLiveData<Boolean>().apply { value = false }

    // TODO: add cameraTarget and zoomLevel to VM and wire it from domain layer to databinding.

    init {

        // TODO fetch markers.
        markers = MutableLiveData()

        // Fetch conference location.
        val conferenceLocationLiveResult = loadConferenceLocationUseCase()
        conferenceLocationBounds = conferenceLocationLiveResult.map {
            errorMessageShown.value = it is Result.Error
            (it as? Result.Success)?.data
        }

        // Fetch map min zoom.
        val minZoomLiveResult = loadConferenceMinZoomUseCase()
        minZoom = minZoomLiveResult.map {
            errorMessageShown.value = errorMessageShown.value!! || it is Result.Error
            (it as? Result.Success)?.data
        }

        // Check if there were errors.
        conferenceLocationLiveResult.map { conferenceLocationResult ->
            minZoomLiveResult.map { minZoomResult ->
                errorMessageShown.value =
                        conferenceLocationResult is Result.Error || minZoomResult is Result.Error
            }
        }

    }
}