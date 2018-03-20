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
import com.google.samples.apps.iosched.shared.data.login.AuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.domain.auth.ObserveUserAuthStateUseCase
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.ui.login.LoginEvent.RequestLogin
import com.google.samples.apps.iosched.ui.login.LoginEvent.RequestLogout
import com.google.samples.apps.iosched.shared.result.Event
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
    val currentFirebaseUser: LiveData<Result<AuthenticatedUserInfo>?>

    /**
     * Live updated value of the current firebase users image url
     */
    val currentUserImageUri: LiveData<Uri?>

    /**
     * Emits Events when a login event should be attempted
     */
    val performLoginEvent: MutableLiveData<Event<LoginEvent>>

    /**
     * Emit an Event on performLoginEvent to request login
     */
    fun emitLoginRequest() = performLoginEvent.postValue(Event(RequestLogin))

    /**
     * Emit an Event on performLoginEvent to request logout
     */
    fun emitLogoutRequest() = performLoginEvent.postValue(Event(RequestLogout))

    fun observeLoggedInUser(): LiveData<Boolean>

    fun observeRegisteredUser(): LiveData<Boolean>

    fun isLoggedIn(): Boolean

    fun isRegistered(): Boolean
}

/**
 * Implementation of LoginViewModel that uses Firebase's auth mechanisms.
 */
internal class FirebaseLoginViewModelPlugin @Inject constructor(
    observeUserAuthStateUseCase: ObserveUserAuthStateUseCase
) : LoginViewModelPlugin {

    override val performLoginEvent = MutableLiveData<Event<LoginEvent>>()
    override val currentFirebaseUser: LiveData<Result<AuthenticatedUserInfo>?>
    override val currentUserImageUri: LiveData<Uri?>

    private val _isRegistered: LiveData<Boolean>
    private val _isLoggedIn: LiveData<Boolean>

    init {
        currentFirebaseUser = observeUserAuthStateUseCase.observe()

        currentUserImageUri = currentFirebaseUser.map { result: Result<AuthenticatedUserInfo?>? ->
            (result as? Result.Success)?.data?.getPhotoUrl()
        }

        _isLoggedIn = currentFirebaseUser.map { isLoggedIn() }

        _isRegistered = currentFirebaseUser.map { isRegistered() }

        observeUserAuthStateUseCase.execute(Any())
    }

    override fun isLoggedIn(): Boolean {
        return (currentFirebaseUser.value as? Result.Success)?.data?.isLoggedIn() == true
    }

    override fun isRegistered(): Boolean {
        return (currentFirebaseUser.value as? Result.Success)?.data?.isRegistered() == true
    }

    override fun observeLoggedInUser(): LiveData<Boolean> {
        return _isLoggedIn
    }

    override fun observeRegisteredUser(): LiveData<Boolean> {
        return _isRegistered
    }

    /**
     * Emit an Event on performLoginEvent to request login
     */
    override fun emitLoginRequest() = performLoginEvent.postValue(Event(RequestLogin))

    /**
     * Emit an Event on performLoginEvent to request logout
     */
    override fun emitLogoutRequest() = performLoginEvent.postValue(Event(RequestLogout))
}
