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
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.samples.apps.iosched.shared.domain.internal.DefaultScheduler
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.CANCEL
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.REQUEST
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus
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
        /**
         * Firestore constants.
         */
        private const val USERS_COLLECTION = "users"
        private const val EVENTS_COLLECTION = "events"
        private const val QUEUE_COLLECTION = "queue"
        internal const val ID = "id"
        internal const val START_TIME = "startTime"
        internal const val END_TIME = "endTime"
        internal const val IS_STARRED = "isStarred"

        internal const val RESERVATION_REQUEST_KEY = "reservationRequest"

        private const val RESERVE_REQ_ACTION = "RESERVE_REQUESTED"
        private const val RESERVE_CANCEL_ACTION = "CANCEL_REQUESTED"

        internal const val RESERVATION_REQUEST_ACTION_KEY = "action"
        internal const val RESERVATION_REQUEST_REQUEST_ID_KEY = "requestId"
        private const val RESERVATION_REQUEST_TIMESTAMP_KEY = "timestamp"

        private const val REQUEST_QUEUE_ACTION_KEY = "action"
        private const val REQUEST_QUEUE_SESSION_KEY = "sessionId"
        private const val REQUEST_QUEUE_REQUEST_ID_KEY = "requestId"
        private const val REQUEST_QUEUE_ACTION_RESERVE = "RESERVE"
        private const val REQUEST_QUEUE_ACTION_CANCEL = "CANCEL"

        internal const val RESERVATION_RESULT_KEY = "reservationResult"
        internal const val RESERVATION_RESULT_TIME_KEY = "timestamp"
        internal const val RESERVATION_RESULT_RESULT_KEY = "requestResult"
        internal const val RESERVATION_RESULT_REQ_ID_KEY = "requestId"

        internal const val RESERVATION_STATUS_KEY = "reservationStatus"
    }

    // Null if the listener is not yet added
    private var eventsChangedListenerSubscription: ListenerRegistration? = null
    private var eventChangedListenerSubscription: ListenerRegistration? = null


    // Observable events
    private val resultEvents = MutableLiveData<UserEventsResult>()
    private val resultSingleEvent = MutableLiveData<UserEventResult>()

    /**
     * Asynchronous method to get the user events.
     *
     * This method generates important messages to the user if a reservation is confirmed or
     * waitlisted.
     */
    override fun getObservableUserEvents(userId: String): LiveData<UserEventsResult> {
        if (userId.isEmpty()) {
            resultEvents.postValue(UserEventsResult(emptyList()))
            return resultEvents
        }

        registerListenerForEvents(resultEvents, userId)
        return resultEvents
    }

    override fun getObservableUserEvent(
            userId: String,
            eventId: String
    ): LiveData<UserEventResult> {

        if (userId.isEmpty()) {
            resultSingleEvent.postValue(UserEventResult(userEvent = null))
            return resultSingleEvent
        }
        registerListenerForSingleEvent(resultSingleEvent, eventId, userId)
        return resultSingleEvent
    }

    private fun registerListenerForEvents(result: MutableLiveData<UserEventsResult>, userId: String) {
        val eventsListener: (QuerySnapshot?, FirebaseFirestoreException?) -> Unit =
                listener@ { snapshot, _ ->
                    snapshot ?: return@listener

                    DefaultScheduler.execute {
                        Timber.d("Events changes detected: ${snapshot.documentChanges.size}")

                        // Generate important user messages, like new reservations, if any.
                        val userMessage = generateReservationChangeMsg(snapshot, result.value)

                        val userEventsResult = UserEventsResult(
                                userEvents = snapshot.documents.map { parseUserEvent(it) },
                                userEventsMessage = userMessage)
                        result.postValue(userEventsResult)
                    }
                }

        val eventsCollection = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(EVENTS_COLLECTION)

        eventsChangedListenerSubscription?.remove() // Remove in case userId changes.
        eventsChangedListenerSubscription = eventsCollection.addSnapshotListener(eventsListener)
    }

    private fun registerListenerForSingleEvent(
            result: MutableLiveData<UserEventResult>,
            sessionId: String,
            userId: String) {

        val singleEventListener: (DocumentSnapshot?, FirebaseFirestoreException?) -> Unit =
                listener@ { snapshot, _ ->
                    snapshot ?: return@listener

                    DefaultScheduler.execute {
                        Timber.d("Event changes detected on session: $sessionId")

                        // If oldValue doesn't exist, it's the first run so don't generate messages.
                        val userMessage = result.value?.userEvent?.let { oldValue: UserEvent ->

                            // Generate message if the reservation changed
                            if (snapshot.exists()) getUserMessageFromChange(oldValue, snapshot, sessionId)
                            else null
                        }

                        val userEvent = if (snapshot.exists()) {
                            parseUserEvent(snapshot)
                        } else {
                            null
                        }

                        val userEventResult = UserEventResult(
                                userEvent = userEvent,
                                userEventMessage = userMessage
                        )
                        result.postValue(userEventResult)
                    }
                }

        val eventDocument = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(EVENTS_COLLECTION)
                .document(sessionId)

        eventChangedListenerSubscription?.remove() // Remove in case userId changes.
        eventChangedListenerSubscription = eventDocument.addSnapshotListener(singleEventListener)
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
