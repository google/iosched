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

package com.google.samples.apps.iosched.shared.domain.repository

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.samples.apps.iosched.shared.data.map.MapMetadataDataSource
import com.google.samples.apps.iosched.shared.data.map.MapMetadataRepository
import com.google.samples.apps.iosched.shared.domain.map.LoadConferenceMinZoomUseCase
import com.google.samples.apps.iosched.shared.result.Result
import org.junit.Assert
import org.junit.Test

/**
 * Unit tests for [LoadConferenceMinZoomUseCaseTest]
 */
class LoadConferenceMinZoomUseCaseTest {

    private val conferenceLocationBoundsWest = 37.423205
    private val conferenceLocationBoundsNorth = -122.081757
    private val conferenceLocationBoundsEast = 37.428479
    private val conferenceLocationBoundsSouth = -122.078109
    private val conferenceLocation = LatLngBounds(
            LatLng(conferenceLocationBoundsWest, conferenceLocationBoundsNorth),
            LatLng(conferenceLocationBoundsEast, conferenceLocationBoundsSouth)
    )
    private val mapMinZoom = 12f

    @Test
    fun returnsMapMinZoom() {
        val useCase = LoadConferenceMinZoomUseCase(MapMetadataRepository(TestMapMetadataDataSource()))


        val actualZoom = (useCase.executeNow(Unit)
                as Result.Success<Float>).data

        // Expected values to assert
        val expectedZoom = mapMinZoom

        Assert.assertEquals(expectedZoom, actualZoom)
    }

    private inner class TestMapMetadataDataSource : MapMetadataDataSource {
        override fun getConferenceLocationBounds(): LatLngBounds {
            return conferenceLocation
        }

        override fun getMapViewportMinZoom(): Float {
            return mapMinZoom
        }
    }
}