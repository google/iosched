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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.iosched.model.Speaker
import com.google.samples.apps.iosched.model.SpeakerId
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionsUseCase
import com.google.samples.apps.iosched.shared.domain.settings.GetTimeZoneUseCase
import com.google.samples.apps.iosched.shared.domain.speakers.LoadSpeakerUseCase
import com.google.samples.apps.iosched.shared.domain.speakers.LoadSpeakerUseCaseResult
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.result.Result.Loading
import com.google.samples.apps.iosched.shared.result.data
import com.google.samples.apps.iosched.shared.result.successOr
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import com.google.samples.apps.iosched.util.WhileViewSubscribed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import org.threeten.bp.ZoneId
import javax.inject.Inject

/**
 * Loads a [Speaker] and their sessions, handles event actions.
 */
@HiltViewModel
class SpeakerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val loadSpeakerUseCase: LoadSpeakerUseCase,
    private val loadSpeakerSessionsUseCase: LoadUserSessionsUseCase,
    getTimeZoneUseCase: GetTimeZoneUseCase,
    signInViewModelDelegate: SignInViewModelDelegate
) : ViewModel(),
    SignInViewModelDelegate by signInViewModelDelegate {

    // TODO: remove hardcoded string when https://issuetracker.google.com/136967621 is available
    private val speakerId: SpeakerId? = savedStateHandle.get<SpeakerId>("speaker_id")

    private val loadSpeakerUseCaseResult: StateFlow<Result<LoadSpeakerUseCaseResult>> =
        flow {
            speakerId?.let { emit(loadSpeakerUseCase(speakerId)) }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, Loading)

    val speakerUserSessions: StateFlow<List<UserSession>> =
        loadSpeakerUseCaseResult.transformLatest { speaker ->
            speaker.data?.let {
                loadSpeakerSessionsUseCase(it.speaker.id to it.sessionIds).collect {
                    it.data?.let { data ->
                        emit(data)
                    }
                }
            }
        }.stateIn(viewModelScope, WhileViewSubscribed, emptyList())

    val speaker: StateFlow<Speaker?> = loadSpeakerUseCaseResult.mapLatest {
        it.data?.speaker
    }.stateIn(viewModelScope, WhileViewSubscribed, null)

    val hasNoProfileImage: StateFlow<Boolean> = loadSpeakerUseCaseResult.mapLatest {
        it.data?.speaker?.imageUrl.isNullOrEmpty()
    }.stateIn(viewModelScope, WhileViewSubscribed, true)

    // Exposed to the view as a StateFlow but it's a one-shot operation.
    // TODO: Rename with timeZoneId when all usages are migrated
    val timeZoneIdFlow = flow<ZoneId> {
        if (getTimeZoneUseCase(Unit).successOr(true)) {
            emit(TimeUtils.CONFERENCE_TIMEZONE)
        } else {
            emit(ZoneId.systemDefault())
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, TimeUtils.CONFERENCE_TIMEZONE)

    // TODO: Replace with timeZoneIdFlow when SearchViewModel is migrated
    val timeZoneId = timeZoneIdFlow.asLiveData()
}
