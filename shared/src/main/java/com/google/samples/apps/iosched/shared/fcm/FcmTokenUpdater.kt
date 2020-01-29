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

package com.google.samples.apps.iosched.shared.fcm

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.iid.FirebaseInstanceId
import com.google.samples.apps.iosched.shared.data.document2019
import com.google.samples.apps.iosched.shared.domain.internal.DefaultScheduler
import javax.inject.Inject
import timber.log.Timber

/**
 * Saves the FCM ID tokens in Firestore.
 */
class FcmTokenUpdater @Inject constructor(val firestore: FirebaseFirestore) {

    fun updateTokenForUser(userId: String) {
        FirebaseInstanceId.getInstance().instanceId.addOnSuccessListener { instanceIdResult ->
            val token = instanceIdResult.token

            // Write token to /users/<userId>/fcmTokens/<token[0..TOKEN_ID_LENGTH]/
            val tokenInfo = mapOf(
                LAST_VISIT_KEY to FieldValue.serverTimestamp(),
                TOKEN_ID_KEY to token
            )

            // All Firestore operations start from the main thread to avoid concurrency issues.
            DefaultScheduler.postToMainThread {
                firestore
                    .document2019()
                    .collection(USERS_COLLECTION)
                    .document(userId)
                    .collection(FCM_IDS_COLLECTION)
                    .document(token.take(TOKEN_ID_LENGTH))
                    .set(tokenInfo, SetOptions.merge()).addOnCompleteListener {
                        if (it.isSuccessful) {
                            Timber.d("FCM ID token successfully uploaded for user $userId\"")
                        } else {
                            Timber.e("FCM ID token: Error uploading for user $userId")
                        }
                    }
            }
        }
    }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val LAST_VISIT_KEY = "lastVisit"
        private const val TOKEN_ID_KEY = "tokenId"
        private const val FCM_IDS_COLLECTION = "fcmTokens"
        private const val TOKEN_ID_LENGTH = 25
    }
}
