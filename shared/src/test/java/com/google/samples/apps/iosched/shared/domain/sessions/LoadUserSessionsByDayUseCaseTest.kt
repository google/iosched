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

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.data.userevent.UserEventMessage
import com.google.samples.apps.iosched.shared.data.userevent.UserEventMessageChangeType
import com.google.samples.apps.iosched.shared.data.userevent.UserEventsResult
import com.google.samples.apps.iosched.shared.domain.repository.TestUserEventDataSource
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.SessionId
import com.google.samples.apps.iosched.shared.model.TestData
import com.google.samples.apps.iosched.shared.model.TestDataRepository
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.shared.util.SyncExecutorRule
import com.google.samples.apps.iosched.test.util.LiveDataTestUtil
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [LoadUserSessionsByDayUseCase]
 */
class LoadUserSessionsByDayUseCaseTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Executes tasks in a synchronous [TaskScheduler]
    @get:Rule var syncExecutorRule = SyncExecutorRule()

    @Test
    fun returnsMapOfSessions() {

        val userEventsResult: MutableLiveData<UserEventsResult> = MutableLiveData()

        val testUserEventRepository = DefaultSessionAndUserEventRepository(
                TestUserEventDataSource(userEventsResult),
                DefaultSessionRepository(TestDataRepository))
        val useCase = LoadUserSessionsByDayUseCase(testUserEventRepository)

        val resultLiveData = useCase.observe()

        useCase.execute(Pair(UserSessionMatcher(), "user1"))

        val result = LiveDataTestUtil.getValue(resultLiveData)
                as Result.Success<LoadUserSessionsByDayUseCaseResult>

        assertThat(
                TestData.userSessionMap,
                `is`(equalTo(result.data.userSessionsPerDay)))
    }

    @Test
    fun userEventsMessage() {

        val userEventsResult: MutableLiveData<UserEventsResult> = MutableLiveData()

        val testUserEventRepository = DefaultSessionAndUserEventRepository(
                TestUserEventDataSource(userEventsResult),
                DefaultSessionRepository(TestDataRepository))
        val useCase = LoadUserSessionsByDayUseCase(testUserEventRepository)

        val resultLiveData = useCase.observe()

        useCase.execute(Pair(UserSessionMatcher(), "user1"))

        userEventsResult.postValue(UserEventsResult(
                userEventsMessage = UserEventMessage(
                        UserEventMessageChangeType.CHANGES_IN_RESERVATIONS),
                userEvents = TestData.userEvents))

        val result = LiveDataTestUtil.getValue(resultLiveData)
                as Result.Success<LoadUserSessionsByDayUseCaseResult>

        assertThat(
                TestData.userSessionMap,
                `is`(equalTo(result.data.userSessionsPerDay)))

        assertThat(
                UserEventMessage(UserEventMessageChangeType.CHANGES_IN_RESERVATIONS),
                `is`(equalTo(result.data.userMessage)))
    }

    @Test
    fun errorCase() {

        val userEventsResult: MutableLiveData<UserEventsResult> = MutableLiveData()

        val testUserEventRepository = DefaultSessionAndUserEventRepository(
                TestUserEventDataSource(userEventsResult),
                FailingSessionRepository
        )

        val useCase = LoadUserSessionsByDayUseCase(testUserEventRepository)

        val resultLiveData = useCase.observe()

        useCase.execute(Pair(UserSessionMatcher(), "user1"))

        val result = LiveDataTestUtil.getValue(resultLiveData)

        assertThat(result, `is`(instanceOf(Result.Error::class.java)))
    }
}

object FailingSessionRepository : SessionRepository{
    override fun getSessions() : List<Session> {
        throw Exception("test")
    }

    override fun getSession(eventId: SessionId): Session {
        throw Exception("test")
    }
}
