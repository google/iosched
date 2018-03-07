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
import com.google.samples.apps.iosched.model.TestDataRepository
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.data.session.UserEventRepository
import com.google.samples.apps.iosched.shared.data.session.agenda.AgendaRepository
import com.google.samples.apps.iosched.shared.data.tag.TagRepository
import com.google.samples.apps.iosched.shared.domain.agenda.LoadAgendaUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsByDayUseCase
import com.google.samples.apps.iosched.shared.domain.tags.LoadTagsByCategoryUseCase
import com.google.samples.apps.iosched.shared.model.Block
import com.google.samples.apps.iosched.shared.model.Tag
import com.google.samples.apps.iosched.shared.model.UserSession
import com.google.samples.apps.iosched.shared.schedule.SessionMatcher
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.test.util.LiveDataTestUtil
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.google.samples.apps.iosched.test.util.fakes.FakeLoginViewModelPlugin
import com.google.samples.apps.iosched.ui.schedule.day.TestUserEventDataSource
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
        // Create test use cases with test data
        val loadSessionsUseCase = LoadUserSessionsByDayUseCase(
                SessionRepository(TestDataRepository),
                UserEventRepository(TestUserEventDataSource)
        )
        val loadAgendaUseCase = LoadAgendaUseCase(AgendaRepository(TestDataRepository))
        val loadTagsUseCase = LoadTagsByCategoryUseCase(TagRepository(TestDataRepository))
        val loginViewModelComponent = createLoginViewModelComponent()

        // Create ViewModel with the use cases
        val viewModel = ScheduleViewModel(
                loadSessionsUseCase, loadAgendaUseCase, loadTagsUseCase, loginViewModelComponent
        )

        // Check that data were loaded correctly
        // Sessions
        for (day in ConferenceDay.values()) {
            assertEquals(
                    TestData.userSessionMap[day],
                    LiveDataTestUtil.getValue(viewModel.getSessionsForDay(day))
            )
        }
        assertFalse(LiveDataTestUtil.getValue(viewModel.isLoading)!!)
        // Tags
        assertEquals(TestData.tagFiltersList, LiveDataTestUtil.getValue(viewModel.tagFilters))
    }

    @Test
    fun profileClicked_whileLoggedIn_logsOut() {
        val loadSessionsUseCase = createSessionsExceptionUseCase()
        val loadAgendaUseCase = createAgendaExceptionUseCase()
        val loadTagsUseCase = createTagsExceptionUseCase()
        val loginViewModelComponent = createLoginViewModelComponent()

        // Create ViewModel with the use cases
        val viewModel = ScheduleViewModel(
                loadSessionsUseCase, loadAgendaUseCase, loadTagsUseCase, loginViewModelComponent
        )

        loginViewModelComponent.injectIsLoggedIn = true

        // click profile
        viewModel.onProfileClicked()

        assertEquals(1, loginViewModelComponent.logoutRequestsEmitted)
    }

    @Test
    fun profileClicked_whileLoggedOut_logsIn() {
        val loadSessionsUseCase = createSessionsExceptionUseCase()
        val loadAgendaUseCase = createAgendaExceptionUseCase()
        val loadTagsUseCase = createTagsExceptionUseCase()
        val loginViewModelComponent = createLoginViewModelComponent()

        // Create ViewModel with the use cases
        val viewModel = ScheduleViewModel(
                loadSessionsUseCase, loadAgendaUseCase, loadTagsUseCase, loginViewModelComponent
        )

        loginViewModelComponent.injectIsLoggedIn = false

        // click profile
        viewModel.onProfileClicked()

        assertEquals(1, loginViewModelComponent.loginRequestsEmitted)
    }

    @Test
    fun testDataIsLoaded_Fails() {
        val loadSessionsUseCase = createSessionsExceptionUseCase()
        val loadAgendaUseCase = createAgendaExceptionUseCase()
        val loadTagsUseCase = createTagsExceptionUseCase()
        val loginViewModelComponent = createLoginViewModelComponent()

        // Create ViewModel with the use case
        val viewModel = ScheduleViewModel(
                loadSessionsUseCase, loadAgendaUseCase, loadTagsUseCase, loginViewModelComponent
        )
        val errorMsg = LiveDataTestUtil.getValue(viewModel.errorMessage)
        assertTrue(errorMsg?.peekContent()?.isNotEmpty() ?: false)
    }

    /**
     * Creates a use case that throws an exception.
     */
    private fun createSessionsExceptionUseCase(): LoadUserSessionsByDayUseCase {
        val sessionRepository = SessionRepository(TestDataRepository)
        val userEventRepository = UserEventRepository(TestUserEventDataSource)

        return object : LoadUserSessionsByDayUseCase(
                sessionRepository,
                userEventRepository
        ) {
            override fun execute(parameters: Pair<SessionMatcher, String>):
                    Map<ConferenceDay, List<UserSession>> {
                throw Exception("Testing exception")
            }
        }
    }

    /**
     * Creates a use case that throws an exception.
     */
    private fun createTagsExceptionUseCase(): LoadTagsByCategoryUseCase {
        return object : LoadTagsByCategoryUseCase(TagRepository(TestDataRepository)) {
            override fun execute(parameters: Unit): List<Tag> {
                throw Exception("Testing exception")
            }
        }
    }

    /**
     * Creates a use case that throws an exception.
     */
    private fun createAgendaExceptionUseCase(): LoadAgendaUseCase {
        return object : LoadAgendaUseCase(AgendaRepository(TestDataRepository)) {
            override fun execute(parameters: Unit): List<Block> {
                throw Exception("Testing exception")
            }
        }
    }

    private fun createLoginViewModelComponent() = FakeLoginViewModelPlugin()
}

