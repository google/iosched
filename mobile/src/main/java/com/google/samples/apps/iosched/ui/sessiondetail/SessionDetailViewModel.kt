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

package com.google.samples.apps.iosched.ui.sessiondetail

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.support.annotation.VisibleForTesting
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCaseResult
import com.google.samples.apps.iosched.shared.domain.users.StarEventParameter
import com.google.samples.apps.iosched.shared.domain.users.StarEventUseCase
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.util.SetIntervalLiveData
import com.google.samples.apps.iosched.util.time.DefaultTime
import org.threeten.bp.Duration
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.login.LoginViewModelPlugin
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import javax.inject.Inject

private const val TEN_SECONDS = 10_000L

/**
 * Loads [Session] data and exposes it to the session detail view.
 */
class SessionDetailViewModel @Inject constructor(
    loginViewModelPlugin: LoginViewModelPlugin,
    private val loadUserSessionUseCase: LoadUserSessionUseCase,
    private val starEventUseCase: StarEventUseCase
) : ViewModel(), LoginViewModelPlugin by loginViewModelPlugin {

    private val loadUserSessionResult: MediatorLiveData<Result<LoadUserSessionUseCaseResult>>
    private val sessionState: LiveData<TimeUtils.SessionState>

    private val _errorMessage = MediatorLiveData<Event<String>>()
    val errorMessage : LiveData<Event<String>>
        get() = _errorMessage

    private val _snackBarMessage = MediatorLiveData<Event<SnackbarMessage>>()
    val snackBarMessage : LiveData<Event<SnackbarMessage>>
        get() = _snackBarMessage

    private var firebaseUserSourceAttached = false

    /**
     * Event to navigate to the sign in Dialog. We only want to consume the event, so the
     * Boolean value isn't used actually.
     */
    private val _navigateToSignInDialogAction = MutableLiveData<Event<Boolean>>()
    val navigateToSignInDialogAction: LiveData<Event<Boolean>>
        get() = _navigateToSignInDialogAction

    val navigateToYouTubeAction = MutableLiveData<Event<String>>()

    val session: LiveData<Session?>
    val userEvent: LiveData<UserEvent?>
    val showRateButton: LiveData<Boolean>
    val hasPhoto: LiveData<Boolean>
    val isPlayable: LiveData<Boolean>
    val hasSpeakers: LiveData<Boolean>
    val hasRelated: LiveData<Boolean>
    val timeUntilStart: LiveData<Duration?>

    init {
        loadUserSessionResult = loadUserSessionUseCase.observe()

        //TODO: Deal with error SessionNotFoundException
        session = loadUserSessionResult.map { (it as? Result.Success)?.data?.userSession?.session }

        userEvent = loadUserSessionResult.map {
            (it as? Result.Success)?.data?.userSession?.userEvent
        }

        // TODO this should also be called when session state is stale (b/74242921)
        sessionState = Transformations.map(session, { currentSession ->
            TimeUtils.getSessionState(currentSession, ZonedDateTime.now())
        })

        hasPhoto = Transformations.map(session, { currentSession ->
            !currentSession?.photoUrl.isNullOrEmpty()
        })

        isPlayable = Transformations.map(session, { currentSession ->
            checkPlayable(currentSession)
        })

        showRateButton = Transformations.map(sessionState, { currentState ->
            currentState == TimeUtils.SessionState.AFTER
        })

        hasSpeakers = Transformations.map(session, { currentSession ->
            currentSession?.speakers?.isNotEmpty() ?: false
        })

        hasRelated = Transformations.map(session, { currentSession ->
            currentSession?.relatedSessions?.isNotEmpty() ?: false
        })

        timeUntilStart = SetIntervalLiveData.mapAtInterval(session, TEN_SECONDS) { currentSession ->
            currentSession?.startTime?.let { startTime ->
                val duration = Duration.between(DefaultTime.now(), startTime)
                val minutes = duration.toMinutes()
                when (minutes) {
                    in 1..5 -> duration
                    else -> null
                }
            }
        }
        // Show an error message if a star request fails
        _snackBarMessage.addSource(starEventUseCase.observe()) { result ->
            // Show a snackbar message on error.
            if (result is Result.Error) {
                _snackBarMessage.postValue(Event(SnackbarMessage(R.string.event_star_error)))
            }
        }
    }

    fun setSessionId(sessionId: String) {
        // Refresh the list of user sessions if the user is updated.
        if (!firebaseUserSourceAttached) {
            firebaseUserSourceAttached = true
            loadUserSessionResult.addSource(currentFirebaseUser) {
                Timber.d("Loading user session with user ${(it as? Result.Success)?.data?.getUid()}")
                loadSessionById(sessionId)
            }
        }
    }

    // TODO: write tests b/74611561
    @VisibleForTesting
    fun loadSessionById(sessionId: String) {
        session.value ?: loadUserSessionUseCase.execute((getUserId() ?: "tempUser") to sessionId)
    }

    /**
     * Called by the UI when play button is clicked
     */
    fun onPlayVideo() {
        val currentSession = session.value
        if (checkPlayable(currentSession)) {
            navigateToYouTubeAction.value = Event(requireSession().youTubeUrl)
        }
    }

    fun onStarClicked(currentUserEvent: UserEvent) {
        if (!isLoggedIn()) {
            Timber.d("Showing Sign-in dialog after star click")
            _navigateToSignInDialogAction.value = Event(true)
            return
        }
        val newIsStarredState = !currentUserEvent.isStarred

        // Update the snackbar message optimistically.
        val snackbarMessage = if(newIsStarredState) {
            SnackbarMessage(R.string.event_starred, R.string.got_it)
        } else {
            SnackbarMessage(R.string.event_unstarred)
        }
        _snackBarMessage.postValue(Event(snackbarMessage))

        getUserId()?.let {
            starEventUseCase.execute(StarEventParameter(it,
                    currentUserEvent.copy(isStarred = newIsStarredState)))
        }
    }

    /**
     * Returns the current user ID or null if not available.
     */
    private fun getUserId() : String? {
        val user = currentFirebaseUser.value
        return (user as? Result.Success)?.data?.getUid()
    }

    private fun requireSession(): Session {
        return session.value ?: throw IllegalStateException("Session should not be null")
    }

    fun checkPlayable(currentSession: Session?): Boolean {
        return currentSession != null && currentSession.youTubeUrl.isNotBlank()
    }
}
