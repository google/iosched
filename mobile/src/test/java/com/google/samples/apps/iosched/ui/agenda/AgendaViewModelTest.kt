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

package com.google.samples.apps.iosched.ui.agenda

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.model.Block
import com.google.samples.apps.iosched.shared.data.agenda.AgendaRepository
import com.google.samples.apps.iosched.shared.domain.agenda.LoadAgendaUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.runBlockingTest
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.hamcrest.Matchers.equalTo as isEqualTo

/**
 * Unit tests for the [AgendaViewModel].
 */
@ExperimentalCoroutinesApi
class AgendaViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule
    var syncTaskExecutorRule = SyncTaskExecutorRule()

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun agendaDataIsLoaded() = coroutineRule.runBlockingTest {
        val repository = FakeAgendaRepository()
        val viewModel = AgendaViewModel(
            LoadAgendaUseCase(
                repository, coroutineRule.testDispatcher
            ),
            GetTimeZoneUseCase(FakePreferenceStorage(), coroutineRule.testDispatcher)
        )
        val blocks = LiveDataTestUtil.getValue(viewModel.agenda)
        assertThat(blocks, isEqualTo(repository.getAgenda(false)))
    }

    @Test
    fun agendaDataIsLoaded_refresh() = coroutineRule.runBlockingTest {
        val repository = FakeAgendaRepository()
        val viewModel = AgendaViewModel(
            LoadAgendaUseCase(
                repository, coroutineRule.testDispatcher
            ),
            GetTimeZoneUseCase(FakePreferenceStorage(), coroutineRule.testDispatcher)
        )

        viewModel.refreshAgenda()

        val blocks = LiveDataTestUtil.getValue(viewModel.agenda)
        assertThat(blocks, isEqualTo(repository.getAgenda(true)))
    }
}

class FakeAgendaRepository : AgendaRepository {

    private val agenda = listOf(validBlock1, validBlock2)
    private val agendaRefreshed = listOf(validBlock2)

    override suspend fun getAgenda(forceRefresh: Boolean): List<Block> {
        return if (forceRefresh) agendaRefreshed else agenda
    }
}

val validBlock1 = Block(
    title = "I'm not disabled",
    type = "meal",
    color = 0xffff00ff.toInt(),
    startTime = TimeUtils.ConferenceDays[0].start.plusHours(1L),
    endTime = TimeUtils.ConferenceDays[0].start.plusHours(2L)
)

val validBlock2 = Block(
    title = "I'm not disabled",
    type = "meal",
    color = 0xffff00ff.toInt(),
    startTime = TimeUtils.ConferenceDays[0].start.plusHours(2L),
    endTime = TimeUtils.ConferenceDays[0].start.plusHours(3L)
)
