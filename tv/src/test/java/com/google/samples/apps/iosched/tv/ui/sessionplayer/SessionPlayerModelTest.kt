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

package com.google.samples.apps.iosched.tv.ui.sessionplayer

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.domain.sessions.LoadSessionUseCase
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.tv.model.TestDataRepository
import com.google.samples.apps.iosched.tv.util.SyncTaskExecutorRule
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SessionPlayerModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule var syncTaskExecutorRule = SyncTaskExecutorRule()

    private lateinit var viewModel: SessionPlayerViewModel
    private val testSession = TestData.session0

    @Before
    fun setup() {
        viewModel = SessionPlayerViewModel(createUseCase())
        viewModel.loadSessionById(testSession.id)
    }

    @Test
    fun testDataIsLoaded_observablesUpdated() {
        assertEquals(testSession, LiveDataTestUtil.getValue(viewModel.session)?.peekContent())
    }

    private fun createUseCase(): LoadSessionUseCase {
        return LoadSessionUseCase(DefaultSessionRepository(TestDataRepository))
    }
}
