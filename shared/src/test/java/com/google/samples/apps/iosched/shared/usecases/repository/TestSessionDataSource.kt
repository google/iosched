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

package com.google.samples.apps.iosched.shared.usecases.repository

import com.google.samples.apps.iosched.shared.data.session.SessionDataSource
import com.google.samples.apps.iosched.shared.model.Room
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.Speaker
import com.google.samples.apps.iosched.shared.model.Tag
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

/**
 * Generates dummy session data to be used in tests.
 */
object TestSessionDataSource : SessionDataSource {
    override fun getSessions(): List<Session> {

        val androidTag = Tag("1", "TRACK", 0, "Android", 0xFFAED581.toInt())
        val webTag = Tag("2", "TRACK", 1, "Web", 0xFFFFF176.toInt())
        val speaker1 = Speaker("1", "Troy McClure", "", "", "", "", "")
        val time1 = ZonedDateTime.of(2017, 3, 12, 12, 0, 0, 0, ZoneId.of("Asia/Tokyo"))
        val time2 = ZonedDateTime.of(2017, 3, 12, 13, 0, 0, 0, ZoneId.of("Asia/Tokyo"))
        val room1 = Room(id = "1", name = "Tent 1", capacity = 40)
        val session1 = Session(id = "1", startTime = time1, endTime = time2,
                title = "Jet Packs", abstract = "", room = room1, sessionUrl = "", liveStreamUrl = "",
                youTubeUrl = "", tags = listOf(androidTag, webTag), speakers = setOf(speaker1),
                photoUrl = "", relatedSessions = emptySet())

        val session2 = Session(id = "2", startTime = time1, endTime = time2,
                title = "Flying Cars", abstract = "", room = room1, sessionUrl = "Title 1",
                liveStreamUrl = "", youTubeUrl = "", tags = listOf(androidTag),
                speakers = setOf(speaker1), photoUrl = "", relatedSessions = emptySet())

        val session3 = Session(id = "3", startTime = time1, endTime = time2,
                title = "Teleportation", abstract = "", room = room1, sessionUrl = "Title 1",
                liveStreamUrl = "", youTubeUrl = "", tags = listOf(webTag),
                speakers = setOf(speaker1), photoUrl = "", relatedSessions = emptySet())

        return listOf(session1, session2, session3)
    }
}
