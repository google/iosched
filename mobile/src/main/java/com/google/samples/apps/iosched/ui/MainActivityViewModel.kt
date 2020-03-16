/*
 * Copyright 2019 Google LLC
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

package com.google.samples.apps.iosched.ui

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.google.samples.apps.iosched.shared.domain.ar.LoadArDebugFlagUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadPinnedSessionsJsonUseCase
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.ui.ar.ArCoreAvailabilityLiveData
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.google.samples.apps.iosched.ui.theme.ThemedActivityDelegate
import kotlinx.coroutines.flow.collect
import javax.inject.Inject

class MainActivityViewModel @Inject constructor(
    signInViewModelDelegate: SignInViewModelDelegate,
    themedActivityDelegate: ThemedActivityDelegate,
    loadPinnedSessionsUseCase: LoadPinnedSessionsJsonUseCase,
    loadArDebugFlagUseCase: LoadArDebugFlagUseCase,
    context: Context
) : ViewModel(),
    SignInViewModelDelegate by signInViewModelDelegate,
    ThemedActivityDelegate by themedActivityDelegate {

    private val _navigateToSignInDialogAction = MutableLiveData<Event<Unit>>()
    val navigateToSignInDialogAction: LiveData<Event<Unit>>
        get() = _navigateToSignInDialogAction

    private val _navigateToSignOutDialogAction = MutableLiveData<Event<Unit>>()
    val navigateToSignOutDialogAction: LiveData<Event<Unit>>
        get() = _navigateToSignOutDialogAction

    val pinnedSessionsJson: LiveData<String> = currentUserInfo.switchMap { user ->
        val uid = user?.getUid()
        liveData {
            if (uid != null) {
                loadPinnedSessionsUseCase(uid).collect { result ->
                    if (result is Result.Success) {
                        emit(result.data)
                    }
                }
            } else {
                emit("")
            }
        }
    }

    val canSignedInUserDemoAr: LiveData<Boolean> = currentUserInfo.switchMap {
        liveData {
            emit(false)
            loadArDebugFlagUseCase(Unit).collect { result ->
                if (result is Result.Success) {
                    emit(result.data)
                }
            }
        }
    }

    val arCoreAvailability = ArCoreAvailabilityLiveData(context)

    fun onProfileClicked() {
        if (isSignedIn()) {
            _navigateToSignOutDialogAction.value = Event(Unit)
        } else {
            _navigateToSignInDialogAction.value = Event(Unit)
        }
    }
}
