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