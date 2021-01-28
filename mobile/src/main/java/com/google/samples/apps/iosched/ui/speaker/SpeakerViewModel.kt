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
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import com.google.samples.apps.iosched.model.Speaker
import com.google.samples.apps.iosched.model.SpeakerId
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.analytics.AnalyticsActions
import com.google.samples.apps.iosched.shared.analytics.AnalyticsHelper
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.domain.speakers.LoadSpeakerUseCase
import com.google.samples.apps.iosched.shared.domain.speakers.LoadSpeakerUseCaseResult
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.shared.result.successOr
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.ui.sessioncommon.EventActionsViewModelDelegate
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import org.threeten.bp.ZoneId
import javax.inject.Inject

/**
 * Loads a [Speaker] and their sessions, handles event actions.
 */
@HiltViewModel
class SpeakerViewModel @Inject constructor(
    private val loadSpeakerUseCase: LoadSpeakerUseCase,
    private val loadSpeakerSessionsUseCase: LoadUserSessionsUseCase,
    getTimeZoneUseCase: GetTimeZoneUseCase,
    signInViewModelDelegate: SignInViewModelDelegate,
    private val eventActionsViewModelDelegate: EventActionsViewModelDelegate,
    private val analyticsHelper: AnalyticsHelper
) : ViewModel(),
    SignInViewModelDelegate by signInViewModelDelegate,
    EventActionsViewModelDelegate by eventActionsViewModelDelegate {

    private val speakerId = MutableLiveData<String>()

    private val loadSpeakerUseCaseResult: LiveData<Result<LoadSpeakerUseCaseResult>> =
        speakerId.switchMap { speakerId ->
            liveData {
                emit(loadSpeakerUseCase(speakerId))
            }
        }

    val speakerUserSessions: LiveData<List<UserSession>> =
        loadSpeakerUseCaseResult.switchMap { speaker ->
            liveData {
                emit(emptyList()) // Reset value
                speaker.data?.let {
                    loadSpeakerSessionsUseCase(it.speaker.id to it.sessionIds).collect {
                        it.data?.let { data ->
                            emit(data)
                        }
                    }
                }
            }
        }

    val speaker: LiveData<Speaker?> = loadSpeakerUseCaseResult.map {
        it.data?.speaker
    }

    val hasNoProfileImage: LiveData<Boolean> = loadSpeakerUseCaseResult.map {
        it.data?.speaker?.imageUrl.isNullOrEmpty()
    }

    val timeZoneId = liveData {
        val timeZone = getTimeZoneUseCase(Unit)
        if (timeZone.successOr(true)) {
            emit(TimeUtils.CONFERENCE_TIMEZONE)
        } else {
            emit(ZoneId.systemDefault())
        }
    }

    /**
     * Provides the speaker ID which initiates all data loading.
     */
    fun setSpeakerId(id: SpeakerId) {
        speakerId.value = id
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
