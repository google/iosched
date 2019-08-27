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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions.USER_ATTENDEE_DIALOG_NOT_SHOWN
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.data.prefs.UserIsAttendee.NO_ANSWER
import com.google.samples.apps.iosched.shared.domain.settings.GetUserIsAttendeeSettingUseCase
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.data
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Checks for user settings and shows dialogs to set them if needed.
 */
class MainActivityViewModel @Inject constructor(
    getUserIsAttendeeSettingUseCase: GetUserIsAttendeeSettingUseCase,
    private val analyticsHelper: AnalyticsHelper
) : ViewModel() {

    private val _navigateToUserAttendeeDialogAction = MutableLiveData<Event<Unit>>()
    val navigateToUserAttendeeDialogAction: LiveData<Event<Unit>>
        get() = _navigateToUserAttendeeDialogAction

    init {
        viewModelScope.launch {
            // Decide if we need to show the "user attendee" dialog.
            getUserIsAttendeeSettingUseCase(Unit).data.let {
                when (it) {
                    NO_ANSWER, null -> _navigateToUserAttendeeDialogAction.value = Event(Unit)
                    else -> analyticsHelper.logUiEvent(
                        it.toString(), USER_ATTENDEE_DIALOG_NOT_SHOWN
                    )
                }
            }
        }
    }
}
