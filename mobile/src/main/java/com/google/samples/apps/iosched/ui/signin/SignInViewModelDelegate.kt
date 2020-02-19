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

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.di.IoDispatcher
import com.google.samples.apps.iosched.shared.di.MainDispatcher
import com.google.samples.apps.iosched.shared.domain.auth.ObserveUserAuthStateUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.NotificationsPrefIsShownUseCase
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.ui.signin.SignInEvent.RequestSignOut
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber

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
    val currentUserInfo: LiveData<AuthenticatedUserInfo?>

    /**
     * Live updated value of the current firebase users image url
     */
    val currentUserImageUri: LiveData<Uri?>

    /**
     * Emits Events when a sign-in event should be attempted
     */
    val performSignInEvent: MutableLiveData<Event<SignInEvent>>

    /**
     * Emits an non-null Event when the dialog to ask the user notifications preference should be
     * shown.
     */
    val shouldShowNotificationsPrefAction: LiveData<Event<Boolean>>

    /**
     * Emit an Event on performSignInEvent to request sign-in
     */
    suspend fun emitSignInRequest()

    /**
     * Emit an Event on performSignInEvent to request sign-out
     */
    suspend fun emitSignOutRequest()

    fun observeSignedInUser(): LiveData<Boolean>

    fun observeRegisteredUser(): LiveData<Boolean>

    fun isSignedIn(): Boolean

    fun isRegistered(): Boolean

    /**
     * Returns the current user ID or null if not available.
     */
    fun getUserId(): String?
}

/**
 * Implementation of SignInViewModelDelegate that uses Firebase's auth mechanisms.
 */
internal class FirebaseSignInViewModelDelegate @Inject constructor(
    observeUserAuthStateUseCase: ObserveUserAuthStateUseCase,
    private val notificationsPrefIsShownUseCase: NotificationsPrefIsShownUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher
) : SignInViewModelDelegate {

    override val performSignInEvent = MutableLiveData<Event<SignInEvent>>()

    private val currentFirebaseUser: Flow<Result<AuthenticatedUserInfo?>> =
        observeUserAuthStateUseCase(Any()).map {
            if (it is Result.Error) {
                Timber.e(it.exception)
            }
            it
        }

    override val currentUserInfo: LiveData<AuthenticatedUserInfo?> = currentFirebaseUser.map {
        (it as? Success)?.data
    }.asLiveData()

    private val notificationsPrefIsShown = currentUserInfo.switchMap {
        liveData {
            emit(notificationsPrefIsShownUseCase(Unit))
        }
    }

    override val currentUserImageUri: LiveData<Uri?> = currentUserInfo.map {
        it?.getPhotoUrl()
    }

    private val isRegistered: LiveData<Boolean> = currentUserInfo.map {
        it?.isRegistered() ?: false
    }

    private val isSignedIn: LiveData<Boolean> = currentUserInfo.map {
        it?.isSignedIn() ?: false
    }

    override val shouldShowNotificationsPrefAction = notificationsPrefIsShown.map {
        showNotificationPref(it)
    }

    private fun showNotificationPref(
        notificationsPrefIsShownValue: Result<Boolean>
    ): Event<Boolean> {
        val shouldShowDialog = notificationsPrefIsShownValue.data == false && isSignedIn()
        // Show the notification if the preference is not set and the event hasn't been handled yet.
        if (shouldShowDialog && (shouldShowNotificationsPrefAction.value == null ||
                shouldShowNotificationsPrefAction.value?.hasBeenHandled == false)
        ) {
            return Event(true)
        }
        return Event(false)
    }

    override suspend fun emitSignInRequest() = withContext(ioDispatcher) {
        // Refresh the notificationsPrefIsShown because it's used to indicate if the
        // notifications preference dialog should be shown
        notificationsPrefIsShownUseCase(Unit)
        withContext(mainDispatcher) {
            performSignInEvent.value = Event(SignInEvent.RequestSignIn)
        }
    }

    override suspend fun emitSignOutRequest() = withContext(mainDispatcher) {
        performSignInEvent.value = Event(RequestSignOut)
    }

    override fun isSignedIn(): Boolean = isSignedIn.value == true

    override fun isRegistered(): Boolean = isRegistered.value == true

    override fun observeSignedInUser(): LiveData<Boolean> = isSignedIn

    override fun observeRegisteredUser(): LiveData<Boolean> = isRegistered

    override fun getUserId(): String? {
        return currentUserInfo.value?.getUid()
    }
}
