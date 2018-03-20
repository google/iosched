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

package com.google.samples.apps.iosched.shared.data.login.datasources

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.samples.apps.iosched.shared.domain.internal.DefaultScheduler
import com.google.samples.apps.iosched.shared.result.Result
import timber.log.Timber
import javax.inject.Inject

/**
 * A [RegisteredUserDataSource] that listens to changes in firestore to indicate whether the
 * current user is registered in the event or not as an attendee.
 */
class FirestoreRegisteredUserDataSource @Inject constructor(
        val firestore: FirebaseFirestore
) : RegisteredUserDataSource {

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val REGISTERED_KEY = "registered"
    }

    private var registeredChangedListenerSubscription: ListenerRegistration? = null

    // Result can contain a null value (not processed) or a null result (not available).
    private val result = MutableLiveData<Result<Boolean?>?>()

    /**
     * Listens to changes in the user document in Firestore. A Change in the "registered" field
     * will emit a new user.
     */
    override fun listenToUserChanges(userId: String) {
        Timber.d("Observing firestore for changes in registration for: $userId")

        // Remove the previous subscription, if it exists:
        registeredChangedListenerSubscription?.remove()

        // Watch the document:
        val registeredChangedListener = {
            snapshot: DocumentSnapshot?, _: FirebaseFirestoreException? ->
            DefaultScheduler.execute {
                val isRegistered: Boolean? = snapshot?.get(REGISTERED_KEY) as? Boolean
                Timber.d("Received registered flag: $isRegistered")
                result.postValue(Result.Success(isRegistered))
            }
        }
        registeredChangedListenerSubscription = firestore.collection(USERS_COLLECTION)
                .document(userId).addSnapshotListener(registeredChangedListener)
    }

    override fun observeResult() : LiveData<Result<Boolean?>?> {
        return result
    }

    override fun clearListener() {
        registeredChangedListenerSubscription?.remove()
    }
}