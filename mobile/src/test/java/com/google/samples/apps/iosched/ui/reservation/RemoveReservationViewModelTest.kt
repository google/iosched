/*
 * Copyright 2021 Google LLC
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

package com.google.samples.apps.iosched.ui.reservation

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.TestDataRepository
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.data.userevent.UserEventDataSource
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.StarReserveNotificationAlarmUpdater
import com.google.samples.apps.iosched.shared.domain.users.ReservationActionUseCase
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.util.fakes.FakeSignInViewModelDelegate
import com.google.samples.apps.iosched.ui.schedule.TestUserEventDataSource
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for the [RemoveReservationViewModel].
 */
class RemoveReservationViewModelTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private val signInViewModelDelegate = FakeSignInViewModelDelegate()
    private val alarmUpdater: StarReserveNotificationAlarmUpdater = mock()

    @Before
    fun setUp() {
        signInViewModelDelegate.loadUser("123")
    }

    @Test
    fun dataIsLoaded() = runTest {
        val removeReservationViewModel = createRemoveReservationViewModel(TestData.session0.id)

        val userSession = removeReservationViewModel.userSession.first()
        assertEquals("0", userSession?.session?.id)
    }

    @Test
    fun reservationIsPlaced() = runTest {
        val removeReservationViewModel = createRemoveReservationViewModel(TestData.session0.id)

        val userSession = removeReservationViewModel.userSession.first()
        assertEquals("0", userSession?.session?.id)

        removeReservationViewModel.removeReservation()

        verify(alarmUpdater).updateSession(any(), any())
    }

    private fun createRemoveReservationViewModel(
        sessionId: SessionId = TestData.session0.id,
        signInViewModelDelegate: SignInViewModelDelegate = this.signInViewModelDelegate,
        loadUserSessionUseCase: LoadUserSessionUseCase = createTestLoadUserSessionUseCase(),
        reservationActionUseCase: ReservationActionUseCase = createReservationActionUseCase()
    ) = RemoveReservationViewModel(
        savedStateHandle = SavedStateHandle(mapOf("session_id" to sessionId)),
        signInViewModelDelegate = signInViewModelDelegate,
        loadUserSessionUseCase = loadUserSessionUseCase,
        reservationActionUseCase = reservationActionUseCase
    )

    private fun createTestLoadUserSessionUseCase(
        userEventDataSource: UserEventDataSource = TestUserEventDataSource()
    ): LoadUserSessionUseCase {
        val sessionRepository = DefaultSessionRepository(TestDataRepository)
        val userEventRepository = DefaultSessionAndUserEventRepository(
            userEventDataSource,
            sessionRepository
        )
        return LoadUserSessionUseCase(userEventRepository, coroutineRule.testDispatcher)
    }

    private fun createReservationActionUseCase() = object : ReservationActionUseCase(
        DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(), DefaultSessionRepository(TestDataRepository)
        ),
        alarmUpdater,
        coroutineRule.testDispatcher
    ) {}
}
