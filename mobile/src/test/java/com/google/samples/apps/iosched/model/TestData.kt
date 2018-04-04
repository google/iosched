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

package com.google.samples.apps.iosched.model

import com.google.samples.apps.iosched.shared.data.ConferenceDataRepository
import com.google.samples.apps.iosched.shared.data.ConferenceDataSource
import com.google.samples.apps.iosched.shared.firestore.entity.ReservationRequestResult
import com.google.samples.apps.iosched.shared.firestore.entity.ReservationRequestResult.ReservationRequestStatus.RESERVE_DENIED_CUTOFF
import com.google.samples.apps.iosched.shared.firestore.entity.ReservationRequestResult.ReservationRequestStatus.RESERVE_DENIED_UNKNOWN
import com.google.samples.apps.iosched.shared.firestore.entity.ReservationRequestResult.ReservationRequestStatus.RESERVE_SUCCEEDED
import com.google.samples.apps.iosched.shared.firestore.entity.ReservationRequestResult.ReservationRequestStatus.RESERVE_WAITLISTED
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent
import com.google.samples.apps.iosched.shared.model.Block
import com.google.samples.apps.iosched.shared.model.ConferenceData
import com.google.samples.apps.iosched.shared.model.Room
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.Speaker
import com.google.samples.apps.iosched.shared.model.Tag
import com.google.samples.apps.iosched.shared.model.UserSession
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_1
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_2
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_3
import com.google.samples.apps.iosched.ui.schedule.filters.TagFilter

/**
 * Test data for unit tests.
 */
object TestData : ConferenceDataSource {

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

    val speaker = Speaker(id = "1", name = "Troy McClure", imageUrl = "",
        company = "", abstract = "", gPlusUrl = "", twitterUrl = "")

    val room = Room(id = "1", name = "Tent 1", capacity = 40)

    val session0 = Session(id = "0", title = "Session 0", abstract = "",
        startTime = DAY_1.start, endTime = DAY_1.end, isLivestream = false,
        room = room, sessionUrl = "", liveStreamUrl = "", youTubeUrl = "", photoUrl = "",
        tags = listOf(androidTag, webTag, sessionsTag), displayTags = listOf(androidTag, webTag),
        speakers = setOf(speaker), relatedSessions = emptySet())

    val session1 = Session(id = "1", title = "Session 1", abstract = "",
        startTime = DAY_1.start, endTime = DAY_1.end, isLivestream = false,
        room = room, sessionUrl = "", liveStreamUrl = "", youTubeUrl = "", photoUrl = "",
        tags = listOf(androidTag, webTag, codelabsTag), displayTags = listOf(androidTag, webTag),
        speakers = setOf(speaker), relatedSessions = emptySet())

    val session2 = Session(id = "2", title = "Session 2", abstract = "",
        startTime = DAY_2.start, endTime = DAY_2.end, isLivestream = false,
        room = room, sessionUrl = "", liveStreamUrl = "", youTubeUrl = "", photoUrl = "",
        tags = listOf(androidTag, sessionsTag, beginnerTag), displayTags = listOf(androidTag),
        speakers = setOf(speaker), relatedSessions = emptySet())

    val session3 = Session(id = "3", title = "Session 3", abstract = "",
        startTime = DAY_3.start, endTime = DAY_3.end, isLivestream = false,
        room = room, sessionUrl = "", liveStreamUrl = "", youTubeUrl = "", photoUrl = "",
        tags = listOf(webTag, sessionsTag, intermediateTag), displayTags = listOf(webTag),
        speakers = setOf(speaker), relatedSessions = emptySet())

    val sessionWithYoutubeUrl = Session(id = "4", title = "Session 4", abstract = "",
        startTime = DAY_3.start.plusMinutes(1), endTime = DAY_3.end, isLivestream = true,
        room = room, sessionUrl = "", liveStreamUrl = "",
        youTubeUrl = "\"https://www.youtube.com/watch?v=dQw4w9WgXcQ\"", photoUrl = "",
        tags = listOf(webTag, advancedTag), displayTags = listOf(webTag),
        speakers = setOf(speaker), relatedSessions = emptySet())

    val sessionsList = listOf(session0, session1, session2, session3, sessionWithYoutubeUrl)

    val sessionIDs = sessionsList.map { it.id }.toList()

    val sessionsMap = mapOf(
        ConferenceDay.DAY_1 to listOf(session0, session1),
        ConferenceDay.DAY_2 to listOf(session2),
        ConferenceDay.DAY_3 to listOf(session3, sessionWithYoutubeUrl))

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

    private val userEvent0 = UserEvent(sessionIDs[0], isStarred = false,
        isReviewed = false,
        startTime = session0.startTime.toInstant().toEpochMilli(),
        endTime = session0.endTime.toInstant().toEpochMilli(),
        reservationStatus = UserEvent.ReservationStatus.RESERVED,
        reservationRequestResult = ReservationRequestResult(
                RESERVE_SUCCEEDED, "123", System.currentTimeMillis())
    )
    private val userEvent1 = UserEvent(sessionIDs[1], isStarred = true,
        isReviewed = true,
        startTime = session1.startTime.toInstant().toEpochMilli(),
        endTime = session1.endTime.toInstant().toEpochMilli(),
        reservationStatus = UserEvent.ReservationStatus.WAITLISTED,
        reservationRequestResult = ReservationRequestResult(
                RESERVE_WAITLISTED, "123", System.currentTimeMillis())
    )
    private val userEvent2 = UserEvent(sessionIDs[2], isStarred = true,
        isReviewed = false,
        startTime = session2.startTime.toInstant().toEpochMilli(),
        endTime = session2.endTime.toInstant().toEpochMilli(),
        reservationStatus = UserEvent.ReservationStatus.NONE,
        reservationRequestResult = ReservationRequestResult(
                RESERVE_DENIED_CUTOFF, "123", System.currentTimeMillis())
    )
    private val userEvent3 = UserEvent(sessionIDs[3], isStarred = false,
        isReviewed = true,
        startTime = session3.startTime.toInstant().toEpochMilli(),
        endTime = session3.endTime.toInstant().toEpochMilli(),
        reservationStatus = UserEvent.ReservationStatus.NONE,
        reservationRequestResult = ReservationRequestResult(
                RESERVE_DENIED_UNKNOWN, "123", System.currentTimeMillis())
    )
    private val userEvent4 = UserEvent(sessionIDs[4], isStarred = false,
            isReviewed = true,
            startTime = sessionWithYoutubeUrl.startTime.toInstant().toEpochMilli(),
            endTime = sessionWithYoutubeUrl.endTime.toInstant().toEpochMilli(),
            reservationRequest = null
    )
    private val userSession0 = UserSession(session0, userEvent0)
    private val userSession1 = UserSession(session1, userEvent1)
    private val userSession2 = UserSession(session2, userEvent2)
    private val userSession3 = UserSession(session3, userEvent3)
    private val userSession4 = UserSession(sessionWithYoutubeUrl, userEvent4)
    val userSessionMap = mapOf(
        ConferenceDay.DAY_1 to listOf(userSession0, userSession1),
        ConferenceDay.DAY_2 to listOf(userSession2),
        ConferenceDay.DAY_3 to listOf(userSession3, userSession4))
    val userEvents = listOf(userEvent0, userEvent1, userEvent2, userEvent3, userEvent4)

    // endregion Declarations
    // region Extras for verification

    val tagFiltersList = listOf(
        androidTag, webTag, cloudTag, // TRACK
        sessionsTag, codelabsTag      // TYPE
    ).map { TagFilter(it, false) }

    // endregion Extras for verification

    private val conferenceData = ConferenceData(
        sessions = sessionsList,
        tags = tagsList,
        rooms = listOf(room),
        speakers = listOf(speaker),
        version = 42
    )

    override fun getRemoteConferenceData() = conferenceData

    override fun getOfflineConferenceData() = conferenceData
}

/** ConferenceDataRepository for tests */
object TestDataRepository : ConferenceDataRepository(TestData, TestData)
