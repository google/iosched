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
import com.google.firebase.auth.UserInfo
import com.google.samples.apps.iosched.shared.data.login.AuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.test.util.LiveDataTestUtil
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.google.samples.apps.iosched.test.util.fakes.FakeAuthenticatedUser
import com.google.samples.apps.iosched.ui.login.LoginEvent.RequestLogin
import com.google.samples.apps.iosched.ui.login.LoginEvent.RequestLogout
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
        val subject = DefaultLoginViewModelPlugin(FakeAuthenticatedUser(null))

        assertEquals(
                null,
                LiveDataTestUtil.getValue(subject.currentFirebaseUser)
        )
        assertEquals(
                null,
                LiveDataTestUtil.getValue(subject.currentUserImageUri)
        )
        assertFalse(subject.isLoggedIn())
    }

    @Test
    fun testLoggedIn() {

        val subject =
                DefaultLoginViewModelPlugin(FakeAuthenticatedUser(FakeAuthenticatedUserInfo))
        assertEquals(
                FakeAuthenticatedUserInfo,
                (LiveDataTestUtil.getValue(subject.currentFirebaseUser) as Result.Success).data
        )
        assertEquals(
                FakeAuthenticatedUserInfo.getPhotoUrl(),
                LiveDataTestUtil.getValue(subject.currentUserImageUri))
        assertTrue(subject.isLoggedIn())
    }

    @Test
    fun testPostLogin() {
        val subject = DefaultLoginViewModelPlugin(FakeAuthenticatedUser(null))

        subject.emitLoginRequest()

        assertEquals(
                LiveDataTestUtil.getValue(subject.performLoginEvent)?.peekContent(), RequestLogin
        )
    }

    @Test
    fun testPostLogout() {
        val subject = DefaultLoginViewModelPlugin(FakeAuthenticatedUser(null))

        subject.emitLogoutRequest()

        assertEquals(
                LiveDataTestUtil.getValue(subject.performLoginEvent)?.peekContent(), RequestLogout
        )
    }
}

object FakeAuthenticatedUserInfo : AuthenticatedUserInfo {

    private val photo = mock<Uri>()
    private const val testUid = "testuid"

    override fun isLoggedIn(): Boolean = true

    override fun getEmail(): String? = TODO("not implemented")

    override fun getProviderData(): MutableList<out UserInfo>? = TODO("not implemented")

    override fun isAnonymous(): Boolean? = TODO("not implemented")

    override fun getPhoneNumber(): String? = TODO("not implemented")

    override fun getUid(): String? = testUid

    override fun isEmailVerified(): Boolean? = TODO("not implemented")

    override fun getDisplayName(): String? = TODO("not implemented")

    override fun getPhotoUrl(): Uri? = photo

    override fun getProviders(): MutableList<String>? = TODO("not implemented")

    override fun getProviderId(): String? = TODO("not implemented")

    override fun getLastSignInTimestamp(): Long? = TODO("not implemented")

    override fun getCreationTimestamp(): Long? = TODO("not implemented")

}