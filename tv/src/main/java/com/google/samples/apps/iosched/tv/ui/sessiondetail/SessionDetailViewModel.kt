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

package com.google.samples.apps.iosched.tv.ui.sessiondetail

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCase
import com.google.samples.apps.iosched.shared.domain.sessions.LoadUserSessionUseCaseResult
import com.google.samples.apps.iosched.shared.result.Result
import com.google.samples.apps.iosched.shared.util.map
import javax.inject.Inject

/**
 * Loads [Session] data and exposes it to the session detail view.
 */
class SessionDetailViewModel @Inject constructor(
    private val loadUserSessionUseCase: LoadUserSessionUseCase
) : ViewModel() {

    private val loadUserSessionResult: LiveData<Result<LoadUserSessionUseCaseResult>>
    val session: LiveData<Session?>

    // TODO: Remove it once the FirebaseUser is available when the app is launched
    val tempUser = "user1"

    init {
        loadUserSessionResult = loadUserSessionUseCase.observe()
        session = loadUserSessionResult.map { (it as? Result.Success)?.data?.userSession?.session }

        // TODO: Deal with error SessionNotFoundException
    }

    // TODO-76284 fix this
    fun loadSessionById(sessionId: SessionId) {
        session.value ?: loadUserSessionUseCase.execute(tempUser to sessionId)
    }
}
