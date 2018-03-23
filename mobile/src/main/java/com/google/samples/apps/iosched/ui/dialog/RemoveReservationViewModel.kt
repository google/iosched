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

package com.google.samples.apps.iosched.ui.dialog

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.google.samples.apps.iosched.shared.domain.users.ReservationActionUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.CANCEL
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestParameters
import com.google.samples.apps.iosched.shared.result.Event
import javax.inject.Inject

/**
 * ViewModel for the dialog when the user is about to remove the reservation.
 */
class RemoveReservationViewModel @Inject constructor(
    private val reservationActionUseCase: ReservationActionUseCase
) : ViewModel(), RemoveReservationListener {

    /**
     * Event to dismiss the opening dialog. We only want to consume the event, the
     * Boolean value isn't used.
     */
    private val _dismissDialogAction = MutableLiveData<Event<Boolean>>()
    val dismissDialogAction: LiveData<Event<Boolean>>
        get() = _dismissDialogAction

    override fun onRemoveClicked(userId: String, sessionId: String) {
        _dismissDialogAction.value = Event(true)
        // The user should be logged in at this point.
        reservationActionUseCase.execute(ReservationRequestParameters(userId, sessionId, CANCEL))
    }

    override fun onCancelClicked() {
        _dismissDialogAction.value = Event(true)
    }
}

interface RemoveReservationListener {

    fun onRemoveClicked(userId: String, sessionId: String)

    fun onCancelClicked()
}
