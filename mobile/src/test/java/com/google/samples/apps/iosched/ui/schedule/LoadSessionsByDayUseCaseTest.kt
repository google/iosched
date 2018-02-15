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

package com.google.samples.apps.iosched.ui.schedule


import com.google.samples.apps.iosched.model.TestData
import com.google.samples.apps.iosched.shared.data.session.SessionDataSource
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [LoadSessionsByDayUseCase]
 */
class LoadSessionsByDayUseCaseTest {

    @Test
    fun returnsMapOfSessions() {
        val useCase = LoadSessionsByDayUseCase(SessionRepository(TestSessionDataSource))
        val sessions = useCase.executeNow(SessionFilters())
                as Result.Success<Map<ConferenceDay, List<Session>>>

        assertEquals(TestData.sessionsMap, sessions.data)
    }

    object TestSessionDataSource : SessionDataSource {
        override fun getSessions(): List<Session> {
            return listOf(TestData.session0, TestData.session1, TestData.session2,
                    TestData.session3)
        }

        override fun getSession(sessionId: String) = TestData.session0
    }
}
