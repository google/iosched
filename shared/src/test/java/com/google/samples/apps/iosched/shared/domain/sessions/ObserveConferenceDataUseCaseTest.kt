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

package com.google.samples.apps.iosched.shared.domain.sessions

import com.google.samples.apps.iosched.shared.model.TestDataRepository
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.notNullValue
import org.hamcrest.core.Is.`is`
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [ObserveConferenceDataUseCase].
 */
class ObserveConferenceDataUseCaseTest {

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun remoteConfDataRefreshed_valueIsUpdated() = runTest {

        val repo = TestDataRepository
        val subject = ObserveConferenceDataUseCase(repo, coroutineRule.testDispatcher)
        repo.refreshCacheWithRemoteConferenceData()
        // Start the listeners
        val result = subject(Unit).first()

        assertThat(result, `is`(notNullValue()))
    }
}
