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

import android.support.v4.util.ArrayMap
import com.google.samples.apps.iosched.shared.data.session.SessionRepository
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.usecases.UseCase
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import javax.inject.Inject

/**
 * Loads sessions into lists keyed by [ConferenceDay].
 */
open class LoadSessionsByDayUseCase @Inject constructor(private val repository: SessionRepository)
    : UseCase<SessionFilters, Map<ConferenceDay, List<Session>>>() {

    override fun execute(filters: SessionFilters): Map<ConferenceDay, List<Session>> {
        val allSessions = repository.getSessions()
        return ArrayMap<ConferenceDay, List<Session>>().apply {
            for (day in ConferenceDay.values()) {
                put(day, allSessions
                        .filter { day.contains(it) }
                        .filter { filters.matchesSessionTags(it.tags) }
                        .sortedBy { it.startTime } )
            }
        }
    }
}
