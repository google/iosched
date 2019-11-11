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

package com.google.samples.apps.iosched.ui.schedule.day

import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.shared.util.TimeUtils
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

/**
 * Find the first session at each start time (rounded down to nearest minute) and return pairs of
 * index to start time. Assumes that [sessions] are sorted by ascending start time.
 */

fun indexSessionHeaders(sessions: List<Session>, zoneId: ZoneId): List<Pair<Int, ZonedDateTime>> {
    return sessions
        .mapIndexed { index, session ->
            index to TimeUtils.zonedTime(session.startTime, zoneId)
        }
        .distinctBy { it.second.hour to it.second.minute }
}
