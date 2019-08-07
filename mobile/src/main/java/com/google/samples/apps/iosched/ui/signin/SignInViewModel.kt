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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.iosched.shared.result.Event
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for *both* the sign in & sign out dialogs.
 */
class SignInViewModel @Inject constructor(
    signInViewModelDelegate: SignInViewModelDelegate
) : ViewModel(), SignInViewModelDelegate by signInViewModelDelegate {

    private val _dismissDialogAction = MutableLiveData<Event<Unit>>()
    val dismissDialogAction: LiveData<Event<Unit>>
        get() = _dismissDialogAction

    fun onSignIn() {
        Timber.d("Sign in requested")
        viewModelScope.launch {
            emitSignInRequest()
        }
    }

    fun onSignOut() {
        Timber.d("Sign out requested")
        emitSignOutRequest()
    }

    fun onCancel() {
        _dismissDialogAction.value = Event(Unit)
    }
}
