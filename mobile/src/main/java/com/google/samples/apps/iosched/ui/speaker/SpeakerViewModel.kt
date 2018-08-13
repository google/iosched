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

package com.google.samples.apps.iosched.ui.speaker

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.samples.apps.iosched.model.Speaker
import com.google.samples.apps.iosched.model.SpeakerId
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsUseCaseResult
import com.google.samples.apps.iosched.shared.domain.speakers.LoadSpeakerUseCase
import com.google.samples.apps.iosched.shared.domain.speakers.LoadSpeakerUseCaseResult
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.ui.sessioncommon.EventActionsViewModelDelegate
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import javax.inject.Inject

/**
 * Loads a [Speaker] and their sessions, handles event actions.
 */
class SpeakerViewModel @Inject constructor(
    private val loadSpeakerUseCase: LoadSpeakerUseCase,
    private val loadSpeakerSessionsUseCase: LoadUserSessionsUseCase,
    signInViewModelDelegate: SignInViewModelDelegate,
    private val eventActionsViewModelDelegate: EventActionsViewModelDelegate,
    private val analyticsHelper: AnalyticsHelper
) : ViewModel(),
    SignInViewModelDelegate by signInViewModelDelegate,
    EventActionsViewModelDelegate by eventActionsViewModelDelegate {

    private val loadSpeakerUseCaseResult = MutableLiveData<Result<LoadSpeakerUseCaseResult>>()

    private val loadSpeakerUserSessions: LiveData<Result<LoadUserSessionsUseCaseResult>>

    private val _speaker = MediatorLiveData<Speaker>()
    val speaker: LiveData<Speaker>
        get() = _speaker

    private val _speakerUserSessions = MediatorLiveData<List<UserSession>>()
    val speakerUserSessions: LiveData<List<UserSession>>
        get() = _speakerUserSessions

    val hasProfileImage: LiveData<Boolean> = _speaker.map {
        !it?.imageUrl.isNullOrEmpty()
    }

    init {
        loadSpeakerUserSessions = loadSpeakerSessionsUseCase.observe()

        // If there's a new result with data, update speaker
        _speaker.addSource(loadSpeakerUseCaseResult) {
            (loadSpeakerUseCaseResult.value as? Result.Success)?.data?.let {
                _speaker.value = it.speaker
            }
        }

        // Also load their sessions
        loadSpeakerUserSessions.addSource(loadSpeakerUseCaseResult) {
            (loadSpeakerUseCaseResult.value as? Result.Success)?.data?.let {
                loadSpeakerSessionsUseCase.execute(getUserId() to it.sessionIds)
            }
        }

        // When their sessions load, update speakerUserSessions
        _speakerUserSessions.addSource(loadSpeakerUserSessions) {
            (loadSpeakerUserSessions.value as? Result.Success)?.data?.let {
                _speakerUserSessions.value = it.userSessions
            }
        }
    }

    /**
     * Provides the speaker ID which initiates all data loading.
     */
    fun setSpeakerId(id: SpeakerId) {
        loadSpeakerUseCase(id, loadSpeakerUseCaseResult)
    }

    /**
     * Clear subscriptions that might be leaked or that will not be used in the future.
     */
    override fun onCleared() {
        loadSpeakerSessionsUseCase.onCleared()
    }

    override fun onStarClicked(userSession: UserSession) {
        eventActionsViewModelDelegate.onStarClicked(userSession)

        // Only recording stars, not un-stars.  Since userEvent.isStarred reflects pre-click value,
        // checking for "old value starred, new value unstarred", in which case we don't record.
        if (userSession.userEvent.isStarred) {
            return
        }

        // Find the session
        val sessionId = userSession.userEvent.id
        val sessions = speakerUserSessions.value

        if (sessions != null) {
            val session = sessions.first { it.session.id == sessionId }.session
            analyticsHelper.logUiEvent(session.title, AnalyticsActions.STARRED)
        }
    }
}
