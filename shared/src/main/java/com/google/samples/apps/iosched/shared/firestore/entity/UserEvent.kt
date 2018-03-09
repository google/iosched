package com.google.samples.apps.iosched.shared.firestore.entity

import com.google.samples.apps.iosched.shared.firestore.entity.ReservationRequestResult.ReservationRequestStatus.RESERVE_SUCCEEDED
import com.google.samples.apps.iosched.shared.firestore.entity.ReservationRequestResult.ReservationRequestStatus.RESERVE_WAITLISTED

/**
 * Data for a user's personalized event stored in a Firestore document.
 */
data class UserEvent(
        /**
         * The unique ID for the event.
         */
        val id: String,

        /** The start time for the event. Stored in Firestore to facilitate detection of overlapping
         * events.
         */
        val startTime: Long,

        /** The end time for the event. Stored in Firestore to facilitate detection of overlapping
         * events.
         */
        val endTime: Long,

        /** Tracks whether the user has starred the event. */
        val isStarred: Boolean = false,

        /** Tracks whether the user has provided feedback for the event. */
        val isReviewed: Boolean = false,

        /** Stores the result of a reservation request for the event. */
        val reservation: ReservationRequestResult? = null,

        /**
         * Whether this entity has a pending write to the server.
         * This flag is set to true when there is a locally modified data, but not synced to the
         * server.
         * See [https://firebase.google.com/docs/firestore/query-data/listen] for more details.
         */
        var hasPendingWrite: Boolean = false
) {
    fun isPinned(): Boolean {
        return isStarred
                || reservation?.status == RESERVE_SUCCEEDED
                || reservation?.status == RESERVE_WAITLISTED
    }
}
