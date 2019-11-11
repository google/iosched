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

package com.google.samples.apps.iosched.shared.data.session

import com.google.samples.apps.iosched.model.ConferenceDay
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.SessionId
import com.google.samples.apps.iosched.shared.data.ConferenceDataRepository
import javax.inject.Inject

/**
 * Single point of access to session data for the presentation layer.
 *
 * The session data is loaded from the bootstrap file.
 */
interface SessionRepository {
    fun getSessions(userIsAttendee: Boolean): List<Session>
    fun getSession(eventId: SessionId): Session
    fun getConferenceDays(): List<ConferenceDay>
}

class DefaultSessionRepository @Inject constructor(
    private val conferenceDataRepository: ConferenceDataRepository
) : SessionRepository {

    override fun getSessions(userIsAttendee: Boolean): List<Session> {
        return conferenceDataRepository.getOfflineConferenceData().sessions
            .filterWhen(!userIsAttendee) { it.isLivestream }
    }

    override fun getSession(eventId: SessionId): Session {
        return conferenceDataRepository.getOfflineConferenceData().sessions.firstOrNull { session ->
            session.id == eventId
        } ?: throw SessionNotFoundException()
    }

    override fun getConferenceDays(): List<ConferenceDay> {
        return conferenceDataRepository.getConferenceDays()
    }
}

private fun <E> List<E>.filterWhen(condition: Boolean, predicate: (E) -> Boolean): List<E> {
    if (!condition) return this
    return this.filter(predicate)
}

class SessionNotFoundException : Throwable()
