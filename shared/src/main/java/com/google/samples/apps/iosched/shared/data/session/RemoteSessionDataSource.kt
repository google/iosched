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

package com.google.samples.apps.iosched.shared.data.session

import com.google.samples.apps.iosched.shared.model.Room
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.Speaker
import com.google.samples.apps.iosched.shared.model.Tag
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import org.threeten.bp.ZonedDateTime

/**
 * TODO: Placeholder
 */
object RemoteSessionDataSource : SessionDataSource {

    private val SESSION: Session by lazy {
        val androidTag = Tag("1", "Technology", 0, "Android", 0xFFF30F30.toInt())
        val webTag = Tag("2", "Technology", 1, "Web", 0xFFF30F30.toInt())

        val speakerSet = HashSet<Speaker>().apply {
            add(Speaker(id = "1", name = "Troy McClure", imageUrl = "",
                    company = "Google", abstract = "Hi I'm Troy McClure", gPlusUrl = "",
                    twitterUrl = ""))
            add(Speaker(id = "2", name = "Ally McBeal", imageUrl = "",
                    company = "Google", abstract = "Hi I'm a lawyer", gPlusUrl = "",
                    twitterUrl = ""))
            add(Speaker(id = "3", name = "Ziggy Stardust", imageUrl = "",
                    company = "Google", abstract = "Hi I'm David Bowie.", gPlusUrl = "",
                    twitterUrl = ""))
            add(Speaker(id = "4", name = "Tiem Song", imageUrl = "",
                    company = "Google", abstract = "Hi I'm an Android DPE", gPlusUrl = "",
                    twitterUrl = ""))
            add(Speaker(id = "5", name = "Lyla Fujiwara", imageUrl = "",
                    company = "Google", abstract = "Hi I'm an Android DA", gPlusUrl = "",
                    twitterUrl = ""))
        }
        val room = Room(id = "1", name = "Tent 1", capacity = 40)
        Session(id = "1", startTime = ZonedDateTime.now(),
                endTime = ZonedDateTime.now().plusHours(1),
                title = "Fuchsia", abstract = "Come learn about the hottest, newest OS",
                room = room, sessionUrl = "", liveStreamUrl = "",
                youTubeUrl = "", tags = listOf(androidTag, webTag), speakers = speakerSet,
                photoUrl = "", relatedSessions = emptySet())
    }

    private val SESSIONS: List<Session> by lazy {
        val androidTag = Tag("tag1", "TRACK", 0, "Android", 0xFFAED581.toInt())
        val webTag = Tag("tag10", "TRACK", 1, "Web", 0xFFFFF176.toInt())
        val speakers = setOf(Speaker("1", "Troy McClure", "", "", "", "", ""))
        val room = Room(id = "1", name = "Tent 1", capacity = 40)

        val list = ArrayList<Session>()
        for (i in 1..90) {
            val startTime = when (i % 3) {
                0 -> ConferenceDay.DAY_1.start.plusHours(2)
                1 -> ConferenceDay.DAY_2.start.plusHours(2)
                else -> ConferenceDay.DAY_3.start.plusHours(2)
            }
            val tags = if (i % 2 == 0) {
                listOf(androidTag)
            } else {
                listOf(webTag)
            }
            list.add(Session(i.toString(),
                    startTime = startTime,
                    endTime = startTime.plusHours(1),
                    title = "Session $i",
                    abstract = "",
                    room = room,
                    sessionUrl = "",
                    liveStreamUrl = "",
                    youTubeUrl = "",
                    photoUrl = "",
                    tags = tags,
                    speakers = speakers,
                    relatedSessions = emptySet()))
        }
        list
    }

    override fun getSession(sessionId: String) = SESSION

    override fun getSessions() = SESSIONS
}
