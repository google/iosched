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
import com.google.firebase.auth.FirebaseUser
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.test.util.LiveDataTestUtil
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.google.samples.apps.iosched.test.util.fakes.FakeFirebaseUserDataSource
import com.google.samples.apps.iosched.ui.login.LoginEvent.RequestLogin
import com.google.samples.apps.iosched.ui.login.LoginEvent.RequestLogout
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import org.junit.Rule
import org.junit.Test

class DefaultLoginViewModelPluginTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule var syncTaskExecutorRule = SyncTaskExecutorRule()

    @Test
    fun testLoggedOut() {
        val subject = DefaultLoginViewModelPlugin(FakeFirebaseUserDataSource(null))

        assertEquals(
                null,
                (LiveDataTestUtil.getValue(subject.currentFirebaseUser) as Result.Success).data
        )
        assertEquals(
                null,
                LiveDataTestUtil.getValue(subject.currentUserImageUri)
        )
        assertFalse(subject.isLoggedIn())
    }

    @Test
    fun testLoggedIn() {
        val mockUri = mock<Uri>()
        val mockFirebaseUser = mock<FirebaseUser> {
            on { photoUrl }.doReturn(mockUri)
        }
        val subject =
                DefaultLoginViewModelPlugin(FakeFirebaseUserDataSource(mockFirebaseUser))
        assertEquals(
                mockFirebaseUser,
                (LiveDataTestUtil.getValue(subject.currentFirebaseUser) as Result.Success).data
        )
        assertEquals(mockUri, LiveDataTestUtil.getValue(subject.currentUserImageUri))
        assertTrue(subject.isLoggedIn())
    }

    @Test
    fun testPostLogin() {
        val subject = DefaultLoginViewModelPlugin(FakeFirebaseUserDataSource(null))

        subject.emitLoginRequest()

        assertEquals(
                LiveDataTestUtil.getValue(subject.performLoginEvent)?.peekContent(), RequestLogin
        )
    }

    @Test
    fun testPostLogout() {
        val subject = DefaultLoginViewModelPlugin(FakeFirebaseUserDataSource(null))

        subject.emitLogoutRequest()

        assertEquals(
                LiveDataTestUtil.getValue(subject.performLoginEvent)?.peekContent(), RequestLogout
        )
    }
}