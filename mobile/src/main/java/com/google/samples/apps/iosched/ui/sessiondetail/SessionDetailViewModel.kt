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
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import com.google.samples.apps.iosched.shared.domain.sessions.LoadSessionUseCase
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.shared.util.map
import com.google.samples.apps.iosched.ui.schedule.Event
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject

/**
 * Loads [Session] data and exposes it to the session detail view.
 */
class SessionDetailViewModel @Inject constructor(
    private val loadSessionUseCase: LoadSessionUseCase
) : ViewModel() {

    private val useCaseResult = MutableLiveData<Result<Session>>()
    private val sessionState: LiveData<TimeUtils.SessionState>

    val navigateToYouTubeAction = MutableLiveData<Event<String>>()

    val session: LiveData<Session?>
    val showRateButton: LiveData<Boolean>
    val hasPhoto: LiveData<Boolean>
    val isPlayable: LiveData<Boolean>
    val hasSpeakers: LiveData<Boolean>
    val hasRelated: LiveData<Boolean>

    init {
        //TODO: Deal with error SessionNotFoundException
        session = useCaseResult.map { (it as? Result.Success)?.data }

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
    }

    // TODO: write tests b/74611561
    fun loadSessionById(sessionId: String) {
        session.value ?: loadSessionUseCase(sessionId, useCaseResult)
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

    private fun requireSession(): Session {
        return session.value ?: throw IllegalStateException("Session should not be null")
    }

    fun checkPlayable(currentSession: Session?): Boolean {
        return currentSession != null && currentSession.youTubeUrl.isNotBlank()
    }
}