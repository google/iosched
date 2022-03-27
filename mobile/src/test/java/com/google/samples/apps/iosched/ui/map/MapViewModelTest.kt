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
import com.google.android.gms.maps.model.LatLng
import com.google.samples.apps.iosched.shared.domain.prefs.MyLocationOptedInUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.OptIntoMyLocationUseCase
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.util.fakes.FakeAnalyticsHelper
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import com.google.samples.apps.iosched.test.util.fakes.FakeSignInViewModelDelegate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * Unit tests for the [MapViewModel].
 */
class MapViewModelTest {

    @get:Rule var coroutineRule = MainCoroutineRule()

    private val storage = FakePreferenceStorage()
    private val signInViewModelDelegate = FakeSignInViewModelDelegate()
    private lateinit var viewModel: MapViewModel

    private val testDispatcher = coroutineRule.testDispatcher

    @Before
    fun createViewModel() {
        // Create ViewModel with the test data
        viewModel = MapViewModel(
            LoadGeoJsonFeaturesUseCase(mock(Context::class.java), testDispatcher),
            FakeAnalyticsHelper(),
            signInViewModelDelegate,
            OptIntoMyLocationUseCase(storage, testDispatcher),
            MyLocationOptedInUseCase(storage, testDispatcher)
        )
    }

    @Test
    fun testDataIsLoaded() = runTest {
        assertTrue(
            viewModel.conferenceLocationBounds.contains(
                // conference center
                LatLng(37.425842, -122.079933)
            )
        )
    }

    @Test
    fun myLocation() = runTest {
        signInViewModelDelegate.injectIsRegistered = true // On-site attendee
        signInViewModelDelegate.loadUser("1")
        assertNotNull(viewModel.userInfo)
        assertFalse(storage.myLocationOptedIn.first())
        assertTrue(viewModel.showMyLocationOption.first())
        viewModel.optIntoMyLocation()
        // The button is gone after opt-in
        assertFalse(viewModel.showMyLocationOption.first())
        // This happens when user revokes the permission on system settings.
        viewModel.optIntoMyLocation(false)
        assertTrue(viewModel.showMyLocationOption.first())
    }

    @Test
    fun myLocation_notSignedIn() = runTest {
        signInViewModelDelegate.injectIsSignedIn = false
        signInViewModelDelegate.loadUser("1")
        assertFalse(viewModel.showMyLocationOption.first())
    }

    @Test
    fun myLocation_notRegistered() = runTest {
        signInViewModelDelegate.injectIsRegistered = false
        signInViewModelDelegate.loadUser("1")
        assertFalse(viewModel.showMyLocationOption.first())
    }
}
