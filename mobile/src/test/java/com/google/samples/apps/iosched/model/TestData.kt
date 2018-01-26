package com.google.samples.apps.iosched.model

import com.google.samples.apps.iosched.shared.model.Room
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.Speaker
import com.google.samples.apps.iosched.shared.model.Tag
import org.threeten.bp.ZonedDateTime

/**
 * TODO: Temporary test data, improve.
 */
object TestData {
    val androidTag = Tag(id = "1", category = "Technology", name = "Android", color = "#F30F30")

    val webTag = Tag(id = "1", category = "Technology", name = "Web", color = "#F30F30")

    val speaker1 = Speaker(id = "1", name = "Troy McClure", imageUrl = "",
            company = "", abstract = "", gPlusUrl = "", twitterUrl = "")

    val room1 = Room(id = "1", name = "Tent 1", capacity = 40)

    val session1 = Session(id = "1", startTime = ZonedDateTime.now(), endTime = ZonedDateTime.now(),
            title = "Fuchsia", abstract = "", room = room1, sessionUrl = "", liveStreamUrl = "",
            youTubeUrl = "", tags = listOf(androidTag, webTag), speakers = setOf(speaker1),
            photoUrl = "", relatedSessions = emptySet())

    val session2 = Session(id = "2", startTime = ZonedDateTime.now(), endTime = ZonedDateTime.now(),
            title = "AIA", abstract = "", room = room1, sessionUrl = "Title 1", liveStreamUrl = "",
            youTubeUrl = "", tags = listOf(androidTag), speakers = setOf(speaker1),
            photoUrl = "", relatedSessions = emptySet())

    val session3 = Session(id = "3", startTime = ZonedDateTime.now(), endTime = ZonedDateTime.now(),
            title = "AMP", abstract = "", room = room1, sessionUrl = "Title 1", liveStreamUrl = "",
            youTubeUrl = "", tags = listOf(webTag), speakers = setOf(speaker1),
            photoUrl = "", relatedSessions = emptySet())

}