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

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.SpeakerId
import com.google.samples.apps.iosched.model.userdata.UserEvent
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCaseResult
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsUseCaseResult
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.domain.users.StarEventAndNotifyUseCase
import com.google.samples.apps.iosched.shared.domain.users.StarEventParameter
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.time.TimeProvider
import com.google.samples.apps.iosched.shared.util.SetIntervalLiveData.DefaultIntervalMapper
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.shared.util.setValueIfNew
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.sessioncommon.EventActions
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import org.threeten.bp.Duration
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import javax.inject.Inject

private const val TEN_SECONDS = 10_000L
private const val SIXTY_SECONDS = 60_000L

/**
 * Loads [Session] data and exposes it to the session detail view.
 */
class SessionDetailViewModel @Inject constructor(
    private val signInViewModelDelegate: SignInViewModelDelegate,
    private val loadUserSessionUseCase: LoadUserSessionUseCase,
    private val loadRelatedSessionUseCase: LoadUserSessionsUseCase,
    private val starEventUseCase: StarEventAndNotifyUseCase,
    getTimeZoneUseCase: GetTimeZoneUseCase,
    timeProvider: TimeProvider,
    private val analyticsHelper: AnalyticsHelper
) : ViewModel(), SessionDetailEventListener, EventActions,
    SignInViewModelDelegate by signInViewModelDelegate {

    private val loadUserSessionResult: MediatorLiveData<Result<LoadUserSessionUseCaseResult>>

    private val loadRelatedUserSessions: LiveData<Result<LoadUserSessionsUseCaseResult>>

    private val sessionTimeRelativeState: LiveData<TimeUtils.SessionRelativeTimeState>

    private val preferConferenceTimeZoneResult = liveData { emit(getTimeZoneUseCase(Unit)) }
    private val showInConferenceTimeZone = preferConferenceTimeZoneResult.map {
        (it as? Result.Success<Boolean>)?.data ?: true
    }
    val timeZoneId = showInConferenceTimeZone.map { inConferenceTimeZone ->
        if (inConferenceTimeZone) {
            TimeUtils.CONFERENCE_TIMEZONE
        } else {
            ZoneId.systemDefault()
        }
    }

    private val _errorMessage = MediatorLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>>
        get() = _errorMessage

    private val _snackBarMessage = MediatorLiveData<Event<SnackbarMessage>>()
    val snackBarMessage: LiveData<Event<SnackbarMessage>>
        get() = _snackBarMessage

    private val _navigateToSignInDialogAction = MutableLiveData<Event<Unit>>()
    val navigateToSignInDialogAction: LiveData<Event<Unit>>
        get() = _navigateToSignInDialogAction

    val navigateToYouTubeAction = MutableLiveData<Event<String>>()

    private val _session = MediatorLiveData<Session>()
    val session: LiveData<Session>
        get() = _session

    private val _userEvent = MediatorLiveData<UserEvent>()
    val userEvent: LiveData<UserEvent>
        get() = _userEvent

    private val _relatedUserSessions = MediatorLiveData<List<UserSession>>()
    val relatedUserSessions: LiveData<List<UserSession>>
        get() = _relatedUserSessions

    val showRateButton: LiveData<Boolean>
    val hasPhotoOrVideo: LiveData<Boolean>
    val isPlayable: LiveData<Boolean>
    val hasSpeakers: LiveData<Boolean>
    val hasRelated: LiveData<Boolean>
    val timeUntilStart: LiveData<Duration?>

    private val sessionId = MutableLiveData<SessionId?>()

    private val _navigateToSessionAction = MutableLiveData<Event<SessionId>>()
    val navigateToSessionAction: LiveData<Event<SessionId>>
        get() = _navigateToSessionAction

    private val _navigateToSpeakerDetail = MutableLiveData<Event<SpeakerId>>()
    val navigateToSpeakerDetail: LiveData<Event<SpeakerId>>
        get() = _navigateToSpeakerDetail

    init {
        loadUserSessionResult = loadUserSessionUseCase.observe()

        loadRelatedUserSessions = loadRelatedSessionUseCase.observe()

        /* Wire observable dependencies */

        // If the user changes, load new data for them
        _userEvent.addSource(currentFirebaseUser) {
            Timber.d("CurrentFirebaseUser changed, refreshing")
            refreshUserSession()
        }

        // If the session ID changes, load new data for it
        _session.addSource(sessionId) {
            Timber.d("SessionId changed, refreshing")
            refreshUserSession()
        }

        /* Wire result dependencies */

        // If there's a new result with data, update the session
        _session.addSource(loadUserSessionResult) {
            (loadUserSessionResult.value as? Result.Success)?.data?.userSession?.session?.let {
                _session.value = it
            }
        }

        // If there's a new result with data, update the UserEvent
        _userEvent.addSource(loadUserSessionResult) {
            (loadUserSessionResult.value as? Result.Success)?.data?.userSession?.userEvent?.let {
                _userEvent.value = it
            }
        }

        // If there's a new Session, then load any related sessions
        loadRelatedUserSessions.addSource(loadUserSessionResult) {
            (loadUserSessionResult.value as? Result.Success)?.data?.userSession?.session?.let {
                val related = it.relatedSessions
                if (related.isNotEmpty()) {
                    loadRelatedSessionUseCase.execute(getUserId() to related)
                }
            }
        }

        _relatedUserSessions.addSource(loadRelatedUserSessions) {
            (loadRelatedUserSessions.value as? Result.Success)?.data?.let {
                _relatedUserSessions.value = it.userSessions
            }
        }

        /* Wire observables exposed for UI elements */

        // TODO this should also be called when session state is stale (b/74242921)
        // If there's a new session, update the relative time status (before, during, after...)
        sessionTimeRelativeState = session.map { currentSession ->
            TimeUtils.getSessionState(currentSession, ZonedDateTime.now())
        }

        hasPhotoOrVideo = session.map { currentSession ->
            !currentSession?.photoUrl.isNullOrEmpty() || !currentSession?.youTubeUrl.isNullOrEmpty()
        }

        isPlayable = session.map { currentSession ->
            currentSession.hasVideo
        }

        showRateButton = sessionTimeRelativeState.map { currentState ->
            // TODO: uncomment when rate session logic is hooked up
            // currentState == TimeUtils.SessionRelativeTimeState.AFTER
            false
        }

        hasSpeakers = session.map { currentSession ->
            currentSession.speakers.isNotEmpty()
        }

        hasRelated = session.map { currentSession ->
            currentSession.relatedSessions.isNotEmpty()
        }

        // Updates periodically with a special [IntervalLiveData]
        timeUntilStart = DefaultIntervalMapper.mapAtInterval(session, TEN_SECONDS) { session ->
            session?.startTime?.let { startTime ->
                val duration = Duration.between(timeProvider.now(), startTime)
                val minutes = duration.toMinutes()
                when (minutes) {
                    in 1..5 -> duration
                    else -> null
                }
            }
        }

        /* Wiring dependencies for stars and reservation. */

        _snackBarMessage.addSource(starEventUseCase.observe()) { result ->
            // Show an error message if a star request fails
            if (result is Result.Error) {
                _snackBarMessage.postValue(Event(SnackbarMessage(R.string.event_star_error)))
            }
        }
    }

    private fun refreshUserSession() {
        getSessionId()?.let {
            Timber.d("Refreshing data with session ID $it and user ${getUserId()}")
            loadUserSessionUseCase.execute(getUserId() to it)
        }
    }

    // TODO: write tests b/74611561
    fun setSessionId(newSessionId: SessionId?) {
        if (newSessionId == null) {
            Timber.e("Session ID is null")
            return
        }
        sessionId.setValueIfNew(newSessionId)
    }

    override fun onCleared() {
        // Clear subscriptions that might be leaked or that will not be used in the future.
        loadUserSessionUseCase.onCleared()
    }

    /**
     * Called by the UI when play button is clicked
     */
    fun onPlayVideo() {
        val currentSession = session.value
        if (currentSession?.hasVideo == true) {
            navigateToYouTubeAction.value = Event(requireSession().youTubeUrl)
        }
    }

    override fun onStarClicked() {

        val userEventSnapshot = userEvent.value ?: return
        val sessionSnapshot = session.value ?: return
        onStarClicked(UserSession(sessionSnapshot, userEventSnapshot))
    }

    override fun onLoginClicked() {
        if (!isSignedIn()) {
            Timber.d("Showing Sign-in dialog")
            _navigateToSignInDialogAction.value = Event(Unit)
        }
    }

    // copied from SchedVM, TODO refactor
    override fun openEventDetail(id: SessionId) {
        _navigateToSessionAction.value = Event(id)
    }

    override fun onStarClicked(userSession: UserSession) {
        if (!isSignedIn()) {
            Timber.d("Showing Sign-in dialog after star click")
            _navigateToSignInDialogAction.value = Event(Unit)
            return
        }
        val newIsStarredState = !userSession.userEvent.isStarred

        val sessionTitle = session.value?.title
        if (sessionTitle != null && newIsStarredState) {
            analyticsHelper.logUiEvent(sessionTitle, AnalyticsActions.STARRED)
        } else {
            Timber.d("Session title is null, can't log")
        }

        // Update the snackbar message optimistically.
        val snackbarMessage = if (newIsStarredState) {
            SnackbarMessage(R.string.event_starred, R.string.got_it)
        } else {
            SnackbarMessage(R.string.event_unstarred)
        }
        _snackBarMessage.postValue(Event(snackbarMessage))

        getUserId()?.let {
            starEventUseCase.execute(
                StarEventParameter(
                    it,
                    userSession.copy(
                        userEvent = userSession.userEvent.copy(isStarred = newIsStarredState))
                )
            )
        }
    }

    override fun onSpeakerClicked(speakerId: SpeakerId) {
        _navigateToSpeakerDetail.postValue(Event(speakerId))
    }

    /**
     * Returns the current session ID or null if not available.
     */
    private fun getSessionId(): SessionId? {
        return sessionId.value
    }

    private fun requireSession(): Session {
        return session.value ?: throw IllegalStateException("Session should not be null")
    }
}

interface SessionDetailEventListener {

    fun onStarClicked()

    fun onLoginClicked()

    fun onSpeakerClicked(speakerId: SpeakerId)
}
