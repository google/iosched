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

package com.google.samples.apps.iosched.shared.data

import androidx.annotation.Keep
import com.google.gson.GsonBuilder
import com.google.gson.JsonIOException
import com.google.gson.JsonSyntaxException
import com.google.samples.apps.iosched.model.ConferenceData
import com.google.samples.apps.iosched.model.Room
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.Speaker
import com.google.samples.apps.iosched.model.Tag
import com.google.samples.apps.iosched.shared.data.session.json.RoomDeserializer
import com.google.samples.apps.iosched.shared.data.session.json.SessionDeserializer
import com.google.samples.apps.iosched.shared.data.session.json.SessionTemp
import com.google.samples.apps.iosched.shared.data.session.json.SpeakerDeserializer
import com.google.samples.apps.iosched.shared.data.session.json.TagDeserializer
import java.io.InputStream

object ConferenceDataJsonParser {

    @Throws(JsonIOException::class, JsonSyntaxException::class)
    fun parseConferenceData(unprocessedSessionData: InputStream): ConferenceData {
        val jsonReader = com.google.gson.stream.JsonReader(unprocessedSessionData.reader())

        val gson = GsonBuilder()
            .registerTypeAdapter(SessionTemp::class.java, SessionDeserializer())
            .registerTypeAdapter(Tag::class.java, TagDeserializer())
            .registerTypeAdapter(Speaker::class.java, SpeakerDeserializer())
            .registerTypeAdapter(Room::class.java, RoomDeserializer())
            .create()

        val tempData: TempConferenceData = gson.fromJson(jsonReader, TempConferenceData::class.java)
        return normalize(tempData)
    }

    /**
     * Adds nested objects like `session.tags` to `sessions`
     */
    private fun normalize(data: TempConferenceData): ConferenceData {
        val sessions = mutableListOf<Session>()
        data.sessions.forEach { session: SessionTemp ->
            val tags = data.tags.filter { it.tagName in session.tagNames }
            val newSession = Session(
                id = session.id,
                startTime = session.startTime,
                endTime = session.endTime,
                title = session.title,
                abstract = session.abstract,
                sessionUrl = session.sessionUrl,
                liveStreamUrl = session.liveStreamUrl,
                isLivestream = session.isLivestream,
                youTubeUrl = session.youTubeUrl,
                tags = tags,
                displayTags = tags.filter { it.category == Tag.CATEGORY_TOPIC },
                speakers = session.speakers.mapNotNull { data.speakers[it] }.toSet(),
                photoUrl = session.photoUrl,
                relatedSessions = session.relatedSessions,
                room = data.rooms.firstOrNull { it.id == session.room }
            )
            sessions.add(newSession)
        }

        return ConferenceData(
            sessions = sessions,
            tags = data.tags,
            speakers = data.speakers.values.toList(),
            rooms = data.rooms,
            version = data.version
        )
    }
}

/**
 * Temporary data type for conference data where some collections are lists of IDs instead
 * of lists of domain objects.
 */
@Keep
data class TempConferenceData(
    val sessions: List<SessionTemp>,
    val speakers: Map<String, Speaker>,
    val rooms: List<Room>,
    val tags: List<Tag>,
    val version: Int
)
