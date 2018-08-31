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

package com.google.samples.apps.adssched.shared.data.signin.datasources

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.samples.apps.adssched.shared.data.signin.AuthenticatedUserInfo
import com.google.samples.apps.adssched.shared.data.signin.FirebaseUserInfo
import com.google.samples.apps.adssched.shared.domain.internal.DefaultScheduler
import com.google.samples.apps.adssched.shared.fcm.FcmTokenUpdater
import com.google.samples.apps.adssched.shared.result.Result
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
class FirebaseAuthStateUserDataSource @Inject constructor(
    val firebase: FirebaseAuth,
    private val tokenUpdater: FcmTokenUpdater
) : AuthStateUserDataSource {

    private val currentFirebaseUserObservable =
        MutableLiveData<Result<AuthenticatedUserInfo?>>()

    private var isAlreadyListening = false

    // Listener that saves the [FirebaseUser], fetches the ID token
    // and updates the user ID observable.
    private val authStateListener: ((FirebaseAuth) -> Unit) = { auth ->
        DefaultScheduler.execute {
            Timber.d("Received a FirebaseAuth update.")
            // Post the current user for observers
            currentFirebaseUserObservable.postValue(
                Result.Success(
                    FirebaseUserInfo(auth.currentUser)
                )
            )

            auth.currentUser?.let { currentUser ->
                // Save the FCM ID token in firestore
                tokenUpdater.updateTokenForUser(currentUser.uid)
            }
        }
    }

    override fun startListening() {
        if (!isAlreadyListening) {
            firebase.addAuthStateListener(authStateListener)
            isAlreadyListening = true
        }
    }

    override fun getBasicUserInfo(): LiveData<Result<AuthenticatedUserInfo?>> {
        return currentFirebaseUserObservable
    }

    override fun clearListener() {
        firebase.removeAuthStateListener(authStateListener)
    }
}
