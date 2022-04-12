/*
 * Copyright 2019 Google LLC
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

import androidx.annotation.IntRange
import androidx.annotation.StringRes
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.model.userdata.UserSession
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCase
import com.google.samples.apps.iosched.shared.domain.users.FeedbackParameter
import com.google.samples.apps.iosched.shared.domain.users.FeedbackUseCase
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.cancelIfActive
import com.google.samples.apps.iosched.ui.signin.SignInViewModelDelegate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class SessionFeedbackViewModel @ViewModelInject constructor(
    private val signInViewModelDelegate: SignInViewModelDelegate,
    private val loadUserSessionUseCase: LoadUserSessionUseCase,
    private val feedbackUseCase: FeedbackUseCase
) : ViewModel(),
    SignInViewModelDelegate by signInViewModelDelegate {

    companion object {
        val MESSAGES = mapOf(
            "q1" to Triple(
                R.string.feedback_q1_text,
                R.string.feedback_q1_label_start,
                R.string.feedback_q1_label_end
            ),
            "q2" to Triple(
                R.string.feedback_q2_text,
                R.string.feedback_q2_label_start,
                R.string.feedback_q2_label_end
            ),
            "q3" to Triple(
                R.string.feedback_q3_text,
                R.string.feedback_q3_label_start,
                R.string.feedback_q3_label_end
            ),
            "q4" to Triple(
                R.string.feedback_q4_text,
                R.string.feedback_q4_label_start,
                R.string.feedback_q4_label_end
            )
        )
    }

    private var loadUserSessionJob: Job? = null

    private var _sessionId: SessionId? = null

    private val _userSession = MutableLiveData<UserSession>()
    val userSession = _userSession

    val questions = MutableLiveData<List<Question>>(MESSAGES.map { (key, value) ->
        val (text, start, end) = value
        Question(key, text, 0, start, end)
    })

    fun setSessionId(sessionId: SessionId) {
        _sessionId = sessionId
        loadUserSessionJob.cancelIfActive()
        loadUserSessionJob = viewModelScope.launch {
            loadUserSessionUseCase(getUserId() to sessionId).collect { result ->
                _userSession.value = (result as? Result.Success)?.data?.userSession
            }
        }
    }

    fun submit(feedbackUpdates: Map<String, Int>) {
        val sessionId = _sessionId ?: return
        val userId = getUserId()
        val userEvent = _userSession.value?.userEvent
        if (userId != null && userEvent != null) {
            viewModelScope.launch {
                feedbackUseCase(
                    FeedbackParameter(
                        userId,
                        userEvent,
                        sessionId,
                        feedbackUpdates
                    )
                )
            }
        }
    }
}

/**
 * Data model for the list.
 */
data class Question(
    val key: String,
    @StringRes
    val text: Int,
    /** 0 means unrated. Actual ratings are 1-5. */
    @IntRange(from = 0, to = 5)
    val currentRating: Int,
    @StringRes
    val labelStart: Int,
    @StringRes
    val labelEnd: Int
)
