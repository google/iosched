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

package com.google.samples.apps.iosched.test.util.fakes

import android.arch.lifecycle.MutableLiveData
import android.net.Uri
import com.google.firebase.auth.FirebaseUser
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.ui.login.LoginEvent
import com.google.samples.apps.iosched.ui.login.LoginViewModelPlugin
import com.google.samples.apps.iosched.ui.schedule.Event

class FakeLoginViewModelPlugin : LoginViewModelPlugin {
    override val currentFirebaseUser = MutableLiveData<Result<FirebaseUser?>>()
    override val currentUserImageUri = MutableLiveData<Uri?>()
    override val performLoginEvent = MutableLiveData<Event<LoginEvent>>()

    var injectIsLoggedIn = true
    var loginRequestsEmitted = 0
    var logoutRequestsEmitted = 0

    override fun isLoggedIn() = injectIsLoggedIn

    override fun emitLoginRequest() {
        loginRequestsEmitted++
    }

    override fun emitLogoutRequest() {
        logoutRequestsEmitted++
    }
}
