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

package com.google.samples.apps.iosched.ui.dialogs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.iosched.shared.domain.prefs.UserIsAttendeePrefSaveActionUseCase
import com.google.samples.apps.iosched.shared.result.Event
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the dialog to mark user as in-person attendee.
 */
class AttendeePreferenceViewModel @Inject constructor(
    private val userIsAttendeePrefSaveActionUseCase: UserIsAttendeePrefSaveActionUseCase
) : ViewModel() {

    private val _dismissEvent = MutableLiveData<Event<Unit>>()
    val dismissDialogEvent: LiveData<Event<Unit>>
        get() = _dismissEvent

    fun onInPersonClicked() {
        savePreference(true)
    }

    fun onRemotelyClicked() {
        savePreference(false)
    }

    private fun savePreference(attendee: Boolean) {
        viewModelScope.launch {
            userIsAttendeePrefSaveActionUseCase(attendee)
            _dismissEvent.value = Event(Unit)
        }
    }
}
