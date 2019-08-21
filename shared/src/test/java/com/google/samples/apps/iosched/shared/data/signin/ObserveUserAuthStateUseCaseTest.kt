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

package com.google.samples.apps.iosched.shared.data.signin

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.shared.data.signin.datasources.AuthStateUserDataSource
import com.google.samples.apps.iosched.shared.domain.auth.ObserveUserAuthStateUseCase
import com.google.samples.apps.iosched.shared.result.Result
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Rule
import org.junit.Test

const val TEST_USER_ID = "testuser"

/**
 * Tests for [ObserveUserAuthStateUseCase].
 */
class ObserveUserAuthStateUseCaseTest {

    @Rule
    @JvmField
    val instantTaskExecutor = InstantTaskExecutorRule()

    @Test
    fun userSuccess() {

        val subject = createObserveUserAuthStateUseCase(
            isAnonymous = false,
            isSuccess = true,
            userId = TEST_USER_ID
        )

        // Start the listeners
        subject.execute(Any())

        val value = LiveDataTestUtil.getValue(subject.observe()) as Result.Success

        assertThat(
            value.data.getUid(),
            `is`(equalTo(TEST_USER_ID))
        )
        assertThat(
            value.data.isSignedIn(),
            `is`(equalTo(true))
        )
    }

    @Test
    fun userError() {
        val subject = createObserveUserAuthStateUseCase(
            isAnonymous = false,
            isSuccess = false,
            userId = TEST_USER_ID
        )

        // Start the listeners
        subject.execute(Any())

        val value = LiveDataTestUtil.getValue(subject.observe())

        assertThat(
            value,
            `is`(instanceOf(Result.Error::class.java))
        )
    }

    @Test
    fun userLogsOut() {
        val subject = createObserveUserAuthStateUseCase(
            isAnonymous = true,
            isSuccess = true,
            userId = TEST_USER_ID
        )

        // Start the listeners
        subject.execute(Any())

        val value = LiveDataTestUtil.getValue(subject.observe()) as Result.Success

        assertThat(
            value.data.getUid(),
            `is`(equalTo(TEST_USER_ID))
        )
        assertThat(
            value.data.isSignedIn(),
            `is`(equalTo(false))
        )
    }

    private fun createObserveUserAuthStateUseCase(
        isAnonymous: Boolean,
        isSuccess: Boolean,
        userId: String
    ): ObserveUserAuthStateUseCase {
        val authStateUserDataSource = FakeAuthStateUserDataSource(
            isAnonymous, isSuccess, userId
        )

        val subject = ObserveUserAuthStateUseCase(authStateUserDataSource)
        return subject
    }
}

class FakeAuthStateUserDataSource(
    private val isAnonymous: Boolean,
    private val successFirebaseUser: Boolean,
    private val userId: String?
) : AuthStateUserDataSource {

    private val _userId = MutableLiveData<String?>()

    private val _firebaseUser = MutableLiveData<Result<AuthenticatedUserInfo?>>()

    override fun startListening() {
        _userId.postValue(userId)

        if (successFirebaseUser) {
            val mockUser = mock<AuthenticatedUserInfo> {
                on { isAnonymous() }.doReturn(isAnonymous)
                on { getUid() }.doReturn(TEST_USER_ID)
                on { isSignedIn() }.doReturn(!isAnonymous)
            }
            _firebaseUser.postValue(Result.Success(mockUser))
        } else {
            _firebaseUser.postValue(Result.Error(Exception("Test")))
        }
    }

    override fun getBasicUserInfo(): LiveData<Result<AuthenticatedUserInfo?>> {
        return _firebaseUser
    }

    override fun clearListener() {
        // Noop
    }
}
