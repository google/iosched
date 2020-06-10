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

package com.google.samples.apps.iosched.shared.domain.users

import androidx.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.model.ConferenceDay
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.userdata.UserEvent
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.data.userevent.ObservableUserEvents
import com.google.samples.apps.iosched.shared.data.userevent.SessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.domain.repository.TestUserEventDataSource
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCaseResult
import com.google.samples.apps.iosched.shared.domain.sessions.StarReserveNotificationAlarmUpdater
import com.google.samples.apps.iosched.shared.model.TestDataRepository
import com.google.samples.apps.iosched.shared.notifications.SessionAlarmManager
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.data.runBlockingTest
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doNothing
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import kotlinx.coroutines.flow.Flow
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [StarEventAndNotifyUseCase]
 */
class StarEventAndNotifyUseCaseTest {

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun sessionIsStarredSuccessfully() = coroutineRule.runBlockingTest {
        val testUserEventRepository = DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(), DefaultSessionRepository(TestDataRepository)
        )
        val useCase = StarEventAndNotifyUseCase(testUserEventRepository,
            mock {},
            coroutineRule.testDispatcher)

        val result = useCase(StarEventParameter("userIdTest", TestData.userSession1))

        Assert.assertEquals(result, Result.Success(StarUpdatedStatus.STARRED))
    }

    @Test
    fun sessionIsStarredUnsuccessfully() = coroutineRule.runBlockingTest {
        val alarmManager: SessionAlarmManager = mock()
        doNothing().whenever(alarmManager).cancelAlarmForSession(any())
        val alarmUpdater = StarReserveNotificationAlarmUpdater(alarmManager)

        val useCase = StarEventAndNotifyUseCase(
            FailingSessionAndUserEventRepository,
            alarmUpdater,
            coroutineRule.testDispatcher
        )

        val result = useCase(StarEventParameter("userIdTest", TestData.userSession3))

        assertTrue(result is Result.Error)
    }

    @Test
    fun sessionIsStarredAndNotificationSet() = coroutineRule.runBlockingTest {
        val testUserEventRepository = DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(), DefaultSessionRepository(TestDataRepository)
        )
        val updater: StarReserveNotificationAlarmUpdater = mock {}

        doNothing().whenever(updater).updateSession(any(), any())

        val useCase = StarEventAndNotifyUseCase(
            testUserEventRepository,
            updater,
            coroutineRule.testDispatcher)

        useCase(StarEventParameter("userIdTest", TestData.userSession3))

        verify(updater).updateSession(TestData.userSession3, false)
    }
}

val FailingSessionAndUserEventRepository = object : SessionAndUserEventRepository {

    val result = MutableLiveData<Result<StarUpdatedStatus>>()

    override suspend fun starEvent(
        userId: String,
        userEvent: UserEvent
    ): Result<StarUpdatedStatus> = Result.Error(Exception("Test"))

    override suspend fun recordFeedbackSent(userId: String, userEvent: UserEvent): Result<Unit> {
        throw NotImplementedError()
    }

    override fun getObservableUserEvents(
        userId: String?
    ): Flow<Result<ObservableUserEvents>> {
        throw NotImplementedError()
    }

    override fun getObservableUserEvent(
        userId: String?,
        eventId: SessionId
    ): Flow<Result<LoadUserSessionUseCaseResult>> {
        throw NotImplementedError()
    }

    override suspend fun changeReservation(
        userId: String,
        sessionId: SessionId,
        action: ReservationRequestAction
    ): Result<ReservationRequestAction> {
        throw NotImplementedError()
    }

    override fun getUserEvents(userId: String?): List<UserEvent> {
        throw NotImplementedError()
    }

    override suspend fun swapReservation(
        userId: String,
        fromId: String,
        toId: String
    ): Result<SwapRequestAction> {
        throw NotImplementedError()
    }

    override fun getUserSession(userId: String, sessionId: SessionId): UserSession {
        throw NotImplementedError()
    }

    override fun getConferenceDays(): List<ConferenceDay> {
        throw NotImplementedError()
    }
}
