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
import timber.log.Timber
import javax.inject.Inject

/**
 * Saves the FCM ID tokens in Firestore.
 */
class FcmTokenUpdater @Inject constructor(
    val firestore: FirebaseFirestore
) {

    fun updateTokenForUser(userId: String) {
        val token = FirebaseInstanceId.getInstance().token

        if (token == null) {
            Timber.e("Error getting FCM ID token for user $userId")
            return
        }

        // Write token to /users/<userId>/fcmTokens/<token[0..TOKEN_ID_LENGTH]/

        val tokenInfo = mapOf(
            LAST_VISIT_KEY to FieldValue.serverTimestamp(),
            TOKEN_ID_KEY to token
        )

        firestore
            .collection(Companion.USERS_COLLECTION)
            .document(userId)
            .collection(FCM_IDS_COLLECTION)
            .document(token.take(TOKEN_ID_LENGTH))
            .set(tokenInfo, SetOptions.merge()).addOnCompleteListener({
                if (it.isSuccessful) {
                    Timber.d("FCM ID token successfully uploaded for user $userId\"")
                } else {
                    Timber.e("FCM ID token: Error uploading for user $userId")
                }
            })

        // Write server timestamp to /users/<userId>/lastUsage

        val lastUsage = mapOf(
            USER_LAST_USAGE_KEY to FieldValue.serverTimestamp()
        )

        firestore
            .collection(Companion.USERS_COLLECTION)
            .document(userId)
            .set(lastUsage, SetOptions.merge()).addOnCompleteListener({
                if (it.isSuccessful) {
                    Timber.d("Last usage timestamp successfully uploaded for user $userId\"")
                } else {
                    Timber.e("Last usage timestamp: Error uploading for user $userId")
                }
            })
    }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val LAST_VISIT_KEY = "lastVisit"
        private const val USER_LAST_USAGE_KEY = "lastUsage"
        private const val TOKEN_ID_KEY = "tokenId"
        private const val FCM_IDS_COLLECTION = "fcmTokens"
        private const val TOKEN_ID_LENGTH = 25
    }
}
