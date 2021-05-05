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

package com.google.samples.apps.iosched.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.iosched.model.ConferenceDay
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.domain.RefreshConferenceDataUseCase
import com.google.samples.apps.iosched.shared.domain.prefs.ScheduleUiHintsShownUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.ConferenceDayIndexer
import com.google.samples.apps.iosched.shared.domain.sessions.LoadScheduleUserSessionsParameters
import com.google.samples.apps.iosched.shared.domain.sessions.LoadScheduleUserSessionsResult
import com.google.samples.apps.iosched.shared.domain.sessions.LoadScheduleUserSessionsUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.ObserveConferenceDataUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.fcm.TopicSubscriber
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Error
import com.google.samples.apps.iosched.shared.result.Result.Success
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.shared.result.successOr
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.tryOffer
import com.google.samples.apps.iosched.ui.messages.SnackbarMessage
import com.google.samples.apps.iosched.ui.messages.SnackbarMessageManager
import com.google.samples.apps.iosched.ui.schedule.ScheduleNavigationAction.ShowScheduleUiHints
import com.google.samples.apps.iosched.ui.sessioncommon.stringRes
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.google.samples.apps.iosched.util.WhileViewSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.BufferOverflow.DROP_LATEST
import kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Lazily
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.threeten.bp.ZoneId
import javax.inject.Inject

/**
 * Loads data and exposes it to the view.
 * By annotating the constructor with [@Inject], Dagger will use that constructor when needing to
 * create the object, so defining a [@Provides] method for this class won't be needed.
 */
@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val loadScheduleUserSessionsUseCase: LoadScheduleUserSessionsUseCase,
    signInViewModelDelegate: SignInViewModelDelegate,
    scheduleUiHintsShownUseCase: ScheduleUiHintsShownUseCase,
    topicSubscriber: TopicSubscriber,
    private val snackbarMessageManager: SnackbarMessageManager,
    getTimeZoneUseCase: GetTimeZoneUseCase,
    private val refreshConferenceDataUseCase: RefreshConferenceDataUseCase,
    observeConferenceDataUseCase: ObserveConferenceDataUseCase,
) : ViewModel(),
    SignInViewModelDelegate by signInViewModelDelegate {

    // Exposed to the view as a StateFlow but it's a one-shot operation.
    val timeZoneId = flow<ZoneId> {
        if (getTimeZoneUseCase(Unit).successOr(true)) {
            emit(TimeUtils.CONFERENCE_TIMEZONE)
        } else {
            emit(ZoneId.systemDefault())
        }
    }.stateIn(viewModelScope, Lazily, TimeUtils.CONFERENCE_TIMEZONE)

    val isConferenceTimeZone: StateFlow<Boolean> = timeZoneId.mapLatest { zoneId ->
        TimeUtils.isConferenceTimeZone(zoneId)
    }.stateIn(viewModelScope, Lazily, true)

    private lateinit var dayIndexer: ConferenceDayIndexer

    // Used to re-run flows on command
    private val refreshSignal = MutableSharedFlow<Unit>()
    // Used to run flows on init and also on command
    private val loadDataSignal: Flow<Unit> = flow {
        emit(Unit)
        emitAll(refreshSignal)
    }

    // Event coming from repository indicating data should be refreshed
    init {
        viewModelScope.launch {
            observeConferenceDataUseCase(Unit).collect {
                refreshUserSessions()
            }
        }
    }

    // Latest user ID
    private val currentUserId = userId.stateIn(viewModelScope, WhileViewSubscribed, null)

    // Refresh sessions when needed and when the user changes
    private val loadSessionsResult: StateFlow<Result<LoadScheduleUserSessionsResult>> =
        loadDataSignal.combineTransform(currentUserId) { _, userId ->
            emitAll(
                loadScheduleUserSessionsUseCase(
                    LoadScheduleUserSessionsParameters(userId)
                )
            )
        }
            .onEach {
                // Side effect: show error messages coming from LoadScheduleUserSessionsUseCase
                if (it is Error) {
                    _errorMessage.tryOffer(it.exception.message ?: "Error")
                }
                // Side effect: show snackbar if the result contains a message
                if (it is Success) {
                    it.data.userMessage?.type?.stringRes()?.let { messageId ->
                        // There is a message to display:
                        snackbarMessageManager.addMessage(
                            SnackbarMessage(
                                messageId = messageId,
                                longDuration = true,
                                session = it.data.userMessageSession,
                                requestChangeId = it.data.userMessage?.changeRequestId
                            )
                        )
                    }
                }
            }
            .stateIn(viewModelScope, WhileViewSubscribed, Result.Loading)

    val isLoading: StateFlow<Boolean> = loadSessionsResult.mapLatest {
        it == Result.Loading
    }.stateIn(viewModelScope, WhileViewSubscribed, true)

    // Expose new UI data when loadSessionsResult changes
    val scheduleUiData: StateFlow<ScheduleUiData> =
        loadSessionsResult.combineTransform(timeZoneId) { sessions, timeZone ->
            sessions.data?.let { data ->
                dayIndexer = data.dayIndexer
                emit(
                    ScheduleUiData(
                        list = data.userSessions,
                        dayIndexer = data.dayIndexer,
                        timeZoneId = timeZone
                    )
                )
            }
        }.stateIn(viewModelScope, WhileViewSubscribed, ScheduleUiData())

    private val _swipeRefreshing = MutableStateFlow(false)
    val swipeRefreshing: StateFlow<Boolean> = _swipeRefreshing

    /** Flows for Actions and Events **/

    // SIDE EFFECTS: Error messages
    // Guard against too many error messages by limiting to 3, keeping the oldest.
    private val _errorMessage = Channel<String>(1, DROP_LATEST)
    val errorMessage: Flow<String> =
        _errorMessage.receiveAsFlow().shareIn(viewModelScope, WhileViewSubscribed)

    // SIDE EFFECTS: Navigation actions
    private val _navigationActions = Channel<ScheduleNavigationAction>(capacity = Channel.CONFLATED)
    // Exposed with receiveAsFlow to make sure that only one observer receives updates.
    val navigationActions = _navigationActions.receiveAsFlow()

    /** Show hints for the schedule if they haven't been shown yet */
    init {
        viewModelScope.launch {
            scheduleUiHintsShownUseCase(Unit).successOr(false).let { scheduleHintsShown ->
                if (!scheduleHintsShown) {
                    _navigationActions.tryOffer(ShowScheduleUiHints)
                }
            }
        }
    }

    // Flags used to indicate if the "scroll to now" feature has been used already.
    var userHasInteracted = false

    // Flow describing which item to scroll to automatically.
    // Using a MutableSharedFlow so a new value can be emitted from a user event and so
    // the values are not replayed.
    private val currentEventIndex = MutableSharedFlow<Int>(
        extraBufferCapacity = 1,
        onBufferOverflow = DROP_OLDEST
    )
    val scrollToEvent: SharedFlow<ScheduleScrollEvent> =
        loadSessionsResult.combineTransform(currentEventIndex) { result, currentEventIndex ->
            if (userHasInteracted) {
                // Setting smoothScroll to false as it's an unnecessary delay.
                emit(ScheduleScrollEvent(currentEventIndex, smoothScroll = false))
            } else {
                val index =
                    (result as? Success)?.data?.firstUnfinishedSessionIndex
                        ?: return@combineTransform
                if (index != -1) {
                    emit(ScheduleScrollEvent(index))
                }
            }
        }.shareIn(viewModelScope, WhileViewSubscribed, replay = 0) // Don't replay on rotation

    init {
        // Subscribe user to schedule updates
        topicSubscriber.subscribeToScheduleUpdates()
    }

    fun onSwipeRefresh() {
        viewModelScope.launch {
            // Ask repository to fetch new data
            _swipeRefreshing.emit(true)
            refreshConferenceDataUseCase(Any())
            _swipeRefreshing.emit(false)
        }
    }

    private fun refreshUserSessions() {
        refreshSignal.tryEmit(Unit)
    }

    fun scrollToStartOfDay(day: ConferenceDay) {
        currentEventIndex.tryEmit(dayIndexer.positionForDay(day))
    }
}

data class ScheduleUiData(
    val list: List<UserSession>? = null,
    val timeZoneId: ZoneId? = null,
    val dayIndexer: ConferenceDayIndexer? = null
)

data class ScheduleScrollEvent(val targetPosition: Int, val smoothScroll: Boolean = false)
