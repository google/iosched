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
import com.google.samples.apps.iosched.shared.firestore.entity.ReservationRequestResult.ReservationRequestStatus.RESERVE_DENIED_UNKNOWN
import com.google.samples.apps.iosched.shared.firestore.entity.ReservationRequestResult.ReservationRequestStatus.RESERVE_SUCCEEDED
import com.google.samples.apps.iosched.shared.firestore.entity.ReservationRequestResult.ReservationRequestStatus.RESERVE_WAITLISTED
import com.google.samples.apps.iosched.shared.model.TestData
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserEventTest {

    private fun createTestEvent(isStarred: Boolean, status: ReservationRequestStatus): UserEvent {
        return TestData.userEvents[0].copy(
            isStarred = isStarred,
            reservation = ReservationRequestResult(status = status, timestamp = 0L)
        )
    }

    @Test
    fun starred_isPinned() {
        val userEvent = createTestEvent(isStarred = true, status = RESERVE_DENIED_UNKNOWN)
        assertTrue(userEvent.isPinned())
    }

    @Test
    fun notStarred_reservedOrWaitlisted_isPinned() {
        val reserved = createTestEvent(isStarred = false, status = RESERVE_SUCCEEDED)
        val waitlisted = createTestEvent(isStarred = false, status = RESERVE_WAITLISTED)
        assertTrue(reserved.isPinned())
        assertTrue(waitlisted.isPinned())
    }

    @Test
    fun notStarredOrReservedOrWaitlisted_isNotPinned() {
        val successStates = listOf(RESERVE_SUCCEEDED, RESERVE_WAITLISTED)
        ReservationRequestStatus.values().subtract(successStates).forEach {
            val userEvent = createTestEvent(isStarred = false, status = it)
            assertFalse(userEvent.isPinned())
        }
    }
}
