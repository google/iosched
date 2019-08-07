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
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.di.IoDispatcher
import com.google.samples.apps.iosched.shared.di.MainDispatcher
import com.google.samples.apps.iosched.shared.domain.auth.ObserveUserAuthStateUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.NotificationsPrefIsShownUseCase
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.ui.signin.SignInEvent.RequestSignOut
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import timber.log.Timber
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
     * Stream of the current Firebase user
     */
    val currentFirebaseUser: Flow<Result<AuthenticatedUserInfo?>>

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
    fun emitSignOutRequest()

    fun observeSignedInUser(): LiveData<Boolean>

    fun isSignedIn(): Boolean

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
    override val currentFirebaseUser: Flow<Result<AuthenticatedUserInfo?>>

    private val _currentUserImageUri = MutableLiveData<Uri?>()
    override val currentUserImageUri: LiveData<Uri?>
        get() = _currentUserImageUri

    override val shouldShowNotificationsPrefAction = MediatorLiveData<Event<Boolean>>()

    private val _isSignedIn = MutableLiveData<Boolean>()

    private var authenticatedUserInfo: AuthenticatedUserInfo? = null

    private val notificationsPrefIsShown = MutableLiveData<Result<Boolean>>()

    init {
        currentFirebaseUser = observeUserAuthStateUseCase(Any()).map {
            if (it is Success) {
                authenticatedUserInfo = it.data
                _currentUserImageUri.value = it.data?.getPhotoUrl()
                _isSignedIn.value = it.data?.isSignedIn() ?: false
                refreshNotificationDialogShown(it)
            } else if (it is Result.Error) {
                Timber.e(it.exception)
            }

            it
        }

        shouldShowNotificationsPrefAction.addSource(notificationsPrefIsShown) {
            showNotificationPref()
        }
    }

    // Refresh whether the notification preference dialog has been shown
    private suspend fun refreshNotificationDialogShown(it: Success<AuthenticatedUserInfo?>) {
        if (it.data?.isSignedIn() == true) {
            notificationsPrefIsShown.value = null
            withContext(ioDispatcher) {
                val notificationShown = notificationsPrefIsShownUseCase(Unit)
                withContext(mainDispatcher) {
                    notificationsPrefIsShown.value = notificationShown
                }
            }
        }
    }

    private fun showNotificationPref() {
        val result = (notificationsPrefIsShown.value as? Success)?.data == false && isSignedIn()
        // Show the notification if the preference is not set and the event hasn't been handled yet.
        if (result && (shouldShowNotificationsPrefAction.value == null ||
                shouldShowNotificationsPrefAction.value?.hasBeenHandled == false)
        ) {
            shouldShowNotificationsPrefAction.value = Event(true)
        }
    }

    override suspend fun emitSignInRequest() {
        // Refresh the notificationsPrefIsShown because it's used to indicate if the
        // notifications preference dialog should be shown
        withContext(ioDispatcher) {
            val notificationShown = notificationsPrefIsShownUseCase(Unit)
            withContext(mainDispatcher) {
                notificationsPrefIsShown.value = notificationShown
                performSignInEvent.value = Event(SignInEvent.RequestSignIn)
            }
        }
    }

    override fun emitSignOutRequest() {
        performSignInEvent.postValue(Event(RequestSignOut))
    }

    override fun isSignedIn(): Boolean {
        return _isSignedIn.value == true
    }

    override fun observeSignedInUser(): LiveData<Boolean> {
        return _isSignedIn
    }

    override fun getUserId(): String? {
        return authenticatedUserInfo?.getUid()
    }
}
