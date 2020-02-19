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

package com.google.samples.apps.iosched.ui.signin

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.google.samples.apps.iosched.test.util.fakes.FakeSignInViewModelDelegate
import junit.framework.Assert.assertEquals
import junit.framework.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class SignInViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule
    var syncTaskExecutorRule = SyncTaskExecutorRule()

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun signedInUser_signsOut() {
        // Given a view model with a signed in user
        val signInViewModelDelegate = FakeSignInViewModelDelegate().apply {
            injectIsSignedIn = true
        }
        val viewModel = SignInViewModel(signInViewModelDelegate)

        // When sign out is requested
        viewModel.onSignOut()

        // Then a sign out request is emitted
        assertEquals(1, signInViewModelDelegate.signOutRequestsEmitted)
    }

    @Test
    fun noSignedInUser_signsIn() {
        // Given a view model with a signed out user
        val signInViewModelDelegate = FakeSignInViewModelDelegate().apply {
            injectIsSignedIn = false
        }
        val viewModel = SignInViewModel(signInViewModelDelegate)

        // When sign out is requested
        viewModel.onSignIn()

        // Then a sign out request is emitted
        assertEquals(1, signInViewModelDelegate.signInRequestsEmitted)
    }

    @Test
    fun onCancel_dialogDismiss() {
        // Given a view model with a signed in user
        val viewModel = SignInViewModel(FakeSignInViewModelDelegate())

        // When cancel is requested
        viewModel.onCancel()

        // Then the dialog is dismissed
        val dismissEvent = LiveDataTestUtil.getValue(viewModel.dismissDialogAction)
        assertNotNull(dismissEvent?.getContentIfNotHandled())
    }
}
