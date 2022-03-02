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

package com.google.samples.apps.iosched.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.samples.apps.iosched.model.Block
import com.google.samples.apps.iosched.shared.data.FakeAppConfigDataSource
import com.google.samples.apps.iosched.shared.data.agenda.DefaultAgendaRepository
import com.google.samples.apps.iosched.shared.domain.agenda.LoadAgendaUseCase
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.shared.result.succeeded
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.collection.IsCollectionWithSize.hasSize
import org.hamcrest.core.Is.`is`
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Simple benchmark test for loading the agenda. To keep startup times optimized, this benchmark
 * test is used to monitor potential hot spots when the app first loads content.
 */
@RunWith(AndroidJUnit4::class)
class LoadAgendaUseCaseBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun loadAgendaUseCase() {
        AndroidThreeTen.init(ApplicationProvider.getApplicationContext())
        // Using FakeAppConfigDataSource to stub out the network call to Firebase's remote config.
        // The repository and useCase do not perform any caching so the creation of the useCase
        // occurs before measuring to focus the benchmark on the creation of the agenda.
        val useCase = LoadAgendaUseCase(
            DefaultAgendaRepository(FakeAppConfigDataSource()),
            coroutineRule.testDispatcher
        )

        benchmarkRule.measureRepeated {
            runTest {
                val result: Result<List<Block>> = useCase.invoke(parameters = true)

                assertThat(result.succeeded, `is`(true))
                assertThat(result.data, hasSize(29))
            }
        }
    }
}
