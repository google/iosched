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
import com.google.samples.apps.iosched.shared.data.login.datasources.StagingAuthenticatedUserInfo
import com.google.samples.apps.iosched.util.signin.SignInHandler
import com.google.samples.apps.iosched.util.signin.SignInResult
import com.google.samples.apps.iosched.util.signin.SignInSuccess
import timber.log.Timber

/**
 * A [SignInHandler] that signs a [StagingAuthenticatedUser] in and out, used to simulate an
 * authentication backend for hermetic development and testing.
 */
class StagingSignInHandler(val user: StagingAuthenticatedUser) : SignInHandler {

    override suspend fun makeSignInIntent(): Intent {
        Timber.d("staging makeSignInIntent called")
        user.signIn()
        return Intent()
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

    // TODO: Unused
    private var signedIn: Boolean = false

    fun signIn() {
        signedIn = true
    }

    fun signOut() {
        signedIn = false
    }
}

class StagingLoggedOutFirebaseUserInfo(
    _context: Context
) : StagingAuthenticatedUserInfo(_context) {

    override fun isSignedIn(): Boolean = false

    override fun isRegistered(): Boolean = false

    override fun getPhotoUrl(): Uri? = null

    override fun isRegistrationDataReady(): Boolean = true
}
