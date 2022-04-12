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

package com.google.samples.apps.iosched.ui.speaker

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.androidtest.util.observeForTesting
import com.google.samples.apps.iosched.model.TestDataRepository
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.domain.speakers.LoadSpeakerUseCase
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.data.runBlockingTest
import com.google.samples.apps.iosched.test.util.fakes.FakeAnalyticsHelper
import com.google.samples.apps.iosched.test.util.fakes.FakeEventActionsViewModelDelegate
import com.google.samples.apps.iosched.test.util.fakes.FakePreferenceStorage
import com.google.samples.apps.iosched.test.util.fakes.FakeSignInViewModelDelegate
import com.google.samples.apps.iosched.ui.schedule.TestUserEventDataSource
import com.google.samples.apps.iosched.ui.sessioncommon.EventActionsViewModelDelegate
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import kotlinx.coroutines.test.TestCoroutineDispatcher
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the [SpeakerViewModel].
 */

class SpeakerViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun setSpeakerId_loadsSpeaker() = coroutineRule.runBlockingTest {
        // Given a speaker view model
        val viewModel = createViewModel()

        // When the speaker ID is set
        viewModel.setSpeakerId(TestData.speaker1.id)

        // Then the speaker is loaded
        assertEquals(TestData.speaker1, LiveDataTestUtil.getValue(viewModel.speaker))
    }

    @Test
    fun setSpeakerId_loadsSpeakersEvents_singleEvent() = coroutineRule.runBlockingTest {
        // Given a speaker view model
        val viewModel = createViewModel()

        // When the ID of a speaker with a single event is set
        viewModel.setSpeakerId(TestData.speaker3.id)

        viewModel.speakerUserSessions.observeForTesting {
            // Then the speakers event is loaded
            assertEquals(
                listOf(TestData.userSession2),
                viewModel.speakerUserSessions.value
            )
        }
    }

    @Test
    fun setSpeakerId_loadsSpeakersEvents_multipleEvents() = coroutineRule.runBlockingTest {
        // Given a speaker view model
        val viewModel = createViewModel()

        viewModel.speakerUserSessions.observeForTesting {
            // When the ID of a speaker with multiple events is set
            viewModel.setSpeakerId(TestData.speaker1.id)

            // Then the speakers events are loaded
            assertEquals(
                listOf(TestData.userSession0, TestData.userSession3, TestData.userSession4),
                viewModel.speakerUserSessions.value
            )
        }
    }

    private fun createViewModel(
        loadSpeakerUseCase: LoadSpeakerUseCase =
            LoadSpeakerUseCase(TestDataRepository, TestCoroutineDispatcher()),
        loadSpeakerSessionsUseCase: LoadUserSessionsUseCase = LoadUserSessionsUseCase(
            DefaultSessionAndUserEventRepository(
                TestUserEventDataSource(),
                DefaultSessionRepository(TestDataRepository)
            ),
            coroutineRule.testDispatcher
        ),
        getTimeZoneUseCase: GetTimeZoneUseCase =
            GetTimeZoneUseCase(FakePreferenceStorage(), coroutineRule.testDispatcher),
        signInViewModelDelegate: SignInViewModelDelegate = FakeSignInViewModelDelegate().apply {
            loadUser("123")
        },
        eventActionsDelegate: EventActionsViewModelDelegate = FakeEventActionsViewModelDelegate(),
        analyticsHelper: AnalyticsHelper = FakeAnalyticsHelper()
    ): SpeakerViewModel {
        return SpeakerViewModel(
            loadSpeakerUseCase,
            loadSpeakerSessionsUseCase,
            getTimeZoneUseCase,
            signInViewModelDelegate,
            eventActionsDelegate,
            analyticsHelper
        )
    }
}
