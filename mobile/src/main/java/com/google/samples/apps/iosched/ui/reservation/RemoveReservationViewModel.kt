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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationActionUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestParameters
import com.google.samples.apps.iosched.shared.result.Result.Error
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.ui.messages.SnackbarMessage
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.google.samples.apps.iosched.util.WhileViewSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RemoveReservationViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    signInViewModelDelegate: SignInViewModelDelegate,
    private val loadUserSessionUseCase: LoadUserSessionUseCase,
    private val reservationActionUseCase: ReservationActionUseCase
) : ViewModel(), SignInViewModelDelegate by signInViewModelDelegate {

    private val sessionId: SessionId? = savedStateHandle.get<SessionId>("session_id")

    private val userIdStateFlow: StateFlow<String?> =
        userId.stateIn(viewModelScope, WhileViewSubscribed, null)

    val userSession: StateFlow<UserSession?> = userIdStateFlow.transform { userId ->
        if (userId != null && sessionId != null) {
            loadUserSessionUseCase(userId to sessionId).collect { loadResult ->
                emit(loadResult.data?.userSession)
            }
        }
    }.stateIn(viewModelScope, WhileViewSubscribed, null)

    private val _snackbarMessages = Channel<SnackbarMessage>(3, BufferOverflow.DROP_LATEST)
    val snackbarMessages: Flow<SnackbarMessage> =
        _snackbarMessages.receiveAsFlow().shareIn(viewModelScope, WhileViewSubscribed)

    fun removeReservation() {
        if (sessionId == null) return
        val userId = userIdStateFlow.value ?: return
        val userSession = userSession.value

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
                _snackbarMessages.send(
                    SnackbarMessage(
                        messageId = R.string.reservation_error,
                        longDuration = true
                    )
                )
            }
        }
    }
}
