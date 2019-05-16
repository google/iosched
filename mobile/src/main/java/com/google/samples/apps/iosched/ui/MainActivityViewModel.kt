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
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.samples.apps.iosched.shared.domain.ar.LoadArDebugFlagUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadPinnedSessionsJsonUseCase
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.ui.ar.ArCoreAvailabilityLiveData
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.google.samples.apps.iosched.ui.theme.ThemedActivityDelegate
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

    private val _pinnedSessionsJson = MediatorLiveData<String>()
    val pinnedSessionsJson = _pinnedSessionsJson

    private val _canSignedInUserDemoAr = MediatorLiveData<Boolean>()
    val canSignedInUserDemoAr = _canSignedInUserDemoAr

    val arCoreAvailability = ArCoreAvailabilityLiveData(context)

    init {
        _pinnedSessionsJson.addSource(currentUserInfo) { user ->
            _pinnedSessionsJson.value = null
            val uid = user?.getUid() ?: return@addSource
            loadPinnedSessionsUseCase.execute(uid)
        }
        _pinnedSessionsJson.addSource(loadPinnedSessionsUseCase.observe()) { result ->
            val data = (result as? Result.Success)?.data ?: return@addSource
            _pinnedSessionsJson.value = data
        }
        _canSignedInUserDemoAr.addSource(currentUserInfo) {
            _canSignedInUserDemoAr.value = false
            loadArDebugFlagUseCase.execute(Unit)
        }
        _canSignedInUserDemoAr.addSource(loadArDebugFlagUseCase.observe()) {
            _canSignedInUserDemoAr.value = (it as? Result.Success)?.data == true
        }
    }

    fun onProfileClicked() {
        if (isSignedIn()) {
            _navigateToSignOutDialogAction.value = Event(Unit)
        } else {
            _navigateToSignInDialogAction.value = Event(Unit)
        }
    }
}
