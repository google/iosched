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

package com.google.samples.apps.iosched.shared.data.signin.datasources

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.samples.apps.iosched.shared.data.document2020
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.tryOffer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
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

    override fun observeUserChanges(userId: String): Flow<Result<Boolean?>> {
        return callbackFlow<Result<Boolean?>> {
            // Watch the document
            val registeredChangedListener =
                { snapshot: DocumentSnapshot?, _: FirebaseFirestoreException? ->
                    if (snapshot == null || !snapshot.exists()) {
                        // When the account signs in for the first time, the document doesn't exist
                        Timber.d("Document for snapshot $userId doesn't exist")
                        tryOffer(Result.Success(false))
                    } else {
                        val isRegistered: Boolean? = snapshot.get(REGISTERED_KEY) as? Boolean
                        Timber.d("Received registered flag: $isRegistered")
                        tryOffer(Result.Success(isRegistered))
                    }
                    Unit // Avoids returning the Boolean from channel.offer
                }

            val registeredChangedListenerSubscription = firestore
                .document2020()
                .collection(USERS_COLLECTION)
                .document(userId)
                .addSnapshotListener(registeredChangedListener)

            awaitClose { registeredChangedListenerSubscription.remove() }
        }
            // Only emit a value if it's a new value or a value change.
            .distinctUntilChanged()
    }
}
