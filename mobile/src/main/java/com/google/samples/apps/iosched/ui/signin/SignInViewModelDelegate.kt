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

package com.google.samples.apps.iosched.ui.signin

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.net.Uri
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.domain.auth.ObserveUserAuthStateUseCase
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.ui.signin.SignInEvent.RequestSignIn
import com.google.samples.apps.iosched.ui.signin.SignInEvent.RequestSignOut
import com.google.samples.apps.iosched.shared.result.Event
import javax.inject.Inject

enum class SignInEvent {
    RequestSignIn, RequestSignOut
}

/**
 * Interface to implement sign-in functionality in a ViewModel.
 *
 * You can inject a implementation of this via Dagger2, then use the implementation as an interface
 * delegate to add sign in functionality without writing any code
 *
 * Example usage
 *
 * ```
 * class MyViewModel @Inject constructor(
 *     signInViewModelComponent: SignInViewModelDelegate
 * ) : ViewModel(), SignInViewModelDelegate by signInViewModelComponent {
 * ```
 */
interface SignInViewModelDelegate {
    /**
     * Live updated value of the current firebase user
     */
    val currentFirebaseUser: LiveData<Result<AuthenticatedUserInfo>?>

    /**
     * Live updated value of the current firebase users image url
     */
    val currentUserImageUri: LiveData<Uri?>

    /**
     * Emits Events when a sign-in event should be attempted
     */
    val performSignInEvent: MutableLiveData<Event<SignInEvent>>

    /**
     * Emit an Event on performSignInEvent to request sign-in
     */
    fun emitSignInRequest() = performSignInEvent.postValue(Event(RequestSignIn))

    /**
     * Emit an Event on performSignInEvent to request sign-out
     */
    fun emitSignOutRequest() = performSignInEvent.postValue(Event(RequestSignOut))

    fun observeSignedInUser(): LiveData<Boolean>

    fun observeRegisteredUser(): LiveData<Boolean>

    fun isSignedIn(): Boolean

    fun isRegistered(): Boolean
}

/**
 * Implementation of SignInViewModelDelegate that uses Firebase's auth mechanisms.
 */
internal class FirebaseSignInViewModelDelegate @Inject constructor(
        observeUserAuthStateUseCase: ObserveUserAuthStateUseCase
) : SignInViewModelDelegate {

    override val performSignInEvent = MutableLiveData<Event<SignInEvent>>()
    override val currentFirebaseUser: LiveData<Result<AuthenticatedUserInfo>?>
    override val currentUserImageUri: LiveData<Uri?>

    private val _isRegistered: LiveData<Boolean>
    private val _isSignedIn: LiveData<Boolean>

    init {
        currentFirebaseUser = observeUserAuthStateUseCase.observe()

        currentUserImageUri = currentFirebaseUser.map { result: Result<AuthenticatedUserInfo?>? ->
            (result as? Result.Success)?.data?.getPhotoUrl()
        }

        _isSignedIn = currentFirebaseUser.map { isSignedIn() }

        _isRegistered = currentFirebaseUser.map { isRegistered() }

        observeUserAuthStateUseCase.execute(Any())
    }

    override fun isSignedIn(): Boolean {
        return (currentFirebaseUser.value as? Result.Success)?.data?.isSignedIn() == true
    }

    override fun isRegistered(): Boolean {
        return (currentFirebaseUser.value as? Result.Success)?.data?.isRegistered() == true
    }

    override fun observeSignedInUser(): LiveData<Boolean> {
        return _isSignedIn
    }

    override fun observeRegisteredUser(): LiveData<Boolean> {
        return _isRegistered
    }
}
