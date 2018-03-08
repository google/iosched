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


import com.google.samples.apps.iosched.shared.data.ConferenceDataRepository
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.data.session.UserEventRepository
import com.google.samples.apps.iosched.shared.domain.repository.TestSessionDataSource
import com.google.samples.apps.iosched.shared.domain.repository.TestUserEventDataSource
import com.google.samples.apps.iosched.shared.model.TestData
import com.google.samples.apps.iosched.shared.model.UserSession
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.schedule.SessionMatcher
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [LoadUserSessionsByDayUseCase]
 */
class LoadUserSessionsByDayUseCaseTest {

    @Test
    fun returnsMapOfSessions() {

        val testConferenceDataRepository = ConferenceDataRepository(
                boostrapDataSource = TestSessionDataSource,
                remoteDataSource = TestSessionDataSource)

        val testUserEventRepository = UserEventRepository(TestUserEventDataSource)

        val useCase = LoadUserSessionsByDayUseCase(SessionRepository(testConferenceDataRepository),
                testUserEventRepository)
        val sessions = useCase.executeNow(Pair(SessionMatcher(), "user1"))
                as Result.Success<Map<ConferenceDay, List<UserSession>>>

        assertEquals(TestData.userSessionMap, sessions.data)
    }
}
