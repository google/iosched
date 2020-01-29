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
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.SessionType
import com.google.samples.apps.iosched.model.SpeakerId
import com.google.samples.apps.iosched.model.userdata.UserEvent
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCaseResult
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsUseCaseResult
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCaseLegacy
import com.google.samples.apps.iosched.shared.domain.users.ReservationActionUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.RequestAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.SwapAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestParameters
import com.google.samples.apps.iosched.shared.domain.users.StarEventAndNotifyUseCase
import com.google.samples.apps.iosched.shared.domain.users.StarEventParameter
import com.google.samples.apps.iosched.shared.domain.users.SwapRequestParameters
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.successOr
import com.google.samples.apps.iosched.shared.time.TimeProvider
import com.google.samples.apps.iosched.shared.util.NetworkUtils
import com.google.samples.apps.iosched.shared.util.SetIntervalLiveData.DefaultIntervalMapper
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.shared.util.setValueIfNew
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.reservation.RemoveReservationDialogParameters
import com.google.samples.apps.iosched.ui.sessioncommon.EventActions
import com.google.samples.apps.iosched.ui.sessioncommon.stringRes
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.google.samples.apps.iosched.util.combine
import java.util.UUID
import javax.inject.Inject
import org.threeten.bp.Duration
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import timber.log.Timber

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
    private val reservationActionUseCase: ReservationActionUseCase,
    getTimeZoneUseCase: GetTimeZoneUseCaseLegacy, // TODO(COROUTINES): Migrate
    private val snackbarMessageManager: SnackbarMessageManager,
    timeProvider: TimeProvider,
    private val networkUtils: NetworkUtils,
    private val analyticsHelper: AnalyticsHelper
) : ViewModel(), SessionDetailEventListener, EventActions,
    SignInViewModelDelegate by signInViewModelDelegate {

    private val loadUserSessionResult: MediatorLiveData<Result<LoadUserSessionUseCaseResult>> =
        loadUserSessionUseCase.observe()

    private val loadRelatedUserSessions: LiveData<Result<LoadUserSessionsUseCaseResult>>

    private val sessionTimeRelativeState: LiveData<TimeUtils.SessionRelativeTimeState>

    private val preferConferenceTimeZoneResult = MutableLiveData<Result<Boolean>>()

    val timeZoneId: LiveData<ZoneId>

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

    val showFeedbackButton: LiveData<Boolean>
    val timeUntilStart: LiveData<Duration?>
    val isReservationDisabled: LiveData<Boolean>
    private val _shouldShowStarInBottomNav = MediatorLiveData<Boolean>()
    val shouldShowStarInBottomNav: LiveData<Boolean> = _shouldShowStarInBottomNav

    private val sessionId = MutableLiveData<SessionId?>()

    private val _navigateToRemoveReservationDialogAction =
        MutableLiveData<Event<RemoveReservationDialogParameters>>()
    val navigateToRemoveReservationDialogAction: LiveData<Event<RemoveReservationDialogParameters>>
        get() = _navigateToRemoveReservationDialogAction

    private val _navigateToSwapReservationDialogAction =
        MediatorLiveData<Event<SwapRequestParameters>>()
    val navigateToSwapReservationDialogAction: LiveData<Event<SwapRequestParameters>>
        get() = _navigateToSwapReservationDialogAction

    private val _navigateToSessionAction = MutableLiveData<Event<SessionId>>()
    val navigateToSessionAction: LiveData<Event<SessionId>>
        get() = _navigateToSessionAction

    private val _navigateToSpeakerDetail = MutableLiveData<Event<SpeakerId>>()
    val navigateToSpeakerDetail: LiveData<Event<SpeakerId>>
        get() = _navigateToSpeakerDetail

    private val _navigateToSessionFeedbackAction = MutableLiveData<Event<SessionId>>()
    val navigateToSessionFeedbackAction: LiveData<Event<SessionId>>
        get() = _navigateToSessionFeedbackAction

    init {

        loadRelatedUserSessions = loadRelatedSessionUseCase.observe()

        getTimeZoneUseCase(Unit, preferConferenceTimeZoneResult)

        /* Wire observable dependencies */

        // If the user changes, load new data for them
        _userEvent.addSource(currentUserInfo) {
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

        _shouldShowStarInBottomNav.addSource(session) {
            _shouldShowStarInBottomNav.value = showStarInBottomNav()
        }
        _shouldShowStarInBottomNav.addSource(observeRegisteredUser()) {
            _shouldShowStarInBottomNav.value = showStarInBottomNav()
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

        val showInConferenceTimeZone = preferConferenceTimeZoneResult.map {
            (it as? Result.Success<Boolean>)?.data ?: true
        }

        timeZoneId = showInConferenceTimeZone.map { inConferenceTimeZone ->
            if (inConferenceTimeZone) {
                TimeUtils.CONFERENCE_TIMEZONE
            } else {
                ZoneId.systemDefault()
            }
        }

        /* Wire observables exposed for UI elements */

        // TODO this should also be called when session state is stale (b/74242921)
        // If there's a new session, update the relative time status (before, during, after...)
        sessionTimeRelativeState = session.map { currentSession ->
            TimeUtils.getSessionState(currentSession, ZonedDateTime.now())
        }

        showFeedbackButton = userEvent.combine(session) { userEvent, currentSession ->
            isSignedIn() &&
                !userEvent.isReviewed &&
                currentSession.type == SessionType.SESSION &&
                TimeUtils.getSessionState(currentSession, ZonedDateTime.now()) ==
                TimeUtils.SessionRelativeTimeState.AFTER
        }

        // Updates periodically with a special [IntervalLiveData]
        timeUntilStart = DefaultIntervalMapper.mapAtInterval(session, TEN_SECONDS) { session ->
            session?.startTime?.let { startTime ->
                val duration = Duration.between(timeProvider.now(), startTime)
                when (duration.toMinutes()) {
                    in 1..5 -> duration
                    else -> null
                }
            }
        }

        isReservationDisabled =
            DefaultIntervalMapper.mapAtInterval(session, SIXTY_SECONDS) { session ->
                session?.startTime?.let { startTime ->
                    // Only allow reservations if the sessions starts more than an hour from now
                    Duration.between(timeProvider.now(), startTime).toMinutes() <= 60
                }
            }

        /* Wiring dependencies for stars and reservation. */

        _snackBarMessage.addSource(starEventUseCase.observe()) { result ->
            // Show an error message if a star request fails
            if (result is Result.Error) {
                _snackBarMessage.postValue(Event(SnackbarMessage(R.string.event_star_error)))
            }
        }

        // Show an error message if a reservation request fails
        _snackBarMessage.addSource(reservationActionUseCase.observe()) {
            if (it is Result.Error) {
                _snackBarMessage.postValue(
                    Event(
                        SnackbarMessage(
                            messageId = R.string.reservation_error,
                            longDuration = true
                        )
                    )
                )
            }
        }
        // Show a message with the result of a reservation
        _snackBarMessage.addSource(loadUserSessionUseCase.observe()) {
            if (it is Result.Success) {
                it.data.userMessage?.type?.stringRes()?.let { messageId ->
                    // There is a message to display
                    snackbarMessageManager.addMessage(
                        SnackbarMessage(
                            messageId = messageId,
                            longDuration = true,
                            session = it.data.userSession.session,
                            requestChangeId = it.data.userMessage?.changeRequestId
                        )
                    )
                }
            }
        }

        _navigateToSwapReservationDialogAction.addSource(reservationActionUseCase.observe()) {
            (it?.successOr(null) as? SwapAction)?.let { swap ->
                _navigateToSwapReservationDialogAction.postValue(Event(swap.parameters))
            }
        }
    }

    private fun refreshUserSession() {
        val registrationDataReady = currentUserInfo.value?.isRegistrationDataReady()
        if (registrationDataReady == false) {
            // No registration information provided by [SignInViewModelDelegate] yet.
            Timber.d("No registration information yet, not refreshing")
            return
        }
        getSessionId()?.let {
            Timber.d("Refreshing data with session ID $it and user ${getUserId()}")
            loadUserSessionUseCase.execute(getUserId() to it)
        }
    }

    // TODO: write tests b/74611561
    fun setSessionId(newSessionId: SessionId?) {
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
        session.value?.let {
            if (it.hasVideo) {
                navigateToYouTubeAction.value = Event(it.youTubeUrl)
            }
        }
    }

    override fun onStarClicked() {
        val userEventSnapshot = userEvent.value ?: return
        val sessionSnapshot = session.value ?: return
        onStarClicked(UserSession(sessionSnapshot, userEventSnapshot))
    }

    override fun onReservationClicked() {
        if (!networkUtils.hasNetworkConnection()) {
            Timber.d("No network connection, ignoring reserve click.")
            _snackBarMessage.postValue(
                Event(
                    SnackbarMessage(
                        messageId = R.string.no_network_connection,
                        requestChangeId = UUID.randomUUID().toString()
                    )
                )
            )
            return
        }
        if (!isSignedIn()) {
            Timber.d("Showing Sign-in dialog after reserve click")
            _navigateToSignInDialogAction.value = Event(Unit)
            return
        }

        val userEventSnapshot = userEvent.value ?: return
        val sessionSnapshot = session.value ?: return
        val isReservationDisabledSnapshot = isReservationDisabled.value ?: return

        val userId = getUserId() ?: return

        if (userEventSnapshot.isReserved() ||
            userEventSnapshot.isWaitlisted() ||
            userEventSnapshot.isReservationPending() ||
            userEventSnapshot.isCancelPending() // Just in case
        ) {
            if (isReservationDisabledSnapshot) {
                _snackBarMessage.postValue(
                    Event(
                        SnackbarMessage(R.string.cancellation_denied_cutoff, longDuration = true)
                    )
                )
                analyticsHelper.logUiEvent(
                    sessionSnapshot.title, AnalyticsActions.RES_CANCEL_FAILED
                )
            } else {
                // Open the dialog to confirm if the user really wants to remove their reservation
                _navigateToRemoveReservationDialogAction.value = Event(
                    RemoveReservationDialogParameters(
                        userId,
                        sessionSnapshot.id,
                        sessionSnapshot.title
                    )
                )
                analyticsHelper.logUiEvent(sessionSnapshot.title, AnalyticsActions.RES_CANCEL)
            }
            return
        }
        if (isReservationDisabledSnapshot) {
            _snackBarMessage.postValue(
                Event(
                    SnackbarMessage(R.string.reservation_denied_cutoff, longDuration = true)
                )
            )
            analyticsHelper.logUiEvent(sessionSnapshot.title, AnalyticsActions.RESERVE_FAILED)
        } else {

            val userSessionResult = loadUserSessionResult.value
            val userSession = if (userSessionResult is Result.Success) {
                userSessionResult.data.userSession
            } else {
                null
            }
            reservationActionUseCase.execute(ReservationRequestParameters(
                userId,
                sessionSnapshot.id,
                RequestAction(),
                userSession
            ))
            analyticsHelper.logUiEvent(sessionSnapshot.title, AnalyticsActions.RESERVE)
        }
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

    override fun onFeedbackClicked() {
        val sessionId = getSessionId()
        if (sessionId != null) {
            _navigateToSessionFeedbackAction.postValue(Event(sessionId))
        }
    }

    /**
     * Returns the current session ID or null if not available.
     */
    private fun getSessionId(): SessionId? {
        return sessionId.value
    }

    private fun showStarInBottomNav(): Boolean {
        return observeRegisteredUser().value == true && session.value?.isReservable == true
    }
}

interface SessionDetailEventListener {

    fun onReservationClicked()

    fun onStarClicked()

    fun onLoginClicked()

    fun onSpeakerClicked(speakerId: SpeakerId)

    fun onFeedbackClicked()
}
