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

import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent.ReservationStatus
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent.ReservationStatus.NONE
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent.ReservationStatus.RESERVED
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent.ReservationStatus.WAITLISTED
import com.google.samples.apps.iosched.shared.model.TestData
import org.junit.Assert.assertTrue
import org.junit.Test

class UserEventTest {

    private fun createTestEvent(isStarred: Boolean, status: ReservationStatus): UserEvent {
        return TestData.userEvents[0].copy(
            isStarred = isStarred, reservationStatus = status)
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

    //TODO: Add tests for isReserved, isWaitlisted, etc.
}
