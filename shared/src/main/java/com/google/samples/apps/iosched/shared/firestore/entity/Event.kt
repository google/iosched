package com.google.samples.apps.iosched.shared.firestore.entity

import kotlin.math.max

/**
 * Data for a conference event found in a Firestore document.
 */
data class Event (
        /** Unique string from the CMS. */
        val id: String,

        /** The time the event begins */
        val startTime: Long,

        /** The time the event ends */
        val endTime: Long,

        /** The venue capacity. */
        val capacity: Int,

        /** The count of users who have a reservation or are on the waitlst of an event. */
        val reservedCount: Int,

        /** List of all users who have a reservation or who are on the waitlist for an event. */
        val reservedUsers: List<ReservedUser>
) {
    val waitlistCount: Int
        get() = max(0, reservedCount - capacity)
}
