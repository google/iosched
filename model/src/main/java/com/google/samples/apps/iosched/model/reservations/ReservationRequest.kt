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

package com.google.samples.apps.iosched.model.reservations

/**
 * Entity that represents the client's latest reservation or cancellation request. Used to figure
 * out whether a reservation request is pending or completed.
 */
data class ReservationRequest(
    /**
     * The action of the reservation request (REQUEST/CANCEL).
     */
    val action: ReservationRequestEntityAction,

    /**
     * An ID set by the client that will be added to the reservation result on completion.
     */
    val requestId: String
) {

    enum class ReservationRequestEntityAction {
        /** The reservation was granted */
        RESERVE_REQUESTED,

        /** The reservation was granted but the user was placed on a waitlist. */
        CANCEL_REQUESTED;

        companion object {

            fun getIfPresent(string: String): ReservationRequestEntityAction? {
                return try {
                    valueOf(string)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }
    }
}
