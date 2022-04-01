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

@file:Suppress("FunctionName")

package com.google.samples.apps.iosched.ui.onboarding

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.shared.domain.prefs.OnboardingCompleteActionUseCase
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import com.google.samples.apps.iosched.test.util.fakes.FakeSignInViewModelDelegate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the [OnboardingViewModel].
 */
class OnboardingViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun onGetStartedClicked_updatesPrefs() = runTest {
        // Given an onboarding view model
        val prefs = FakePreferenceStorage()
        val onboardingCompleteActionUseCase =
            OnboardingCompleteActionUseCase(prefs, coroutineRule.testDispatcher)
        val signInDelegate = FakeSignInViewModelDelegate()
        val viewModel = OnboardingViewModel(onboardingCompleteActionUseCase, signInDelegate)

        // When getStarted is called
        viewModel.getStartedClick()

        // Then verify that local storage was updated
        val onboardingCompleted = prefs.onboardingCompleted.first()
        assertTrue(onboardingCompleted)

        // And that the navigation event was fired
        val navigateEvent = viewModel.navigationActions.first()
        assertEquals(navigateEvent, OnboardingNavigationAction.NavigateToMainScreen)
    }

    @Test
    fun onSigninClicked() = runTest {
        // Given an onboarding view model
        val prefs = FakePreferenceStorage()
        val onboardingCompleteActionUseCase =
            OnboardingCompleteActionUseCase(prefs, coroutineRule.testDispatcher)
        val signInDelegate = FakeSignInViewModelDelegate()
        val viewModel = OnboardingViewModel(onboardingCompleteActionUseCase, signInDelegate)

        // When getStarted is called
        viewModel.onSigninClicked()

        // And that the navigation event was fired
        val navigateEvent = viewModel.navigationActions.first()
        assertEquals(navigateEvent, OnboardingNavigationAction.NavigateToSignInDialog)
    }
}
