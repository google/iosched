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

@file:Suppress("FunctionName")

package com.google.samples.apps.iosched.tv.ui.schedule

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCaseParameters
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDays
import com.google.samples.apps.iosched.tv.model.TestDataRepository
import com.google.samples.apps.iosched.tv.model.TestUserEventDataSource
import com.google.samples.apps.iosched.tv.util.SyncTaskExecutorRule
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.core.Is.`is`
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the [ScheduleViewModel].
 */
class ScheduleViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule
    var syncTaskExecutorRule = SyncTaskExecutorRule()

    @Test
    fun testDataIsLoaded_ObservablesUpdated() {
        // Create a test use cases with test data
        val loadSessionsUseCase = createUseCase()

        // Create ViewModel with the use case
        val viewModel = ScheduleViewModel(loadSessionsUseCase)

        // Check that data was loaded correctly
        val day1Result = LiveDataTestUtil.getValue(
            viewModel.getSessionsGroupedByTimeForDay(ConferenceDays[0])
        )
        val day2Result = LiveDataTestUtil.getValue(
            viewModel.getSessionsGroupedByTimeForDay(ConferenceDays[1])
        )
        val day3Result = LiveDataTestUtil.getValue(
            viewModel.getSessionsGroupedByTimeForDay(ConferenceDays[2])
        )

        assertThat(day1Result?.size, `is`(equalTo(1)))
        assertThat(day2Result?.size, `is`(equalTo(1)))
        assertThat(day3Result?.size, `is`(equalTo(2)))

        assertThat(
            "Once sessions are loaded, isLoading should be false",
            LiveDataTestUtil.getValue(viewModel.isLoading),
            `is`(false)
        )
    }

    @Test
    fun testDataIsLoaded_ErrorMessageOnFailure() {
        val loadSessionsUseCase = createSessionsExceptionUseCase()

        // Create ViewModel with the use case
        val viewModel = ScheduleViewModel(loadSessionsUseCase)

        assertFalse(LiveDataTestUtil.getValue(viewModel.errorMessage).isNullOrBlank())
    }

    /**
     * Creates a use case that will return the provided list of sessions.
     */
    private fun createUseCase(): LoadUserSessionsByDayUseCase {
        return LoadUserSessionsByDayUseCase(
            DefaultSessionAndUserEventRepository(
                TestUserEventDataSource, DefaultSessionRepository(TestDataRepository)
            )
        )
    }

    /**
     * Creates a use case that throws an exception.
     */
    private fun createSessionsExceptionUseCase(): LoadUserSessionsByDayUseCase {
        val sessionRepository = DefaultSessionRepository(TestDataRepository)
        val userEventRepository = DefaultSessionAndUserEventRepository(
            TestUserEventDataSource, sessionRepository
        )

        return object : LoadUserSessionsByDayUseCase(userEventRepository) {
            override fun execute(parameters: LoadUserSessionsByDayUseCaseParameters) {
                result.postValue(Result.Error(Exception("Testing exception")))
            }
        }
    }
}
