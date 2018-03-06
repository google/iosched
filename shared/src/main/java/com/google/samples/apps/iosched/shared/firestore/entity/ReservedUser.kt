package com.google.samples.apps.iosched.shared.firestore.entity

/**
 * Data for a Firestore document for a user who has a reservation for an event or is
 * waitlisted for that event.
 */
data class ReservedUser(
        /** Unique Id, like the uid provided as a result of the signing in.*/
        val uid: String,

        /** The status of the user's reservation for an event. */
        val reservationStatus: ReservationStatus,

        /** The time that the user acquired reservation status. */
        val timestamp: Long
) {
        enum class ReservationStatus {RESERVED, WAITLISTED};
}