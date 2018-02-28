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

import android.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.usecases.repository.LoadSessionsUseCase
import com.google.samples.apps.iosched.test.util.LiveDataTestUtil
import com.google.samples.apps.iosched.tv.util.SyncTaskExecutorRule
import org.hamcrest.collection.IsCollectionWithSize.hasSize
import org.hamcrest.core.Is.`is`
import org.junit.Assert.assertThat
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
        val expectedSessions = TestSessionDataSource.getSessions()
        val loadSessionsUseCase = createUseCase()

        // Create ViewModel with the use case
        val viewModel = ScheduleViewModel(loadSessionsUseCase)

        // Check that data was loaded correctly
        assertThat("There should be ${expectedSessions.size} session in the ViewModel",
                LiveDataTestUtil.getValue(viewModel.sessions),
                hasSize(expectedSessions.size))

        assertThat("Once sessions are loaded, isLoading should be false",
                LiveDataTestUtil.getValue(viewModel.isLoading),
                `is`(false))
    }

    /**
     * Creates a use case that will return the provided list of sessions.
     */
    private fun createUseCase(): LoadSessionsUseCase {
        return LoadSessionsUseCase(SessionRepository(TestSessionDataSource))
    }
}

