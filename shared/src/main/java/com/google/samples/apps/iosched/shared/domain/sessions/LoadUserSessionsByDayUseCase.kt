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
import com.google.samples.apps.iosched.shared.data.session.UserEventRepository
import com.google.samples.apps.iosched.shared.domain.UseCase
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent
import com.google.samples.apps.iosched.shared.model.UserSession
import com.google.samples.apps.iosched.shared.schedule.SessionMatcher
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import javax.inject.Inject

/**
 * Loads sessions into lists keyed by [ConferenceDay].
 */
open class LoadUserSessionsByDayUseCase @Inject constructor(
        private val sessionRepository: SessionRepository,
        private val userEventRepository: UserEventRepository)
    : UseCase<Pair<SessionMatcher, String>, Map<ConferenceDay, List<UserSession>>>() {

    override fun execute(parameters: Pair<SessionMatcher, String>):
            Map<ConferenceDay, List<UserSession>> {
        val (sessionMatcher, userID) = parameters
        val allSessions = sessionRepository.getSessions()
        val userEvents = userEventRepository.getUserEvents(userID)
        val eventIdToUserEvent: Map<String, UserEvent?> = userEvents.map {it.id to it}.toMap()
        val allUserSessions = allSessions.map { UserSession(it, eventIdToUserEvent[it.id]) }

        return ConferenceDay.values().map {day -> day to
                allUserSessions
                    .filter { day.contains(it.session) }
                    .filter { sessionMatcher.matchesSessionTags(it.session.tags) }
                    .sortedBy { it.session.startTime }}.toMap()
    }
}
