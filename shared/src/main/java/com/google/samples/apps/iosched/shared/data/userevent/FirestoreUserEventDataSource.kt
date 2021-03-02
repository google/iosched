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

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.userdata.UserEvent
import com.google.samples.apps.iosched.shared.data.document2020
import com.google.samples.apps.iosched.shared.di.IoDispatcher
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.CancelAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.RequestAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.SwapAction
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus
import com.google.samples.apps.iosched.shared.domain.users.SwapRequestAction
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Error
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.shared.util.tryOffer
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * The data source for user data stored in firestore. It observes user data and also updates
 * stars and reservations.
 */
@ExperimentalCoroutinesApi
class FirestoreUserEventDataSource @Inject constructor(
    val firestore: FirebaseFirestore,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : UserEventDataSource {

    companion object {
        /**
         * Firestore constants.
         */
        private const val USERS_COLLECTION = "users"
        private const val EVENTS_COLLECTION = "events"
        private const val QUEUE_COLLECTION = "queue"
        internal const val ID = "id"
        internal const val IS_STARRED = "isStarred"
        internal const val REVIEWED = "reviewed"

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
        private const val REQUEST_QUEUE_ACTION_SWAP = "SWAP"
        private const val SWAP_QUEUE_RESERVE_SESSION_ID_KEY = "reserveSessionId"
        private const val SWAP_QUEUE_CANCEL_SESSION_ID_KEY = "cancelSessionId"

        internal const val RESERVATION_RESULT_KEY = "reservationResult"
        internal const val RESERVATION_RESULT_TIME_KEY = "timestamp"
        internal const val RESERVATION_RESULT_RESULT_KEY = "requestResult"
        internal const val RESERVATION_RESULT_REQ_ID_KEY = "requestId"

        internal const val RESERVATION_STATUS_KEY = "reservationStatus"
    }

    /**
     * Asynchronous method to get the user events.
     *
     * This method generates important messages to the user if a reservation is confirmed or
     * waitlisted.
     */
    override fun getObservableUserEvents(userId: String): Flow<UserEventsResult> {
        if (userId.isEmpty()) {
            return flow { emit(UserEventsResult(emptyList())) }
        } else {
            return (
                callbackFlow<UserEventsResult> {
                    val eventsCollection = firestore
                        .document2020()
                        .collection(USERS_COLLECTION)
                        .document(userId)
                        .collection(EVENTS_COLLECTION)

                    var currentValue: UserEventsResult? = null

                    // TODO: Flow refactor check addSnapshotListener documentation (null value?)
                    val subscription = eventsCollection.addSnapshotListener { snapshot, _ ->
                        if (snapshot == null) {
                            return@addSnapshotListener
                        }

                        Timber.d("Events changes detected: ${snapshot.documentChanges.size}")

                        // Generate important user messages, like new reservations, if any.
                        val userMessage =
                            generateReservationChangeMsg(snapshot, currentValue)
                        val userEventsResult = UserEventsResult(
                            userEvents = snapshot.documents.map { parseUserEvent(it) },
                            userEventsMessage = userMessage
                        )
                        currentValue = userEventsResult
                        tryOffer(userEventsResult)
                    }

                    // The callback inside awaitClose will be executed when the flow is
                    // either closed or cancelled
                    awaitClose { subscription.remove() }
                }
                )
                .flowOn(Dispatchers.Main)
        }
    }

    override fun getObservableUserEvent(
        userId: String,
        eventId: SessionId
    ): Flow<UserEventResult> {
        return if (userId.isEmpty()) {
            flow {
                emit(UserEventResult(userEvent = null))
            }
        } else {
            (
                callbackFlow<UserEventResult> {
                    val eventDocument = firestore
                        .document2020()
                        .collection(USERS_COLLECTION)
                        .document(userId)
                        .collection(EVENTS_COLLECTION)
                        .document(eventId)
                    var currentValue: UserEventResult? = null
                    val subscription = eventDocument.addSnapshotListener { snapshot, _ ->
                        if (snapshot == null) {
                            return@addSnapshotListener
                        }
                        Timber.d("Event changes detected on session: $eventId")
                        val userEvent = if (snapshot.exists()) {
                            parseUserEvent(snapshot)
                        } else {
                            UserEvent(id = eventId)
                        }
                        val userMessage = currentValue?.userEvent?.let {
                            getUserMessageFromChange(it, snapshot, eventId)
                        }

                        val userEventResult = UserEventResult(
                            userEvent = userEvent,
                            userEventMessage = userMessage
                        )
                        currentValue = userEventResult
                        tryOffer(userEventResult)
                    }
                    // The callback inside awaitClose will be executed when the flow is
                    // either closed or cancelled
                    awaitClose { subscription.remove() }
                }
                )
                .flowOn(ioDispatcher)
        }
    }

    override fun getUserEvents(userId: String): List<UserEvent> {
        if (userId.isEmpty()) {
            return emptyList()
        }

        val task = firestore
            .document2020()
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(EVENTS_COLLECTION).get()
        val snapshot = Tasks.await(task, 20, TimeUnit.SECONDS)
        return snapshot.documents.map { parseUserEvent(it) }
    }

    override fun getUserEvent(userId: String, eventId: SessionId): UserEvent? {
        if (userId.isEmpty()) {
            return null
        }

        val task = firestore
            .document2020()
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(EVENTS_COLLECTION)
            .document(eventId).get()
        val snapshot = Tasks.await(task, 20, TimeUnit.SECONDS)
        return parseUserEvent(snapshot)
    }

    override fun clearSingleEventSubscriptions() {
        Timber.d("Firestore Event data source: Clearing subscriptions")
    }

    /** Firestore writes **/

    /**
     * Stars or unstars an event.
     *
     * @returns a result via as a [Result].
     */
    override suspend fun starEvent(
        userId: String,
        userEvent: UserEvent
    ): Result<StarUpdatedStatus> = withContext(ioDispatcher) {
        // The suspendCancellableCoroutine method suspends a coroutine manually. With the
        // continuation object you receive in the lambda, you can resume the coroutine
        // after the work is done.
        suspendCancellableCoroutine<Result<StarUpdatedStatus>> { continuation ->

            val data = mapOf(
                ID to userEvent.id,
                IS_STARRED to userEvent.isStarred
            )

            firestore
                .document2020()
                .collection(USERS_COLLECTION)
                .document(userId)
                .collection(EVENTS_COLLECTION)
                .document(userEvent.id).set(data, SetOptions.merge())
                .addOnCompleteListener {
                    if (it.isSuccessful) {
                        continuation.resume(
                            Success(
                                if (userEvent.isStarred) StarUpdatedStatus.STARRED
                                else StarUpdatedStatus.UNSTARRED
                            )
                        )
                    } else {
                        continuation.resume(
                            Error(
                                it.exception ?: RuntimeException("Error updating star.")
                            )
                        )
                    }
                }.addOnFailureListener {
                    continuation.resumeWithException(it)
                }
        }
    }

    override suspend fun recordFeedbackSent(
        userId: String,
        userEvent: UserEvent
    ): Result<Unit> {
        val data = mapOf(
            ID to userEvent.id,
            "reviewed" to true
        )

        return suspendCancellableCoroutine { continuation ->

            firestore
                .document2020()
                .collection(USERS_COLLECTION)
                .document(userId)
                .collection(EVENTS_COLLECTION)
                .document(userEvent.id).set(data, SetOptions.merge())
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        continuation.resume(Success(Unit))
                    } else {
                        continuation.resume(
                            Error(
                                task.exception ?: RuntimeException(
                                    "Error updating feedback."
                                )
                            )
                        )
                    }
                }
        }
    }

    /**
     * Requests a reservation for an event.
     *
     * This method makes two write operations at once.
     *
     * @return a LiveData indicating whether the request was successful (not whether the event
     * was reserved)
     */
    override suspend fun requestReservation(
        userId: String,
        session: Session,
        action: ReservationRequestAction
    ): Result<ReservationRequestAction> {
        val logCancelOrReservation = if (action is CancelAction) "Cancel" else "Request"

        Timber.d("Requesting $logCancelOrReservation for session ${session.id}")

        // Get a new write batch. This is a lightweight transaction.
        val batch = firestore.batch()

        val newRandomRequestId = UUID.randomUUID().toString()

        // Write #1: Mark this session as reserved. This is for clients to track.
        val userSession = firestore
            .document2020()
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(EVENTS_COLLECTION)
            .document(session.id)

        val reservationRequest = mapOf(
            RESERVATION_REQUEST_ACTION_KEY to getReservationRequestedEventAction(action),
            RESERVATION_REQUEST_REQUEST_ID_KEY to newRandomRequestId,
            RESERVATION_REQUEST_TIMESTAMP_KEY to FieldValue.serverTimestamp()
        )

        val userSessionData = mapOf(
            ID to session.id,
            RESERVATION_REQUEST_KEY to reservationRequest
        )

        batch.set(userSession, userSessionData, SetOptions.merge())

        // Write #2: Send a request to the server. The result will appear in the UserSession. A
        // success in this reservation only means that the request was accepted. Even offline, this
        // request will succeed.
        val newRequest = firestore
            .document2020()
            .collection(QUEUE_COLLECTION)
            .document(userId)

        val queueReservationRequest = mapOf(
            REQUEST_QUEUE_ACTION_KEY to getReservationRequestedQueueAction(action),
            REQUEST_QUEUE_SESSION_KEY to session.id,
            REQUEST_QUEUE_REQUEST_ID_KEY to newRandomRequestId
        )

        batch.set(newRequest, queueReservationRequest)

        return withContext(ioDispatcher) {
            suspendCancellableCoroutine<Result<ReservationRequestAction>> { continuation ->
                // Commit write batch
                batch.commit().addOnSuccessListener {
                    Timber.d("$logCancelOrReservation request for session ${session.id} succeeded")
                    continuation.resume(Success(action))
                }.addOnFailureListener {
                    Timber.e(it, "$logCancelOrReservation request for session ${session.id} failed")
                    continuation.resume(Error(it))
                }
            }
        }
    }

    override suspend fun swapReservation(
        userId: String,
        fromSession: Session,
        toSession: Session
    ): Result<SwapRequestAction> {
        Timber.d("Swapping reservations from: ${fromSession.id} to: ${toSession.id}")

        // Get a new write batch. This is a lightweight transaction.
        val batch = firestore.batch()

        val newRandomRequestId = UUID.randomUUID().toString()
        val serverTimestamp = FieldValue.serverTimestamp()

        // Write #1: Mark the toSession as reserved. This is for clients to track.
        val toUserSession = firestore
            .document2020()
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(EVENTS_COLLECTION)
            .document(toSession.id)
        val toSwapRequest = mapOf(
            RESERVATION_REQUEST_ACTION_KEY to RESERVE_REQ_ACTION,
            RESERVATION_REQUEST_REQUEST_ID_KEY to newRandomRequestId,
            RESERVATION_REQUEST_TIMESTAMP_KEY to serverTimestamp
        )
        val userSessionData = mapOf(
            ID to toSession.id,
            RESERVATION_REQUEST_KEY to toSwapRequest
        )
        batch.set(toUserSession, userSessionData, SetOptions.merge())

        // Write #2: Mark the fromSession as canceled. This is for clients to track.
        val fromUserSession = firestore
            .document2020()
            .collection(USERS_COLLECTION)
            .document(userId)
            .collection(EVENTS_COLLECTION)
            .document(fromSession.id)
        val fromSwapRequest = mapOf(
            RESERVATION_REQUEST_ACTION_KEY to RESERVE_CANCEL_ACTION,
            RESERVATION_REQUEST_REQUEST_ID_KEY to newRandomRequestId,
            RESERVATION_REQUEST_TIMESTAMP_KEY to serverTimestamp
        )
        val fromUserSessionData = mapOf(
            ID to fromSession.id,
            RESERVATION_REQUEST_KEY to fromSwapRequest
        )
        batch.set(fromUserSession, fromUserSessionData, SetOptions.merge())

        // Write #3: Send a request to the server. The result will appear in the both UserSessions
        // (from and to). success in this reservation only means that the request was accepted.
        // Even offline, this request will succeed.
        val newRequest = firestore
            .document2020()
            .collection(QUEUE_COLLECTION)
            .document(userId)

        val queueSwapRequest = mapOf(
            REQUEST_QUEUE_ACTION_KEY to REQUEST_QUEUE_ACTION_SWAP,
            SWAP_QUEUE_RESERVE_SESSION_ID_KEY to toSession.id,
            SWAP_QUEUE_CANCEL_SESSION_ID_KEY to fromSession.id,
            REQUEST_QUEUE_REQUEST_ID_KEY to newRandomRequestId
        )

        batch.set(newRequest, queueSwapRequest)

        return withContext(ioDispatcher) {
            suspendCancellableCoroutine<Result<SwapRequestAction>> { continuation ->
                // Commit write batch
                batch.commit().addOnSuccessListener {
                    Timber.d(
                        """Queueing the swap request from:
                            |${fromSession.id} to: ${toSession.id} succeeded""".trimMargin()
                    )
                    continuation.resume(Success(SwapRequestAction()))
                }.addOnFailureListener {
                    Timber.d(
                        """Queueing the swap request from:
                            |${fromSession.id} to: ${toSession.id} failed""".trimMargin()
                    )
                    continuation.resume(Error(it))
                }
            }
        }
    }

    private fun getReservationRequestedEventAction(action: ReservationRequestAction): String =
        when (action) {
            is RequestAction -> RESERVE_REQ_ACTION
            is CancelAction -> RESERVE_CANCEL_ACTION
            // This should not happen because there is a dedicated method for the swap request
            is SwapAction -> throw IllegalStateException()
        }

    private fun getReservationRequestedQueueAction(action: ReservationRequestAction) =
        when (action) {
            is RequestAction -> REQUEST_QUEUE_ACTION_RESERVE
            is CancelAction -> REQUEST_QUEUE_ACTION_CANCEL
            // This should not happen because there is a dedicated method for the swap request
            is SwapAction -> throw IllegalStateException()
        }
}
