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

import android.content.Context
import android.text.TextUtils
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.data.geojson.GeoJsonFeature
import com.google.maps.android.data.geojson.GeoJsonLayer
import com.google.samples.apps.iosched.shared.domain.UseCase
import javax.inject.Inject

/** Parameters for this use case. */
typealias LoadGeoJsonParams = Pair<GoogleMap, Int>

/** Data loaded by this use case. */
data class GeoJsonData(
    val geoJsonLayer: GeoJsonLayer,
    val featureMap: Map<String, GeoJsonFeature>
)

/** Use case that loads a GeoJsonLayer and its features. */
class LoadGeoJsonFeaturesUseCase @Inject constructor(
    private val context: Context
) : UseCase<LoadGeoJsonParams, GeoJsonData>() {

    override fun execute(parameters: LoadGeoJsonParams): GeoJsonData {
        val layer = GeoJsonLayer(parameters.first, parameters.second, context)
        processGeoJsonLayer(layer, context)
        layer.isLayerOnMap
        return GeoJsonData(layer, buildFeatureMap(layer))
    }

    private fun buildFeatureMap(layer: GeoJsonLayer): Map<String, GeoJsonFeature> {
        val featureMap: MutableMap<String, GeoJsonFeature> = mutableMapOf()
        layer.features.forEach {
            val id = it.getProperty("id")
            if (!TextUtils.isEmpty(id)) {
                // Marker can map to multiple room IDs
                for (part in id.split(",")) {
                    featureMap[part] = it
                }
            }
        }
        return featureMap
    }
}
