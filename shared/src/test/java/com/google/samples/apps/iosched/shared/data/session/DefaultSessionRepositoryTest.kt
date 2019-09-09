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

package com.google.samples.apps.iosched.shared.data.session

import com.google.samples.apps.iosched.shared.model.TestDataRepository
import com.google.samples.apps.iosched.test.data.TestData
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [DefaultSessionRepository].
 */
class DefaultSessionRepositoryTest {

    private val repository = DefaultSessionRepository(TestDataRepository)

    @Test
    fun getSessions_userIsAttendee() {
        // When the user is not an attendee
        val sessions = repository.getSessions(true)

        // All sessions are loaded
        assertTrue(sessions.size == TestData.sessionsList.size)
    }

    @Test
    fun getSessions_userIsRemote() {
        // When the user is not an attendee
        val sessions = repository.getSessions(false)

        // Only the livestreamed sessions are loaded
        assertTrue(sessions.size == 1)
        assertTrue(sessions.first() == TestData.sessionWithYoutubeUrl)
    }
}