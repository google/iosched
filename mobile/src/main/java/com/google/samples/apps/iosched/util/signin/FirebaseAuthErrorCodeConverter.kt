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

package com.google.samples.apps.iosched.util.signin

import com.firebase.ui.auth.ErrorCodes
import com.google.samples.apps.iosched.R
import timber.log.Timber

/**
 * Converts [ErrorCodes] from firebase to translatable strings.
 */
object FirebaseAuthErrorCodeConverter {

    fun convert(code: Int): Int {
        return when (code) {
            ErrorCodes.NO_NETWORK -> {
                Timber.d("FirebaseAuth error: no_network")
                R.string.firebase_auth_no_network_connection
            }
            ErrorCodes.DEVELOPER_ERROR -> {
                Timber.w("FirebaseAuth error: developer_error")
                R.string.firebase_auth_unknown_error
            }
            ErrorCodes.PLAY_SERVICES_UPDATE_CANCELLED -> {
                Timber.d("FirebaseAuth error: play_services_update_cancelled")
                R.string.firebase_auth_unknown_error
            }
            ErrorCodes.PROVIDER_ERROR -> {
                Timber.w("FirebaseAuth error: provider_error")
                R.string.firebase_auth_unknown_error
            }
            else -> {
                Timber.w("FirebaseAuth error: unknown_error")
                R.string.firebase_auth_unknown_error
            }
        }
    }
}
