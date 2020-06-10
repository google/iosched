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

package com.google.samples.apps.iosched.ui.sessioncommon

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.di.ApplicationScope
import com.google.samples.apps.iosched.shared.di.MainDispatcher
import com.google.samples.apps.iosched.shared.domain.users.StarEventAndNotifyUseCase
import com.google.samples.apps.iosched.shared.domain.users.StarEventParameter
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * A delegate providing common functionality for displaying a list of events and responding to
 * actions performed on them.
 */
interface EventActionsViewModelDelegate : EventActions {
    val navigateToEventAction: LiveData<Event<SessionId>>
    val navigateToSignInDialogAction: LiveData<Event<Unit>>
    val snackBarMessage: LiveData<Event<SnackbarMessage>>
}

class DefaultEventActionsViewModelDelegate @Inject constructor(
    signInViewModelDelegate: SignInViewModelDelegate,
    private val starEventUseCase: StarEventAndNotifyUseCase,
    private val snackbarMessageManager: SnackbarMessageManager,
    @ApplicationScope private val externalScope: CoroutineScope,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher
) : EventActionsViewModelDelegate, SignInViewModelDelegate by signInViewModelDelegate {

    private val _navigateToEventAction = MutableLiveData<Event<SessionId>>()
    override val navigateToEventAction: LiveData<Event<SessionId>>
        get() = _navigateToEventAction

    private val _navigateToSignInDialogAction = MutableLiveData<Event<Unit>>()
    override val navigateToSignInDialogAction: LiveData<Event<Unit>>
        get() = _navigateToSignInDialogAction

    private val _snackBarMessage = MediatorLiveData<Event<SnackbarMessage>>()
    override val snackBarMessage: LiveData<Event<SnackbarMessage>>
        get() = _snackBarMessage

    override fun openEventDetail(id: SessionId) {
        _navigateToEventAction.value = Event(id)
    }

    override fun onStarClicked(userSession: UserSession) {
        if (!isSignedIn()) {
            Timber.d("Showing Sign-in dialog after star click")
            _navigateToSignInDialogAction.value = Event(Unit)
            return
        }
        val newIsStarredState = !userSession.userEvent.isStarred

        // Update the snackbar message optimistically.
        val stringResId = if (newIsStarredState) {
            R.string.event_starred
        } else {
            R.string.event_unstarred
        }
        snackbarMessageManager.addMessage(
            SnackbarMessage(
                messageId = stringResId,
                actionId = R.string.dont_show,
                requestChangeId = UUID.randomUUID().toString()
            )
        )

        externalScope.launch(mainDispatcher) {
            getUserId()?.let {
                val result = starEventUseCase(
                    StarEventParameter(
                        it, userSession.copy(
                            userEvent = userSession.userEvent.copy(isStarred = newIsStarredState)
                        )
                    )
                )
                // Show an error message if a star request fails
                if (result is Result.Error) {
                    _snackBarMessage.value = Event(SnackbarMessage(R.string.event_star_error))
                }
            }
        }
    }
}
