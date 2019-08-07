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

import com.google.samples.apps.iosched.model.ConferenceDay
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.shared.data.session.DefaultSessionRepository
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.data.userevent.DefaultSessionAndUserEventRepository
import com.google.samples.apps.iosched.shared.domain.repository.TestUserEventDataSource
import com.google.samples.apps.iosched.shared.model.TestDataRepository
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.schedule.UserSessionMatcher
import com.google.samples.apps.iosched.test.data.MainCoroutineRule
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.data.runBlockingTest
import kotlinx.coroutines.flow.single
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.junit.Assert.assertThat
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [LoadUserSessionsByDayUseCase]
 */
class LoadUserSessionsByDayUseCaseTest {

    // Overrides Dispatchers.Main used in Coroutines
    @get:Rule
    var coroutineRule = MainCoroutineRule()

    @Test
    fun returnsMapOfSessions() = coroutineRule.runBlockingTest {

        val useCase = useCaseWithRepository()

        val result = useCase(LoadUserSessionsByDayUseCaseParameters(UserSessionMatcher(), "user1"))
            .single()

        assertThat(TestData.userSessionMap, `is`(equalTo(result.data?.userSessionsPerDay)))
    }

    @Test
    fun userEventsMessage() = coroutineRule.runBlockingTest {

        val useCase = useCaseWithRepository()

        val result = useCase(LoadUserSessionsByDayUseCaseParameters(UserSessionMatcher(), "user1"))
            .single()

        assertThat(TestData.userSessionMap, `is`(equalTo(result.data?.userSessionsPerDay)))
    }

    @Test
    fun errorCase() = coroutineRule.runBlockingTest {

        val useCase = useCaseWithRepository(FailingSessionRepository)

        val result = useCase(LoadUserSessionsByDayUseCaseParameters(UserSessionMatcher(), "user1"))
            .single()

        assertThat(result, `is`(instanceOf(Result.Error::class.java)))
    }

    @Test
    fun returnsCurrentEventIndex() = coroutineRule.runBlockingTest {

        val sessionRepository = DefaultSessionRepository(TestDataRepository)
        val useCase = useCaseWithRepository(sessionRepository)

        // When we execute it, passing Day 2 +3hrs as the current time
        val now = sessionRepository.getConferenceDays().first().start.plusHours(3L)

        val result = useCase(
            LoadUserSessionsByDayUseCaseParameters(UserSessionMatcher(), "user1", now)
        ).single()

        assertThat(EventLocation(0, 0), `is`(equalTo(result.data?.firstUnfinishedSession)))
    }

    @Test
    fun midConference_afterDayEnd_returnsCurrentEventIndex() = coroutineRule.runBlockingTest {

        val sessionRepository = DefaultSessionRepository(TestDataRepository)
        val useCase = useCaseWithRepository(sessionRepository)

        // When we execute it, passing Day 1 *after the end of day*
        val now = sessionRepository.getConferenceDays()[0].end.plusHours(3L)

        val result = useCase(
            LoadUserSessionsByDayUseCaseParameters(UserSessionMatcher(), "user1", now)
        ).single()

        assertThat(EventLocation(1, 0), `is`(equalTo(result.data?.firstUnfinishedSession)))
    }

    @Test
    fun beforeConference_returnsNoCurrentEventIndex() = coroutineRule.runBlockingTest {

        val useCase = useCaseWithRepository()

        // When we execute it, passing a current time *before* the conference
        val now = TestData.TestConferenceDays.first().start.minusDays(2L)

        val result = useCase(
            LoadUserSessionsByDayUseCaseParameters(UserSessionMatcher(), "user1", now)
        ).single()

        assertThat(null, `is`(equalTo(result.data?.firstUnfinishedSession)))
    }

    @Test
    fun afterConference_returnsNoCurrentEventIndex() = coroutineRule.runBlockingTest {

        val useCase = useCaseWithRepository()

        // When we execute it, passing a current time *after* the conference
        val now = TestData.TestConferenceDays.last().end.plusHours(2L)

        val result = useCase(
            LoadUserSessionsByDayUseCaseParameters(UserSessionMatcher(), "user1", now)
        ).single()

        assertThat(null, `is`(equalTo(result.data?.firstUnfinishedSession)))
    }

    private fun useCaseWithRepository(
        repository: SessionRepository = DefaultSessionRepository(TestDataRepository)
    ): LoadUserSessionsByDayUseCase {
        val testUserEventRepository = DefaultSessionAndUserEventRepository(
            TestUserEventDataSource(),
            repository
        )
        return LoadUserSessionsByDayUseCase(testUserEventRepository, coroutineRule.testDispatcher)
    }
}

object FailingSessionRepository : SessionRepository {

    override fun getSessions(): List<Session> {
        throw Exception("test")
    }

    override fun getSession(eventId: SessionId): Session {
        throw Exception("test")
    }

    override fun getConferenceDays(): List<ConferenceDay> = TestData.TestConferenceDays
}
