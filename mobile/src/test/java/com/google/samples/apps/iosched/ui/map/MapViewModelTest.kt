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
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.maps.model.LatLng
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.shared.domain.prefs.MyLocationOptedInUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.OptIntoMyLocationUseCase
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.google.samples.apps.iosched.test.util.fakes.FakeAnalyticsHelper
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import com.google.samples.apps.iosched.test.util.fakes.FakeSignInViewModelDelegate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineDispatcher
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
@ExperimentalCoroutinesApi
class MapViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule var syncTaskExecutorRule = SyncTaskExecutorRule()

    private val storage = FakePreferenceStorage()
    private val signInViewModelDelegate = FakeSignInViewModelDelegate()
    private lateinit var viewModel: MapViewModel

    private val testDispatcher = TestCoroutineDispatcher()

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
    fun testDataIsLoaded() {
        assertTrue(
            viewModel.conferenceLocationBounds.contains(
                // conference center
                LatLng(37.425842, -122.079933)
            )
        )
    }

    @Test
    fun myLocation() {
        signInViewModelDelegate.injectIsRegistered = true // On-site attendee
        signInViewModelDelegate.loadUser("1")
        assertNotNull(LiveDataTestUtil.getValue(viewModel.currentUserInfo))
        assertFalse(storage.myLocationOptedIn)
        assertTrue(LiveDataTestUtil.getValue(viewModel.showMyLocationOption)!!)
        viewModel.optIntoMyLocation()
        // The button is gone after opt-in
        assertFalse(LiveDataTestUtil.getValue(viewModel.showMyLocationOption)!!)
        // This happens when user revokes the permission on system settings.
        viewModel.optIntoMyLocation(false)
        assertTrue(LiveDataTestUtil.getValue(viewModel.showMyLocationOption)!!)
    }

    @Test
    fun myLocation_notSignedIn() {
        signInViewModelDelegate.injectIsSignedIn = false
        signInViewModelDelegate.loadUser("1")
        assertFalse(LiveDataTestUtil.getValue(viewModel.showMyLocationOption)!!)
    }

    @Test
    fun myLocation_notRegistered() {
        signInViewModelDelegate.injectIsRegistered = false
        signInViewModelDelegate.loadUser("1")
        assertFalse(LiveDataTestUtil.getValue(viewModel.showMyLocationOption)!!)
    }
}
