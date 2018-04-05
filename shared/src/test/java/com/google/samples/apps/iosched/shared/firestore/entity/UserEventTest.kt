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

package com.google.samples.apps.iosched.shared.firestore.entity

import com.google.samples.apps.iosched.shared.firestore.entity.ReservationRequestResult.ReservationRequestStatus
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent.ReservationStatus
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent.ReservationStatus.NONE
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent.ReservationStatus.RESERVED
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent.ReservationStatus.WAITLISTED
import com.google.samples.apps.iosched.shared.model.TestData
import org.hamcrest.core.Is.`is`
import org.junit.Assert.assertThat
import org.junit.Assert.assertTrue
import org.junit.Test
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

class UserEventTest {

    private fun createTestEvent(isStarred: Boolean = false,
                                status: ReservationStatus = NONE,
                                requestResult: ReservationRequestResult? = null): UserEvent {
        return TestData.userEvents[0].copy(
            isStarred = isStarred,
                reservationStatus = status,
                reservationRequestResult = requestResult)
    }

    private fun createRequestResult(requestStatus: ReservationRequestStatus):
            ReservationRequestResult {
        return ReservationRequestResult(requestStatus, requestId = "dummy", timestamp = -1)
    }

    @Test
    fun starred_isPinned() {
        val userEvent = createTestEvent(isStarred = true, status = NONE)
        assertTrue(userEvent.isPinned())
    }

    @Test
    fun notStarred_waitlisted_isPinned() {
        val waitlisted = createTestEvent(isStarred = false, status = WAITLISTED)
        assertTrue(waitlisted.isPinned())
    }

    @Test
    fun notStarred_reserved_isPinned() {
        val reserved = createTestEvent(isStarred = false, status = RESERVED)
        assertTrue(reserved.isPinned())
    }

    @Test
    fun changedBySwap_isLastRequestResultBySwap() {
        val reservedUserEvent = createTestEvent(status = RESERVED,
                requestResult = createRequestResult(ReservationRequestStatus.SWAP_SUCCEEDED))
        assertTrue(reservedUserEvent.isLastRequestResultBySwap())

        val noneUserEvent = createTestEvent(status = NONE,
                requestResult = createRequestResult(ReservationRequestStatus.SWAP_SUCCEEDED))
        assertTrue(noneUserEvent.isLastRequestResultBySwap())

        val waitlistedUserEvent = createTestEvent(status = WAITLISTED,
                requestResult = createRequestResult(ReservationRequestStatus.SWAP_WAITLISTED))
        assertTrue(waitlistedUserEvent.isLastRequestResultBySwap())
    }

    @Test
    fun testIsOverlapping_bothHaveSameTime() {
        testIsOverlapping(eventStart = 1000L, eventEnd = 2000L,
                sessionStart = 1000L, sessionEnd = 2000L, expected = true)
    }

    @Test
    fun testIsOverlapping_sessionEndInTheMiddle() {
        testIsOverlapping(eventStart = 1000L, eventEnd = 2000L,
                sessionStart = 500L, sessionEnd = 1500L, expected = true)
    }

    @Test
    fun testIsOverlapping_sessionStartsBeforeAndEndsAfter() {
        testIsOverlapping(eventStart = 1000L, eventEnd = 2000L,
                sessionStart = 500L, sessionEnd = 2500L, expected = true)
    }

    @Test
    fun testIsOverlapping_sessionStartsInTheMiddle() {
        testIsOverlapping(eventStart = 1000L, eventEnd = 2000L,
                sessionStart = 1500, sessionEnd = 2500, expected = true)
    }

    @Test
    fun testIsOverlapping_sessionIsWithinUserEvent() {
        testIsOverlapping(eventStart = 1000L, eventEnd = 2000L,
                sessionStart = 1200L, sessionEnd = 1700L, expected = true)
    }

    @Test
    fun testIsNotOverlapping_sessionStartsFromUserEventEnds() {
        testIsOverlapping(eventStart = 1000L, eventEnd = 2000L,
                sessionStart = 2000L, sessionEnd = 3000L, expected = false)
    }

    @Test
    fun testIsNotOverlapping_sessionStartsAfterUserEventEnds() {
        testIsOverlapping(eventStart = 1000L, eventEnd = 2000L,
                sessionStart = 3000L, sessionEnd = 4000L, expected = false)
    }

    private fun testIsOverlapping(eventStart: Long,
                                  eventEnd: Long,
                                  sessionStart: Long,
                                  sessionEnd: Long,
                                  expected: Boolean) {
        val userEvent = TestData.userEvents[0].copy(startTime = eventStart,
                endTime = eventEnd)
        val sessionStartTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(sessionStart),
                ZoneId.of(ZoneId.SHORT_IDS["JST"]))
        val sessionEndTime = ZonedDateTime.ofInstant(Instant.ofEpochMilli(sessionEnd),
                ZoneId.of(ZoneId.SHORT_IDS["JST"]))
        val session = TestData.session0.copy(startTime = sessionStartTime, endTime = sessionEndTime)
        assertThat(userEvent.isOverlapping(session), `is`(expected))
    }

    //TODO: Add tests for isReserved, isWaitlisted, etc.
}
