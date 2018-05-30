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

package com.google.samples.apps.iosched.ui.reservation

import androidx.annotation.StringRes
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.userdata.UserEvent

/**
 * Models the different states of a reservation and a corresponding content description.
 */
enum class ReservationViewState(
    val state: IntArray,
    @StringRes val text: Int,
    @StringRes val contentDescription: Int
) {
    RESERVABLE(
        intArrayOf(R.attr.state_reservable),
        R.string.reservation_reservable,
        R.string.a11y_reservation_available
    ),
    WAIT_LIST_AVAILABLE(
        intArrayOf(R.attr.state_wait_list_available),
        R.string.reservation_waitlist_available,
        R.string.a11y_reservation_wait_list_available
    ),
    WAIT_LISTED(
        intArrayOf(R.attr.state_wait_listed),
        R.string.reservation_waitlisted,
        R.string.a11y_reservation_wait_listed
    ),
    RESERVED(
        intArrayOf(R.attr.state_reserved),
        R.string.reservation_reserved,
        R.string.a11y_reservation_reserved
    ),
    RESERVATION_PENDING(
        intArrayOf(R.attr.state_reservation_pending),
        R.string.reservation_pending,
        R.string.a11y_reservation_pending
    ),
    RESERVATION_DISABLED(
        intArrayOf(R.attr.state_reservation_disabled),
        R.string.reservation_disabled,
        R.string.a11y_reservation_disabled
    );

    companion object {
        fun fromUserEvent(userEvent: UserEvent?, unavailable: Boolean): ReservationViewState {
            return when {
                // Order is significant, e.g. a pending cancellation is also reserved.
                userEvent?.isReservationPending() == true ||
                    userEvent?.isCancelPending() == true -> {
                    // Treat both pending reservations & cancellations the same. This is important
                    // as the icon animations all expect to do through the same pending state.
                    RESERVATION_PENDING
                }
                userEvent?.isReserved() == true -> RESERVED
                userEvent?.isWaitlisted() == true -> WAIT_LISTED
                // TODO ?? -> WAIT_LIST_AVAILABLE
                unavailable -> RESERVATION_DISABLED
                else -> RESERVABLE
            }
        }
    }
}
