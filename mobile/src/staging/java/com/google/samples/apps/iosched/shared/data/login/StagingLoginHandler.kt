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
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.samples.apps.iosched.shared.data.login.datasources.StagingAuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.util.login.LoginHandler
import com.google.samples.apps.iosched.util.login.LoginResult
import com.google.samples.apps.iosched.util.login.LoginSuccess
import timber.log.Timber

/**
 * A [LoginHandler] that logs a [StagingAuthenticatedUser] in and out, used to simulate an
 * authentication backend for hermetic development and testing.
 */
class StagingLoginHandler(val user: StagingAuthenticatedUser): LoginHandler {

    override fun makeLoginIntent(): Intent? {
        Timber.d("staging makeLoginIntent called")
        user.logIn()
        return null
    }

    override fun handleLogin(resultCode: Int, data: Intent?, onComplete: (LoginResult) -> Unit) {
        Timber.d("staging handleLogin called")
        onComplete(LoginSuccess)
    }

    override fun logout(context: Context, onComplete: () -> Unit) {
        Timber.d("staging handleLogin called")
        onComplete()
        user.logOut()
    }
}

/**
 * A data source for used for [StagingLoginHandler]
 */
class StagingAuthenticatedUser(val context: Context) {

    private val stagingLoggedInFirebaseUser = StagingAuthenticatedUserInfo(context)
    private val stagingLoggedOutFirebaseUser = StagingLoggedOutFirebaseUserInfo(context)

    val currentUserResult = MutableLiveData<Result<AuthenticatedUserInfo>?>()

    init {
        currentUserResult.value = Result.Success(stagingLoggedInFirebaseUser)
    }

    private var loggedIn: Boolean = false

    fun logIn() {
        loggedIn = true
        currentUserResult.postValue(Result.Success(stagingLoggedInFirebaseUser))
    }

    fun logOut() {
        loggedIn = false
        currentUserResult.postValue(Result.Success(stagingLoggedOutFirebaseUser))
    }

}

class StagingLoggedOutFirebaseUserInfo(
        _context: Context
) : StagingAuthenticatedUserInfo(_context) {

    override fun isLoggedIn(): Boolean = false

    override fun isRegistered(): Boolean = false

    override fun getPhotoUrl(): Uri? = null
}

