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

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.shared.data.login.datasources.StagingAuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.util.signin.SignInHandler
import com.google.samples.apps.iosched.util.signin.SignInResult
import com.google.samples.apps.iosched.util.signin.SignInSuccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import timber.log.Timber

/**
 * A [SignInHandler] that signs a [StagingAuthenticatedUser] in and out, used to simulate an
 * authentication backend for hermetic development and testing.
 */
class StagingSignInHandler(val user: StagingAuthenticatedUser) : SignInHandler {

    override fun makeSignInIntent(): Flow<Intent?> {
        Timber.d("staging makeSignInIntent called")
        user.signIn()
        return flow { }
    }

    override fun signIn(resultCode: Int, data: Intent?, onComplete: (SignInResult) -> Unit) {
        Timber.d("staging signIn called")
        onComplete(SignInSuccess)
    }

    override fun signOut(context: Context, onComplete: () -> Unit) {
        Timber.d("staging signIn called")
        onComplete()
        user.signOut()
    }
}

/**
 * A data source for used for [StagingSignInHandler]
 */
class StagingAuthenticatedUser(val context: Context) {

    private val stagingSignedInFirebaseUser = StagingAuthenticatedUserInfo(context)
    private val stagingSignedOutFirebaseUser = StagingLoggedOutFirebaseUserInfo(context)

    val currentUserResult = MutableLiveData<Result<AuthenticatedUserInfo>?>()

    init {
        currentUserResult.value = Result.Success(stagingSignedInFirebaseUser)
    }

    private var signedIn: Boolean = false

    fun signIn() {
        signedIn = true
        currentUserResult.postValue(Result.Success(stagingSignedInFirebaseUser))
    }

    fun signOut() {
        signedIn = false
        currentUserResult.postValue(Result.Success(stagingSignedOutFirebaseUser))
    }
}

class StagingLoggedOutFirebaseUserInfo(
    _context: Context
) : StagingAuthenticatedUserInfo(_context) {

    override fun isSignedIn(): Boolean = false

    override fun getPhotoUrl(): Uri? = null
}
