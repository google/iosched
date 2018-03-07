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

import android.arch.lifecycle.MutableLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.WeakLiveDataHolder
import javax.inject.Inject

interface FirebaseUserDataSource {
    fun watch(onTokenChanged: (String) -> Unit)
    /**
     * Register a MutableLiveData to receive updates as the current user changes.
     */
    fun addObservableFirebaseUser(into: MutableLiveData<Result<FirebaseUser?>>)
}

internal class FirebaseUserDataSourceImpl @Inject constructor(private val firebase: FirebaseAuth) :
        FirebaseUserDataSource {

    private val liveDataHolder = WeakLiveDataHolder<Result<FirebaseUser?>>()

    private var initialized: Boolean = false

    /**
     * Watch for changes to auth state and notify the caller via onTokenChanged.
     *
     * This function is for use by Repository classes. UI or ViewModel code should
     * use a use case to get this data.
     *
     * @see ObservableFirebaseUserUseCase
     *
     * @param onTokenChanged called with the new user token whenever the user changes (at least once
     *                 per app session)
     */
    override fun watch(onTokenChanged: (String) -> Unit) {
        initialized = true

        firebase.addAuthStateListener { auth ->
            notifyObservers(auth.currentUser)
            auth.currentUser?.let {
                processUserForTokenCallback(it, onTokenChanged)
            }
        }
    }

    private fun processUserForTokenCallback(
        currentUser: FirebaseUser,
        onTokenChanged: (String) -> Unit
    ) {
        // then, inform any repository callbacks of the change
        if (currentUser.isAnonymous) return

        val tokenTask = currentUser.getIdToken(false)
        tokenTask.addOnCompleteListener { result ->
            if (result.isSuccessful) {
                val token = result.result.token ?: return@addOnCompleteListener
                onTokenChanged(token)
            }
        }
    }

    private fun notifyObservers(currentUser: FirebaseUser?) {
        liveDataHolder.notifyAll(Result.Success(currentUser))
    }

    override fun addObservableFirebaseUser(into: MutableLiveData<Result<FirebaseUser?>>) {
        liveDataHolder.addLiveDataObserver(into)
        liveDataHolder.notifyIfChanged(Result.Success(firebase.currentUser))

        if (!initialized) {
            watch {} // force initialization to observe future updates
        }
    }
}
