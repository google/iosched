package com.google.samples.apps.iosched.shared.firestore.entity


/**
 * Data for a reservation request stored in a Firebase document.
 *
 * Default values needed for Firestore's deserialization.
 */
data class ReservationRequestResult(
        /**
         * The status of the reservation request.
         */
        val requestResult: ReservationRequestStatus? = null,

        /* ID of the [ReservationRequest] that originated this result.*/
        val requestId: String,

        /** The time the status was acquired. */
        val timestamp: Long = -1
)
{

    enum class ReservationRequestStatus {
        /** The reservation was granted */
        RESERVE_SUCCEEDED,

        /** The reservation was granted but the user was placed on a waitlist. */
        RESERVE_WAITLISTED,

        /** The reservation request was denied because it was too close to the start of the
         * event. */
        RESERVE_DENIED_CUTOFF,

        /** The reservation was denied because it overlapped with another reservation or
         * waitlist. */
        RESERVE_DENIED_CLASH,

        /** The reservation was denied for unknown reasons. */
        RESERVE_DENIED_UNKNOWN,

        /** The reservation was successfully canceled. */
        CANCEL_SUCCEEDED,

        /** The cancellation request was denied because it was too close to the start of
         * the event. */
        CANCEL_DENIED_CUTOFF,

        /** The cancellation request was denied for unknown reasons. */
        CANCEL_DENIED_UNKNOWN;

        companion object {

            fun getIfPresent(string: String): ReservationRequestStatus? {
                return try {
                    valueOf(string)
                } catch(e: IllegalArgumentException) {
                    null
                }
            }
        }
    }
}
