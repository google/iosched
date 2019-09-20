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

package com.google.samples.apps.iosched.shared.domain.agenda

import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.shared.util.FakeAgendaRepository
import com.google.samples.apps.iosched.shared.util.afterConferenceBlock
import com.google.samples.apps.iosched.shared.util.beforeConferenceBlock
import com.google.samples.apps.iosched.shared.util.disabledBlock
import com.google.samples.apps.iosched.shared.util.validBlock1
import com.google.samples.apps.iosched.shared.util.validBlock2
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.runBlockingTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.core.IsEqual.equalTo
import org.hamcrest.core.IsInstanceOf.instanceOf
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test

@ExperimentalCoroutinesApi
class LoadAgendaUseCaseTest {

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun testLoadAgenda_singleBlock() = coroutineRule.runBlockingTest {
        val useCase = LoadAgendaUseCase(
            FakeAgendaRepository(validBlock1),
            coroutineRule.testDispatcher
        )

        val result = useCase(false)

        assertThat(result, instanceOf(Success::class.java))
        assertThat(result.data, equalTo(listOf(validBlock1)))
    }

    @Test
    fun testFakeRepository_distinctBlocks() = coroutineRule.runBlockingTest {
        val fakeAgendaRepository = FakeAgendaRepository(validBlock1)
        val useCase = LoadAgendaUseCase(
            fakeAgendaRepository,
            coroutineRule.testDispatcher
        )

        fakeAgendaRepository.agenda.add(validBlock1)

        val result = useCase(false)

        assertThat(result, instanceOf(Success::class.java))
        assertThat(result.data, equalTo(listOf(validBlock1)))
    }

    @Test
    fun testFakeRepository_multipleBlocks() = coroutineRule.runBlockingTest {
        val fakeAgendaRepository = FakeAgendaRepository(validBlock1)
        val useCase = LoadAgendaUseCase(
            fakeAgendaRepository,
            coroutineRule.testDispatcher
        )

        fakeAgendaRepository.agenda.add(validBlock2)

        val result = useCase(false)

        assertThat(result, instanceOf(Success::class.java))
        assertThat(result.data, equalTo(listOf(validBlock1, validBlock2)))
    }

    @Test
    fun testLoadAgenda_withDisabledItem_afterConference() = coroutineRule.runBlockingTest {
        val fakeAgendaRepository = FakeAgendaRepository(validBlock1)
        val useCase = LoadAgendaUseCase(
            fakeAgendaRepository,
            coroutineRule.testDispatcher
        )

        fakeAgendaRepository.agenda.add(afterConferenceBlock)

        // Load data
        val result = useCase(false)

        // Item should not be added
        assertThat(result.data, equalTo(listOf(validBlock1)))
    }

    @Test
    fun testLoadAgenda_withDisabledItem_beforeConference() = coroutineRule.runBlockingTest {
        val fakeAgendaRepository = FakeAgendaRepository(validBlock1)
        val useCase = LoadAgendaUseCase(
            fakeAgendaRepository,
            coroutineRule.testDispatcher
        )

        fakeAgendaRepository.agenda.add(beforeConferenceBlock)

        // Load data
        val result = useCase(false)

        // Item should not be added
        assertThat(result.data, equalTo(listOf(validBlock1)))
    }

    @Test
    fun testLoadAgenda_withDisabledItem_startEndTimesEqual() = coroutineRule.runBlockingTest {
        val fakeAgendaRepository = FakeAgendaRepository(validBlock1)
        val useCase = LoadAgendaUseCase(
            fakeAgendaRepository,
            coroutineRule.testDispatcher
        )

        fakeAgendaRepository.agenda.add(disabledBlock)

        // Load data
        val result = useCase(false)

        // Item should not be added
        assertThat(result.data, equalTo(listOf(validBlock1)))
    }
}
