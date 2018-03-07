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

package com.google.samples.apps.iosched.ui.login

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.net.Uri
import com.google.firebase.auth.FirebaseUser
import com.google.samples.apps.iosched.shared.domain.login.ObservableFirebaseUserUseCase
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.shared.util.checkAllMatched
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.ui.login.LoginEvent.RequestLogin
import com.google.samples.apps.iosched.ui.login.LoginEvent.RequestLogout
import com.google.samples.apps.iosched.ui.schedule.Event
import javax.inject.Inject

enum class LoginEvent {
    RequestLogin, RequestLogout
}

/**
 * Interface to implement login functionality in a ViewModel.
 *
 * You can inject a implementation of this via Dagger2, then use the implementation as an interface
 * delegate to add login functionality without writing any code
 *
 * Example usage
 *
 * ```
 * class MyViewModel @Inject constructor(
 *     loginViewModelComponent: LoginViewModelPlugin
 * ) : ViewModel(), LoginViewModelPlugin by loginViewModelComponent {
 * ```
 */
interface LoginViewModelPlugin {
    /**
     * Live updated value of the current firebase user
     */
    val currentFirebaseUser: MutableLiveData<Result<FirebaseUser?>>

    /**
     * Live updated value of the current firebase users image url
     */
    val currentUserImageUri: LiveData<Uri?>

    /**
     * Emits Events when a login event should be attempted
     */
    val performLoginEvent: MutableLiveData<Event<LoginEvent>>

    fun isLoggedIn(): Boolean {
        val currentUser = currentFirebaseUser.value
        return when (currentUser) {
            is Success -> currentUser.data != null
            else -> false
        }.checkAllMatched
    }

    /**
     * Emit an Event on performLoginEvent to request login
     */
    fun emitLoginRequest() = performLoginEvent.postValue(Event(RequestLogin))

    /**
     * Emit an Event on performLoginEvent to request logout
     */
    fun emitLogoutRequest() = performLoginEvent.postValue(Event(RequestLogout))
}

/**
 * Implementation of LoginViewModel that can be used as an interface delegate.
 */
internal class LoginViewModelPluginImpl @Inject constructor(
    observableFirebaseUserUseCase: ObservableFirebaseUserUseCase
) : LoginViewModelPlugin {
    override val performLoginEvent = MutableLiveData<Event<LoginEvent>>()
    override val currentFirebaseUser = MutableLiveData<Result<FirebaseUser?>>()
    override val currentUserImageUri: LiveData<Uri?> = currentFirebaseUser.map { result ->
        when (result) {
            is Success -> result.data?.photoUrl
            else -> null
        }
    }

    init {
        observableFirebaseUserUseCase(Unit, currentFirebaseUser)
    }
}
