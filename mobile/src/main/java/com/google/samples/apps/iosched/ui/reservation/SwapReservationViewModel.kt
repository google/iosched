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

package com.google.samples.apps.iosched.ui.reservation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.samples.apps.iosched.shared.domain.users.SwapActionUseCase
import com.google.samples.apps.iosched.shared.domain.users.SwapRequestParameters
import com.google.samples.apps.iosched.shared.result.Event
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for the dialog when the user is about to remove the reservation.
 */
class SwapReservationViewModel @Inject constructor(
    private val swapActionUseCase: SwapActionUseCase
) : ViewModel(), SwapReservationListener {

    var userId: String? = null

    var fromId: String? = null

    var fromTitle: String? = null

    var toId: String? = null

    var toTitle: String? = null

    /**
     * Event to dismiss the opening dialog. We only want to consume the event, the
     * Boolean value isn't used.
     */
    private val _dismissDialogAction = MutableLiveData<Event<Boolean>>()
    val dismissDialogAction: LiveData<Event<Boolean>>
        get() = _dismissDialogAction

    override fun onSwapClicked() {
        _dismissDialogAction.value = Event(true)

        val immutableUserId = userId
        val immutableFromId = fromId
        val immutableFromTitle = fromTitle
        val immutableToId = toId
        val immutableToTitle = toId
        // The user should be logged in at this point.
        if (immutableUserId == null || immutableFromId == null ||
            immutableFromTitle == null || immutableToId == null || immutableToTitle == null
        ) {
            Timber.e("Tried to replace reservations with a null user or IDs")
            return
        }
        swapActionUseCase.execute(
            SwapRequestParameters(
                immutableUserId,
                immutableFromId,
                immutableFromTitle,
                immutableToId,
                immutableToTitle
            )
        )
    }

    override fun onCancelClicked() {
        _dismissDialogAction.value = Event(true)
    }
}

interface SwapReservationListener {

    fun onCancelClicked()

    fun onSwapClicked()
}
