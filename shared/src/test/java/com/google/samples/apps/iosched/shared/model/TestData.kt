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

package com.google.samples.apps.iosched.shared.model

import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_1
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_2
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_3

/**
 * Test data for :shared unit tests.
 */
object TestData {

    val androidTag = Tag("1", "TRACK", 0, "Android", 0xFFAED581.toInt())
    val webTag = Tag("2", "TRACK", 1, "Web", 0xFFFFF176.toInt())
    val sessionsTag = Tag("101", "TYPE", 0, "Sessions", 0)
    val codelabsTag = Tag("102", "TYPE", 1, "Codelabs", 0)
    val beginnerTag = Tag("201", "LEVEL", 0, "Beginner", 0)
    val intermediateTag = Tag("202", "LEVEL", 1, "Intermediate", 0)
    val advancedTag = Tag("203", "LEVEL", 2, "Advanced", 0)

    val speaker = Speaker(id = "1", name = "Troy McClure", imageUrl = "",
            company = "", abstract = "", gPlusUrl = "", twitterUrl = "")

    val room = Room(id = "1", name = "Tent 1", capacity = 40)

    val session0 = Session(id = "0", title = "Session 0", abstract = "",
            startTime = DAY_1.start, endTime = DAY_1.end,
            room = room, sessionUrl = "", liveStreamUrl = "", youTubeUrl = "", photoUrl = "",
            tags = listOf(androidTag, webTag), speakers = setOf(speaker),
            relatedSessions = emptySet())

    val session1 = Session(id = "1", title = "Session 1", abstract = "",
            startTime = DAY_1.start, endTime = DAY_1.end,
            room = room, sessionUrl = "", liveStreamUrl = "", youTubeUrl = "", photoUrl = "",
            tags = listOf(androidTag, webTag), speakers = setOf(speaker),
            relatedSessions = emptySet())

    val session2 = Session(id = "2", title = "Session 2", abstract = "",
            startTime = DAY_2.start, endTime = DAY_2.end,
            room = room, sessionUrl = "", liveStreamUrl = "", youTubeUrl = "", photoUrl = "",
            tags = listOf(androidTag), speakers = setOf(speaker), relatedSessions = emptySet())

    val session3 = Session(id = "3", title = "Session 3", abstract = "",
            startTime = DAY_3.start, endTime = DAY_3.end,
            room = room, sessionUrl = "", liveStreamUrl = "", youTubeUrl = "", photoUrl = "",
            tags = listOf(webTag), speakers = setOf(speaker), relatedSessions = emptySet())

    val sessionsMap = mapOf(ConferenceDay.DAY_1 to listOf(session0, session1),
            ConferenceDay.DAY_2 to listOf(session2),
            ConferenceDay.DAY_3 to listOf(session3))

    val tagsList = listOf(androidTag, webTag, sessionsTag, codelabsTag, beginnerTag,
            intermediateTag, advancedTag)

    val block1 = Block(
        title = "Keynote",
        type = "keynote",
        color = 0xffff00ff.toInt(),
        startTime = DAY_1.start,
        endTime = DAY_1.start.plusHours(1L))

    val block2 = Block(
        title = "Breakfast",
        type = "meal",
        color = 0xffff00ff.toInt(),
        startTime = DAY_1.start.plusHours(1L),
        endTime = DAY_1.start.plusHours(2L))

    val agenda = listOf(block1, block2)
}
