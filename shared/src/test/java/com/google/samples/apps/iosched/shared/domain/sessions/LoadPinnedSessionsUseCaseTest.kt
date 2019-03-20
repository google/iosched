/*
 * Copyright 2019 Google LLC
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

package com.google.samples.apps.iosched.shared.domain.sessions

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.data.userevent.UserEventsResult
import com.google.samples.apps.iosched.shared.domain.repository.TestUserEventDataSource
import com.google.samples.apps.iosched.shared.model.TestDataRepository
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.SyncExecutorRule
import com.google.samples.apps.iosched.test.data.TestData
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [LoadPinnedSessionsUseCase]
 */
class LoadPinnedSessionsUseCaseTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule var syncExecutorRule = SyncExecutorRule()

    @Test
    fun returnedUserSessions_areStarredOrReserved() {
        // Arrange
        val userEventsResult: MutableLiveData<UserEventsResult> = MutableLiveData()
        val testUserEventRepository = DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(userEventsResult),
            DefaultSessionRepository(TestDataRepository)
        )
        val useCase = LoadPinnedSessionsUseCase(testUserEventRepository)
        val resultLiveData = useCase.observe()

        // Act
        useCase.execute("user1")

        // Assert
        val result = LiveDataTestUtil.getValue(resultLiveData)
                as Result.Success<List<UserSession>>
        assertThat(
            result.data, `is`(
                equalTo(
                    listOf(TestData.userSession0, TestData.userSession1, TestData.userSession2)
                )
            )
        )
    }
}
