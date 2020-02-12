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

package com.google.samples.apps.iosched.ui.agenda

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.model.Block
import com.google.samples.apps.iosched.shared.data.agenda.AgendaRepository
import com.google.samples.apps.iosched.shared.domain.agenda.LoadAgendaUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo as isEqualTo
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the [AgendaViewModel].
 */
class AgendaViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler] TODO(COROUTINES): Remove
    @get:Rule
    var syncTaskExecutorRule = SyncTaskExecutorRule()

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun agendaDataIsLoaded() {
        val viewModel = AgendaViewModel(
            LoadAgendaUseCase(FakeAgendaRepository(), coroutineRule.testDispatcher),
            GetTimeZoneUseCase(FakePreferenceStorage(), coroutineRule.testDispatcher)
        )

        val blocks = LiveDataTestUtil.getValue(viewModel.agenda)
        assertThat(blocks, isEqualTo(TestData.agenda))
    }

    internal class FakeAgendaRepository : AgendaRepository {

        override suspend fun getAgenda(forceRefresh: Boolean): List<Block> = TestData.agenda
    }
}
