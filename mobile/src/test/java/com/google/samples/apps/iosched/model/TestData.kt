package com.google.samples.apps.iosched.model

import com.google.samples.apps.iosched.shared.model.Room
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.Speaker
import com.google.samples.apps.iosched.shared.model.Tag
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_1
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_2
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_3
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.PRECONFERENCE_DAY

/**
 * TODO: Temporary test data, improve.
 */
object TestData {
    val androidTag = Tag(id = "1", category = "TRACK", orderInCategory = 0, name = "Android",
            color = 0xFFAED581.toInt())

    val webTag = Tag(id = "2", category = "TRACK", orderInCategory = 1, name = "Web",
            color = 0xFFFFF176.toInt())

    val speaker = Speaker(id = "1", name = "Troy McClure", imageUrl = "",
            company = "", abstract = "", gPlusUrl = "", twitterUrl = "")

    val room = Room(id = "1", name = "Tent 1", capacity = 40)

    val session0 = Session(id = "0", title = "Session 0", abstract = "",
            startTime = PRECONFERENCE_DAY.start, endTime = PRECONFERENCE_DAY.end,
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

    val sessionsMap = mapOf(ConferenceDay.PRECONFERENCE_DAY to listOf(session0),
            ConferenceDay.DAY_1 to listOf(session1),
            ConferenceDay.DAY_2 to listOf(session2),
            ConferenceDay.DAY_3 to listOf(session3))
}
