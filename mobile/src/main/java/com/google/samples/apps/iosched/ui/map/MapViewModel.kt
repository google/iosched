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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.TileProvider
import com.google.samples.apps.iosched.BuildConfig
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.shared.util.map
import javax.inject.Inject

class MapViewModel @Inject constructor(
    loadMapTileProviderUseCase: LoadMapTileProviderUseCase
) : ViewModel() {

    /**
     * Area covered by the venue. Determines the viewport of the map.
     */
    val conferenceLocationBounds = LatLngBounds(
        BuildConfig.MAP_VIEWPORT_BOUND_NW,
        BuildConfig.MAP_VIEWPORT_BOUND_SE
    )

    /**
     * True if any errors occur in fetching the data.
     */
    val errorMessageShown = MutableLiveData<Boolean>().apply { value = false }

    private val _mapCenter = MutableLiveData<LatLng>()
    val mapCenter: LiveData<LatLng>
        get() = _mapCenter

    private val tileProviderResult = MutableLiveData<Result<TileProvider>>()
    val tileProvider: LiveData<TileProvider?>

    init {
        loadMapTileProviderUseCase(R.drawable.map_tile, tileProviderResult)
        tileProvider = tileProviderResult.map { result ->
            (result as? Success)?.data
        }
    }
}
