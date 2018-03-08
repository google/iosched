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

package com.google.samples.apps.iosched.util.login

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.gms.auth.api.signin.GoogleSignInOptions

private val providers = mutableListOf(
        AuthUI.IdpConfig.GoogleBuilder().setSignInOptions(
                GoogleSignInOptions.Builder()
                    .requestId()
                    .requestProfile()
                    .requestEmail()
                    .build())
            .build()
)

class LoginHandler {

    /**
     * Request a login intent.
     *
     * To observe the result you must pass this to startActivityForResult.
     */
    fun makeLoginIntent() = AuthUI.getInstance()
        .createSignInIntentBuilder()
        .setAvailableProviders(providers)
        .build()

    /**
     * Parse the response from a a login request, helper to call from onActivityResult.
     *
     * ```
     * processLoginResult(resultCode, data) { result ->
     *    return when(result) {
     *        is LoginSuccess -> // all good
     *        is LoginFailed -> result?.error // access FirebaseUiException - can be null (e.g. canceled)
     *    }
     * }
     * ```
     *
     * @param resultCode activity result code
     * @param data activity result intent
     * @param callback callback to pass parsed result
     *
     */
    fun handleLogin(
        resultCode: Int,
        data: Intent?,
        callback: (LoginResult) -> Unit
    ) {
        when (resultCode) {
            Activity.RESULT_OK ->  callback(LoginSuccess())
            else -> callback(LoginFailed(IdpResponse.fromResultIntent(data)?.error))
        }
    }

    /**
     * Attempt to log the current user out.
     *
     * @param context any context
     * @param callback used to notify of logout completion.
     */
    fun logout(context: Context, callback: () -> Unit = {}) {
        AuthUI.getInstance()
            .signOut(context)
            .addOnCompleteListener { callback() }
    }
}
