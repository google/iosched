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

package com.google.samples.apps.adssched.shared.data.userevent

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.google.samples.apps.adssched.model.SessionId
import com.google.samples.apps.adssched.model.userdata.UserEvent
import com.google.samples.apps.adssched.shared.domain.internal.DefaultScheduler
import com.google.samples.apps.adssched.shared.domain.users.StarUpdatedStatus
import com.google.samples.apps.adssched.shared.result.Result
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
        /**
         * Firestore constants.
         */
        private const val USERS_COLLECTION = "users"
        private const val EVENTS_COLLECTION = "events"
        internal const val ID = "id"
        internal const val START_TIME = "startTime"
        internal const val END_TIME = "endTime"
        internal const val IS_STARRED = "isStarred"
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
        eventId: SessionId
    ): LiveData<UserEventResult> {
        if (userId.isEmpty()) {
            resultSingleEvent.postValue(UserEventResult(userEvent = null))
            return resultSingleEvent
        }
        registerListenerForSingleEvent(eventId, userId)
        return resultSingleEvent
    }

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

    private fun registerListenerForEvents(
        result: MutableLiveData<UserEventsResult>,
        userId: String
    ) {
        val eventsListener: (QuerySnapshot?, FirebaseFirestoreException?) -> Unit =
            listener@{ snapshot, _ ->
                snapshot ?: return@listener

                DefaultScheduler.execute {
                    Timber.d("Events changes detected: ${snapshot.documentChanges.size}")

                    // Generate important user messages, like new reservations, if any.
                    val userEventsResult = UserEventsResult(
                        userEvents = snapshot.documents.map { parseUserEvent(it) }
                    )
                    result.postValue(userEventsResult)
                }
            }

        val eventsCollection = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(EVENTS_COLLECTION)

        eventsChangedListenerSubscription?.remove() // Remove in case userId changes.
        // Set a value in case there are no changes to the data on start
        // This needs to be set to avoid that the upper layer LiveData detects the old data as a
        // new data.
        // When addSource was called in DefaultSessionAndUserEventRepository#getObservableUserEvents,
        // the old data was considered as a new data even though it's for another user's data
        result.value = null
        eventsChangedListenerSubscription = eventsCollection.addSnapshotListener(eventsListener)
    }

    private fun registerListenerForSingleEvent(
        sessionId: SessionId,
        userId: String
    ) {
        val result = resultSingleEvent

        val singleEventListener: (DocumentSnapshot?, FirebaseFirestoreException?) -> Unit =
            listener@{ snapshot, _ ->
                snapshot ?: return@listener

                DefaultScheduler.execute {
                    Timber.d("Event changes detected on session: $sessionId")

                    val userEvent = if (snapshot.exists()) {
                        parseUserEvent(snapshot)
                    } else {
                        null
                    }

                    val userEventResult = UserEventResult(
                        userEvent = userEvent
                    )
                    result.postValue(userEventResult)
                }
            }

        val eventDocument = firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(EVENTS_COLLECTION)
            .document(sessionId)

        eventChangedListenerSubscription?.remove() // Remove in case userId changes.
        resultSingleEvent.value = null
        eventChangedListenerSubscription = eventDocument.addSnapshotListener(singleEventListener)
    }

    override fun clearSingleEventSubscriptions() {
        Timber.d("Firestore Event data source: Clearing subscriptions")
        resultSingleEvent.value = null
        eventChangedListenerSubscription?.remove() // Remove to avoid leaks
    }

    /** Firestore writes **/

    /**
     * Stars or unstars an event.
     *
     * @returns a result via a LiveData.
     */
    override fun starEvent(
        userId: String,
        userEvent: UserEvent
    ): LiveData<Result<StarUpdatedStatus>> {
        val result = MutableLiveData<Result<StarUpdatedStatus>>()

        val data = mapOf(
            ID to userEvent.id,
            IS_STARRED to userEvent.isStarred
        )

        firestore.collection(USERS_COLLECTION)
            .document(userId)
            .collection(EVENTS_COLLECTION)
            .document(userEvent.id).set(data, SetOptions.merge()).addOnCompleteListener {
                if (it.isSuccessful) {
                    result.postValue(
                        Result.Success(
                            if (userEvent.isStarred) StarUpdatedStatus.STARRED
                            else StarUpdatedStatus.UNSTARRED
                        )
                    )
                } else {
                    result.postValue(
                        Result.Error(
                            it.exception ?: RuntimeException("Error updating star.")
                        )
                    )
                }
            }
        return result
    }
}
