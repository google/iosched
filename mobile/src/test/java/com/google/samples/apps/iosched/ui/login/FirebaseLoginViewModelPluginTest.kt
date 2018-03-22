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

package com.google.samples.apps.iosched.ui.login

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.net.Uri
import com.google.samples.apps.iosched.shared.data.login.AuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.data.login.AuthenticatedUserInfoBasic
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.test.util.LiveDataTestUtil
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.google.samples.apps.iosched.ui.schedule.FakeObserveUserAuthStateUseCase
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [FirebaseLoginViewModelPlugin]
 */
class FirebaseLoginViewModelPluginTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule var syncTaskExecutorRule = SyncTaskExecutorRule()

    @Test
    fun testLoggedOut() {
        val subject = FirebaseLoginViewModelPlugin(FakeObserveUserAuthStateUseCase(
                user = Result.Success(null),
                isRegistered = Result.Success(false)))

        val currentFirebaseUser =
                LiveDataTestUtil.getValue(subject.currentFirebaseUser) as Result.Success<AuthenticatedUserInfo>
        assertEquals(
                null,
                currentFirebaseUser.data.getUid()
        )
        assertEquals(
                null,
                LiveDataTestUtil.getValue(subject.currentUserImageUri)
        )
        assertFalse(subject.isLoggedIn())
    }

    @Test
    fun testLoggedInRegistered() {

        val user = mock<AuthenticatedUserInfoBasic> {
            on { getUid() }.doReturn("123")
            on { getPhotoUrl() }.doReturn(mock<Uri> {})
            on { isLoggedIn() }.doReturn(true)
        }
        val subject = FirebaseLoginViewModelPlugin(FakeObserveUserAuthStateUseCase(
                user = Result.Success(user),
                isRegistered = Result.Success(true)))

        assertEquals(
                user.getUid(),
                (LiveDataTestUtil.getValue(subject.currentFirebaseUser) as Result.Success).data.getUid()
        )
        assertEquals(
                user.getPhotoUrl(),
                LiveDataTestUtil.getValue(subject.currentUserImageUri))
        assertTrue(subject.isLoggedIn())
        assertTrue(subject.isRegistered())
    }

    @Test
    fun testLoggedInNotRegistered() {

        val user = mock<AuthenticatedUserInfoBasic> {
            on { getUid() }.doReturn("123")
            on { getPhotoUrl() }.doReturn(mock<Uri> {})
            on { isLoggedIn() }.doReturn(true)
        }
        val subject = FirebaseLoginViewModelPlugin(FakeObserveUserAuthStateUseCase(
                user = Result.Success(user),
                isRegistered = Result.Success(false)))

        assertEquals(
                user.getUid(),
                (LiveDataTestUtil.getValue(subject.currentFirebaseUser) as Result.Success).data.getUid()
        )
        assertEquals(
                user.getPhotoUrl(),
                LiveDataTestUtil.getValue(subject.currentUserImageUri))
        assertTrue(subject.isLoggedIn())
        assertFalse(subject.isRegistered())
    }


    @Test
    fun testPostLogin() {
        val subject = FirebaseLoginViewModelPlugin(FakeObserveUserAuthStateUseCase(
                user = Result.Success(null),
                isRegistered = Result.Success(false)))

        subject.emitLoginRequest()

        // Check that the emitted event is a login request
        assertEquals(
                LiveDataTestUtil.getValue(subject.performLoginEvent)?.peekContent(),
                LoginEvent.RequestLogin
        )
    }

    @Test
    fun testPostLogout() {
        val subject = FirebaseLoginViewModelPlugin(FakeObserveUserAuthStateUseCase(
                user = Result.Success(null),
                isRegistered = Result.Success(false)))

        subject.emitLogoutRequest()

        assertEquals(
                LiveDataTestUtil.getValue(subject.performLoginEvent)?.peekContent(),
                LoginEvent.RequestLogout
        )
    }
}
