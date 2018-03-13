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

package com.google.samples.apps.iosched.shared.data.login

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuth.AuthStateListener
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.test.util.LiveDataTestUtil
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Tests for [DefaultFirebaseUserDataSource].
 */
class DefaultFirebaseUserDataSourceTest {

    @Rule
    @JvmField
    val instantTaskExecutor = InstantTaskExecutorRule()

    private val expected = "a user token"

    private val tokenResult = GetTokenResult(expected)

    private val mockSuccessfulTask = mock<Task<GetTokenResult>> {
        on { isSuccessful }.doReturn(true)
        on { result }.doReturn(tokenResult)
    }

    private val mockUnsuccessfulTask = mock<Task<GetTokenResult>> {
        on { isSuccessful }.doReturn(false)
        on { result }.doReturn(tokenResult)
    }

    private val mockNonAnonymousUser = mock<FirebaseUser> {
        on { isAnonymous }.doReturn(false)
        on { getIdToken(false) }.doReturn(mockSuccessfulTask)
    }

    private val mockNonAnonymousUserUnsuccessfulTaskUser = mock<FirebaseUser> {
        on { isAnonymous }.doReturn(false)
        on { getIdToken(false) }.doReturn(mockUnsuccessfulTask)
    }

    private val mockAnonymousUser = mock<FirebaseUser> {
        on { isAnonymous }.doReturn(true)
        on { getIdToken(false) }.doReturn(mockSuccessfulTask)
    }

    private val mockedNonAnonymousFirebaseAuth = mock<FirebaseAuth> {
        on { currentUser }.doReturn(mockNonAnonymousUser)
    }

    private val mockedNonAnonymousUnsuccessfulFirebaseAuth = mock<FirebaseAuth> {
        on { currentUser }.doReturn(mockNonAnonymousUserUnsuccessfulTaskUser)
    }

    private val mockedAnonymousFirebaseAuth = mock<FirebaseAuth> {
        on { currentUser }.doReturn(mockAnonymousUser)
    }

    @Test
    fun loggedInUser() {
        val subject = DefaultFirebaseUserDataSource(mockedNonAnonymousFirebaseAuth)

        // trigger onAuthStateChanged
        argumentCaptor<AuthStateListener>().apply {
            verify(mockedNonAnonymousFirebaseAuth).addAuthStateListener(capture())
            lastValue.onAuthStateChanged(mockedNonAnonymousFirebaseAuth)
        }

        // trigger OnCompleteListener.onComplete
        argumentCaptor<OnCompleteListener<GetTokenResult>>().apply {

            verify(mockSuccessfulTask).addOnCompleteListener(capture())
            lastValue.onComplete(mockSuccessfulTask)
        }

        val currentUser = LiveDataTestUtil.getValue(subject.getCurrentUser())
        assertThat(
                (currentUser as Result.Success).data?.isAnonymous,
                `is`(equalTo(false)))

        val token = LiveDataTestUtil.getValue(subject.getToken())
        assertThat((token as Result.Success).data, `is`(equalTo(expected)))
    }

    @Test
    fun anonymousUser() {
        val subject = DefaultFirebaseUserDataSource(mockedAnonymousFirebaseAuth)

        // trigger onAuthStateChanged
        argumentCaptor<AuthStateListener>().apply {
            verify(mockedAnonymousFirebaseAuth).addAuthStateListener(capture())
            lastValue.onAuthStateChanged(mockedAnonymousFirebaseAuth)
        }

        // trigger OnCompleteListener.onComplete
        argumentCaptor<OnCompleteListener<GetTokenResult>>().apply {

            verify(mockSuccessfulTask).addOnCompleteListener(capture())
            lastValue.onComplete(mockSuccessfulTask)
        }

        val value = LiveDataTestUtil.getValue(subject.getCurrentUser())
        assertThat(
                (value as Result.Success).data?.isAnonymous,
                `is`(equalTo(true)))

        val token = LiveDataTestUtil.getValue(subject.getToken())
        assertThat((token as Result.Success).data, `is`(equalTo(expected)))

    }

    @Test
    fun errorGettingIdToken() {
        val subject = DefaultFirebaseUserDataSource(mockedNonAnonymousUnsuccessfulFirebaseAuth)

        // trigger onAuthStateChanged
        argumentCaptor<AuthStateListener>().apply {
            verify(mockedNonAnonymousUnsuccessfulFirebaseAuth).addAuthStateListener(capture())
            lastValue.onAuthStateChanged(mockedNonAnonymousUnsuccessfulFirebaseAuth)
        }

        // trigger OnCompleteListener.onComplete
        argumentCaptor<OnCompleteListener<GetTokenResult>>().apply {
            verify(mockUnsuccessfulTask).addOnCompleteListener(capture())
            lastValue.onComplete(mockUnsuccessfulTask)
        }

        val value = LiveDataTestUtil.getValue(subject.getCurrentUser())
        assertThat(
                (value as Result.Success).data?.isAnonymous,
                `is`(equalTo(false)))

        assertTrue(LiveDataTestUtil.getValue(subject.getToken()) is Result.Error)
    }
}

