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

import com.google.samples.apps.iosched.model.Announcement
import com.google.samples.apps.iosched.model.Block
import com.google.samples.apps.iosched.model.Codelab
import com.google.samples.apps.iosched.model.ConferenceData
import com.google.samples.apps.iosched.model.ConferenceDay
import com.google.samples.apps.iosched.model.Moment
import com.google.samples.apps.iosched.model.Room
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.Speaker
import com.google.samples.apps.iosched.model.Tag
import com.google.samples.apps.iosched.model.reservations.ReservationRequestResult
import com.google.samples.apps.iosched.model.reservations.ReservationRequestResult.ReservationRequestStatus.RESERVE_DENIED_CUTOFF
import com.google.samples.apps.iosched.model.reservations.ReservationRequestResult.ReservationRequestStatus.RESERVE_DENIED_UNKNOWN
import com.google.samples.apps.iosched.model.reservations.ReservationRequestResult.ReservationRequestStatus.RESERVE_SUCCEEDED
import com.google.samples.apps.iosched.model.reservations.ReservationRequestResult.ReservationRequestStatus.RESERVE_WAITLISTED
import com.google.samples.apps.iosched.model.userdata.UserEvent
import com.google.samples.apps.iosched.model.userdata.UserEvent.ReservationStatus.NONE
import com.google.samples.apps.iosched.model.userdata.UserSession
import org.threeten.bp.ZonedDateTime

/**
 * Test data for unit tests.
 */
object TestData {

    private const val CONFERENCE_DAY1_START = "2019-05-07T07:00:00-07:00"
    private const val CONFERENCE_DAY1_END = "2019-05-07T22:00:01-07:00"
    private const val CONFERENCE_DAY2_END = "2019-05-08T22:00:01-07:00"
    private const val CONFERENCE_DAY2_START = "2019-05-08T08:00:00-07:00"
    private const val CONFERENCE_DAY3_END = "2019-05-09T22:00:00-07:00"
    private const val CONFERENCE_DAY3_START = "2019-05-09T08:00:00-07:00"

    val TestConferenceDays = listOf(
        ConferenceDay(
            ZonedDateTime.parse(CONFERENCE_DAY1_START),
            ZonedDateTime.parse(CONFERENCE_DAY1_END)
        ),
        ConferenceDay(
            ZonedDateTime.parse(CONFERENCE_DAY2_START),
            ZonedDateTime.parse(CONFERENCE_DAY2_END)
        ),
        ConferenceDay(
            ZonedDateTime.parse(CONFERENCE_DAY3_START),
            ZonedDateTime.parse(CONFERENCE_DAY3_END)
        )
    )
    // region Declarations

    val androidTag = Tag("1", "topic", "track_android", 0, "Android", 0xFFAED581.toInt())
    val cloudTag = Tag("2", "topic", "track_cloud", 1, "Cloud", 0xFFFFF176.toInt())
    val webTag = Tag("3", "topic", "track_web", 2, "Web", 0xFFFFF176.toInt())
    val sessionsTag = Tag("101", "type", "type_sessions", 0, "Sessions", 0)
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
        biography = ""
    )

    val speaker2 = Speaker(
        id = "2",
        name = "Disco Stu",
        imageUrl = "",
        company = "",
        biography = ""
    )

    val speaker3 = Speaker(
        id = "3",
        name = "Hans Moleman",
        imageUrl = "",
        company = "",
        biography = ""
    )

    val room = Room(id = "1", name = "Tent 1")

    val session0 = Session(
        id = "0", title = "Session 0", description = "This session is awesome",
        startTime = TestConferenceDays[0].start, endTime = TestConferenceDays[0].end,
        isLivestream = false,
        room = room, sessionUrl = "", youTubeUrl = "", photoUrl = "", doryLink = "",
        tags = listOf(androidTag, webTag, sessionsTag),
        displayTags = listOf(androidTag, webTag),
        speakers = setOf(speaker1), relatedSessions = emptySet()
    )

    val session1 = Session(
        id = "1", title = "Session 1", description = "",
        startTime = TestConferenceDays[0].start, endTime = TestConferenceDays[0].end,
        isLivestream = false,
        room = room, sessionUrl = "", youTubeUrl = "", photoUrl = "", doryLink = "",
        tags = listOf(androidTag, webTag, codelabsTag),
        displayTags = listOf(androidTag, webTag),
        speakers = setOf(speaker2), relatedSessions = emptySet()
    )

    val session2 = Session(
        id = "2", title = "Session 2", description = "",
        startTime = TestConferenceDays[1].start, endTime = TestConferenceDays[1].end,
        isLivestream = false,
        room = room, sessionUrl = "", youTubeUrl = "", photoUrl = "", doryLink = "",
        tags = listOf(androidTag, sessionsTag, beginnerTag), displayTags = listOf(androidTag),
        speakers = setOf(speaker3), relatedSessions = emptySet()
    )

    val session3 = Session(
        id = "3", title = "Session 3", description = "",
        startTime = TestConferenceDays[2].start, endTime = TestConferenceDays[2].end,
        isLivestream = false,
        room = room, sessionUrl = "", youTubeUrl = "", photoUrl = "", doryLink = "",
        tags = listOf(webTag, sessionsTag, intermediateTag), displayTags = listOf(webTag),
        speakers = setOf(speaker1, speaker2), relatedSessions = emptySet()
    )

    val sessionWithYoutubeUrl = Session(
        id = "4", title = "Session 4", description = "",
        startTime = TestConferenceDays[2].start.plusMinutes(1),
        endTime = TestConferenceDays[2].end,
        isLivestream = true,
        room = room, sessionUrl = "",
        youTubeUrl = "\"https://www.youtube.com/watch?v=dQw4w9WgXcQ\"", photoUrl = "",
        doryLink = "",
        tags = listOf(webTag, advancedTag), displayTags = listOf(webTag),
        speakers = setOf(speaker1), relatedSessions = emptySet()
    )

    val sessionsList = listOf(session0, session1, session2, session3, sessionWithYoutubeUrl)

    val sessionIDs = sessionsList.map { it.id }.toList()

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
        sessionIDs[0],
        isStarred = false,
        isReviewed = false,
        reservationStatus = UserEvent.ReservationStatus.RESERVED,
        reservationRequestResult = ReservationRequestResult(
            RESERVE_SUCCEEDED, "123", System.currentTimeMillis()
        )
    )
    private val userEvent1 = UserEvent(
        sessionIDs[1],
        isStarred = true,
        isReviewed = true,
        reservationStatus = UserEvent.ReservationStatus.WAITLISTED,
        reservationRequestResult = ReservationRequestResult(
            RESERVE_WAITLISTED, "123", System.currentTimeMillis()
        )
    )
    private val userEvent2 = UserEvent(
        sessionIDs[2],
        isStarred = true,
        isReviewed = false,
        reservationStatus = NONE,
        reservationRequestResult = ReservationRequestResult(
            RESERVE_DENIED_CUTOFF, "123", System.currentTimeMillis()
        )
    )
    private val userEvent3 = UserEvent(
        sessionIDs[3],
        isStarred = false,
        isReviewed = true,
        reservationStatus = NONE,
        reservationRequestResult = ReservationRequestResult(
            RESERVE_DENIED_UNKNOWN, "123", System.currentTimeMillis()
        )
    )
    private val userEvent4 = UserEvent(
        sessionIDs[4],
        isStarred = false,
        isReviewed = true,
        reservationRequest = null
    )
    val userSession0 = UserSession(session0, userEvent0)
    val userSession1 = UserSession(session1, userEvent1)
    val userSession2 = UserSession(session2, userEvent2)
    val userSession3 = UserSession(session3, userEvent3)
    val userSession4 = UserSession(sessionWithYoutubeUrl, userEvent4)

    val userSessionList = listOf(
        userSession0,
        userSession1,
        userSession2,
        userSession3,
        userSession4
    )

    val starredOrReservedSessions = listOf(
        userSession0,
        userSession1,
        userSession2
    )

    val userEvents = listOf(userEvent0, userEvent1, userEvent2, userEvent3, userEvent4)

    val codelab0 = Codelab(
        id = "codelab0", title = "Android is Cool", description = "Make Android apps in 5 minutes!",
        durationMinutes = 6, iconUrl = null, codelabUrl = "", sortPriority = 0,
        tags = listOf(androidTag)
    )

    val codelab1 = Codelab(
        id = "codelab1", title = "HTML 6", description = "Webs aren't just for spiders anymore.",
        durationMinutes = 37, iconUrl = null, codelabUrl = "", sortPriority = 0,
        tags = listOf(webTag)
    )

    val codelab2 = Codelab(
        id = "codelab2", title = "Martian Learning", description = "Machine Learning in Space",
        durationMinutes = 20, iconUrl = null, codelabUrl = "", sortPriority = 1,
        tags = listOf(cloudTag)
    )

    val codelabs = listOf(codelab0, codelab1, codelab2)
    val codelabsSorted = listOf(codelab2, codelab0, codelab1)

    // endregion Declarations

    val conferenceData = ConferenceData(
        sessions = sessionsList,
        speakers = listOf(speaker1, speaker2, speaker3),
        rooms = listOf(room),
        codelabs = codelabs,
        tags = tagsList,
        version = 42
    )

    val feedItem1 = Announcement(
        id = "0", title = "Item 1", message = "", timestamp = TestConferenceDays[0].start,
        imageUrl = "", color = 0, category = "", priority = true, emergency = true
    )

    val feedItem2 = Announcement(
        id = "1", title = "Item 2", message = "", timestamp = TestConferenceDays[0].end,
        imageUrl = "", color = 0, category = "", priority = true, emergency = false
    )

    val feedItem3 = Announcement(
        id = "2", title = "Item 3", message = "", timestamp = TestConferenceDays[1].start,
        imageUrl = "", color = 0, category = "", priority = false, emergency = false
    )

    val feedItem4 = Announcement(
        id = "3", title = "Item 4", message = "", timestamp = TestConferenceDays[1].end,
        imageUrl = "", color = 0, category = "", priority = false, emergency = false
    )

    val announcements = listOf(feedItem1, feedItem2, feedItem3, feedItem4)
    val moment1 = Moment(
        id = "1",
        title = "KeyNote: Day 1",
        streamUrl = "https://www.youtube.com",
        startTime = TestConferenceDays[0].start,
        endTime = TestConferenceDays[0].end,
        textColor = 123,
        ctaType = Moment.CTA_LIVE_STREAM,
        imageUrl = "",
        imageUrlDarkTheme = "",
        attendeeRequired = false,
        timeVisible = false,
        featureId = "",
        featureName = ""
    )

    val moments = listOf(moment1)
}
