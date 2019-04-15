/*
 * Copyright 2019 Google LLC
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

import com.google.android.gms.maps.model.UrlTileProvider
import com.google.samples.apps.iosched.BuildConfig
import java.net.URL

class MapTileProvider(
    private val tileSize: Int,
    private val variant: MapVariant
) : UrlTileProvider(tileSize, tileSize) {

    companion object {
        // Order of format arguments: variant name, tile size, zoom level, tile x, tile y
        private const val BASE_URL = BuildConfig.MAP_TILE_URL_BASE + "/%s/%d/%d/%d_%d.png"

        private const val BASE_TILE_SIZE = 256

        fun forDensity(densityDpi: Float, variant: MapVariant): MapTileProvider {
            // Choose a size suitable for the given screen density. Adding .3f makes tvdpi (1.3x)
            // use a higher scale and looks nicer.
            val scale = Math.round(densityDpi + .3f)
                .coerceIn(1, 3) // we only support up to 3x the base tile size
            val tileSize = BASE_TILE_SIZE * scale
            return MapTileProvider(tileSize, variant)
        }
    }

    override fun getTileUrl(x: Int, y: Int, zoom: Int): URL {
        return URL(BASE_URL.format(variant.mapTilePrefix, tileSize, zoom, x, y))
    }
}
