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

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.SessionType
import com.google.samples.apps.iosched.model.SpeakerId
import com.google.samples.apps.iosched.model.userdata.UserEvent
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.di.DefaultDispatcher
import com.google.samples.apps.iosched.shared.di.ReservationEnabledFlag
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationActionUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.RequestAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.SwapAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestParameters
import com.google.samples.apps.iosched.shared.domain.users.StarEventAndNotifyUseCase
import com.google.samples.apps.iosched.shared.domain.users.StarEventParameter
import com.google.samples.apps.iosched.shared.domain.users.SwapRequestParameters
import com.google.samples.apps.iosched.shared.result.Event
import com.google.samples.apps.iosched.shared.result.Result.Error
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.shared.result.successOr
import com.google.samples.apps.iosched.shared.time.TimeProvider
import com.google.samples.apps.iosched.shared.util.IntervalMediatorLiveData
import com.google.samples.apps.iosched.shared.util.NetworkUtils
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.cancelIfActive
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.shared.util.setValueIfNew
import com.google.samples.apps.iosched.ui.SnackbarMessage
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.reservation.RemoveReservationDialogParameters
import com.google.samples.apps.iosched.ui.sessioncommon.EventActions
import com.google.samples.apps.iosched.ui.sessioncommon.stringRes
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.google.samples.apps.iosched.util.combine
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.threeten.bp.Duration
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.util.UUID

private const val TEN_SECONDS = 10_000L
private const val SIXTY_SECONDS = 60_000L

/**
 * Loads [Session] data and exposes it to the session detail view.
 */
@ExperimentalCoroutinesApi
class SessionDetailViewModel @ViewModelInject constructor(
    private val signInViewModelDelegate: SignInViewModelDelegate,
    private val loadUserSessionUseCase: LoadUserSessionUseCase,
    private val loadRelatedSessionUseCase: LoadUserSessionsUseCase,
    private val starEventUseCase: StarEventAndNotifyUseCase,
    private val reservationActionUseCase: ReservationActionUseCase,
    getTimeZoneUseCase: GetTimeZoneUseCase,
    private val snackbarMessageManager: SnackbarMessageManager,
    timeProvider: TimeProvider,
    private val networkUtils: NetworkUtils,
    private val analyticsHelper: AnalyticsHelper,
    @ReservationEnabledFlag val isReservationEnabledByRemoteConfig: Boolean,
    @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
) : ViewModel(), SessionDetailEventListener, EventActions,
    SignInViewModelDelegate by signInViewModelDelegate {

    // Keeps track of the coroutine that listens for a user session
    private var loadUserSessionJob: Job? = null

    // Keeps track of the coroutine that listens for related user sessions
    private var loadRelatedSessionJob: Job? = null

    private val _relatedUserSessions = MediatorLiveData<List<UserSession>>()
    val relatedUserSessions: LiveData<List<UserSession>>
        get() = _relatedUserSessions

    private val reservationActionResult = MutableLiveData<ReservationRequestAction>()

    private val _errorMessage = MediatorLiveData<Event<String>>()
    val errorMessage: LiveData<Event<String>> = _errorMessage

    private val _snackBarMessage = MediatorLiveData<Event<SnackbarMessage>>()
    val snackBarMessage: LiveData<Event<SnackbarMessage>> = _snackBarMessage

    private val _navigateToSignInDialogAction = MutableLiveData<Event<Unit>>()
    val navigateToSignInDialogAction: LiveData<Event<Unit>> = _navigateToSignInDialogAction

    val navigateToYouTubeAction = MutableLiveData<Event<String>>()

    private val _session = MediatorLiveData<Session>()
    val session: LiveData<Session> = _session

    private val _userEvent = MediatorLiveData<UserEvent>()
    val userEvent: LiveData<UserEvent> = _userEvent

    val showFeedbackButton: LiveData<Boolean> =
        userEvent.combine(session) { userEvent, currentSession ->
            isSignedIn() &&
                !userEvent.isReviewed &&
                currentSession.type == SessionType.SESSION &&
                TimeUtils.getSessionState(currentSession, ZonedDateTime.now()) ==
                TimeUtils.SessionRelativeTimeState.AFTER
        }

    // Updates periodically with a special [IntervalMediatorLiveData]
    val timeUntilStart = IntervalMediatorLiveData(
        source = session, dispatcher = defaultDispatcher, intervalMs = TEN_SECONDS
    ) { session ->
        session?.startTime?.let { startTime ->
            val duration = Duration.between(timeProvider.now(), startTime)
            when (duration.toMinutes()) {
                in 1..5 -> duration
                else -> null
            }
        }
    }
    val isReservationDeniedByCutoff =
        IntervalMediatorLiveData(
            source = session, dispatcher = defaultDispatcher, intervalMs = SIXTY_SECONDS
        ) { session ->
            session?.startTime?.let { startTime ->
                // Only allow reservations if the sessions starts more than an hour from now
                Duration.between(timeProvider.now(), startTime).toMinutes() <= 60
            }
        }
    private val _shouldShowStarInBottomNav = MediatorLiveData<Boolean>().apply {
        addSource(session) {
            value = showStarInBottomNav()
        }
        addSource(observeRegisteredUser()) {
            value = showStarInBottomNav()
        }
    }
    val shouldShowStarInBottomNav: LiveData<Boolean> = _shouldShowStarInBottomNav

    private val sessionId = MutableLiveData<SessionId?>()

    private val showInConferenceTimeZone: LiveData<Boolean> = liveData {
        emit(getTimeZoneUseCase(Unit).successOr(true))
    }
    val timeZoneId: LiveData<ZoneId> = showInConferenceTimeZone.map { inConferenceTimeZone ->
        if (inConferenceTimeZone) {
            TimeUtils.CONFERENCE_TIMEZONE
        } else {
            ZoneId.systemDefault()
        }
    }

    private val _navigateToRemoveReservationDialogAction =
        MutableLiveData<Event<RemoveReservationDialogParameters>>()
    val navigateToRemoveReservationDialogAction: LiveData<Event<RemoveReservationDialogParameters>>
        get() = _navigateToRemoveReservationDialogAction

    private val _navigateToSwapReservationDialogAction =
        MediatorLiveData<Event<SwapRequestParameters>>().apply {
            addSource(reservationActionResult) {
                (it as? SwapAction)?.let { swap ->
                    value = Event(swap.parameters)
                }
            }
        }
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

    val isReservable: LiveData<Boolean> = session.map {
        it.isReservable && isReservationEnabledByRemoteConfig
    }

    init {
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
            listenForUserSessionChanges(it)
        }
    }

    private fun listenForUserSessionChanges(sessionId: SessionId) {
        // Cancels listening for the old session
        loadUserSessionJob.cancelIfActive()
        // Cancels listening for old related user sessions
        loadRelatedSessionJob.cancelIfActive()

        loadUserSessionJob = viewModelScope.launch {
            loadUserSessionUseCase(getUserId() to sessionId).collect {
                val result = it.data ?: return@collect
                val session = result.userSession.session
                // At this point the result is guaranteed as Result.Success

                _session.value = session
                _userEvent.value = result.userSession.userEvent
                result.userMessage?.type?.stringRes()?.let { messageId ->
                    // There is a message to display
                    snackbarMessageManager.addMessage(
                        SnackbarMessage(
                            messageId = messageId,
                            longDuration = true,
                            session = result.userSession.session,
                            requestChangeId = result.userMessage?.changeRequestId
                        )
                    )
                }
                val related = session.relatedSessions
                if (related.isNotEmpty()) {
                    listenForRelatedSessions(related)
                }
            }
        }
    }

    private suspend fun listenForRelatedSessions(related: Set<SessionId>) {
        // if this fails, we don't want to propagate the error and
        // stop listening for user session changes
        supervisorScope {
            loadRelatedSessionJob = launch {
                loadRelatedSessionUseCase(getUserId() to related).collect {
                    it.data?.let { userSessionResult ->
                        _relatedUserSessions.value = userSessionResult
                    }
                }
            }
        }
    }

    // TODO: write tests b/74611561
    fun setSessionId(newSessionId: SessionId?) {
        sessionId.setValueIfNew(newSessionId)
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
        val isReservationDeniedByCutoffSnapshot = isReservationDeniedByCutoff.value ?: return

        val userId = getUserId() ?: return

        if (userEventSnapshot.isReserved() ||
            userEventSnapshot.isWaitlisted() ||
            userEventSnapshot.isReservationPending() ||
            userEventSnapshot.isCancelPending() // Just in case
        ) {
            if (isReservationDeniedByCutoffSnapshot) {
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
        if (isReservationDeniedByCutoffSnapshot) {
            _snackBarMessage.postValue(
                Event(
                    SnackbarMessage(R.string.reservation_denied_cutoff, longDuration = true)
                )
            )
            analyticsHelper.logUiEvent(sessionSnapshot.title, AnalyticsActions.RESERVE_FAILED)
        } else {
            val userSession = UserSession(sessionSnapshot, userEventSnapshot)

            viewModelScope.launch {
                val result = reservationActionUseCase(
                    ReservationRequestParameters(
                        userId,
                        sessionSnapshot.id,
                        RequestAction(),
                        userSession
                    )
                )
                when (result) {
                    is Success -> reservationActionResult.value = result.data
                    is Error -> {
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

        viewModelScope.launch {
            getUserId()?.let {
                val result = starEventUseCase(
                    StarEventParameter(
                        it, userSession.copy(
                            userEvent = userSession.userEvent.copy(isStarred = newIsStarredState)
                        )
                    )
                )
                // Show an error message if a star request fails
                if (result is Error) {
                    _snackBarMessage.value = Event(SnackbarMessage(R.string.event_star_error))
                }
            }
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
