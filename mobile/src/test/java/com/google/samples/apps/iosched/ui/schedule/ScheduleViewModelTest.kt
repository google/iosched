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
import com.google.samples.apps.iosched.shared.data.session.agenda.AgendaRepository
import com.google.samples.apps.iosched.shared.data.tag.TagRepository
import com.google.samples.apps.iosched.shared.model.Block
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.Tag
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.test.util.LiveDataTestUtil
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.google.samples.apps.iosched.ui.schedule.agenda.LoadAgendaUseCase
import com.google.samples.apps.iosched.ui.schedule.agenda.TestAgendaDataSource
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
        val loadSessionsUseCase = createSessionsUseCase(TestData.sessionsMap)
        val loadAgendaUseCase = createAgendaUseCase(TestData.agenda)
        val loadTagsUseCase = createTagsUseCase(TestData.tagsList)

        // Create ViewModel with the use cases
        val viewModel = ScheduleViewModel(loadSessionsUseCase, loadAgendaUseCase, loadTagsUseCase)

        // Check that data were loaded correctly
        // Sessions
        for (day in ConferenceDay.values()) {
            assertEquals(TestData.sessionsMap[day],
                    LiveDataTestUtil.getValue(viewModel.getSessionsForDay(day)))
        }
        assertFalse(LiveDataTestUtil.getValue(viewModel.isLoading)!!)
        // Tags
        assertEquals(TestData.tagsList, LiveDataTestUtil.getValue(viewModel.tags))
    }

    @Test
    fun testDataIsLoaded_Fails() {
        val loadSessionsUseCase = createSessionsExceptionUseCase()
        val loadAgendaUseCase = createAgendaUseCase(TestData.agenda)
        val loadTagsUseCase = createTagsExceptionUseCase()

        // Create ViewModel with the use case
        val viewModel = ScheduleViewModel(loadSessionsUseCase, loadAgendaUseCase, loadTagsUseCase)

        assertTrue(!LiveDataTestUtil.getValue(viewModel.errorMessage).isNullOrEmpty())
    }

    /**
     * Creates a use case that will return the provided list of sessions.
     */
    private fun createSessionsUseCase(
            sessions: Map<ConferenceDay, List<Session>>): LoadSessionsByDayUseCase {
        return object : LoadSessionsByDayUseCase(SessionRepository(TestSessionDataSource)) {
            override fun execute(filters: SessionFilters): Map<ConferenceDay, List<Session>> {
                return sessions
            }
        }
    }

    /**
     * Creates a use case that throws an exception.
     */
    private fun createSessionsExceptionUseCase(): LoadSessionsByDayUseCase {
        return object : LoadSessionsByDayUseCase(SessionRepository(TestSessionDataSource)) {
            override fun execute(filters: SessionFilters): Map<ConferenceDay, List<Session>> {
                throw Exception("Testing exception")
            }
        }
    }

    /**
     * Creates a use case that will return the provided list of agenda blocks.
     */
    private fun createAgendaUseCase(agenda: List<Block>): LoadAgendaUseCase {
        return object : LoadAgendaUseCase(AgendaRepository(TestAgendaDataSource)) {
            override fun execute(parameters: Unit): List<Block> {
                return agenda
            }
        }
    }

    /**
     * Creates a use case that will return the provided list of tags.
     */
    private fun createTagsUseCase(tags: List<Tag>): LoadTagsByCategoryUseCase {
        return object : LoadTagsByCategoryUseCase(TagRepository(TestTagDataSource)) {
            override fun execute(parameters: Unit): List<Tag> {
                return tags
            }
        }
    }

    /**
     * Creates a use case that throws an exception.
     */
    private fun createTagsExceptionUseCase(): LoadTagsByCategoryUseCase {
        return object : LoadTagsByCategoryUseCase(TagRepository(TestTagDataSource)) {
            override fun execute(parameters: Unit): List<Tag> {
                throw Exception("Testing exception")
            }
        }
    }
}

