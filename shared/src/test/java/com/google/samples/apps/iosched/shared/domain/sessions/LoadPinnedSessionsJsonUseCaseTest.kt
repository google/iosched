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
import com.google.gson.GsonBuilder
import com.google.samples.apps.iosched.androidtest.util.observeForTesting
import com.google.samples.apps.iosched.model.schedule.PinnedSession
import com.google.samples.apps.iosched.model.schedule.PinnedSessionsSchedule
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.domain.repository.TestUserEventDataSource
import com.google.samples.apps.iosched.shared.model.TestDataRepository
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.shared.util.SyncExecutorRule
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.toEpochMilli
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.data.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [LoadPinnedSessionsJsonUseCase]
 */
class LoadPinnedSessionsJsonUseCaseTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule var syncExecutorRule = SyncExecutorRule()

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun returnedUserSessions_areStarredOrReserved() = coroutineRule.runBlockingTest {
        // Arrange
        val testUserEventRepository = DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(),
            DefaultSessionRepository(TestDataRepository)
        )
        val useCase = LoadPinnedSessionsJsonUseCase(testUserEventRepository)
        val resultLiveData = useCase.observe()
        resultLiveData.observeForTesting {

            // Act
            useCase.execute("user1")

            // Assert
            val expected = PinnedSessionsSchedule(
                listOf(TestData.session0, TestData.session1, TestData.session2)
                    .map {
                        PinnedSession(
                            name = it.title,
                            location = it.room?.name ?: "",
                            day = TimeUtils.abbreviatedDayForAr(it.startTime),
                            time = TimeUtils.abbreviatedTimeForAr(it.startTime),
                            timestamp = it.startTime.toEpochMilli(),
                            description = it.description
                        )
                    }
            )

            val gson = GsonBuilder().create()
            assertThat(resultLiveData.value?.data, `is`(equalTo(gson.toJson(expected))))
        }
    }
}
