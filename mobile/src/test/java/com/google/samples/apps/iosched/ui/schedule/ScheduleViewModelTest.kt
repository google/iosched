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

package com.google.samples.apps.iosched.ui.schedule

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.model.TestData
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.util.LiveDataTestUtil
import com.google.samples.apps.iosched.util.SyncTaskExecutorRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the [ScheduleViewModel].
 */
class ScheduleViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule var syncTaskExecutorRule = SyncTaskExecutorRule()

    @Test
    fun testDataIsLoaded_ObservablesUpdated() {
        // Create a test use cases with test data
        val testData = TestData.sessionsMap
        val loadSessionsUseCase = createUseCase(testData)

        // Create ViewModel with the use case
        val viewModel = ScheduleViewModel(loadSessionsUseCase)

        // Check that data were loaded correctly
        for (day in ConferenceDay.values()) {
            assertEquals(testData[day], LiveDataTestUtil.getValue(viewModel.getSessionsForDay(day)))
        }
        assertFalse(LiveDataTestUtil.getValue(viewModel.isLoading)!!)
    }

    @Test
    fun testDataIsLoaded_Fails() {
        // Create a test use cases with test data
        val testData = TestData.sessionsMap
        val loadSessionsUseCase = createUseCase(testData)

        // Create ViewModel with the use case
        val viewModel = ScheduleViewModel(createExceptionUseCase())

        assertTrue(!LiveDataTestUtil.getValue(viewModel.errorMessage).isNullOrEmpty())
    }

    /**
     * Creates a use case that will return the provided list of sessions.
     */
    private fun createUseCase(
            sessions: Map<ConferenceDay, List<Session>>): LoadSessionsByDayUseCase {
        return object : LoadSessionsByDayUseCase(SessionRepository(TestSessionDataSource)) {
            override fun execute(parameters: SessionFilters): Map<ConferenceDay, List<Session>> {
                return sessions
            }
        }
    }

    /**
     * Creates a use case that throws an exception.
     */
    private fun createExceptionUseCase(): LoadSessionsByDayUseCase {
        return object : LoadSessionsByDayUseCase(SessionRepository(TestSessionDataSource)) {
            override fun execute(parameters: SessionFilters): Map<ConferenceDay, List<Session>> {
                throw Exception("Testing exception")
            }
        }
    }
}

