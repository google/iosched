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

package com.google.samples.apps.iosched.shared.domain.auth

import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.data.signin.FirebaseRegisteredUserInfo
import com.google.samples.apps.iosched.shared.data.signin.datasources.AuthStateUserDataSource
import com.google.samples.apps.iosched.shared.data.signin.datasources.RegisteredUserDataSource
import com.google.samples.apps.iosched.shared.domain.MediatorUseCase
import com.google.samples.apps.iosched.shared.result.Result
import timber.log.Timber
import javax.inject.Inject

/**
 * A [MediatorUseCase] that observes two data sources to generate an [AuthenticatedUserInfo]
 * that includes whether the user is registered (is an attendee).
 *
 * [RegisteredUserDataSource] provides general user information, like user IDs, while
 * [AuthStateUserDataSource] observes a different data source to provide a flag indicating
 * whether the user is registered.
 */
open class ObserveUserAuthStateUseCase @Inject constructor(
        registeredUserDataSource: RegisteredUserDataSource,
        val authStateUserDataSource: AuthStateUserDataSource
) : MediatorUseCase<Any, AuthenticatedUserInfo>() {
    
    private val currentFirebaseUserObservable = authStateUserDataSource.getBasicUserInfo()

    private val userIdObservable = authStateUserDataSource.getUserId()

    private val isUserRegisteredObservable = registeredUserDataSource.observeResult()

    init {
        // When the user ID changes, start observing the user in firestore
        result.addSource(userIdObservable) {
            it?.let { registeredUserDataSource.listenToUserChanges(it) }
        }

        // If the Firebase user changes, update result.
        result.addSource(currentFirebaseUserObservable) {
            updateUserObservable()
        }

        // If the Firestore information about the user changes, update result.
        result.addSource(isUserRegisteredObservable) {
            // When the flag that indicates if an user is an attendee is fetched,
            // update the user result with it:
            updateUserObservable()
        }
    }

    override fun execute(parameters: Any) {
        // Start listening to the [AuthStateUserDataSource] for changes in auth state.
        authStateUserDataSource.startListening()
    }

    private fun updateUserObservable() {
        Timber.d("Updating observable user")
        val currentFbUser = currentFirebaseUserObservable.value
        val isRegistered = isUserRegisteredObservable.value

        if (currentFbUser is Result.Success) {

            // If the isRegistered value is an error, assign it false
            val isRegisteredValue = (isRegistered as? Result.Success)?.data == true

            result.postValue(
                    Result.Success(
                            FirebaseRegisteredUserInfo(currentFbUser.data, isRegisteredValue)))
        } else {
            Timber.e("There was a registration error.")
            result.postValue(Result.Error(Exception("Registration error")))
        }
    }
}