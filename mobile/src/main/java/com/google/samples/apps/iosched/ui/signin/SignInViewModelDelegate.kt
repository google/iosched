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
import com.google.samples.apps.iosched.shared.data.signin.AuthenticatedUserInfo
import com.google.samples.apps.iosched.shared.di.ApplicationScope
import com.google.samples.apps.iosched.shared.di.IoDispatcher
import com.google.samples.apps.iosched.shared.di.MainDispatcher
import com.google.samples.apps.iosched.shared.di.ReservationEnabledFlag
import com.google.samples.apps.iosched.shared.domain.auth.ObserveUserAuthStateUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.NotificationsPrefIsShownUseCase
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.shared.util.tryOffer
import com.google.samples.apps.iosched.ui.signin.SignInNavigationAction.ShowNotificationPreferencesDialog
import com.google.samples.apps.iosched.util.WhileViewSubscribed
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

enum class SignInNavigationAction {
    RequestSignIn, RequestSignOut, ShowNotificationPreferencesDialog
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
    val userInfo: StateFlow<AuthenticatedUserInfo?>

    /**
     * Live updated value of the current firebase users image url
     */
    val currentUserImageUri: StateFlow<Uri?>

    /**
     * Emits Events when a sign-in event should be attempted or a dialog shown
     */
    val signInNavigationActions: Flow<SignInNavigationAction>

    /**
     * Emits whether or not to show reservations for the current user
     */
    val showReservations: StateFlow<Boolean>

    /**
     * Emit an Event on performSignInEvent to request sign-in
     */
    suspend fun emitSignInRequest()

    /**
     * Emit an Event on performSignInEvent to request sign-out
     */
    suspend fun emitSignOutRequest()

    val userId: Flow<String?>

    /**
     * Returns the current user ID or null if not available.
     */
    val userIdValue: String?

    val isUserSignedIn: StateFlow<Boolean>

    val isUserSignedInValue: Boolean

    val isUserRegistered: StateFlow<Boolean>

    val isUserRegisteredValue: Boolean
}

/**
 * Implementation of SignInViewModelDelegate that uses Firebase's auth mechanisms.
 */
internal class FirebaseSignInViewModelDelegate @Inject constructor(
    observeUserAuthStateUseCase: ObserveUserAuthStateUseCase,
    private val notificationsPrefIsShownUseCase: NotificationsPrefIsShownUseCase,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher,
    @ReservationEnabledFlag val isReservationEnabledByRemoteConfig: Boolean,
    @ApplicationScope val applicationScope: CoroutineScope
) : SignInViewModelDelegate {

    private val _signInNavigationActions = Channel<SignInNavigationAction>(Channel.CONFLATED)
    override val signInNavigationActions = _signInNavigationActions.receiveAsFlow()

    private val currentFirebaseUser: Flow<Result<AuthenticatedUserInfo?>> =
        observeUserAuthStateUseCase(Any()).map {
            if (it is Result.Error) {
                Timber.e(it.exception)
            }
            it
        }

    override val userInfo: StateFlow<AuthenticatedUserInfo?> = currentFirebaseUser.map {
        (it as? Success)?.data
    }.stateIn(applicationScope, WhileViewSubscribed, null)

    override val currentUserImageUri: StateFlow<Uri?> = userInfo.map {
        it?.getPhotoUrl()
    }.stateIn(applicationScope, WhileViewSubscribed, null)

    override val isUserSignedIn: StateFlow<Boolean> = userInfo.map {
        it?.isSignedIn() ?: false
    }.stateIn(applicationScope, WhileViewSubscribed, false)

    override val isUserRegistered: StateFlow<Boolean> = userInfo.map {
        it?.isRegistered() ?: false
    }.stateIn(applicationScope, WhileViewSubscribed, false)

    init {
        applicationScope.launch {
            userInfo.collect {
                if (notificationsPrefIsShownUseCase(Unit).data == false && isUserSignedInValue) {
                    _signInNavigationActions.tryOffer(ShowNotificationPreferencesDialog)
                }
            }
        }
    }

    override val showReservations: StateFlow<Boolean> = userInfo.map {
        (isUserRegisteredValue || !isUserSignedInValue) &&
            isReservationEnabledByRemoteConfig
    }.stateIn(applicationScope, WhileViewSubscribed, false)

    override suspend fun emitSignInRequest(): Unit = withContext(ioDispatcher) {
        // Refresh the notificationsPrefIsShown because it's used to indicate if the
        // notifications preference dialog should be shown
        notificationsPrefIsShownUseCase(Unit)
        _signInNavigationActions.tryOffer(SignInNavigationAction.RequestSignIn)
    }

    override suspend fun emitSignOutRequest(): Unit = withContext(mainDispatcher) {
        _signInNavigationActions.tryOffer(SignInNavigationAction.RequestSignOut)
    }

    override val isUserSignedInValue: Boolean
        get() = isUserSignedIn.value

    override val isUserRegisteredValue: Boolean
        get() = isUserRegistered.value

    override val userIdValue: String?
        get() = userInfo.value?.getUid()

    override val userId: StateFlow<String?>
        get() = userInfo.mapLatest { it?.getUid() }
            .stateIn(applicationScope, WhileViewSubscribed, null)
}
