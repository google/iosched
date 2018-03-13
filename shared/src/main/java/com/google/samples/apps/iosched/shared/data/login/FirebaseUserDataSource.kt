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

package com.google.samples.apps.iosched.shared.data.login

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GetTokenResult
import com.google.samples.apps.iosched.shared.result.Result
import timber.log.Timber
import javax.inject.Inject

interface FirebaseUserDataSource {
    fun getToken(): LiveData<Result<String>>

    fun getCurrentUser(): LiveData<Result<FirebaseUser?>?>
}

internal class DefaultFirebaseUserDataSource @Inject constructor(
        firebase: FirebaseAuth
) : FirebaseUserDataSource {

    private val tokenChangedObservable = MutableLiveData<Result<String>>()

    private val currentUserObservable = MutableLiveData<Result<FirebaseUser?>?>()
            .apply { value = null }

    init {
        // Listen for changes in the authentication state
        firebase.addAuthStateListener { auth ->
            // Save the current user
            currentUserObservable.postValue(Result.Success(auth.currentUser))
            auth.currentUser?.let {
                // Get the ID token (no force refresh)
                val tokenTask = it.getIdToken(false)
                tokenTask.addOnCompleteListener { result: Task<GetTokenResult> ->
                    if (result.isSuccessful && result.result.token != null) {
                        // Save the current token
                        tokenChangedObservable.postValue(Result.Success(result.result.token!!))
                    } else {
                        Timber.e(result.exception?.message ?: "Error getting ID token")
                        tokenChangedObservable.postValue(
                                Result.Error(result.exception
                                        ?: RuntimeException("Error getting ID token")))
                    }
                }
            }
        }
    }

    override fun getToken(): LiveData<Result<String>> {
        return tokenChangedObservable
    }

    override fun getCurrentUser(): LiveData<Result<FirebaseUser?>?> {
        return currentUserObservable
    }
}
