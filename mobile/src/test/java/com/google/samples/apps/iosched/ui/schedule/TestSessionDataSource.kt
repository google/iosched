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

import com.google.samples.apps.iosched.shared.data.session.SessionDataSource
import com.google.samples.apps.iosched.shared.data.tag.TagDataSource
import com.google.samples.apps.iosched.shared.model.Room
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.Speaker
import com.google.samples.apps.iosched.shared.model.Tag
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

/**
 * Generates dummy session data to be used in tests.
 *
 * TODO: Move to testutils module b/72216577
 */
object TestSessionDataSource : SessionDataSource, TagDataSource {

    private val androidTag = Tag("1", "TRACK", 0, "Android", 0xFFAED581.toInt())
    private val webTag = Tag("2", "TRACK", 1, "Web", 0xFFFFF176.toInt())
    private val sessionsTag = Tag("101", "TYPE", 0, "Sessions", 0)
    private val codelabsTag = Tag("102", "TYPE", 1, "Codelabs", 0)
    private val beginnerTag = Tag("201", "LEVEL", 0, "Beginner", 0)
    private val intermediateTag = Tag("202", "LEVEL", 1, "Intermediate", 0)

    private val time1 = ZonedDateTime.of(2017, 3, 12, 12, 0, 0, 0, ZoneId.of("Asia/Tokyo"))
    private val time2 = ZonedDateTime.of(2017, 3, 12, 13, 0, 0, 0, ZoneId.of("Asia/Tokyo"))
    private val room1 = Room(id = "1", name = "Tent 1", capacity = 40)
    private val speaker1 = Speaker("1", "Troy McClure", "", "", "", "", "")

    private val session1 = Session(id = "1", startTime = time1, endTime = time2,
            title = "Jet Packs", abstract = "", room = room1, sessionUrl = "",
            liveStreamUrl = "", youTubeUrl = "", tags = listOf(androidTag, webTag),
            speakers = setOf(speaker1), photoUrl = "", relatedSessions = emptySet())

    private val session2 = Session(id = "2", startTime = time1, endTime = time2,
            title = "Flying Cars", abstract = "", room = room1, sessionUrl = "Title 1",
            liveStreamUrl = "", youTubeUrl = "", tags = listOf(androidTag),
            speakers = setOf(speaker1), photoUrl = "", relatedSessions = emptySet())

    private val session3 = Session(id = "3", startTime = time1, endTime = time2,
            title = "Teleportation", abstract = "", room = room1, sessionUrl = "Title 1",
            liveStreamUrl = "", youTubeUrl = "", tags = listOf(webTag),
            speakers = setOf(speaker1), photoUrl = "", relatedSessions = emptySet())

    override fun getSessions() = listOf(session1, session2, session3)

    override fun getTags() =
            listOf(androidTag, webTag, sessionsTag, codelabsTag, beginnerTag, intermediateTag)

    override fun getSession(sessionId: String) = getSessions()[0]
}