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
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.samples.apps.iosched.shared.data.map.MapMetadataRepository
import com.google.samples.apps.iosched.shared.data.map.RemoteMapMetadataDataSource
import com.google.samples.apps.iosched.shared.usecases.repository.LoadConferenceLocationUseCase
import com.google.samples.apps.iosched.shared.usecases.repository.LoadConferenceMinZoomUseCase
import com.google.samples.apps.iosched.test.util.LiveDataTestUtil
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import org.junit.Assert
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the [MapViewModel].
 */
class MapViewModelTest {
    private val conferenceLocationBoundsWest = 37.423205
    private val conferenceLocationBoundsNorth = -122.081757
    private val conferenceLocationBoundsEast = 37.428479
    private val conferenceLocationBoundsSouth = -122.078109
    private val conferenceLocation = LatLngBounds(
            LatLng(conferenceLocationBoundsWest, conferenceLocationBoundsNorth),
            LatLng(conferenceLocationBoundsEast, conferenceLocationBoundsSouth)
    )
    private val mapMinZoom = 12f

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule var syncTaskExecutorRule = SyncTaskExecutorRule()

    @Test
    fun testDataIsLoaded_ObservablesUpdated() {
        // Create a test use cases with test data
        val loadConferenceLocationUseCase = createConferenceLocationUseCase(conferenceLocation)
        val loadConferenceMinZoomUseCase = createMapMinZoomUseCase(mapMinZoom)
        // Create ViewModel with the use case
        val viewModel = MapViewModel(loadConferenceLocationUseCase, loadConferenceMinZoomUseCase)

        Assert.assertEquals(conferenceLocation,
                LiveDataTestUtil.getValue(viewModel.conferenceLocationBounds))
        Assert.assertEquals(mapMinZoom, LiveDataTestUtil.getValue(viewModel.minZoom))
    }

    @Test
    fun testDataIsLoaded_Fails() {
        // Create ViewModel with the use case
        // TODO: BUG - this test fails because the execute is run async even though rules are set.
//        val viewModel = MapViewModel(createConferenceLocationExceptionUseCase(),
// createMapMinZoomExceptionUseCase())
//        Assert.assertTrue(LiveDataTestUtil.getValue(viewModel.errorMessageShown)!!)
    }

    /**
     * Creates a use case that will return the provided list of sessions.
     */
    private fun createConferenceLocationUseCase(
            bounds: LatLngBounds
    ): LoadConferenceLocationUseCase {
        return object : LoadConferenceLocationUseCase(
                MapMetadataRepository(RemoteMapMetadataDataSource())) {
            override fun execute(parameters: Unit): LatLngBounds {
                return bounds
            }
        }
    }

    /**
     * Creates a use case that throws an exception.
     */
    private fun createConferenceLocationExceptionUseCase(): LoadConferenceLocationUseCase {
        return object : LoadConferenceLocationUseCase(
                MapMetadataRepository(RemoteMapMetadataDataSource())) {
            override fun execute(parameters: Unit): LatLngBounds {
                throw Exception("Testing exception")
            }
        }
    }

    /**
     * Creates a use case that will return the provided list of sessions.
     */
    private fun createMapMinZoomUseCase(minZoom: Float): LoadConferenceMinZoomUseCase {
        return object : LoadConferenceMinZoomUseCase(
                MapMetadataRepository(RemoteMapMetadataDataSource())) {
            override fun execute(parameters: Unit): Float {
                return minZoom
            }
        }
    }

    /**
     * Creates a use case that throws an exception.
     */
    private fun createMapMinZoomExceptionUseCase(): LoadConferenceMinZoomUseCase {
        return object : LoadConferenceMinZoomUseCase(
                MapMetadataRepository(RemoteMapMetadataDataSource())) {
            override fun execute(parameters: Unit): Float {
                throw Exception("Testing exception")
            }
        }
    }
}