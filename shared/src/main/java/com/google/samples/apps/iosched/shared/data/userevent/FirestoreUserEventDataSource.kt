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

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.samples.apps.iosched.shared.domain.sessions.UserEventsMessage
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.CANCEL
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.REQUEST
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus
import com.google.samples.apps.iosched.shared.firestore.entity.ReservationRequest
import com.google.samples.apps.iosched.shared.firestore.entity.ReservationRequestResult
import com.google.samples.apps.iosched.shared.firestore.entity.ReservationRequestResult.ReservationRequestStatus
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.toEpochMilli
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * The data source for user data stored in firestore. It observes user data and also updates
 * stars and reservations.
 */
class FirestoreUserEventDataSource @Inject constructor(
        val firestore: FirebaseFirestore
) : UserEventDataSource {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val EVENTS_COLLECTION = "events"
        private const val QUEUE_COLLECTION = "queue"
        private const val ID = "id"
        private const val START_TIME = "startTime"
        private const val END_TIME = "endTime"
        private const val IS_STARRED = "isStarred"
        private const val RESERVATION_RESULT_KEY = "reservationResult"
        private const val RESERVATION_RESULT_TIME_KEY = "timestamp"
        private const val RESERVATION_RESULT_RESULT_KEY = "requestResult"
        private const val RESERVATION_RESULT_REQ_ID_KEY = "requestId"

        private const val RESERVATION_REQUEST_KEY = "reservationRequest"

        private const val RESERVE_REQ_ACTION = "RESERVE_REQUESTED"
        private const val RESERVE_CANCEL_ACTION = "CANCEL_REQUESTED"

        private const val RESERVATION_REQUEST_ACTION_KEY = "action"
        private const val RESERVATION_REQUEST_REQUEST_ID_KEY = "requestId"
        private const val RESERVATION_REQUEST_TIMESTAMP_KEY = "timestamp"

        private const val RESERVATION_STATUS_KEY = "reservationStatus"

        private const val REQUEST_QUEUE_ACTION_KEY = "action"
        private const val REQUEST_QUEUE_SESSION_KEY = "sessionId"
        private const val REQUEST_QUEUE_REQUEST_ID_KEY = "requestId"
        private const val REQUEST_QUEUE_ACTION_RESERVE = "RESERVE"
        private const val REQUEST_QUEUE_ACTION_CANCEL = "CANCEL"

    }

    /**
     * Asynchronous method to get the user events.
     *
     * This method generates important messages to the user if a reservation is confirmed or
     * waitlisted.
     */
    //TODO: Add also RESERVE_DENIED_* and CANCEL_DENIED_* messages
    override fun getObservableUserEvents(userId: String): LiveData<UserEventsResult> {
        val result = MutableLiveData<UserEventsResult>()
        if (userId.isEmpty()) {
            result.postValue(UserEventsResult(emptyList()))
            return result
        }

        firestore.collection(USERS_COLLECTION)
                .document(userId)
                // TODO: Add a way to clear this listener
                .collection(EVENTS_COLLECTION).addSnapshotListener { snapshot, _ ->
                    snapshot ?: return@addSnapshotListener

                    Timber.d("Events changes detected")

                    // Generate important user messages, like new reservations, if any.
                    val userMessage = generateReservationChangeMsg(snapshot, result.value)

                    val userEventsResult = UserEventsResult(
                            userEvents = snapshot.documents.map { parseUserEvent(it) },
                            userEventsMessage = userMessage)
                    result.postValue(userEventsResult)
                }
        return result
    }

    override fun getObservableUserEvent(
            userId: String,
            eventId: String
    ): LiveData<UserEventResult> {
        val result = MutableLiveData<UserEventResult>()

        firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(EVENTS_COLLECTION)
                .document(eventId)
                .addSnapshotListener({ snapshot, _ ->
                    snapshot ?: return@addSnapshotListener

                    // Generate message if the reservation changed
                    val userMessage = generateReservationChangeMsgFromDocument(snapshot,
                            result.value)

                    val userEvent = if (snapshot.exists()) {
                        parseUserEvent(snapshot)
                    } else {
                        null
                    }

                    val userEventResult = UserEventResult(
                            userEvent = userEvent,
                            userEventsMessage = userMessage
                    )
                    result.postValue(userEventResult)
                })
        return result
    }

    /**
     * Go through all the changes and generate user messages in case there are reservation changes.
     */
    private fun generateReservationChangeMsg(
            snapshot: QuerySnapshot,
            oldValue: UserEventsResult?
    ): UserEventsMessage? {

        // If oldValue doesn't exist, it's the first run so don't generate messages.
        if (oldValue == null) return null

        var userMessage: UserEventsMessage? = null

        snapshot.documentChanges.forEach { change ->
            val changedId: String = change.document.data[ID] as String
            // Get the old state. If there's none, ignore.
            val oldState = oldValue.userEvents.firstOrNull { it.id == changedId }

            val newMessage = if (oldState != null) {
                getUserMessageFromChange(change.document, oldState)
            } else {
                null
            }
            // Reservation changes have priority
            if (newMessage == UserEventsMessage.CHANGES_IN_RESERVATIONS) {
                userMessage = newMessage
                return@forEach //TODO should this be a @loop, to emulate a break instead of a continue?
            }
            // Waitlist message has less priority
            if (newMessage == UserEventsMessage.CHANGES_IN_WAITLIST) {
                if (userMessage == null) {
                    userMessage = newMessage
                }
            }
        }
        return userMessage
    }


    /**
     * Look at changes in a [UserEvent] and generate a user messages in case there are reservation or
     * waitlist changes.
     */
    private fun generateReservationChangeMsgFromDocument(
            snapshot: DocumentSnapshot,
            oldEventResult: UserEventResult?
    ): UserEventsMessage? {

        if (oldEventResult?.userEvent == null) return null
        return getUserMessageFromChange(snapshot, oldEventResult.userEvent)
    }

    /**
     * Given a change in a document, generate a user message to indicate a change in reservations.
     */
    private fun getUserMessageFromChange(
            documentSnapshot: DocumentSnapshot,
            oldState: UserEvent
    ): UserEventsMessage? {

        val changedId: String = documentSnapshot.data[ID] as String

        // Get the new state
        val newState = parseUserEvent(documentSnapshot)

        // If the old data wasn't reserved and it's reserved now, there's a change.
        if (!oldState.isReserved() && newState.isReserved()) {
            Timber.d("Reservation change detected: $changedId")
            return UserEventsMessage.CHANGES_IN_RESERVATIONS
        }
        // If the user was waiting for a reservation and they're waitlisted, there's a change.
        if (oldState.isReservationPending() && newState.isWaitlisted()) {
            Timber.d("Waitlist change detected: $changedId")
            return UserEventsMessage.CHANGES_IN_WAITLIST
        }

        // User canceled reservation
        if (oldState.isReserved() && !newState.isReserved()) {
            Timber.d("Reservation cancellation detected: $changedId")
            return UserEventsMessage.RESERVATION_CANCELED
        }

        // User canceled waitlist
        if (oldState.isWaitlisted() && !newState.isReserved() && !newState.isWaitlisted()) {
            Timber.d("Reservation cancellation detected: $changedId")
            return UserEventsMessage.WAITLIST_CANCELED
        }

        // Errors: only show if it's a new one.
        if (newState.hasRequestResultError()
                && newState.isDifferentRequestResult(oldState.getReservationRequestResultId())) {

            // Reserve cut-off
            if (newState.isRequestResultErrorReserveDeniedCutoff()) {
                Timber.d("Reservation error cut-off: $changedId")
                return UserEventsMessage.RESERVATION_DENIED_CUTOFF
            }
            // Reserve clash
            if (newState.isRequestResultErrorReserveDeniedClash()) {
                Timber.d("Reservation error clash: $changedId")
                return UserEventsMessage.RESERVATION_DENIED_CLASH
            }
            // Reserve unknown
            if (newState.isRequestResultErrorReserveDeniedUnknown()) {
                Timber.d("Reservation unknown error: $changedId")
                return UserEventsMessage.RESERVATION_DENIED_UNKNOWN
            }
            // Cancel cut-off
            if (newState.isRequestResultErrorCancelDeniedCutoff()) {
                Timber.d("Cancellation error cut-off: $changedId")
                return UserEventsMessage.CANCELLATION_DENIED_CUTOFF
            }
            // Cancel unknown
            if (newState.isRequestResultErrorCancelDeniedUnknown()) {
                Timber.d("Cancellation unknown error: $changedId")
                return UserEventsMessage.CANCELLATION_DENIED_UNKNOWN
            }

        }
        return null
    }

    private fun parseUserEvent(snapshot: DocumentSnapshot): UserEvent {

        val reservationRequestResult: ReservationRequestResult? =
                generateReservationRequestResult(snapshot)

        val reservationRequest = parseReservationRequest(snapshot)

        val reservationStatus = (snapshot[RESERVATION_STATUS_KEY] as? String)?.let {
            UserEvent.ReservationStatus.getIfPresent(it)
        }

        return UserEvent(id = snapshot.id,
                startTime = snapshot[START_TIME] as Long,
                endTime = snapshot[END_TIME] as Long,
                reservationRequestResult = reservationRequestResult,
                reservationStatus = reservationStatus,
                isStarred = snapshot[IS_STARRED] as? Boolean ?: false,
                reservationRequest = reservationRequest
        )
    }

    private fun generateReservationRequestResult(
            snapshot: DocumentSnapshot
    ): ReservationRequestResult? {

        (snapshot[RESERVATION_RESULT_KEY] as? Map<*, *>)?.let { reservation ->
            val requestResult = (reservation[RESERVATION_RESULT_RESULT_KEY] as? String)
                    ?.let { ReservationRequestStatus.getIfPresent(it) }

            val requestId = (reservation[RESERVATION_RESULT_REQ_ID_KEY] as? String)

            val timestamp = reservation[RESERVATION_RESULT_TIME_KEY] as? Long ?: -1

            // Mandatory fields or fail:
            if (requestResult == null || requestId == null) {
                Timber.e("Error parsing reservation request result: some fields null")
                return null
            }

            return ReservationRequestResult(
                    requestResult = requestResult,
                    requestId = requestId,
                    timestamp = timestamp
            )
        }
        // If there's no reservation:
        return null
    }

    private fun parseReservationRequest(
            snapshot: DocumentSnapshot
    ): ReservationRequest? {

        (snapshot[RESERVATION_REQUEST_KEY] as? Map<*, *>)?.let { request ->
            val action = (request[RESERVATION_REQUEST_ACTION_KEY] as? String)?.let {
                ReservationRequest.ReservationRequestEntityAction.getIfPresent(it)
            }
            val requestId = (request[RESERVATION_REQUEST_REQUEST_ID_KEY] as? String)

            // Mandatory fields or fail:
            if (action == null || requestId == null) {
                Timber.e("Error parsing reservation request from Firestore")
                return null
            }

            return ReservationRequest(action, requestId)
        }
        // If there's no reservation request:
        return null
    }

    /** Firestore writes **/

    /**
     * Stars or unstars an event.
     *
     * @returns a result via a LiveData.
     */
    override fun starEvent(userId: String, userEvent: UserEvent):
            LiveData<Result<StarUpdatedStatus>> {

        val result = MutableLiveData<Result<StarUpdatedStatus>>()

        val data = mapOf(ID to userEvent.id,
                START_TIME to userEvent.startTime,
                END_TIME to userEvent.endTime,
                IS_STARRED to userEvent.isStarred)
        firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(EVENTS_COLLECTION)
                .document(userEvent.id).set(data, SetOptions.merge()).addOnCompleteListener({
                    if (it.isSuccessful) {
                        result.postValue(Result.Success(
                                if (userEvent.isStarred) StarUpdatedStatus.STARRED
                                else StarUpdatedStatus.UNSTARRED))
                    } else {
                        result.postValue(Result.Error(
                                it.exception ?: RuntimeException("Error updating star.")))
                    }
                })
        return result
    }

    /**
     * Requests a reservation for an event.
     *
     * This method makes two write operations at once.
     *
     * @return a LiveData indicating whether the request was successful (not whether the event
     * was reserved)
     */
    override fun requestReservation(
            userId: String,
            session: Session,
            action: ReservationRequestAction
    ): LiveData<Result<ReservationRequestAction>> {

        val result = MutableLiveData<Result<ReservationRequestAction>>()

        val logCancelOrReservation =
                if (action == CANCEL) "Cancel" else "Reservation"

        Timber.d("Requesting $logCancelOrReservation for session ${session.id}")

        // Get a new write batch. This is a lightweight transaction.
        val batch = firestore.batch()

        val newRandomRequestId = UUID.randomUUID().toString()

        // Write #1: Mark this session as reserved. This is for clients to track.
        val userSession = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(EVENTS_COLLECTION)
                .document(session.id)

        val reservationRequest = mapOf(
                RESERVATION_REQUEST_ACTION_KEY to getReservationRequestedEventAction(action),
                RESERVATION_REQUEST_REQUEST_ID_KEY to newRandomRequestId,
                RESERVATION_REQUEST_TIMESTAMP_KEY to FieldValue.serverTimestamp()
        )

        val userSessionData = mapOf(ID to session.id,
                START_TIME to session.startTime.toEpochMilli(),
                END_TIME to session.endTime.toEpochMilli(),
                RESERVATION_REQUEST_KEY to reservationRequest)

        batch.set(userSession, userSessionData, SetOptions.merge())

        // Write #2: Send a request to the server. The result will appear in the UserSession. A
        // success in this reservation only means that the request was accepted. Even offline, this
        // request will succeed.
        val newRequest = firestore
                .collection(QUEUE_COLLECTION)
                .document(userId)

        val queueReservationRequest = mapOf(
                REQUEST_QUEUE_ACTION_KEY to getReservationRequestedQueueAction(action),
                REQUEST_QUEUE_SESSION_KEY to session.id,
                REQUEST_QUEUE_REQUEST_ID_KEY to newRandomRequestId)

        batch.set(newRequest, queueReservationRequest)

        // Commit write batch

        batch.commit().addOnSuccessListener {
            Timber.d("$logCancelOrReservation request for session ${session.id} succeeded")
            result.postValue(Result.Success(action))
        }.addOnFailureListener {
            Timber.e(it, "$logCancelOrReservation request for session ${session.id} failed")
            result.postValue(Result.Error(it))
        }

        return result
    }

    private fun getReservationRequestedEventAction(action: ReservationRequestAction): String =
            when (action) {
                REQUEST -> RESERVE_REQ_ACTION
                CANCEL -> RESERVE_CANCEL_ACTION
            }

    private fun getReservationRequestedQueueAction(action: ReservationRequestAction) =
            when (action) {
                REQUEST -> REQUEST_QUEUE_ACTION_RESERVE
                CANCEL -> REQUEST_QUEUE_ACTION_CANCEL
            }
}
