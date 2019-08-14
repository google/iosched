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

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.androidtest.util.LiveDataTestUtil
import com.google.samples.apps.iosched.model.ConferenceDay
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.data.userevent.UserEventMessage
import com.google.samples.apps.iosched.shared.data.userevent.UserEventMessageChangeType
import com.google.samples.apps.iosched.shared.data.userevent.UserEventsResult
import com.google.samples.apps.iosched.shared.domain.repository.TestUserEventDataSource
import com.google.samples.apps.iosched.shared.model.TestDataRepository
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.shared.util.SyncExecutorRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.data.TestData.TestConferenceDays
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [LoadFilteredUserSessionsUseCase]
 */
class LoadFilteredUserSessionsUseCaseTest {

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
            DefaultSessionRepository(TestDataRepository)
        )
        val useCase = LoadFilteredUserSessionsUseCase(testUserEventRepository)

        val resultLiveData = useCase.observe()

        useCase.execute(LoadFilteredUserSessionsParameters(UserSessionMatcher(), "user1"))

        val result = LiveDataTestUtil.getValue(resultLiveData)
            as Result.Success<LoadFilteredUserSessionsResult>

        assertThat(TestData.userSessionList, `is`(equalTo(result.data.userSessions)))
    }

    @Test
    fun userEventsMessage() {

        val userEventsResult: MutableLiveData<UserEventsResult> = MutableLiveData()

        val testUserEventRepository = DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(userEventsResult),
            DefaultSessionRepository(TestDataRepository)
        )
        val useCase = LoadFilteredUserSessionsUseCase(testUserEventRepository)

        val resultLiveData = useCase.observe()

        useCase.execute(LoadFilteredUserSessionsParameters(UserSessionMatcher(), "user1"))

        userEventsResult.postValue(
            UserEventsResult(
                userEventsMessage = UserEventMessage(
                    UserEventMessageChangeType.CHANGES_IN_RESERVATIONS
                ),
                userEvents = TestData.userEvents
            )
        )

        val result = LiveDataTestUtil.getValue(resultLiveData)
            as Result.Success<LoadFilteredUserSessionsResult>

        assertThat(TestData.userSessionList, `is`(equalTo(result.data.userSessions)))

        // TODO(b/122306429) fix this
//        assertThat(
//            UserEventMessage(UserEventMessageChangeType.CHANGES_IN_RESERVATIONS),
//            `is`(equalTo(result.data.userMessage))
//        )
    }

    @Test
    fun errorCase() {
        val userEventsResult: MutableLiveData<UserEventsResult> = MutableLiveData()
        val testUserEventRepository = DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(userEventsResult),
            FailingSessionRepository
        )

        val useCase = LoadFilteredUserSessionsUseCase(testUserEventRepository)
        val resultLiveData = useCase.observe()

        useCase.execute(LoadFilteredUserSessionsParameters(UserSessionMatcher(), "user1"))

        val result = LiveDataTestUtil.getValue(resultLiveData)

        assertThat(result, `is`(instanceOf(Result.Error::class.java)))
    }

    @Test
    fun returnsCurrentEventIndex() {
        // Given the use case
        val userEventsResult: MutableLiveData<UserEventsResult> = MutableLiveData()

        val testUserEventRepository = DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(userEventsResult),
            DefaultSessionRepository(TestDataRepository)
        )
        val useCase = LoadFilteredUserSessionsUseCase(testUserEventRepository)
        val resultLiveData = useCase.observe()

        // When we execute it, passing Day 2 +3hrs as the current time
        val now = TestConferenceDays.first().start.plusHours(3L)
        useCase.execute(LoadFilteredUserSessionsParameters(UserSessionMatcher(), "user1", now))

        // Then the expected indexes are returned
        val result = LiveDataTestUtil.getValue(resultLiveData)
            as Result.Success<LoadFilteredUserSessionsResult>

        assertThat(result.data.firstUnfinishedSessionIndex, `is`(equalTo(0)))
    }

    @Test
    fun midConference_afterDayEnd_returnsCurrentEventIndex() {
        // Given the use case
        val userEventsResult: MutableLiveData<UserEventsResult> = MutableLiveData()

        val testUserEventRepository = DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(userEventsResult),
            DefaultSessionRepository(TestDataRepository)
        )
        val useCase = LoadFilteredUserSessionsUseCase(testUserEventRepository)
        val resultLiveData = useCase.observe()

        // When we execute it, passing Day 2 *after the end of day*
        val now = TestConferenceDays[1].end.plusHours(3L)
        useCase.execute(LoadFilteredUserSessionsParameters(UserSessionMatcher(), "user1", now))

        // Then returns the index of the first session the next day
        val result = LiveDataTestUtil.getValue(resultLiveData)
            as Result.Success<LoadFilteredUserSessionsResult>

        assertThat(result.data.firstUnfinishedSessionIndex, `is`(equalTo(3)))
    }

    @Test
    fun beforeConference_returnsNoCurrentEventIndex() {
        // Given the use case
        val userEventsResult: MutableLiveData<UserEventsResult> = MutableLiveData()

        val testUserEventRepository = DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(userEventsResult),
            DefaultSessionRepository(TestDataRepository)
        )
        val useCase = LoadFilteredUserSessionsUseCase(testUserEventRepository)
        val resultLiveData = useCase.observe()

        // When we execute it, passing a current time *before* the conference
        val now = TestConferenceDays.first().start.minusDays(2L)
        useCase.execute(LoadFilteredUserSessionsParameters(UserSessionMatcher(), "user1", now))

        // Then the expected indexes are returned
        val result = LiveDataTestUtil.getValue(resultLiveData)
            as Result.Success<LoadFilteredUserSessionsResult>

        assertThat(result.data.firstUnfinishedSessionIndex, `is`(equalTo(-1)))
    }

    @Test
    fun afterConference_returnsNoCurrentEventIndex() {
        // Given the use case
        val userEventsResult: MutableLiveData<UserEventsResult> = MutableLiveData()

        val testUserEventRepository = DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(userEventsResult),
            DefaultSessionRepository(TestDataRepository)
        )
        val useCase = LoadFilteredUserSessionsUseCase(testUserEventRepository)
        val resultLiveData = useCase.observe()

        // When we execute it, passing a current time *after* the conference
        val now = TestConferenceDays.last().end.plusHours(2L)
        useCase.execute(LoadFilteredUserSessionsParameters(UserSessionMatcher(), "user1", now))

        // Then the expected indexes are returned
        val result = LiveDataTestUtil.getValue(resultLiveData)
            as Result.Success<LoadFilteredUserSessionsResult>

        assertThat(result.data.firstUnfinishedSessionIndex, `is`(equalTo(-1)))
    }
}

object FailingSessionRepository : SessionRepository {
    override fun getSessions(): List<Session> {
        throw Exception("test")
    }

    override fun getSession(eventId: SessionId): Session {
        throw Exception("test")
    }

    override fun getConferenceDays(): List<ConferenceDay> {
        throw Exception("test")
    }
}
