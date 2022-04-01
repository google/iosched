/*
 * Copyright 2020 Google LLC
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
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.domain.repository.TestUserEventDataSource
import com.google.samples.apps.iosched.shared.model.TestDataRepository
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class LoadStarredAndReservedSessionsUseCaseTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var useCase: LoadStarredAndReservedSessionsUseCase

    @Before
    fun setup() {
        val testUserEventRepository = DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(),
            DefaultSessionRepository(TestDataRepository)
        )

        useCase = LoadStarredAndReservedSessionsUseCase(
            testUserEventRepository,
            coroutineRule.testDispatcher
        )
    }

    @Test
    fun `returns starred or reserved sessions`() = runTest {
        val result = useCase("user1").first { it is Result.Success }
        Assert.assertThat(
            result.data,
            `is`(equalTo(TestData.starredOrReservedSessions))
        )
    }

    @Test
    fun `null user id returns empty list`() = runTest {
        val result = useCase(null).first { it is Result.Success }
        Assert.assertThat(
            result.data,
            `is`(equalTo(emptyList()))
        )
    }
}
