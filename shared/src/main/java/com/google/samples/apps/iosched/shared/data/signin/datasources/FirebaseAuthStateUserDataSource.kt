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
import com.google.firebase.auth.GetTokenResult
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfoBasic
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserRegistration
import com.google.samples.apps.iosched.shared.data.signin.FirebaseUserInfo
import com.google.samples.apps.iosched.shared.domain.sessions.NotificationAlarmUpdater
import com.google.samples.apps.iosched.shared.fcm.FcmTokenUpdater
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.shared.util.suspendAndWait
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.map
import timber.log.Timber

/**
 * An [AuthStateUserDataSource] that listens to changes in [FirebaseAuth].
 *
 * When a [FirebaseUser] is available, it
 *  * Posts it to the user observable
 *  * Fetches the ID token
 *  * Uses the ID token to trigger the registration point
 *  * Stores the FCM ID Token in Firestore
 *  * Posts the user ID to the observable
 *
 * This data source doesn't find if a user is registered or not (is an attendee). Once the
 * registration point is called, the server will generate a field in the user document, which
 * is observed by [RegisteredUserDataSource] in its implementation
 * [FirestoreRegisteredUserDataSource].
 */
class FirebaseAuthStateUserDataSource @Inject constructor(
    val firebase: FirebaseAuth,
    private val tokenUpdater: FcmTokenUpdater,
    private val notificationAlarmUpdater: NotificationAlarmUpdater
) : AuthStateUserDataSource {

    // lastUid can be potentially consumed and written from different threads
    // Making it thread safe with @Volatile
    @Volatile
    private var lastUid: String? = null

    override fun getBasicUserInfo(): Flow<Result<AuthenticatedUserInfoBasic?>> {
        return channelFlow<FirebaseAuth> {
            val authStateListener: ((FirebaseAuth) -> Unit) = { auth ->
                // This callback gets always executed on the main thread because of Firebase
                channel.offer(auth)
            }
            firebase.addAuthStateListener(authStateListener)
            awaitClose { firebase.removeAuthStateListener(authStateListener) }
        }.map { authState ->
            // This map gets executed in the Flow's context
            processAuthState(authState)
        }
    }

    private suspend fun processAuthState(auth: FirebaseAuth): Result<AuthenticatedUserInfoBasic?> {
        // Listener that saves the [FirebaseUser], fetches the ID token
        // and updates the user ID observable.
        Timber.d("Received a FirebaseAuth update.")

        auth.currentUser?.let { currentUser ->
            // Get the ID token (force refresh)
            val tokenTask = currentUser.getIdToken(true)
            try {
                val tokenResult: GetTokenResult = tokenTask.suspendAndWait()
                tokenResult.token?.let {
                    // Call registration point to generate a result in Firestore
                    Timber.d("User authenticated, hitting registration endpoint")
                    AuthenticatedUserRegistration.callRegistrationEndpoint(it)
                }
            } catch (e: Exception) {
                Timber.e(e)
                return@let
            }
            // Save the FCM ID token in firestore
            tokenUpdater.updateTokenForUser(currentUser.uid)
        }

        if (auth.currentUser == null) {
            // Logout, cancel all alarms
            notificationAlarmUpdater.cancelAll()
        }

        auth.currentUser?.let {
            if (lastUid != auth.uid) { // Prevent duplicates
                notificationAlarmUpdater.updateAll(it.uid)
            }
        }

        // Save the last UID to prevent setting too many alarms.
        lastUid = auth.uid

        // Send the current user for observers
        return Success(FirebaseUserInfo(auth.currentUser))
    }
}
