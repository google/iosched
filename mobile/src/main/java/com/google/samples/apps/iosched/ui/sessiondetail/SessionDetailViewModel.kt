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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
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
import com.google.samples.apps.iosched.shared.di.ReservationEnabledFlag
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationActionUseCase
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.RequestAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestAction.SwapAction
import com.google.samples.apps.iosched.shared.domain.users.ReservationRequestParameters
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Error
import com.google.samples.apps.iosched.shared.result.Result.Loading
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.shared.result.successOr
import com.google.samples.apps.iosched.shared.time.TimeProvider
import com.google.samples.apps.iosched.shared.util.NetworkUtils
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.tryOffer
import com.google.samples.apps.iosched.ui.messages.SnackbarMessage
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.reservation.RemoveReservationDialogParameters
import com.google.samples.apps.iosched.ui.sessioncommon.stringRes
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailNavigationAction.NavigateToSessionFeedback
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailNavigationAction.NavigateToSignInDialogAction
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailNavigationAction.NavigateToSpeakerDetail
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailNavigationAction.NavigateToSwapReservationDialogAction
import com.google.samples.apps.iosched.ui.sessiondetail.SessionDetailNavigationAction.NavigateToYoutube
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.google.samples.apps.iosched.util.WhileViewSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import org.threeten.bp.Duration
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

private const val TEN_SECONDS = 10_000L
private const val SIXTY_SECONDS = 60_000L

/**
 * Loads [Session] data and exposes it to the session detail view.
 */
@HiltViewModel
class SessionDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val signInViewModelDelegate: SignInViewModelDelegate,
    private val loadUserSessionUseCase: LoadUserSessionUseCase,
    private val loadRelatedSessionUseCase: LoadUserSessionsUseCase,
    private val reservationActionUseCase: ReservationActionUseCase,
    getTimeZoneUseCase: GetTimeZoneUseCase,
    private val timeProvider: TimeProvider,
    private val networkUtils: NetworkUtils,
    private val analyticsHelper: AnalyticsHelper,
    private val snackbarMessageManager: SnackbarMessageManager,
    @ReservationEnabledFlag val isReservationEnabledByRemoteConfig: Boolean
) : ViewModel(),
    SessionDetailEventListener,
    SignInViewModelDelegate by signInViewModelDelegate {

    // TODO: remove hardcoded string when https://issuetracker.google.com/136967621 is available
    private val sessionId = savedStateHandle.get<SessionId>("session_id")

    // Start observing the user ID right away from the SignInViewModelDelegate
    private val userIdFlow: StateFlow<Result<String?>> = userId.map { Success(it) }
        .stateIn(viewModelScope, started = Eagerly, initialValue = Loading)

    // Session & UserData are updated with new user IDs
    private val sessionUserData = userIdFlow.transformLatest { userId ->
        if (sessionId != null) {
            emitAll(loadUserSessionUseCase(userId.data to sessionId))
        } else {
            Timber.e("Session ID is null")
            emit(Error(Exception("Session not found")))
        }
    }.stateIn(viewModelScope, WhileViewSubscribed, Loading)
    // WhileViewSubscribed cancels the subscription to loadUserSessionUseCase when it's not needed.

    init {
        // SIDE EFFECTS: show Snackbar when there's a message in user data
        sessionUserData.onEach { result ->
            result.data?.let { userData ->
                userData.userMessage?.type?.stringRes()?.let { messageId ->
                    snackbarMessageManager.addMessage(
                        SnackbarMessage(
                            messageId = messageId,
                            longDuration = true,
                            session = userData.userSession.session,
                            requestChangeId = userData.userMessage?.changeRequestId
                        )
                    )
                }
            }
        }.launchIn(viewModelScope)
    }

    // UserSession is exposed as a StateFlow to the view. It's extracted from sessionUserData.
    val userSession: StateFlow<UserSession?> = sessionUserData.transformLatest { result ->
        result.data?.userSession?.let { emit(it) }
    }.stateIn(viewModelScope, started = WhileViewSubscribed, initialValue = null)

    val userEvent: StateFlow<UserEvent?> = userSession.transform { userSession ->
        userSession?.userEvent?.let { emit(it) }
    }.stateIn(viewModelScope, started = WhileViewSubscribed, initialValue = null)

    val session: StateFlow<Session?> = userSession.transform { userSession ->
        userSession?.session?.let { emit(it) }
    }.stateIn(viewModelScope, started = WhileViewSubscribed, initialValue = null)

    // Related user sessions are exposed as a StateFlow and depend on the current session
    // and user (for favorites).
    val relatedUserSessions: StateFlow<Result<List<UserSession>>> =
        session.combineTransform(userIdFlow) { session, userId ->
            session?.relatedSessions?.let { related ->
                emitAll(loadRelatedSessionUseCase(userId.data to related))
            }
        }.stateIn(viewModelScope, started = WhileViewSubscribed, initialValue = Loading)

    // Exposed to the view to decide whether the feedback button should be visible or not.
    val showFeedbackButton: StateFlow<Boolean> =
        sessionUserData.mapLatest { sessionUser ->
            val currentSession = sessionUser.data?.userSession?.session
            val userEvent = sessionUser.data?.userSession?.userEvent
            isUserSignedInValue &&
                userEvent?.isReviewed == false &&
                currentSession?.type == SessionType.SESSION &&
                (
                    TimeUtils.getSessionState(currentSession, ZonedDateTime.now()) ==
                        TimeUtils.SessionRelativeTimeState.AFTER
                    )
        }.stateIn(viewModelScope, started = WhileViewSubscribed, initialValue = false)

    // Exposed to the view to show the  Duration until session start, if applicable.
    val timeUntilStart: LiveData<Duration?> = session.transformLatest { session ->
        while (true) { // Emit periodically
            session?.startTime?.let { startTime ->
                val duration = Duration.between(timeProvider.now(), startTime)
                when (duration.toMinutes()) {
                    in 1..5 -> emit(duration)
                    else -> emit(null)
                }
            }
            delay(TEN_SECONDS)
        }
    }.asLiveData() // TODO: Used by Data Binding https://issuetracker.google.com/184935697

    // Exposed to the view to prevent reservations.
    val isReservationDeniedByCutoff = session.transformLatest { session ->
        while (true) { // Emit periodically
            // Only allow reservations if the sessions starts more than an hour from now
            checkReservationDeniedByCutoff(session)?.let { isDisabled ->
                emit(isDisabled)
            }
            delay(SIXTY_SECONDS)
        }
    }.asLiveData() // TODO: Used by Data Binding https://issuetracker.google.com/184935697

    private fun checkReservationDeniedByCutoff(session: Session?): Boolean? {
        return session?.startTime?.let { startTime ->
            Duration.between(timeProvider.now(), startTime).toMinutes() <= 60
        }
    }

    // Show the star in bottom nav instead of the FAB if the FAB shows the reservation button.
    val shouldShowStarInBottomNav = session.combine(isUserRegistered) { session, isRegistered ->
        isRegistered && session?.isReservable == true
    }

    // Exposed to the view to indicate whether the session can be reserved.
    val isReservable: StateFlow<Boolean> = session.map {
        it?.isReservable == true && isReservationEnabledByRemoteConfig
    }.stateIn(viewModelScope, WhileViewSubscribed, false)

    // Exposed to the view as a StateFlow but it's a one-shot operation.
    val timeZoneId = flow<ZoneId> {
        if (getTimeZoneUseCase(Unit).successOr(true)) {
            emit(TimeUtils.CONFERENCE_TIMEZONE)
        } else {
            emit(ZoneId.systemDefault())
        }
    }.stateIn(viewModelScope, WhileViewSubscribed, TimeUtils.CONFERENCE_TIMEZONE)

    // SIDE EFFECTS: Navigation actions
    private val _navigationActions = Channel<SessionDetailNavigationAction>(capacity = CONFLATED)
    // Exposed with receiveAsFlow to make sure that only one observer receives updates.
    val navigationActions = _navigationActions.receiveAsFlow()

    /**
     * Methods called by the UI
     */

    fun onPlayVideo() {
        session.value?.let {
            if (it.hasVideo) {
                _navigationActions.tryOffer(NavigateToYoutube(it.youTubeUrl))
            }
        }
    }

    // TODO this needs to be implemented to satisfy the interface, but star handling is being moved
    // to a delegate.
    override fun onStarClicked() {}

    override fun onReservationClicked() {
        if (!networkUtils.hasNetworkConnection()) {
            Timber.d("No network connection, ignoring reserve click.")
            snackbarMessageManager.addMessage(
                SnackbarMessage(
                    messageId = R.string.no_network_connection,
                    requestChangeId = UUID.randomUUID().toString()
                )
            )
            return
        }
        if (!isUserSignedInValue) {
            Timber.d("Showing Sign-in dialog after reserve click")
            _navigationActions.tryOffer(NavigateToSignInDialogAction)
            return
        }

        val userEventSnapshot = userEvent.value ?: return
        val sessionSnapshot = session.value ?: return
        val isReservationDeniedByCutoffSnapshot =
            checkReservationDeniedByCutoff(sessionSnapshot) ?: return

        val userId = userIdFlow.value.data ?: return

        if (userEventSnapshot.isReserved() ||
            userEventSnapshot.isWaitlisted() ||
            userEventSnapshot.isReservationPending() ||
            userEventSnapshot.isCancelPending() // Just in case
        ) {
            if (isReservationDeniedByCutoffSnapshot) {
                snackbarMessageManager.addMessage(
                    SnackbarMessage(R.string.cancellation_denied_cutoff, longDuration = true)
                )
                analyticsHelper.logUiEvent(
                    sessionSnapshot.title, AnalyticsActions.RES_CANCEL_FAILED
                )
            } else {
                // Open the dialog to confirm if the user really wants to remove their reservation
                _navigationActions.tryOffer(
                    SessionDetailNavigationAction.RemoveReservationDialogAction(
                        RemoveReservationDialogParameters(
                            userId,
                            sessionSnapshot.id,
                            sessionSnapshot.title
                        )
                    )
                )
                analyticsHelper.logUiEvent(sessionSnapshot.title, AnalyticsActions.RES_CANCEL)
            }
            return
        }
        if (isReservationDeniedByCutoffSnapshot) {
            snackbarMessageManager.addMessage(
                SnackbarMessage(R.string.reservation_denied_cutoff, longDuration = true)
            )
            analyticsHelper.logUiEvent(sessionSnapshot.title, AnalyticsActions.RESERVE_FAILED)
        } else {
            // New reservation

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
                    is Success -> {
                        val reservationActionResult = result.data
                        if (reservationActionResult is SwapAction) {
                            _navigationActions.tryOffer(
                                NavigateToSwapReservationDialogAction(
                                    reservationActionResult.parameters
                                )
                            )
                        }
                    }
                    is Error -> {
                        snackbarMessageManager.addMessage(
                            SnackbarMessage(R.string.reservation_error, longDuration = true)
                        )
                    }
                    Loading -> throw IllegalStateException()
                }
            }
            analyticsHelper.logUiEvent(sessionSnapshot.title, AnalyticsActions.RESERVE)
        }
    }

    override fun onLoginClicked() {
        if (!isUserSignedInValue) {
            Timber.d("Showing Sign-in dialog")
            _navigationActions.tryOffer(NavigateToSignInDialogAction)
        }
    }

    override fun onSpeakerClicked(speakerId: SpeakerId) {
        _navigationActions.tryOffer(NavigateToSpeakerDetail(speakerId))
    }

    override fun onFeedbackClicked() {
        session.value?.id?.let { sessionId ->
            _navigationActions.tryOffer(NavigateToSessionFeedback(sessionId))
        }
    }
}

interface SessionDetailEventListener {

    fun onReservationClicked()

    fun onStarClicked()

    fun onLoginClicked()

    fun onSpeakerClicked(speakerId: SpeakerId)

    fun onFeedbackClicked()
}
