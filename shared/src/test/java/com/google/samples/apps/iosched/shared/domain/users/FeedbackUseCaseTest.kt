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

package com.google.samples.apps.iosched.shared.domain.users

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.shared.data.feedback.FeedbackEndpoint
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.domain.repository.TestUserEventDataSource
import com.google.samples.apps.iosched.shared.model.TestDataRepository
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.SyncExecutorRule
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.data.runBlockingTest
import junit.framework.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [FeedbackUseCase]
 */
class FeedbackUseCaseTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule
    var syncExecutorRule = SyncExecutorRule()

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun submit() = coroutineRule.runBlockingTest {
        val testFeedbackEndpoint = object : FeedbackEndpoint {
            override suspend fun sendFeedback(
                sessionId: SessionId,
                responses: Map<String, Int>
            ): Result<Unit> {
                return Result.Success(Unit)
            }
        }
        val testUserEventRepository = DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(), DefaultSessionRepository(TestDataRepository)
        )

        val result = FeedbackUseCase(
            testFeedbackEndpoint,
            testUserEventRepository,
            coroutineRule.testDispatcher
        )(FeedbackParameter(
            "userIdTest",
            TestData.userEvents[0],
            TestData.userEvents[0].id,
            mapOf("q1" to 1)
        ))

        assertEquals(Result.Success(Unit), result)
    }
}
