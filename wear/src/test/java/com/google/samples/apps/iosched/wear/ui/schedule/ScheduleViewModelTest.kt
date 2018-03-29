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

package com.google.samples.apps.iosched.wear.ui.schedule

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.data.userevent.UserEventDataSource
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCase
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.test.util.LiveDataTestUtil
import com.google.samples.apps.iosched.wear.domain.repository.TestUserEventDataSource
import com.google.samples.apps.iosched.wear.model.TestData
import com.google.samples.apps.iosched.wear.model.TestDataRepository
import com.google.samples.apps.iosched.wear.util.SyncTaskExecutorRule
import org.junit.Assert
import org.junit.Assert.assertFalse
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
        val loadSessionsUseCase = LoadUserSessionsByDayUseCase(
                DefaultSessionAndUserEventRepository(
                        TestUserEventDataSource(), DefaultSessionRepository(TestDataRepository))
        )

        // Create ViewModel with the use cases
        val viewModel = createScheduleViewModel(loadSessionsUseCase = loadSessionsUseCase)

        // Observe viewmodel to load sessions
        viewModel.getSessionsForDay(ConferenceDay.DAY_1).observeForever {}

        // Check that data were loaded correctly
        // Sessions
        for (day in ConferenceDay.values()) {
            Assert.assertEquals(
                    TestData.userSessionMap[day],
                    LiveDataTestUtil.getValue(viewModel.getSessionsForDay(day))
            )
        }
        assertFalse(LiveDataTestUtil.getValue(viewModel.isLoading)!!)
    }

    @Test
    fun testDataIsLoaded_Fails() {

        val loadSessionsExceptionUseCase = createSessionsExceptionUseCase()

        // Create ViewModel with the use cases
        val viewModel =
            createScheduleViewModel(loadSessionsUseCase = loadSessionsExceptionUseCase)

        val errorMsg = LiveDataTestUtil.getValue(viewModel.errorMessage)
        Assert.assertTrue(errorMsg?.peekContent()?.isNotEmpty() ?: false)
    }

    private fun createScheduleViewModel(
        loadSessionsUseCase: LoadUserSessionsByDayUseCase = createTestLoadUserSessionsByDayUseCase()
    ): ScheduleViewModel {
        return ScheduleViewModel(
                loadSessionsUseCase)
    }

    /**
     * Creates a test [LoadUserSessionsByDayUseCase].
     */
    private fun createTestLoadUserSessionsByDayUseCase(
        userEventDataSource: UserEventDataSource = TestUserEventDataSource()
    ): LoadUserSessionsByDayUseCase {

        val sessionRepository = DefaultSessionRepository(TestDataRepository)
        val userEventRepository = DefaultSessionAndUserEventRepository(
                userEventDataSource, sessionRepository)

        return LoadUserSessionsByDayUseCase(userEventRepository)
    }

    /**
     * Creates a use case that throws an exception.
     */
    private fun createSessionsExceptionUseCase(): LoadUserSessionsByDayUseCase {
        val sessionRepository = DefaultSessionRepository(TestDataRepository)
        val userEventRepository = DefaultSessionAndUserEventRepository(
                TestUserEventDataSource(), sessionRepository)


        return object : LoadUserSessionsByDayUseCase(userEventRepository) {
            override fun execute(parameters: Pair<UserSessionMatcher, String?>) {
                result.postValue(Result.Error(Exception("Testing exception")))
            }
        }
    }
}
