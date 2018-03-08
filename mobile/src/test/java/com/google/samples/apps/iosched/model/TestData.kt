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

import com.google.samples.apps.iosched.shared.firestore.entity.ReservationRequestResult
import com.google.samples.apps.iosched.shared.firestore.entity.ReservationRequestResult.ReservationRequestStatus.RESERVE_DENIED_CUTOFF
import com.google.samples.apps.iosched.shared.firestore.entity.ReservationRequestResult.ReservationRequestStatus.RESERVE_DENIED_UNKNOWN
import com.google.samples.apps.iosched.shared.firestore.entity.ReservationRequestResult.ReservationRequestStatus.RESERVE_SUCCEEDED
import com.google.samples.apps.iosched.shared.firestore.entity.ReservationRequestResult.ReservationRequestStatus.RESERVE_WAITLISTED
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent
import com.google.samples.apps.iosched.shared.model.Block
import com.google.samples.apps.iosched.shared.model.Room
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.Speaker
import com.google.samples.apps.iosched.shared.model.Tag
import com.google.samples.apps.iosched.shared.model.UserSession
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_1
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_2
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_3

/**
 * TODO: Temporary test data, improve.
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

    private val room = Room(id = "1", name = "Tent 1", capacity = 40)

    val sessionIDs = listOf("0", "1", "2", "3")
    val session0 = Session(id = sessionIDs[0], title = "Session 0", abstract = "",
            startTime = DAY_1.start, endTime = DAY_1.end,
            room = room, sessionUrl = "", liveStreamUrl = "", youTubeUrl = "", photoUrl = "",
            tags = listOf(androidTag, webTag), speakers = setOf(speaker),
            relatedSessions = emptySet())

    val session1 = Session(id = sessionIDs[1], title = "Session 1", abstract = "",
            startTime = DAY_1.start, endTime = DAY_1.end,
            room = room, sessionUrl = "", liveStreamUrl = "", youTubeUrl = "", photoUrl = "",
            tags = listOf(androidTag, webTag), speakers = setOf(speaker),
            relatedSessions = emptySet())

    val session2 = Session(id = sessionIDs[2], title = "Session 2", abstract = "",
            startTime = DAY_2.start, endTime = DAY_2.end,
            room = room, sessionUrl = "", liveStreamUrl = "", youTubeUrl = "", photoUrl = "",
            tags = listOf(androidTag), speakers = setOf(speaker), relatedSessions = emptySet())

    val session3 = Session(id = sessionIDs[3], title = "Session 3", abstract = "",
            startTime = DAY_3.start, endTime = DAY_3.end,
            room = room, sessionUrl = "", liveStreamUrl = "", youTubeUrl = "", photoUrl = "",
            tags = listOf(webTag), speakers = setOf(speaker), relatedSessions = emptySet())

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

    private val userEvent0 = UserEvent(sessionIDs[0], isStarred = false,
            isReviewed = false,
            startTime = TestData.session0.startTime.toInstant().toEpochMilli(),
            endTime = TestData.session0.endTime.toInstant().toEpochMilli(),
            reservation = ReservationRequestResult(RESERVE_SUCCEEDED, System.currentTimeMillis()))
    private val userEvent1 = UserEvent(sessionIDs[1], isStarred = true,
            isReviewed = true,
            startTime = TestData.session1.startTime.toInstant().toEpochMilli(),
            endTime = TestData.session1.endTime.toInstant().toEpochMilli(),
            reservation = ReservationRequestResult(RESERVE_WAITLISTED, System.currentTimeMillis()))
    private val userEvent2 = UserEvent(sessionIDs[2], isStarred = true,
            isReviewed = false,
            startTime = TestData.session2.startTime.toInstant().toEpochMilli(),
            endTime = TestData.session2.endTime.toInstant().toEpochMilli(),
            reservation = ReservationRequestResult(RESERVE_DENIED_CUTOFF,
                    System.currentTimeMillis()))
    private val userEvent3 = UserEvent(sessionIDs[3], isStarred = false,
            isReviewed = true,
            startTime = TestData.session3.startTime.toInstant().toEpochMilli(),
            endTime = TestData.session3.endTime.toInstant().toEpochMilli(),
            reservation = ReservationRequestResult(RESERVE_DENIED_UNKNOWN,
                    System.currentTimeMillis()))
    private val userSession0 = UserSession(session0, userEvent0)
    private val userSession1 = UserSession(session1, userEvent1)
    private val userSession2 = UserSession(session2, userEvent2)
    private val userSession3 = UserSession(session3, userEvent3)
    val userSessionMap = mapOf(ConferenceDay.DAY_1 to listOf(userSession0, userSession1),
            ConferenceDay.DAY_2 to listOf(userSession2),
            ConferenceDay.DAY_3 to listOf(userSession3))
    val userEvents = listOf(userEvent0, userEvent1, userEvent2, userEvent3)
}
