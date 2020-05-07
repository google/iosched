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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.samples.apps.iosched.shared.data.BootstrapConferenceDataSource
import org.hamcrest.core.IsNot.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmark test for parsing offline data. To keep startup times optimized, this benchmark
 * test is used to monitor potential hot spots when the app first loads content.
 */
@RunWith(AndroidJUnit4::class)
class BootstrapConferenceDataSourceBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    // Parsing JSON data can be quite time consuming. We have custom deserializers to improve the
    // performance. We parse this data when the app starts up which is a critical user path. This
    // benchmark is to ensure we maintain the level of quality around bootstrapping data to minimize
    // the app startup latency. Note that loadAndParseBootstrapData() also normalizes after it has
    // been parsed.
    @Test
    fun benchmark_json_parsing() = benchmarkRule.measureRepeated {
        BootstrapConferenceDataSource.loadAndParseBootstrapData()
    }
}
