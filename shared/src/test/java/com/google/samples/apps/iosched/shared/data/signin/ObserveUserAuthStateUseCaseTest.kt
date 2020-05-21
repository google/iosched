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
import com.google.samples.apps.iosched.shared.data.signin.datasources.AuthStateUserDataSource
import com.google.samples.apps.iosched.shared.data.signin.datasources.RegisteredUserDataSource
import com.google.samples.apps.iosched.shared.domain.auth.ObserveUserAuthStateUseCase
import com.google.samples.apps.iosched.shared.fcm.TopicSubscriber
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.test.data.CoroutineScope
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.runBlockingTest
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
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

    @get:Rule
    val instantTaskExecutor = InstantTaskExecutorRule()

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun userSuccessRegistered() = coroutineRule.runBlockingTest {

        val topicSubscriber = mock<TopicSubscriber> {}

        val subject = createObserveUserAuthStateUseCase(
            isAnonymous = false,
            isRegistered = true,
            isSuccess = true,
            userId = TEST_USER_ID,
            topicSubscriber = topicSubscriber
        )

        val result = subject(Any()).first().data

        assertThat(
            result?.getUid(),
            `is`(equalTo(TEST_USER_ID))
        )
        assertThat(
            result?.isSignedIn(),
            `is`(equalTo(true))
        )
        assertThat(
            result?.isRegistered(),
            `is`(equalTo(true))
        )

        verify(topicSubscriber).subscribeToAttendeeUpdates()
        verify(topicSubscriber, never()).unsubscribeFromAttendeeUpdates()
    }

    @Test
    fun userSuccessNotRegistered() = coroutineRule.runBlockingTest {
        val topicSubscriber = mock<TopicSubscriber> {}

        val subject = createObserveUserAuthStateUseCase(
            isAnonymous = false,
            isRegistered = false,
            isSuccess = true,
            userId = TEST_USER_ID,
            topicSubscriber = topicSubscriber
        )

        val result = subject(Any()).first().data

        assertThat(
            result?.getUid(),
            `is`(equalTo(TEST_USER_ID))
        )
        assertThat(
            result?.isSignedIn(),
            `is`(equalTo(true))
        )
        assertThat(
            result?.isRegistered(),
            `is`(equalTo(false))
        )
        verify(topicSubscriber, never()).subscribeToAttendeeUpdates()
        verify(topicSubscriber, never()).unsubscribeFromAttendeeUpdates()
    }

    @Test
    fun userErrorNotRegistered() = coroutineRule.runBlockingTest {
        val topicSubscriber = mock<TopicSubscriber> {}

        val subject = createObserveUserAuthStateUseCase(
            isAnonymous = false,
            isRegistered = true,
            isSuccess = false,
            userId = TEST_USER_ID,
            topicSubscriber = topicSubscriber
        )

        val result = subject(Any()).first()

        assertThat(
            result,
            `is`(instanceOf(Result.Error::class.java))
        )
        verify(topicSubscriber, never()).subscribeToAttendeeUpdates()
        verify(topicSubscriber, never()).unsubscribeFromAttendeeUpdates()
    }

    @Test
    fun userLogsOut() = coroutineRule.runBlockingTest {

        val topicSubscriber = mock<TopicSubscriber> {}

        val subject = createObserveUserAuthStateUseCase(
            isAnonymous = true,
            isRegistered = false,
            isSuccess = true,
            userId = TEST_USER_ID,
            topicSubscriber = topicSubscriber
        )

        val result = subject(Any()).first().data

        assertThat(
            result?.getUid(),
            `is`(equalTo(TEST_USER_ID))
        )
        assertThat(
            result?.isSignedIn(),
            `is`(equalTo(false))
        )
        assertThat(
            result?.isRegistered(),
            `is`(equalTo(false))
        )

        verify(topicSubscriber).unsubscribeFromAttendeeUpdates()
        verify(topicSubscriber, never()).subscribeToAttendeeUpdates()
    }

    private fun createObserveUserAuthStateUseCase(
        isAnonymous: Boolean,
        isRegistered: Boolean,
        isSuccess: Boolean,
        userId: String,
        topicSubscriber: TopicSubscriber = mock {}
    ): ObserveUserAuthStateUseCase {
        val authStateUserDataSource = FakeAuthStateUserDataSource(
            isAnonymous, isSuccess, userId
        )

        val registeredUserDataSource = FakeRegisteredUserDataSource(isRegistered)
        return ObserveUserAuthStateUseCase(
            registeredUserDataSource,
            authStateUserDataSource,
            topicSubscriber,
            coroutineRule.CoroutineScope(),
            coroutineRule.testDispatcher
        )
    }
}

class FakeRegisteredUserDataSource(private val isRegistered: Boolean) : RegisteredUserDataSource {
    override fun observeUserChanges(userId: String): Flow<Result<Boolean?>> = flow {
        emit(Result.Success(isRegistered))
    }
}

class FakeAuthStateUserDataSource(
    private val isAnonymous: Boolean,
    private val successFirebaseUser: Boolean,
    private val userId: String?
) : AuthStateUserDataSource {

    override fun getBasicUserInfo(): Flow<Result<AuthenticatedUserInfoBasic?>> = flow {
        if (successFirebaseUser) {
            val mockUser = mock<AuthenticatedUserInfoBasic> {
                on { isAnonymous() }.doReturn(isAnonymous)
                on { getUid() }.doReturn(userId)
                on { isSignedIn() }.doReturn(!isAnonymous)
            }
            emit(Result.Success(mockUser))
        } else {
            emit(Result.Error(Exception("Test")))
        }
    }
}
