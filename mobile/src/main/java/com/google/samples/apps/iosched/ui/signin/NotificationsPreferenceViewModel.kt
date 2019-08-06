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
import com.google.samples.apps.iosched.shared.domain.prefs.NotificationsPrefSaveActionUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.NotificationsPrefShownActionUseCase
import com.google.samples.apps.iosched.shared.result.Event
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the dialog to show notifications preference
 */
class NotificationsPreferenceViewModel @Inject constructor(
    private val notificationsPrefShownActionUseCase: NotificationsPrefShownActionUseCase,
    private val notificationsPrefSaveActionUseCase: NotificationsPrefSaveActionUseCase
) : ViewModel() {

    private val _installAppEvent = MutableLiveData<Event<Unit>>()
    private val _dismissEvent = MutableLiveData<Event<Unit>>()

    val installAppEvent: LiveData<Event<Unit>>
        get() = _installAppEvent

    val dismissDialogEvent: LiveData<Event<Unit>>
        get() = _dismissEvent

    fun onYesClicked() {
        viewModelScope.launch {
            notificationsPrefSaveActionUseCase(true)
        }
        _dismissEvent.value = Event(Unit)
    }

    fun onInstallClicked() {
        _installAppEvent.value = Event(Unit)
    }
    fun onNoClicked() {
        viewModelScope.launch {
            notificationsPrefSaveActionUseCase(false)
        }
        _dismissEvent.value = Event(Unit)
    }

    fun onDismissed() {
        viewModelScope.launch {
            notificationsPrefShownActionUseCase(true)
        }
    }
}
