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
import org.hamcrest.core.Is.`is`
import org.hamcrest.core.IsEqual.equalTo
import org.junit.Assert.assertThat
import org.junit.Test

class FirebaseAuthErrorCodeConverterTest {

    @Test
    fun convert_noInternet() {
        val resource = FirebaseAuthErrorCodeConverter.convert(ErrorCodes.NO_NETWORK)

        assertThat(resource, `is`(equalTo(R.string.firebase_auth_no_network_connection)))
    }

    @Test
    fun convert_unknown() {
        // All error codes except for NO_NETWORK should convert to
        // R.string.firebase_auth_unknown_error
        val errorCodes = listOf(
            ErrorCodes.DEVELOPER_ERROR,
            ErrorCodes.PLAY_SERVICES_UPDATE_CANCELLED,
            ErrorCodes.PROVIDER_ERROR,
            ErrorCodes.UNKNOWN_ERROR
        )

        errorCodes.map { FirebaseAuthErrorCodeConverter.convert(it) }
            .forEach { assertThat(it, `is`(equalTo(R.string.firebase_auth_unknown_error))) }
    }
}
