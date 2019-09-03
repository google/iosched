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

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.data.signin.FirebaseUserInfo
import com.google.samples.apps.iosched.shared.domain.sessions.NotificationAlarmUpdater
import com.google.samples.apps.iosched.shared.fcm.FcmTokenUpdater
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Success
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import timber.log.Timber
import javax.inject.Inject

/**
 * An [AuthStateUserDataSource] that listens to changes in [FirebaseAuth].
 *
 * When a [FirebaseUser] is available, it
 *  * Posts it to the user observable
 *  * Fetches the ID token
 *  * Stores the FCM ID Token in Firestore
 *  * Posts the user ID to the observable
 */
@ExperimentalCoroutinesApi
class FirebaseAuthStateUserDataSource @Inject constructor(
    val firebase: FirebaseAuth,
    private val tokenUpdater: FcmTokenUpdater,
    private val notificationAlarmUpdater: NotificationAlarmUpdater
) : AuthStateUserDataSource {

    private var isListening = false
    private var lastUid: String? = null

    // Channel that keeps track of User Authentication
    private val channel = ConflatedBroadcastChannel<Result<AuthenticatedUserInfo>>()

    // Listener that saves the [FirebaseUser], fetches the ID token
    // and updates the user ID observable.
    val listener: ((FirebaseAuth) -> Unit) = { auth ->
        Timber.d("Received a FirebaseAuth update")

        auth.currentUser?.let { currentUser ->
            // Save the FCM ID token in firestore
            tokenUpdater.updateTokenForUser(currentUser.uid)

            if (lastUid != auth.uid) { // Prevent duplicates
                notificationAlarmUpdater.updateAll(currentUser.uid)
            }
        }

        if (auth.currentUser == null) {
            // Logout, cancel all alarms
            notificationAlarmUpdater.cancelAll()
        }

        // Save the last UID to prevent setting too many alarms.
        lastUid = auth.uid

        // Send the current user for observers
        if (!channel.isClosedForSend) {
            channel.offer(Success(FirebaseUserInfo(auth.currentUser)))
        } else {
            unregisterListener()
        }
    }

    // Synchronized method, multiple calls to this method at the same time isn't allowed since
    // isListening is read and can be modified
    @FlowPreview
    @Synchronized
    override fun getBasicUserInfo(): Flow<Result<AuthenticatedUserInfo>> {
        if (!isListening) {
            firebase.addAuthStateListener(listener)
            isListening = true
        }
        return channel.asFlow()
    }

    private fun unregisterListener() {
        firebase.removeAuthStateListener(listener)
    }
}
