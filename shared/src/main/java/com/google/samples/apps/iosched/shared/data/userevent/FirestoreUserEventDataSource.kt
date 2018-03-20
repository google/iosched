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
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QueryListenOptions
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.samples.apps.iosched.shared.domain.sessions.UserEventsMessage
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus
import com.google.samples.apps.iosched.shared.firestore.entity.LastReservationRequested
import com.google.samples.apps.iosched.shared.firestore.entity.ReservationRequestResult
import com.google.samples.apps.iosched.shared.firestore.entity.ReservationRequestResult.ReservationRequestStatus
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.toEpochMilli
import timber.log.Timber
import java.util.concurrent.TimeUnit
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
        private const val REQUESTS_COLLECTION = "requests"
        private const val ID = "id"
        private const val START_TIME = "startTime"
        private const val END_TIME = "endTime"
        private const val IS_STARRED = "isStarred"
        private const val RESERVATION = "reservation"
        private const val RESERVATION_STATUS = "status"
        private const val RESERVATION_TIME = "timestamp"
        private const val RESERVATION_REQUESTED_KEY = "reservationRequested"
        private const val RESERVE_REQ_ACTION = "RESERVE_REQUESTED"
        private const val RESERVE_CANCEL_ACTION = "CANCEL_REQUESTED"
        private const val REQUEST_ACTION_KEY = "action"
        private const val REQUEST_QUEUE_ACTION_RESERVE = "reserve"
        private const val REQUEST_QUEUE_ACTION_CANCEL = "return"
        private const val REQUEST_SESSION_KEY = "session_id"

    }

    /**
     * Synchronous method to get the user events.
     */
    //TODO: Not used
    override fun getUserEvents(userId: String): List<UserEvent> {
        if (userId.isEmpty()) {
            return emptyList()
        }
        val task = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(EVENTS_COLLECTION).get()
        val snapshot = Tasks.await(task, 20, TimeUnit.SECONDS) // TODO refactor if ever used
        return snapshot.documents.map {
            UserEvent(id = it.id,
                    startTime = it[START_TIME] as Long,
                    endTime = it[END_TIME] as Long,
                    isStarred = it[IS_STARRED] as Boolean)
        }
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
            result.postValue(UserEventsResult(true, emptyList()))
            return result
        }

        // Need to include this option to let the Metadata#hasPendingWrite() is called
        // When there is locally modified data, which isn't synced to the server
        val options = QueryListenOptions().includeDocumentMetadataChanges()
        firestore.collection(USERS_COLLECTION)
                .document(userId)
                // TODO: Add a way to clear this listener
                .collection(EVENTS_COLLECTION).addSnapshotListener(options, { snapshot, _ ->
                    snapshot ?: return@addSnapshotListener

                    // Generate important user messages, like new reservations, if any.
                    val userMessage = generateReservationChangeMsg(snapshot, result.value)

                    // Add a dirty flag to the user event if it's not synced with the server.
                    if (snapshot.metadata.hasPendingWrites()) {
                        // This means locally modified data isn't synced with the server
                        val changedIds =
                                snapshot.documentChanges.map { it.document.data[ID] }.toSet()

                        val userEventsResult = UserEventsResult(
                                allDataSynced = false,
                                userEvents = snapshot.documents.map {
                                        parseUserEvent(it).apply {
                                            hasPendingWrite = it[ID] in changedIds
                                        }

                                    },
                                userEventsMessage = UserEventsMessage.DATA_NOT_SYNCED
                        )

                        result.postValue(userEventsResult)

                    } else { // Has no pending writes
                        val userEventsResult = UserEventsResult(
                                allDataSynced = true,
                                userEvents = snapshot.documents.map {
                                    parseUserEvent(it).apply {
                                        hasPendingWrite = false
                                    }
                                },
                                userEventsMessage = userMessage)
                        result.postValue(userEventsResult)
                    }
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
            val newMessage = getUserMessageFromChange(change, oldValue)
            // Reservation changes have priority
            if (newMessage == UserEventsMessage.CHANGES_IN_RESERVATIONS) {
                userMessage = newMessage
                return@forEach
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
     * Given a change in a document, generate a user message to indicate a change in reservations.
     */
    private fun getUserMessageFromChange(
            change: DocumentChange,
            result: UserEventsResult
    ): UserEventsMessage? {

        val changedId: String = change.document.data[ID] as String
        // Get the old value
        val oldData = result.userEvents.firstOrNull { it.id == changedId }
        // If this is new data, ignore
        if (oldData == null) return null
        val newReservationState = generateReservationRequestResult(change.document)
        val newReservationRequested = parseReservationRequested(change.document)
        if (oldData.isReservationPending()
                && newReservationRequested == null
                && newReservationState?.status == ReservationRequestStatus.RESERVE_SUCCEEDED) {
            Timber.d("Reservation change detected: ${changedId}")
            return UserEventsMessage.CHANGES_IN_RESERVATIONS
        }
        // The session is waiting for a change
        if (oldData.isReservationPending()
                && newReservationRequested == null
                && newReservationState?.status == ReservationRequestStatus.RESERVE_WAITLISTED) {
            Timber.d("Waitlist change detected: ${changedId}")
            return UserEventsMessage.CHANGES_IN_WAITLIST
        }
        return null
    }

    private fun parseUserEvent(it: DocumentSnapshot): UserEvent {

        val reservationRequestResult: ReservationRequestResult? =
                generateReservationRequestResult(it)

        return UserEvent(id = it.id,
                startTime = it[START_TIME] as Long,
                endTime = it[END_TIME] as Long,
                reservation = reservationRequestResult,
                isStarred = it[IS_STARRED] as? Boolean ?: false,
                reservationRequested = parseReservationRequested(it)
        )
    }

    private fun generateReservationRequestResult(it: DocumentSnapshot): ReservationRequestResult? {
        return (it[RESERVATION] as? Map<String?, Any?>) // TODO: unsafe
                ?.let { reservation: Map<String?, Any?> ->
                    ReservationRequestResult(
                            status = (reservation[RESERVATION_STATUS] as? String)
                                    ?.let { ReservationRequestStatus.getIfPresent(it) },
                            timestamp = reservation[RESERVATION_TIME] as? Long
                                    ?: -1
                    )
                }
    }

    private fun parseReservationRequested(it: DocumentSnapshot) =
            when(it[RESERVATION_REQUESTED_KEY] as? String) {
                RESERVE_REQ_ACTION -> LastReservationRequested.RESERVATION
                RESERVE_CANCEL_ACTION -> LastReservationRequested.CANCEL
                else -> null
            }

    /** Firestore writes **/

    /**
     * Stars or unstars an event.
     *
     * @returns a result via a LiveData.
     */
    override fun updateStarred(userId: String, session: Session, isStarred: Boolean):
            LiveData<Result<StarUpdatedStatus>> {

        val result = MutableLiveData<Result<StarUpdatedStatus>>()

        val data = mapOf(ID to session.id,
                START_TIME to session.startTime.toEpochMilli(),
                END_TIME to session.endTime.toEpochMilli(),
                IS_STARRED to isStarred)
        firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(EVENTS_COLLECTION)
                .document(session.id).set(data, SetOptions.merge()).addOnCompleteListener({
                    if (it.isSuccessful) {
                        result.postValue(Result.Success(
                                if (isStarred) StarUpdatedStatus.STARRED
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
    ): LiveData<Result<LastReservationRequested>> {

        val result = MutableLiveData<Result<LastReservationRequested>>()

        val logCancelOrReservation =
                if (action == ReservationRequestAction.CANCEL) "Cancel" else "Reservation"

        Timber.d("Requesting $logCancelOrReservation for session ${session.id}")

        // Get a new write batch. This is a lightweight transaction.
        val batch = firestore.batch()

        // Write #1: Mark this session as reserved. This is for clients to track.
        val userSession = firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(EVENTS_COLLECTION)
                .document(session.id)

        val userSessionData = mapOf(ID to session.id,
                START_TIME to session.startTime.toEpochMilli(),
                END_TIME to session.endTime.toEpochMilli(),
                RESERVATION_REQUESTED_KEY to getReservationRequestedEventAction(action))

        batch.set(userSession, userSessionData, SetOptions.merge())

        // Write #2: Send a request to the server. The result will appear in the UserSession. A
        // success in this reservation only means that the request was accepted. Even offline, this
        // request will succeed.
        val newRequest = firestore
                .collection(QUEUE_COLLECTION)
                .document(userId)

        val reservationRequest = HashMap<String, Any>()
        reservationRequest[REQUEST_ACTION_KEY] = getReservationRequestedQueueAction(action)
        reservationRequest[REQUEST_SESSION_KEY] = session.id

        batch.set(newRequest, reservationRequest)

        // Commit write batch

        batch.commit().addOnSuccessListener {
            Timber.d("$logCancelOrReservation request for session ${session.id} succeeded")
            val resultMessage = if (action == ReservationRequestAction.REQUEST)
                LastReservationRequested.RESERVATION else LastReservationRequested.CANCEL

            result.postValue(Result.Success(resultMessage))
        }.addOnFailureListener {
            Timber.e(it, "$logCancelOrReservation request for session ${session.id} failed")
            result.postValue(Result.Error(it))
        }

        return result
    }

    private fun getReservationRequestedEventAction(action: ReservationRequestAction) =
            when (action) {
                ReservationRequestAction.REQUEST -> RESERVE_REQ_ACTION
                ReservationRequestAction.CANCEL -> RESERVE_CANCEL_ACTION
            }

    private fun getReservationRequestedQueueAction(action: ReservationRequestAction) =
            when (action) {
                ReservationRequestAction.REQUEST -> REQUEST_QUEUE_ACTION_RESERVE
                ReservationRequestAction.CANCEL -> REQUEST_QUEUE_ACTION_CANCEL
            }
}
