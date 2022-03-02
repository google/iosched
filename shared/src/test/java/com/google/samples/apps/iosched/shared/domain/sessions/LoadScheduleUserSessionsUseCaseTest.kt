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
import com.google.samples.apps.iosched.model.ConferenceDay
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.domain.repository.TestUserEventDataSource
import com.google.samples.apps.iosched.shared.model.TestDataRepository
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.data.TestData.TestConferenceDays
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.Matchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [LoadScheduleUserSessionsUseCase]
 */
class LoadScheduleUserSessionsUseCaseTest {

    // Executes tasks in the Architecture Components in the same thread
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    private lateinit var useCase: LoadScheduleUserSessionsUseCase

    @Before
    fun setup() {
        val testUserEventRepository = DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(),
            DefaultSessionRepository(TestDataRepository)
        )
        useCase =
            LoadScheduleUserSessionsUseCase(testUserEventRepository, coroutineRule.testDispatcher)
    }

    @Test
    fun returnsMapOfSessions() = runTest {
        val useCaseResult = useCase(LoadScheduleUserSessionsParameters("user1"))
            .first { it is Result.Success }

        assertThat(TestData.userSessionList, `is`(equalTo(useCaseResult.data?.userSessions)))
    }

    /* to fix this: b/149009118
    @Test
    fun userEventsMessage() {

        val userEventsResult: MutableLiveData<UserEventsResult> = MutableLiveData()

        val testUserEventRepository = DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(userEventsResult),
            DefaultSessionRepository(TestDataRepository)
        )
        val useCase = LoadFilteredUserSessionsUseCase(
            testUserEventRepository,
            coroutineRule.testDispatcher
        )



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
     */

    @Test
    fun errorCase() = runTest {
        // Use a repository that throws an error
        val testUserEventRepository = DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(),
            FailingSessionRepository
        )
        val errorUseCase =
            LoadScheduleUserSessionsUseCase(testUserEventRepository, coroutineRule.testDispatcher)

        val useCaseResult = errorUseCase(LoadScheduleUserSessionsParameters("user1"))
            .first { it is Result.Error }

        assertTrue(useCaseResult is Result.Error)
    }

    @Test
    fun returnsCurrentEventIndex() = runTest {
        // When we execute it, passing Day 2 +3hrs as the current time
        val now = TestConferenceDays.first().start.plusHours(3L)
        val useCaseResult = useCase(LoadScheduleUserSessionsParameters("user1", now))
            .first { it is Result.Success }

        assertThat(useCaseResult.data?.firstUnfinishedSessionIndex, `is`(equalTo(0)))
    }

    @Test
    fun midConference_afterDayEnd_returnsCurrentEventIndex() = runTest {
        // When we execute it, passing Day 2 *after the end of day*
        val now = TestConferenceDays[1].end.plusHours(3L)

        val useCaseResult = useCase(LoadScheduleUserSessionsParameters("user1", now))
            .first { it is Result.Success }

        assertThat(useCaseResult.data?.firstUnfinishedSessionIndex, `is`(equalTo(3)))
    }

    @Test
    fun beforeConference_returnsNoCurrentEventIndex() = runTest {
        // When we execute it, passing a current time *before* the conference
        val now = TestConferenceDays.first().start.minusDays(2L)
        val useCaseResult = useCase(LoadScheduleUserSessionsParameters("user1", now))
            .first { it is Result.Success }

        assertThat(useCaseResult.data?.firstUnfinishedSessionIndex, `is`(equalTo(-1)))
    }

    @Test
    fun afterConference_returnsNoCurrentEventIndex() = runTest {
        // When we execute it, passing a current time *after* the conference
        val now = TestConferenceDays.last().end.plusHours(2L)
        val useCaseResult = useCase(LoadScheduleUserSessionsParameters("user1", now))
            .first { it is Result.Success }

        assertThat(useCaseResult.data?.firstUnfinishedSessionIndex, `is`(equalTo(-1)))
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
