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

import android.net.Uri
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfoBasic
import com.google.samples.apps.iosched.shared.domain.auth.ObserveUserAuthStateUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.NotificationsPrefIsShownUseCase
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.test.data.CoroutineScope
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import com.google.samples.apps.iosched.ui.schedule.FakeObserveUserAuthStateUseCase
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [FirebaseSignInViewModelDelegate]
 */
class FirebaseSignInViewModelDelegateTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun testSignedOut() = runTest {
        val subject = createFirebaseSignInViewModelDelegate(
            observeUserAuthStateUseCase = FakeObserveUserAuthStateUseCase(
                user = Result.Success(null),
                isRegistered = Result.Success(false),
                coroutineScope = coroutineRule.CoroutineScope(),
                coroutineDispatcher = coroutineRule.testDispatcher
            )
        )

        assertEquals(
            null,
            subject.userInfo.first()?.getUid()
        )
        assertEquals(
            null,
            subject.currentUserImageUri.first()
        )
        assertFalse(subject.isUserSignedInValue)
    }

    @Test
    fun testSignedInRegistered() = runTest {

        val user = mock<AuthenticatedUserInfoBasic> {
            on { getUid() }.doReturn("123")
            on { getPhotoUrl() }.doReturn(mock<Uri> {})
            on { isSignedIn() }.doReturn(true)
        }

        val fakeObserveUserAuthStateUseCase = FakeObserveUserAuthStateUseCase(
            user = Result.Success(user),
            isRegistered = Result.Success(true),
            coroutineScope = coroutineRule.CoroutineScope(),
            coroutineDispatcher = coroutineRule.testDispatcher
        )

        val subject = createFirebaseSignInViewModelDelegate(
            observeUserAuthStateUseCase = fakeObserveUserAuthStateUseCase
        )

        assertEquals(
            user.getUid(),
            subject.userInfo.first()?.getUid()
        )
        assertEquals(
            user.getPhotoUrl(),
            subject.currentUserImageUri.first()
        )
        assertTrue(subject.isUserSignedIn.first())
        assertTrue(subject.isUserSignedInValue)
        assertTrue(subject.isUserRegistered.first())
        assertTrue(subject.isUserRegisteredValue)
    }

    @Test
    fun testSignedInNotRegistered() = runTest {

        val user = mock<AuthenticatedUserInfoBasic> {
            on { getUid() }.doReturn("123")
            on { getPhotoUrl() }.doReturn(mock<Uri> {})
            on { isSignedIn() }.doReturn(true)
        }
        val fakeObserveUserAuthStateUseCase = FakeObserveUserAuthStateUseCase(
            user = Result.Success(user),
            isRegistered = Result.Success(false),
            coroutineScope = coroutineRule.CoroutineScope(),
            coroutineDispatcher = coroutineRule.testDispatcher
        )

        val subject = createFirebaseSignInViewModelDelegate(
            observeUserAuthStateUseCase = fakeObserveUserAuthStateUseCase
        )
        assertEquals(
            user.getUid(),
            subject.userInfo.first()?.getUid()
        )
        assertEquals(
            user.getPhotoUrl(),
            subject.currentUserImageUri.first()
        )

        assertTrue(subject.isUserSignedIn.first())
        assertTrue(subject.isUserSignedInValue)
        assertFalse(subject.isUserRegistered.first())
        assertFalse(subject.isUserRegisteredValue)
    }

    @Test
    fun testPostSignIn() = runTest {
        val subject = createFirebaseSignInViewModelDelegate(
            observeUserAuthStateUseCase = FakeObserveUserAuthStateUseCase(
                user = Result.Success(null),
                isRegistered = Result.Success(false),
                coroutineScope = coroutineRule.CoroutineScope(),
                coroutineDispatcher = coroutineRule.testDispatcher
            )
        )

        subject.emitSignInRequest()

        // Check that the emitted event is a sign in request
        assertEquals(
            subject.signInNavigationActions.first(),
            SignInNavigationAction.RequestSignIn
        )
    }

    @Test
    fun testPostSignOut() = runTest {
        val subject = createFirebaseSignInViewModelDelegate(
            observeUserAuthStateUseCase = FakeObserveUserAuthStateUseCase(
                user = Result.Success(null),
                isRegistered = Result.Success(false),
                coroutineScope = coroutineRule.CoroutineScope(),
                coroutineDispatcher = coroutineRule.testDispatcher
            )
        )

        subject.emitSignOutRequest()

        assertEquals(
            subject.signInNavigationActions.first(),
            SignInNavigationAction.RequestSignOut
        )
    }

    private fun createNotificationsPrefIsShownUseCase(): NotificationsPrefIsShownUseCase {
        return NotificationsPrefIsShownUseCase(
            FakePreferenceStorage(),
            coroutineRule.testDispatcher
        )
    }

    private fun createFirebaseSignInViewModelDelegate(
        observeUserAuthStateUseCase: ObserveUserAuthStateUseCase =
            FakeObserveUserAuthStateUseCase(
                user = Result.Success(null),
                isRegistered = Result.Success(true),
                coroutineScope = coroutineRule.CoroutineScope(),
                coroutineDispatcher = coroutineRule.testDispatcher
            ),
        notificationsPrefIsShownUseCase: NotificationsPrefIsShownUseCase =
            createNotificationsPrefIsShownUseCase(),
        ioDispatcher: CoroutineDispatcher = coroutineRule.testDispatcher,
        mainDispatcher: CoroutineDispatcher = coroutineRule.testDispatcher,
        isReservationEnabledByRemoteConfig: Boolean = true
    ): FirebaseSignInViewModelDelegate {
        return FirebaseSignInViewModelDelegate(
            observeUserAuthStateUseCase,
            notificationsPrefIsShownUseCase,
            ioDispatcher,
            mainDispatcher,
            isReservationEnabledByRemoteConfig,
            applicationScope = coroutineRule.CoroutineScope()
        )
    }
}
