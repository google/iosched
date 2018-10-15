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

package com.google.samples.apps.adssched.ui.agenda

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.adssched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.adssched.model.Block
import com.google.samples.apps.adssched.shared.data.session.agenda.AgendaRepository
import com.google.samples.apps.adssched.shared.domain.agenda.LoadAgendaUseCase
import com.google.samples.apps.adssched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.adssched.test.data.TestData
import com.google.samples.apps.adssched.test.util.SyncTaskExecutorRule
import com.google.samples.apps.adssched.test.util.fakes.FakePreferenceStorage
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Rule
import org.junit.Test
import org.hamcrest.Matchers.equalTo as isEqualTo

/**
 * Unit tests for the [AgendaViewModel].
 */
class AgendaViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule
    var syncTaskExecutorRule = SyncTaskExecutorRule()

    @Test
    fun agendaDataIsLoaded() {
        val viewModel = AgendaViewModel(
            LoadAgendaUseCase(FakeAgendaRepository()),
            GetTimeZoneUseCase(FakePreferenceStorage())
        )

        val blocks = LiveDataTestUtil.getValue(viewModel.agenda)
        assertThat(blocks, isEqualTo(TestData.agenda))
    }

    internal class FakeAgendaRepository : AgendaRepository {
        override fun getAgenda(): List<Block> = TestData.agenda
    }
}
