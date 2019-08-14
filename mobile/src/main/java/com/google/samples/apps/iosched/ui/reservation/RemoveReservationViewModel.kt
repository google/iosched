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

package com.google.samples.apps.iosched.ui.reservation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationActionUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestParameters
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import javax.inject.Inject

class RemoveReservationViewModel @Inject constructor(
    signInViewModelDelegate: SignInViewModelDelegate,
    private val loadUserSessionUseCase: LoadUserSessionUseCase,
    private val reservationActionUseCase: ReservationActionUseCase
) : ViewModel(), SignInViewModelDelegate by signInViewModelDelegate {

    private val _sessionId = MutableLiveData<SessionId>()

    private val loadUserSessionResult = loadUserSessionUseCase.observe()

    private val _userSession = loadUserSessionResult.map { result ->
        if (result is Result.Success) {
            result.data.userSession
        } else {
            null
        }
    }

    fun setSessionId(sessionId: SessionId) {
        _sessionId.value = sessionId
        loadUserSessionUseCase.execute(getUserId() to sessionId)
    }

    fun removeReservation() {
        val userId = getUserId() ?: return
        val sessionId = _sessionId.value ?: return
        val userSession = _userSession.value
        reservationActionUseCase.execute(
            ReservationRequestParameters(
                userId,
                sessionId,
                ReservationRequestAction.CancelAction(),
                userSession
            )
        )
    }
}
