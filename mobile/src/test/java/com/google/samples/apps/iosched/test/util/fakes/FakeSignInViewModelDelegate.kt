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

import android.net.Uri
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfo
import com.google.samples.apps.iosched.ui.signin.SignInNavigationAction
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class FakeSignInViewModelDelegate : SignInViewModelDelegate {

    override val userInfo = MutableStateFlow<AuthenticatedUserInfo?>(null)
    override val currentUserImageUri = MutableStateFlow<Uri?>(null)
    override val signInNavigationActions = flow<SignInNavigationAction> { }
    override val showReservations = MutableStateFlow<Boolean>(false)

    var injectIsSignedIn = true
    var injectIsRegistered = false
    var signInRequestsEmitted = 0
    var signOutRequestsEmitted = 0

    override val isUserSignedInValue: Boolean
        get() = injectIsSignedIn

    override val isUserSignedIn get() = TODO("Not implemented")

    override val isUserRegistered get() = MutableStateFlow(injectIsSignedIn)

    override val isUserRegisteredValue: Boolean
        get() = injectIsRegistered

    override suspend fun emitSignInRequest() {
        signInRequestsEmitted++
    }

    override suspend fun emitSignOutRequest() {
        signOutRequestsEmitted++
    }

    override val userIdValue: String?
        get() {
            return userInfo.value?.getUid()
        }

    fun loadUser(id: String) {
        val mockUser = mock<AuthenticatedUserInfo> {
            on { getUid() }.doReturn(id)
            on { getPhotoUrl() }.doReturn(mock<Uri> {})
            on { isSignedIn() }.doReturn(true)
            on { this@on.isRegistered() }.doReturn(injectIsRegistered)
            on { isRegistrationDataReady() }.doReturn(true)
        }
        userInfo.value = mockUser
    }

    override val userId: Flow<String?>
        get() = flow { emitAll(userInfo.map { it?.getUid() }) }
}
