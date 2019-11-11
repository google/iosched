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

package com.google.samples.apps.iosched.test.data

import com.google.samples.apps.iosched.model.Block
import com.google.samples.apps.iosched.model.ConferenceData
import com.google.samples.apps.iosched.model.ConferenceDay
import com.google.samples.apps.iosched.model.Room
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.Speaker
import com.google.samples.apps.iosched.model.Tag
import com.google.samples.apps.iosched.model.userdata.UserEvent
import com.google.samples.apps.iosched.model.userdata.UserSession
import org.threeten.bp.ZonedDateTime

/**
 * Test data for unit tests.
 */
object TestData {
    val CONFERENCE_DAY1_START = "2019-10-23T07:00:00-07:00"
    val CONFERENCE_DAY1_END = "2019-10-23T22:00:01-07:00"
    val CONFERENCE_DAY2_START = "2019-10-24T08:00:00-07:00"
    val CONFERENCE_DAY2_END = "2019-10-24T22:00:01-07:00"

    val TestConferenceDays = listOf(
        ConferenceDay(
            ZonedDateTime.parse(CONFERENCE_DAY1_START),
            ZonedDateTime.parse(CONFERENCE_DAY1_END)
        ),
        ConferenceDay(
            ZonedDateTime.parse(CONFERENCE_DAY2_START),
            ZonedDateTime.parse(CONFERENCE_DAY2_END)
        )
    )
    // region Declarations

    val androidTag = Tag("1", "topic", "track_android", 0, "Android", 0xFFAED581.toInt())
    val cloudTag = Tag("2", "topic", "track_cloud", 1, "Cloud", 0xFFFFF176.toInt())
    val webTag = Tag("3", "topic", "track_web", 2, "Web", 0xFFFFF176.toInt())
    val sessionsTag = Tag("101", "type", "type_session", 0, "Sessions", 0)
    val codelabsTag = Tag("102", "type", "type_codelabs", 1, "Codelabs", 0)
    val beginnerTag = Tag("201", "level", "level_beginner", 0, "Beginner", 0)
    val intermediateTag = Tag("202", "level", "level_intermediate", 1, "Intermediate", 0)
    val advancedTag = Tag("203", "level", "level_advanced", 2, "Advanced", 0)

    val tagsList = listOf(
        androidTag, cloudTag, webTag, sessionsTag, codelabsTag, beginnerTag,
        intermediateTag, advancedTag
    )

    val speaker1 = Speaker(
        id = "1",
        name = "Troy McClure",
        imageUrl = "",
        company = "",
        abstract = ""
    )

    val speaker2 = Speaker(
        id = "2",
        name = "Disco Stu",
        imageUrl = "",
        company = "",
        abstract = ""
    )

    val speaker3 = Speaker(
        id = "3",
        name = "Hans Moleman",
        imageUrl = "",
        company = "",
        abstract = ""
    )

    val room = Room(id = "1", name = "Tent 1")

    val session0 = Session(
        id = "0", title = "Session 0", abstract = "This session is awesome",
        startTime = TestConferenceDays[0].start, endTime = TestConferenceDays[0].end,
        isLivestream = false,
        room = room, sessionUrl = "", liveStreamUrl = "", youTubeUrl = "", photoUrl = "",
        tags = listOf(androidTag, webTag, sessionsTag), displayTags = listOf(androidTag, webTag),
        speakers = setOf(speaker1), relatedSessions = emptySet()
    )

    val session1 = Session(
        id = "1", title = "Session 1", abstract = "",
        startTime = TestConferenceDays[0].start, endTime = TestConferenceDays[0].end,
        isLivestream = false,
        room = room, sessionUrl = "", liveStreamUrl = "", youTubeUrl = "", photoUrl = "",
        tags = listOf(androidTag, webTag, codelabsTag), displayTags = listOf(androidTag, webTag),
        speakers = setOf(speaker2), relatedSessions = emptySet()
    )

    val session2 = Session(
        id = "2", title = "Session 2", abstract = "",
        startTime = TestConferenceDays[1].start, endTime = TestConferenceDays[1].end,
        isLivestream = false,
        room = room, sessionUrl = "", liveStreamUrl = "", youTubeUrl = "", photoUrl = "",
        tags = listOf(androidTag, sessionsTag, beginnerTag), displayTags = listOf(androidTag),
        speakers = setOf(speaker3), relatedSessions = emptySet()
    )

    val session3 = Session(
        id = "3", title = "Session 3", abstract = "",
        startTime = TestConferenceDays[1].start, endTime = TestConferenceDays[1].end,
        isLivestream = false,
        room = room, sessionUrl = "", liveStreamUrl = "", youTubeUrl = "", photoUrl = "",
        tags = listOf(webTag, sessionsTag, intermediateTag), displayTags = listOf(webTag),
        speakers = setOf(speaker1, speaker2), relatedSessions = emptySet()
    )

    val sessionWithYoutubeUrl = Session(
        id = "4", title = "Session 4", abstract = "",
        startTime = TestConferenceDays[1].start.plusMinutes(1), endTime = TestConferenceDays[1].end,
        isLivestream = true,
        room = room, sessionUrl = "", liveStreamUrl = "",
        youTubeUrl = "\"https://www.youtube.com/watch?v=dQw4w9WgXcQ\"", photoUrl = "",
        tags = listOf(webTag, advancedTag), displayTags = listOf(webTag),
        speakers = setOf(speaker1), relatedSessions = emptySet()
    )

    val sessionsList = listOf(session0, session1, session2, session3, sessionWithYoutubeUrl)

    private val sessionIDs = sessionsList.map { it.id }.toList()

    val sessionsMap = mapOf(
        TestConferenceDays[0] to listOf(session0, session1),
        TestConferenceDays[1] to listOf(session2, session3, sessionWithYoutubeUrl)
    )

    val block1 = Block(
        title = "Keynote",
        type = "keynote",
        color = 0xffff00ff.toInt(),
        startTime = TestConferenceDays[0].start,
        endTime = TestConferenceDays[0].start.plusHours(1L)
    )

    val block2 = Block(
        title = "Breakfast",
        type = "meal",
        color = 0xffff00ff.toInt(),
        startTime = TestConferenceDays[0].start.plusHours(1L),
        endTime = TestConferenceDays[0].start.plusHours(2L)
    )

    val agenda = listOf(block1, block2)

    private val userEvent0 = UserEvent(
        sessionIDs[0], isStarred = false,
        isReviewed = false
    )
    private val userEvent1 = UserEvent(
        sessionIDs[1], isStarred = true,
        isReviewed = true
    )
    private val userEvent2 = UserEvent(
        sessionIDs[2], isStarred = true,
        isReviewed = false
    )
    private val userEvent3 = UserEvent(
        sessionIDs[3], isStarred = false,
        isReviewed = true
    )
    private val userEvent4 = UserEvent(
        sessionIDs[4], isStarred = false,
        isReviewed = true
    )
    val userSession0 = UserSession(session0, userEvent0)
    val userSession1 = UserSession(session1, userEvent1)
    val userSession2 = UserSession(session2, userEvent2)
    val userSession3 = UserSession(session3, userEvent3)
    val userSession4 = UserSession(sessionWithYoutubeUrl, userEvent4)

    val userSessionMap = mapOf(
        TestConferenceDays[0] to listOf(userSession0, userSession1),
        TestConferenceDays[1] to listOf(userSession2, userSession3, userSession4)
    )
    val userEvents = listOf(userEvent0, userEvent1, userEvent2, userEvent3, userEvent4)

    // endregion Declarations

    val conferenceData = ConferenceData(
        sessions = sessionsList,
        tags = tagsList,
        rooms = listOf(room),
        speakers = listOf(speaker1, speaker2, speaker3),
        version = 42
    )
}
