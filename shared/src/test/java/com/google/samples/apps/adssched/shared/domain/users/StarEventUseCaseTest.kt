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

package com.google.samples.apps.adssched.shared.domain.users

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.samples.apps.adssched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.adssched.model.ConferenceDay
import com.google.samples.apps.adssched.model.SessionId
import com.google.samples.apps.adssched.model.userdata.UserEvent
import com.google.samples.apps.adssched.model.userdata.UserSession
import com.google.samples.apps.adssched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.adssched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.adssched.shared.data.userevent.SessionAndUserEventRepository
import com.google.samples.apps.adssched.shared.domain.repository.TestUserEventDataSource
import com.google.samples.apps.adssched.shared.domain.sessions.LoadUserSessionUseCaseResult
import com.google.samples.apps.adssched.shared.domain.sessions.LoadUserSessionsByDayUseCaseResult
import com.google.samples.apps.adssched.shared.domain.sessions.StarNotificationAlarmUpdater
import com.google.samples.apps.adssched.shared.model.TestDataRepository
import com.google.samples.apps.adssched.shared.notifications.SessionAlarmManager
import com.google.samples.apps.adssched.shared.result.Result
import com.google.samples.apps.adssched.shared.util.SyncExecutorRule
import com.google.samples.apps.adssched.test.data.TestData
import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doNothing
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [StarEventAndNotifyUseCase]
 */
class StarEventAndNotifyUseCaseTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule
    var syncExecutorRule = SyncExecutorRule()

    @Test
    fun sessionIsStarredSuccessfully() {
        val testUserEventRepository = DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(), DefaultSessionRepository(TestDataRepository)
        )
        val useCase = StarEventAndNotifyUseCase(testUserEventRepository, mock {})

        val resultLiveData = useCase.observe()

        useCase.execute(StarEventParameter("userIdTest", TestData.userSession1))

        val result = LiveDataTestUtil.getValue(resultLiveData)
        Assert.assertEquals(result, Result.Success(StarUpdatedStatus.STARRED))
    }

    @Test
    fun sessionIsStarredUnsuccessfully() {
        val alarmManager: SessionAlarmManager = mock()
        doNothing().whenever(alarmManager).cancelAlarmForSession(any())
        val starNotificationAlarmUpdater = StarNotificationAlarmUpdater(alarmManager)

        val useCase = StarEventAndNotifyUseCase(
            FailingSessionAndUserEventRepository,
            starNotificationAlarmUpdater
        )

        val resultLiveData = useCase.observe()

        useCase.execute(StarEventParameter("userIdTest", TestData.userSession0))

        val result = LiveDataTestUtil.getValue(resultLiveData)
        assertTrue(result is Result.Error)
    }

    @Test
    fun sessionIsStarredAndNotificationSet() {
        val testUserEventRepository = DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(), DefaultSessionRepository(TestDataRepository)
        )
        val updater: StarNotificationAlarmUpdater = mock {}

        doNothing().whenever(updater).updateSession(any(), any())

        val useCase = StarEventAndNotifyUseCase(testUserEventRepository, updater)

        val resultLiveData = useCase.observe()

        useCase.execute(StarEventParameter("userIdTest", TestData.userSession0))

        LiveDataTestUtil.getValue(resultLiveData)

        verify(updater).updateSession(TestData.userSession0.session, false)
    }
}

val FailingSessionAndUserEventRepository = object : SessionAndUserEventRepository {

    val result = MutableLiveData<Result<StarUpdatedStatus>>()

    override fun starEvent(
        userId: String,
        userEvent: UserEvent
    ): LiveData<Result<StarUpdatedStatus>> {
        result.postValue(Result.Error(Exception("Test")))
        return result
    }

    override fun getObservableUserEvents(
        userId: String?
    ): LiveData<Result<LoadUserSessionsByDayUseCaseResult>> {
        throw NotImplementedError()
    }

    override fun getObservableUserEvent(
        userId: String?,
        eventId: SessionId
    ): LiveData<Result<LoadUserSessionUseCaseResult>> {
        throw NotImplementedError()
    }

    override fun getUserEvents(userId: String?): List<UserEvent> {
        throw NotImplementedError()
    }

    override fun getUserSession(userId: String, sessionId: SessionId): UserSession {
        throw NotImplementedError()
    }

    override fun clearSingleEventSubscriptions() {}

    override fun getConferenceDays(): List<ConferenceDay> {
        throw NotImplementedError()
    }
}
