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

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.content.Context
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.samples.apps.iosched.shared.data.map.MapMetadataRepository
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito

/**
 * Unit tests for the [MapViewModel].
 */
class MapViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule var syncTaskExecutorRule = SyncTaskExecutorRule()

    @Test
    fun testDataIsLoaded() {
        // Create ViewModel with the test data
        val viewModel = MapViewModel(
            TestMapMetadataRepository,
            LoadMapTileProviderUseCase(Mockito.mock(Context::class.java))
        )

        Assert.assertEquals(
            viewModel.conferenceLocationBounds,
            TestMapMetadataRepository.getConferenceLocationBounds()
        )
        Assert.assertEquals(
            viewModel.minZoom,
            TestMapMetadataRepository.getMapViewportMinZoom()
        )
        Assert.assertEquals(
            viewModel.defaultCameraPosition,
            TestMapMetadataRepository.getDefaultCameraPosition()
        )
    }

    object TestMapMetadataRepository: MapMetadataRepository {

        override fun getConferenceLocationBounds() = LatLngBounds(
            LatLng(37.423205, -122.081757),
            LatLng(37.428479, -122.078109)
        )
        override fun getMapViewportMinZoom() = 12f

        override fun getDefaultCameraPosition() = CameraPosition.builder()
            .bearing(330f)
            .tilt(0f)
            .zoom(15f)
            .target(LatLng(37.428479, -122.078109))
            .build()
    }
}
