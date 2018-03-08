package com.google.samples.apps.iosched.shared.firestore.entity

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
        val isStarred: Boolean,

        /** Tracks whether the user has provided feedback for the event. */
        val isReviewed: Boolean,

        /** Stores the result of a reservation request for the event. */
        val reservation: ReservationRequestResult
)