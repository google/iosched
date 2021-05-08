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
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationActionUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestParameters
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result.Error
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.shared.util.cancelIfActive
import com.google.samples.apps.iosched.ui.messages.SnackbarMessage
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemoveReservationViewModel @Inject constructor(
    signInViewModelDelegate: SignInViewModelDelegate,
    private val loadUserSessionUseCase: LoadUserSessionUseCase,
    private val reservationActionUseCase: ReservationActionUseCase
) : ViewModel(), SignInViewModelDelegate by signInViewModelDelegate {

    private var loadUserSessionJob: Job? = null
    private val _sessionId = MutableLiveData<SessionId>()

    private val _userSession = MutableLiveData<UserSession>()

    private val _snackBarMessage = MutableLiveData<Event<SnackbarMessage>>()
    val snackBarMessage = _snackBarMessage

    fun setSessionId(sessionId: SessionId) {
        _sessionId.value = sessionId
        loadUserSessionJob.cancelIfActive()
        loadUserSessionJob = viewModelScope.launch {
            loadUserSessionUseCase(getUserId() to sessionId).collect { loadResult ->
                loadResult.data?.userSession?.let {
                    _userSession.value = it
                }
            }
        }
    }

    fun removeReservation() {
        val userId = getUserId() ?: return
        val sessionId = _sessionId.value ?: return
        val userSession = _userSession.value
        viewModelScope.launch {
            val result = reservationActionUseCase(
                ReservationRequestParameters(
                    userId,
                    sessionId,
                    ReservationRequestAction.CancelAction(),
                    userSession
                )
            )
            if (result is Error) {
                _snackBarMessage.value =
                    Event(
                        SnackbarMessage(
                            messageId = R.string.reservation_error,
                            longDuration = true
                        )
                    )
            }
        }
    }
}
