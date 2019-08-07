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

import com.google.samples.apps.iosched.shared.data.signin.datasources.AuthStateUserDataSource
import com.google.samples.apps.iosched.shared.domain.auth.ObserveUserAuthStateUseCase
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.runBlockingTest
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.single
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

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun userSuccess() = coroutineRule.runBlockingTest {
        val subject = createObserveUserAuthStateUseCase(isAnonymous = false, isSuccess = true)

        val result = subject(Any()).single()

        assertThat(result.data?.getUid(), `is`(equalTo(TEST_USER_ID)))
        assertThat(result.data?.isSignedIn(), `is`(equalTo(true)))
    }

    @Test
    fun userError() = coroutineRule.runBlockingTest {
        val subject = createObserveUserAuthStateUseCase(isAnonymous = false, isSuccess = false)

        val result = subject(Any()).single()

        assertThat(result, `is`(instanceOf(Result.Error::class.java)))
    }

    @Test
    fun userLogsOut() = coroutineRule.runBlockingTest {
        val subject = createObserveUserAuthStateUseCase(isAnonymous = true, isSuccess = true)

        val result = subject(Any()).single()

        assertThat(result.data?.getUid(), `is`(equalTo(TEST_USER_ID)))
        assertThat(result.data?.isSignedIn(), `is`(equalTo(false)))
    }

    private fun createObserveUserAuthStateUseCase(
        isAnonymous: Boolean,
        isSuccess: Boolean
    ): ObserveUserAuthStateUseCase {
        val authStateUserDataSource = FakeAuthStateUserDataSource(isAnonymous, isSuccess)
        return ObserveUserAuthStateUseCase(authStateUserDataSource, coroutineRule.testDispatcher)
    }
}

class FakeAuthStateUserDataSource(
    private val isAnonymous: Boolean,
    private val successFirebaseUser: Boolean
) : AuthStateUserDataSource {

    override fun getBasicUserInfo(): Flow<Result<AuthenticatedUserInfo>> = flow {
        if (successFirebaseUser) {
            val mockUser = mock<AuthenticatedUserInfo> {
                on { isAnonymous() }.doReturn(isAnonymous)
                on { getUid() }.doReturn(TEST_USER_ID)
                on { isSignedIn() }.doReturn(!isAnonymous)
            }
            emit(Result.Success(mockUser))
        } else {
            emit(Result.Error(Exception("Test")))
        }
    }
}
