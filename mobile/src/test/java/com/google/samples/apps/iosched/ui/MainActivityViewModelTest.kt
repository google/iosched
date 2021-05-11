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

package com.google.samples.apps.iosched.ui

import android.content.Context
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.firebase.functions.FirebaseFunctions
import com.google.gson.GsonBuilder
import com.google.samples.apps.iosched.model.TestDataRepository
import com.google.samples.apps.iosched.shared.data.ar.DefaultArDebugFlagEndpoint
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.domain.ar.LoadArDebugFlagUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadPinnedSessionsJsonUseCase
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.runBlockingTest
import com.google.samples.apps.iosched.test.util.fakes.FakeSignInViewModelDelegate
import com.google.samples.apps.iosched.test.util.fakes.FakeThemedActivityDelegate
import com.google.samples.apps.iosched.ui.schedule.TestUserEventDataSource
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.google.samples.apps.iosched.ui.theme.ThemedActivityDelegate
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

class MainActivityViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private fun createMainActivityViewModel(
        signInViewModelDelegate: SignInViewModelDelegate = FakeSignInViewModelDelegate(),
        themedActivityDelegate: ThemedActivityDelegate = FakeThemedActivityDelegate()
    ): MainActivityViewModel {
        return MainActivityViewModel(
            signInViewModelDelegate = signInViewModelDelegate,
            themedActivityDelegate = themedActivityDelegate,
            loadPinnedSessionsUseCase = LoadPinnedSessionsJsonUseCase(
                DefaultSessionAndUserEventRepository(
                    TestUserEventDataSource(), DefaultSessionRepository(TestDataRepository)
                ),
                GsonBuilder().create(),
                coroutineRule.testDispatcher
            ),
            loadArDebugFlagUseCase = LoadArDebugFlagUseCase(
                DefaultArDebugFlagEndpoint(
                    mock(FirebaseFunctions::class.java)
                ),
                coroutineRule.testDispatcher
            ),
            context = mock(Context::class.java)
        )
    }

    @Test
    fun notLoggedIn_profileClicked_showsSignInDialog() = coroutineRule.runBlockingTest {
        // Given a ViewModel with a signed out user
        val signInViewModelDelegate = FakeSignInViewModelDelegate().apply {
            injectIsSignedIn = false
        }
        val viewModel =
            createMainActivityViewModel(signInViewModelDelegate = signInViewModelDelegate)

        // When profile is clicked
        viewModel.onProfileClicked()

        // Then the sign in dialog should be shown
        val signInEvent = viewModel.navigationActions.first()
        assertEquals(signInEvent, MainNavigationAction.OpenSignIn)
    }

    @Test
    fun loggedIn_profileClicked_showsSignOutDialog() = coroutineRule.runBlockingTest {
        // Given a ViewModel with a signed in user
        val signInViewModelDelegate = FakeSignInViewModelDelegate().apply {
            injectIsSignedIn = true
        }
        val viewModel =
            createMainActivityViewModel(signInViewModelDelegate = signInViewModelDelegate)

        // When profile is clicked
        viewModel.onProfileClicked()

        // Then the sign out dialog should be shown
        val signOutEvent = viewModel.navigationActions.first()
        assertEquals(signOutEvent, MainNavigationAction.OpenSignOut)
    }
}
