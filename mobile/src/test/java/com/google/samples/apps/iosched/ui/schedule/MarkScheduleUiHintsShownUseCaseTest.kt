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

package com.google.samples.apps.iosched.ui.schedule

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.shared.data.prefs.PreferenceStorage
import com.google.samples.apps.iosched.shared.domain.invoke
import com.google.samples.apps.iosched.shared.domain.prefs.MarkScheduleUiHintsShownUseCase
import com.google.samples.apps.iosched.test.util.SyncTaskExecutorRule
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the [ScheduleUiHintsDialogViewModel].
 */
class MarkScheduleUiHintsShownUseCaseTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule
    var syncTaskExecutorRule = SyncTaskExecutorRule()

    @Test
    fun dismissed_navigateUiHintsShowAction() {
        val mockPrefs = mock<PreferenceStorage> {}
        val useCase = MarkScheduleUiHintsShownUseCase(mockPrefs, TestCoroutineDispatcher())

        useCase.invoke()
        verify(mockPrefs).scheduleUiHintsShown = true
    }
}
