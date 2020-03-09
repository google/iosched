/*
 * Copyright 2020 Google LLC
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

package com.google.samples.apps.iosched.shared.domain

import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.runBlockingTest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import org.hamcrest.CoreMatchers.instanceOf
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test

class FlowUseCaseTest {

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val testDispatcher = coroutineRule.testDispatcher

    @Test
    fun `exception emits Result#Error`() = coroutineRule.runBlockingTest {
        val useCase = ExceptionUseCase(testDispatcher)
        val result = useCase(Unit)
        assertThat(result.first(), instanceOf(Result.Error::class.java))
    }

    class ExceptionUseCase(dispatcher: CoroutineDispatcher) : FlowUseCase<Unit, Unit>(dispatcher) {
        override fun execute(parameters: Unit): Flow<Result<Unit>> = flow {
            throw Exception("Test exception")
        }
    }
}
