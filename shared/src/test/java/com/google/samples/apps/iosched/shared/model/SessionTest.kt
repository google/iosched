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

package com.google.samples.apps.iosched.model

import com.google.samples.apps.iosched.model.SessionType.CODELAB
import com.google.samples.apps.iosched.model.SessionType.SESSION
import com.google.samples.apps.iosched.model.SessionType.UNKNOWN
import com.google.samples.apps.iosched.test.data.TestData
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.Is
import org.junit.Test
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

/**
 * Unit tests for [Session].
 */
class SessionTest {

    @Test
    fun testIsOverlapping_bothHaveSameTime() {
        testIsOverlapping(
            eventStart = 1000L, eventEnd = 2000L,
            sessionStart = 1000L, sessionEnd = 2000L, expected = true
        )
    }

    @Test
    fun testIsOverlapping_sessionEndInTheMiddle() {
        testIsOverlapping(
            eventStart = 1000L, eventEnd = 2000L,
            sessionStart = 500L, sessionEnd = 1500L, expected = true
        )
    }

    @Test
    fun testIsOverlapping_sessionStartsBeforeAndEndsAfter() {
        testIsOverlapping(
            eventStart = 1000L, eventEnd = 2000L,
            sessionStart = 500L, sessionEnd = 2500L, expected = true
        )
    }

    @Test
    fun testIsOverlapping_sessionStartsInTheMiddle() {
        testIsOverlapping(
            eventStart = 1000L, eventEnd = 2000L,
            sessionStart = 1500, sessionEnd = 2500, expected = true
        )
    }

    @Test
    fun testIsOverlapping_sessionIsWithinSession() {
        testIsOverlapping(
            eventStart = 1000L, eventEnd = 2000L,
            sessionStart = 1200L, sessionEnd = 1700L, expected = true
        )
    }

    @Test
    fun testIsNotOverlapping_sessionStartsFromSessionEnds() {
        testIsOverlapping(
            eventStart = 1000L, eventEnd = 2000L,
            sessionStart = 2000L, sessionEnd = 3000L, expected = false
        )
    }

    @Test
    fun testIsNotOverlapping_sessionStartsAfterSessionEnds() {
        testIsOverlapping(
            eventStart = 1000L, eventEnd = 2000L,
            sessionStart = 3000L, sessionEnd = 4000L, expected = false
        )
    }

    @Test
    fun testType_sessions() {
        assertThat(TestData.session0.type, `is`(SESSION))
    }

    @Test
    fun testType_codelabs() {
        assertThat(TestData.session1.type, `is`(CODELAB))
    }

    @Test
    fun testType_unknown() {
        val session = TestData.session0.copy(tags = listOf())
        assertThat(session.type, `is`(UNKNOWN))
    }

    @Test
    fun testDescription() {
        assertThat(TestData.session0.getCalendarDescription("\n\n", ", "), `is`(equalTo(
            """
                This session is awesome

                Troy McClure
            """.trimIndent()
        )))
    }

    private fun testIsOverlapping(
        eventStart: Long,
        eventEnd: Long,
        sessionStart: Long,
        sessionEnd: Long,
        expected: Boolean
    ) {
        val baseSession = TestData.session0.copy(
            startTime = toZonedTime(eventStart), endTime = toZonedTime(eventEnd)
        )
        val session = TestData.session0.copy(
            startTime = toZonedTime(sessionStart), endTime = toZonedTime(sessionEnd)
        )
        assertThat(baseSession.isOverlapping(session), Is.`is`(expected))
    }

    private fun toZonedTime(sessionStart: Long): ZonedDateTime {
        return ZonedDateTime.ofInstant(
            Instant.ofEpochMilli(sessionStart), ZoneId.of(ZoneId.SHORT_IDS["JST"])
        )
    }
}
