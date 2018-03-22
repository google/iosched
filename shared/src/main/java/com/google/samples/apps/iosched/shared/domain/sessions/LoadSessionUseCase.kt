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

package com.google.samples.apps.iosched.shared.domain.sessions

import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.domain.UseCase
import com.google.samples.apps.iosched.shared.model.Session
import javax.inject.Inject

open class LoadSessionUseCase @Inject constructor(private val repository: SessionRepository)
    : UseCase<String, Session>() {

    override fun execute(parameters: String): Session {
        val session = repository.getSessions().firstOrNull { it -> it.id == parameters }
                ?: throw SessionNotFoundException()
        session.type // Compute type from tags now so it's done in the background
        return session
    }
}

class SessionNotFoundException : Throwable()
