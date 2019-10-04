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

package com.google.samples.apps.iosched.shared.data.userevent

import com.google.samples.apps.iosched.model.reservations.ReservationRequest
import com.google.samples.apps.iosched.model.reservations.ReservationRequest.ReservationRequestEntityAction
import com.google.samples.apps.iosched.model.reservations.ReservationRequestResult
import com.google.samples.apps.iosched.model.reservations.ReservationRequestResult.ReservationRequestStatus
import com.google.samples.apps.iosched.model.reservations.ReservationRequestResult.ReservationRequestStatus.SWAP_SUCCEEDED
import com.google.samples.apps.iosched.model.reservations.ReservationRequestResult.ReservationRequestStatus.SWAP_WAITLISTED
import com.google.samples.apps.iosched.model.userdata.UserEvent
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsEqual.equalTo
import org.hamcrest.core.IsNull.nullValue
import org.junit.Assert.assertThat
import org.junit.Test

/**
 * Unit tests for [compareOldAndNewUserEvents].
 */
class CompareOldAndNewUserEventsTest {

    @Test
    fun generateReservationChangeMsg_noMessage() {

        val oldState = createUserEvent()
        val newState = createUserEvent()

        val result = compareOldAndNewUserEvents(oldState, newState, oldState.id)

        assertThat(result?.type, `is`(nullValue()))
    }

    @Test
    fun compareOldAndNew_newStateReserved_reservationChangeMessage() {

        val oldState = createUserEvent()
        val newState = createUserEvent(reservationStatus = UserEvent.ReservationStatus.RESERVED)

        val result = compareOldAndNewUserEvents(oldState, newState, oldState.id)

        assertThat(result?.type, `is`(equalTo(UserEventMessageChangeType.CHANGES_IN_RESERVATIONS)))
    }

    @Test
    fun compareOldAndNew_newStateReservedBySwap_reservationReplacedMessage() {
        val oldState = createUserEvent()
        val newState = createUserEvent(
            reservationStatus = UserEvent.ReservationStatus.RESERVED,
            reservationRequestResult = createReservationResult(requestResult = SWAP_SUCCEEDED)
        )

        val result = compareOldAndNewUserEvents(oldState, newState, oldState.id)

        assertThat(result?.type, `is`(equalTo(UserEventMessageChangeType.RESERVATIONS_REPLACED)))
    }

    @Test
    fun compareOldAndNew_newStateWaitlistedBySwap_waitlistChangeMessage() {
        val oldState = createUserEvent(
            reservationRequest = createReservationRequest(
                ReservationRequestEntityAction.RESERVE_REQUESTED
            )
        )
        val newState = createUserEvent(
            reservationStatus = UserEvent.ReservationStatus.WAITLISTED,
            reservationRequestResult = createReservationResult(requestResult = SWAP_WAITLISTED)
        )

        val result = compareOldAndNewUserEvents(oldState, newState, oldState.id)

        assertThat(result?.type, `is`(equalTo(UserEventMessageChangeType.CHANGES_IN_WAITLIST)))
    }

    @Test
    fun compareOldAndNew_newStateUnreserved_reservationChangeMessage() {

        val oldState = createUserEvent(reservationStatus = UserEvent.ReservationStatus.RESERVED)
        val newState = createUserEvent()

        val result = compareOldAndNewUserEvents(oldState, newState, oldState.id)

        assertThat(result?.type, `is`(equalTo(UserEventMessageChangeType.RESERVATION_CANCELED)))
    }

    @Test
    fun compareOldAndNew_newStateWaitlisted_waitlistChangeMessage() {

        val oldState = createUserEvent(
            reservationRequest = createReservationRequest(
                ReservationRequestEntityAction.RESERVE_REQUESTED
            )
        )

        val newState = createUserEvent(reservationStatus = UserEvent.ReservationStatus.WAITLISTED)

        val result = compareOldAndNewUserEvents(oldState, newState, oldState.id)

        assertThat(result?.type, `is`(equalTo(UserEventMessageChangeType.CHANGES_IN_WAITLIST)))
    }

    @Test
    fun compareOldAndNew_newStateCancelWaitlist_waitlistCancelMessage() {

        val oldState = createUserEvent(reservationStatus = UserEvent.ReservationStatus.WAITLISTED)
        val newState = createUserEvent()

        val result = compareOldAndNewUserEvents(oldState, newState, oldState.id)

        assertThat(result?.type, `is`(equalTo(UserEventMessageChangeType.WAITLIST_CANCELED)))
    }

    @Test
    fun compareOldAndNew_requestMade_noMessage() {

        val oldState = createUserEvent()
        val newStateRequest = createReservationRequest(
            action = ReservationRequestEntityAction.RESERVE_REQUESTED
        )

        val newState = createUserEvent(
            reservationRequest = newStateRequest
        )
        val result = compareOldAndNewUserEvents(oldState, newState, oldState.id)
        assertThat(result?.type, `is`(nullValue()))
    }

    @Test
    fun compareOldAndNew_differentIdsOldResult_noMessage() {

        val oldState = createUserEvent()
        val newStateRequest = createReservationRequest(
            action = ReservationRequestEntityAction.RESERVE_REQUESTED,
            requestId = "something"
        )
        val newRequestResult = createReservationResult(
            requestResult = ReservationRequestStatus.RESERVE_DENIED_CLASH,
            requestId = "something_different"

        )

        val newState = createUserEvent(
            reservationRequest = newStateRequest,
            reservationRequestResult = newRequestResult
        )

        val result = compareOldAndNewUserEvents(oldState, newState, oldState.id)

        assertThat(result?.type, `is`(nullValue()))
    }

    @Test
    fun compareOldAndNew_sameId_errorMessageClash() {

        val result = generateErrorResult(ReservationRequestStatus.RESERVE_DENIED_CLASH)

        assertThat(result?.type, `is`(equalTo(UserEventMessageChangeType.RESERVATION_DENIED_CLASH)))
    }

    @Test
    fun compareOldAndNew_sameId_errorMessageClashBySwap() {

        val result = generateErrorResult(ReservationRequestStatus.SWAP_DENIED_CLASH)

        assertThat(result?.type, `is`(equalTo(UserEventMessageChangeType.RESERVATION_DENIED_CLASH)))
    }

    @Test
    fun compareOldAndNew_sameId_errorMessageCutoff() {

        val result = generateErrorResult(ReservationRequestStatus.RESERVE_DENIED_CUTOFF)

        assertThat(
            result?.type,
            `is`(equalTo(UserEventMessageChangeType.RESERVATION_DENIED_CUTOFF))
        )
    }

    @Test
    fun compareOldAndNew_sameId_errorMessageCutoffBySwap() {

        val result = generateErrorResult(ReservationRequestStatus.SWAP_DENIED_CUTOFF)

        assertThat(
            result?.type,
            `is`(equalTo(UserEventMessageChangeType.RESERVATION_DENIED_CUTOFF))
        )
    }

    @Test
    fun compareOldAndNew_sameId_errorMessageUnknown() {

        val result = generateErrorResult(ReservationRequestStatus.RESERVE_DENIED_UNKNOWN)

        assertThat(
            result?.type,
            `is`(equalTo(UserEventMessageChangeType.RESERVATION_DENIED_UNKNOWN))
        )
    }

    @Test
    fun compareOldAndNew_sameId_errorMessageUnknownBySwap() {

        val result = generateErrorResult(ReservationRequestStatus.SWAP_DENIED_UNKNOWN)

        assertThat(
            result?.type,
            `is`(equalTo(UserEventMessageChangeType.RESERVATION_DENIED_UNKNOWN))
        )
    }

    @Test
    fun compareOldAndNew_sameId_errorMessageCancelCutoff() {

        val result = generateErrorResult(ReservationRequestStatus.CANCEL_DENIED_CUTOFF)

        assertThat(
            result?.type,
            `is`(equalTo(UserEventMessageChangeType.CANCELLATION_DENIED_CUTOFF))
        )
    }

    @Test
    fun compareOldAndNew_sameId_errorMessageCancelUknown() {

        val result = generateErrorResult(ReservationRequestStatus.CANCEL_DENIED_UNKNOWN)

        assertThat(
            result?.type,
            `is`(equalTo(UserEventMessageChangeType.CANCELLATION_DENIED_UNKNOWN))
        )
    }

    private fun generateErrorResult(errorResult: ReservationRequestStatus): UserEventMessage? {
        val oldState = createUserEvent()
        val newStateRequest = createReservationRequest(
            action = ReservationRequestEntityAction.RESERVE_REQUESTED,
            requestId = "42"
        )
        val newRequestResult = createReservationResult(
            requestResult = errorResult,
            requestId = "42"

        )

        val newState = createUserEvent(
            reservationRequest = newStateRequest,
            reservationRequestResult = newRequestResult
        )

        return compareOldAndNewUserEvents(oldState, newState, oldState.id)
    }

    private fun createUserEvent(
        id: String = "123",
        isStarred: Boolean = false,
        isReviewed: Boolean = false,
        reservationStatus: UserEvent.ReservationStatus? = null,
        reservationRequestResult: ReservationRequestResult? = null,
        reservationRequest: ReservationRequest? = null
    ): UserEvent {
        return UserEvent(
            id,
            isStarred,
            isReviewed,
            reservationStatus,
            reservationRequestResult,
            reservationRequest
        )
    }

    private fun createReservationResult(
        requestResult: ReservationRequestStatus? = null,
        requestId: String = "213",
        timestamp: Long = -1
    ): ReservationRequestResult {

        return ReservationRequestResult(
            requestResult = requestResult,
            requestId = requestId,
            timestamp = timestamp
        )
    }

    private fun createReservationRequest(
        action: ReservationRequestEntityAction =
            ReservationRequestEntityAction.RESERVE_REQUESTED,
        requestId: String = "321"
    ): ReservationRequest {
        return ReservationRequest(
            action = action,
            requestId = requestId
        )
    }
}
