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

package com.google.samples.apps.iosched.ui.sessiondetail

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.TestDataRepository
import com.google.samples.apps.iosched.shared.data.feedback.FeedbackEndpoint
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.data.userevent.UserEventDataSource
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCase
import com.google.samples.apps.iosched.shared.domain.users.FeedbackUseCase
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.shared.util.NetworkUtils
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.data.runBlockingTest
import com.google.samples.apps.iosched.test.util.fakes.FakeSignInViewModelDelegate
import com.google.samples.apps.iosched.test.util.time.FixedTimeExecutorRule
import com.google.samples.apps.iosched.ui.schedule.TestUserEventDataSource
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the [SessionFeedbackViewModel].
 */
class SessionFeedbackViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Allows explicit setting of "now"
    @get:Rule
    var fixedTimeExecutorRule = FixedTimeExecutorRule()

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var viewModel: SessionFeedbackViewModel
    private val testSession = TestData.session0

    private lateinit var mockNetworkUtils: NetworkUtils

    @Before
    fun setup() {
        mockNetworkUtils = mock {
            on { hasNetworkConnection() }.doReturn(true)
        }

        viewModel = createSessionFeedbackViewModel()
        viewModel.setSessionId(testSession.id)
    }

    @Test
    fun title() = coroutineRule.runBlockingTest {
        assertEquals(
            testSession.title,
            viewModel.userSession.first().data?.session?.title
        )
    }

    @Test
    fun questions() {
        // TODO: pointless test, left here for when these are dynamic.
        val questions = viewModel.questions
        assertEquals(4, questions.size)
        assertEquals(0, questions[0].currentRating)
        assertEquals(0, questions[1].currentRating)
        assertEquals(0, questions[2].currentRating)
        assertEquals(0, questions[3].currentRating)
    }

    private fun createSessionFeedbackViewModel(
        signInViewModelPlugin: SignInViewModelDelegate = FakeSignInViewModelDelegate(),
        loadUserSessionUseCase: LoadUserSessionUseCase = createTestLoadUserSessionUseCase(),
        feedbackUseCase: FeedbackUseCase = createTestFeedbackUseCase()
    ): SessionFeedbackViewModel {
        return SessionFeedbackViewModel(
            signInViewModelDelegate = signInViewModelPlugin,
            loadUserSessionUseCase = loadUserSessionUseCase,
            feedbackUseCase = feedbackUseCase
        )
    }

    private fun createTestLoadUserSessionUseCase(
        userEventDataSource: UserEventDataSource = TestUserEventDataSource()
    ): LoadUserSessionUseCase {
        return LoadUserSessionUseCase(
            DefaultSessionAndUserEventRepository(
                userEventDataSource,
                DefaultSessionRepository(TestDataRepository)
            ),
            coroutineRule.testDispatcher
        )
    }

    private fun createTestFeedbackUseCase(
        userEventDataSource: UserEventDataSource = TestUserEventDataSource()
    ): FeedbackUseCase {
        return FeedbackUseCase(
            object : FeedbackEndpoint {
                override suspend fun sendFeedback(
                    sessionId: SessionId,
                    responses: Map<String, Int>
                ): Result<Unit> {
                    return Result.Success(Unit)
                }
            },
            DefaultSessionAndUserEventRepository(
                userEventDataSource,
                DefaultSessionRepository(TestDataRepository)
            ),
            coroutineRule.testDispatcher
        )
    }
}
