/*
 * Copyright 2021 Google LLC
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

package com.google.samples.apps.iosched.ui.sessioncommon

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.data.prefs.PreferenceStorage
import com.google.samples.apps.iosched.shared.domain.prefs.StopSnackbarActionUseCase
import com.google.samples.apps.iosched.shared.domain.users.StarEventAndNotifyUseCase
import com.google.samples.apps.iosched.test.data.CoroutineScope
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.data.runBlockingTest
import com.google.samples.apps.iosched.test.util.fakes.FakeAnalyticsHelper
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import com.google.samples.apps.iosched.test.util.fakes.FakeSignInViewModelDelegate
import com.google.samples.apps.iosched.test.util.fakes.FakeStarEventUseCase
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import kotlinx.coroutines.flow.first
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class OnSessionStarClickDelegateTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val testDispatcher = coroutineRule.testDispatcher

    private fun createOnSessionStarClickDelegate(
        signInViewModelDelegate: SignInViewModelDelegate = FakeSignInViewModelDelegate(),
        starEventUseCase: StarEventAndNotifyUseCase = FakeStarEventUseCase(testDispatcher),
        snackbarMessageManager: SnackbarMessageManager = createSnackbarMessageManager(),
        analyticsHelper: AnalyticsHelper = FakeAnalyticsHelper()
    ): OnSessionStarClickDelegate {
        return DefaultOnSessionStarClickDelegate(
            signInViewModelDelegate,
            starEventUseCase,
            snackbarMessageManager,
            analyticsHelper,
            coroutineRule.CoroutineScope(),
            coroutineRule.testDispatcher
        )
    }

    private fun createSnackbarMessageManager(
        preferenceStorage: PreferenceStorage = FakePreferenceStorage()
    ): SnackbarMessageManager {
        return SnackbarMessageManager(
            preferenceStorage,
            coroutineRule.CoroutineScope(),
            StopSnackbarActionUseCase(preferenceStorage, testDispatcher)
        )
    }

    @Test
    fun testStarEvent() {
        val snackbarMessageManager = createSnackbarMessageManager()
        val delegate = createOnSessionStarClickDelegate(
            snackbarMessageManager = snackbarMessageManager
        )

        delegate.onStarClicked(TestData.userSession0)

        val message = snackbarMessageManager.currentSnackbar.value
        assertEquals(R.string.event_starred, message?.messageId)
        assertEquals(R.string.dont_show, message?.actionId)
    }

    @Test
    fun testUnstarEvent() {
        val snackbarMessageManager = createSnackbarMessageManager()
        val delegate = createOnSessionStarClickDelegate(
            snackbarMessageManager = snackbarMessageManager
        )

        delegate.onStarClicked(TestData.userSession1)

        val message = snackbarMessageManager.currentSnackbar.value
        assertEquals(R.string.event_unstarred, message?.messageId)
        assertEquals(R.string.dont_show, message?.actionId)
    }

    @Test
    fun testStar_notLoggedInUser() = coroutineRule.runBlockingTest {
        // Create test use cases with test data
        val signInDelegate = FakeSignInViewModelDelegate()
        signInDelegate.injectIsSignedIn = false

        val snackbarMessageManager = createSnackbarMessageManager()

        val delegate = createOnSessionStarClickDelegate(
            signInViewModelDelegate = signInDelegate,
            snackbarMessageManager = snackbarMessageManager
        )

        delegate.onStarClicked(TestData.userSession1)

        val message = snackbarMessageManager.currentSnackbar.value
        Assert.assertNull(message)

        // Verify that the sign in dialog was triggered
        val signInEvent = delegate.navigateToSignInDialogEvents.first()
        assertNotNull(signInEvent)
    }
}
