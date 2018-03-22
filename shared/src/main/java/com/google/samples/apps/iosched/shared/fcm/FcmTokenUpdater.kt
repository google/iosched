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
        val tokenInfo = mapOf(LAST_VISIT_KEY to FieldValue.serverTimestamp())

        firestore
                .collection(Companion.USERS_COLLECTION)
                .document(userId)
                .collection(FCM_IDS_COLLECTION)
                .document(token)
                .set(tokenInfo, SetOptions.merge()).addOnCompleteListener( {
                    if (it.isSuccessful) {
                        Timber.d("FCM ID token successfully uploaded for user $userId\"")
                    } else {
                        Timber.e("FCM ID token: Error uploading for user $userId")
                    }
                })

    }

    companion object {
        private const val USERS_COLLECTION = "users"
        private const val LAST_VISIT_KEY = "lastVisit"
        private const val FCM_IDS_COLLECTION = "fcmTokens"
    }
}
