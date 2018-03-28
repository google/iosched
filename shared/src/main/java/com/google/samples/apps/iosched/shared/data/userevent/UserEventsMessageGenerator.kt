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

import android.support.annotation.VisibleForTesting
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.QuerySnapshot
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent
import timber.log.Timber


/**
 * Go through all the changes and generate user messages in case there are reservation changes.
 */
fun generateReservationChangeMsg(
        snapshot: QuerySnapshot,
        oldValue: UserEventsResult?
): UserEventMessage? {

    // If oldValue doesn't exist, it's the first run so don't generate messages.
    if (oldValue == null) return null

    var userMessage: UserEventMessage? = null

    snapshot.documentChanges.forEach { change ->

        val changedId: String = change.document.data[FirestoreUserEventDataSource.ID] as String

        val eventOldValue = oldValue.userEvents.firstOrNull { it.id == changedId } ?: return null

        val newMessage = getUserMessageFromChange(eventOldValue, change.document, changedId)

        // If there are multiple messages, show just one according to order in enum.
        if (newMessage != null) {
            val userMessageSnapshot = userMessage
            // Order in enum is definition order
            if (userMessageSnapshot == null || newMessage < userMessageSnapshot) {
                userMessage = newMessage
            }
        }
    }
    return userMessage
}

/**
 * Given a change in a document, generate a user message to indicate a change in reservations.
 */
fun getUserMessageFromChange(
        oldValue: UserEvent,
        documentSnapshot: DocumentSnapshot,
        changeId: String
): UserEventMessage? {

    // Get the new state
    val newState = parseUserEvent(documentSnapshot)

    return compareOldAndNewUserEvents(oldValue, newState, changeId)
}

@VisibleForTesting
fun compareOldAndNewUserEvents(
        oldState: UserEvent,
        newState: UserEvent,
        changedId: String
): UserEventMessage? {

    // If the old data wasn't reserved and it's reserved now, there's a change.
    if (!oldState.isReserved() && newState.isReserved()) {
        Timber.d("Request change detected: $changedId")
        return if (newState.isLastRequestResultBySwap()) {
            UserEventMessage.RESERVATIONS_REPLACED
        } else {
            UserEventMessage.CHANGES_IN_RESERVATIONS
        }

    }
    // If the user was waiting for a reservation and they're waitlisted, there's a change.
    if (oldState.isReservationPending() && newState.isWaitlisted()) {
        Timber.d("Waitlist change detected: $changedId")
        return UserEventMessage.CHANGES_IN_WAITLIST
    }

    // User canceled reservation
    if (oldState.isReserved() && !newState.isReserved()) {
        Timber.d("Reservation cancellation detected: $changedId")
        return UserEventMessage.RESERVATION_CANCELED
    }

    // User canceled waitlist
    if (oldState.isWaitlisted() && !newState.isReserved() && !newState.isWaitlisted()) {
        Timber.d("Reservation cancellation detected: $changedId")
        return UserEventMessage.WAITLIST_CANCELED
    }

    // Errors: only show if it's a new one.
    if (newState.hasRequestResultError()
            && newState.isDifferentRequestResult(oldState.getReservationRequestResultId())) {

        // Reserve cut-off
        if (newState.isRequestResultErrorReserveDeniedCutoff()) {
            Timber.d("Reservation error cut-off: $changedId")
            return UserEventMessage.RESERVATION_DENIED_CUTOFF
        }
        // Reserve clash
        if (newState.isRequestResultErrorReserveDeniedClash()) {
            Timber.d("Reservation error clash: $changedId")
            return UserEventMessage.RESERVATION_DENIED_CLASH
        }
        // Reserve unknown
        if (newState.isRequestResultErrorReserveDeniedUnknown()) {
            Timber.d("Reservation unknown error: $changedId")
            return UserEventMessage.RESERVATION_DENIED_UNKNOWN
        }
        // Cancel cut-off
        if (newState.isRequestResultErrorCancelDeniedCutoff()) {
            Timber.d("Cancellation error cut-off: $changedId")
            return UserEventMessage.CANCELLATION_DENIED_CUTOFF
        }
        // Cancel unknown
        if (newState.isRequestResultErrorCancelDeniedUnknown()) {
            Timber.d("Cancellation unknown error: $changedId")
            return UserEventMessage.CANCELLATION_DENIED_UNKNOWN
        }
    }
    return null
}

/**
 * Enum of messages notified to the end user.
 * Need to be ordered by importance
 * (e.g. CHANGE_IN_RESERVATIONS is more important than CHANGES_IN_WAITLIST)
 */
enum class UserEventMessage {
    CHANGES_IN_RESERVATIONS,
    RESERVATIONS_REPLACED,
    CHANGES_IN_WAITLIST,
    RESERVATION_CANCELED,
    WAITLIST_CANCELED,
    RESERVATION_DENIED_CUTOFF,
    RESERVATION_DENIED_CLASH,
    RESERVATION_DENIED_UNKNOWN,
    CANCELLATION_DENIED_CUTOFF,
    CANCELLATION_DENIED_UNKNOWN
}

