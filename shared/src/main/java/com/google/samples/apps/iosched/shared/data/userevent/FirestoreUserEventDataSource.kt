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

import androidx.annotation.MainThread
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.userdata.UserEvent
import com.google.samples.apps.iosched.shared.domain.users.StarUpdatedStatus
import com.google.samples.apps.iosched.shared.result.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

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
        internal const val ID = "id"
        internal const val IS_STARRED = "isStarred"
    }

    /**
     * Asynchronous method to get the user events.
     *
     * This method generates important messages to the user if a reservation is confirmed or
     * waitlisted.
     */
    override fun getObservableUserEvents(userId: String): Flow<UserEventsResult> {
        return if (userId.isEmpty()) {
            flow { emit(UserEventsResult(emptyList())) }
        } else {
            (channelFlow {
                val eventsCollection = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(EVENTS_COLLECTION)

                // TODO: Flow refactor check addSnapshotListener documentation (null value?)
                val subscription = eventsCollection.addSnapshotListener { snapshot, _ ->
                    if (snapshot == null) {
                        return@addSnapshotListener
                    }

                    Timber.d("Events changes detected: ${snapshot.documentChanges.size}")

                    // Generate important user messages, like new reservations, if any.
                    val userEventsResult = UserEventsResult(
                        userEvents = snapshot.documents.map { parseUserEvent(it) }
                    )
                    offer(userEventsResult)
                }

                // The callback inside awaitClose will be executed when the channel is
                // either closed or cancelled
                awaitClose { subscription.remove() }
            }).flowOn(Dispatchers.Main)
        }
    }

    override fun getObservableUserEvent(userId: String, eventId: SessionId): Flow<UserEventResult> {
        return if (userId.isEmpty()) {
            flow {
                emit(UserEventResult(userEvent = null))
            }
        } else {
            (channelFlow<UserEventResult> {

                val eventDocument = firestore.collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(EVENTS_COLLECTION)
                    .document(eventId)

                val subscription = eventDocument.addSnapshotListener { snapshot, _ ->
                    if (snapshot == null) {
                        return@addSnapshotListener
                    }

                    Timber.d("Event changes detected on session: $eventId")
                    val userEvent = if (snapshot.exists()) {
                        parseUserEvent(snapshot)
                    } else {
                        null
                    }

                    val userEventResult = UserEventResult(
                        userEvent = userEvent
                    )
                    channel.offer(userEventResult)
                }

                // The callback inside awaitClose will be executed when the channel is
                // either closed or cancelled
                awaitClose { subscription.remove() }
            }).flowOn(Dispatchers.Main)
        }
    }

    @MainThread // Firestore limitation b/116784117
    override fun getUserEvents(userId: String): List<UserEvent> {
        if (userId.isEmpty()) {
            return emptyList()
        }

        val task = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(EVENTS_COLLECTION).get()
        val snapshot = Tasks.await(task, 20, TimeUnit.SECONDS)
        return snapshot.documents.map { parseUserEvent(it) }
    }

    override fun getUserEvent(userId: String, eventId: SessionId): UserEvent? {
        if (userId.isEmpty()) {
            return null
        }

        val task = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(EVENTS_COLLECTION)
            .document(eventId).get()
        val snapshot = Tasks.await(task, 20, TimeUnit.SECONDS)
        return parseUserEvent(snapshot)
    }

    /**
     * Stars or unstars an event. This has to be run on the Main thread because of Firestore
     * limitations.
     */
    override suspend fun starEvent(
        userId: String,
        userEvent: UserEvent
    ): Result<StarUpdatedStatus> = withContext(Dispatchers.Main) {
        // The suspendCancellableCoroutine method suspends a coroutine manually. With the
        // continuation object you receive in the lambda, you can resume the coroutine
        // after the work is done.
        suspendCancellableCoroutine<Result<StarUpdatedStatus>> { continuation ->

            val data = mapOf(
                ID to userEvent.id,
                IS_STARRED to userEvent.isStarred
            )

            firestore.collection(USERS_COLLECTION)
                .document(userId)
                .collection(EVENTS_COLLECTION)
                .document(userEvent.id).set(data, SetOptions.merge())
                .addOnCompleteListener {
                    if (!continuation.isActive) return@addOnCompleteListener
                    if (it.isSuccessful) {
                        continuation.resume(
                            Result.Success(
                                if (userEvent.isStarred) StarUpdatedStatus.STARRED
                                else StarUpdatedStatus.UNSTARRED
                            )
                        )
                    } else {
                        continuation.resume(
                            Result.Error(
                                it.exception ?: RuntimeException("Error updating star.")
                            )
                        )
                    }
                }.addOnFailureListener {
                    if (!continuation.isActive) return@addOnFailureListener
                    continuation.resumeWithException(it)
                }
        }
    }
}
