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

package com.google.samples.apps.iosched.shared.util

import com.google.gson.GsonBuilder
import com.google.samples.apps.iosched.shared.model.Block
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.model.Tag
import com.google.samples.apps.iosched.shared.util.testdata.BlockDeserializer
import com.google.samples.apps.iosched.shared.util.testdata.SessionDeserializer
import com.google.samples.apps.iosched.shared.util.testdata.SessionTemp
import com.google.samples.apps.iosched.shared.util.testdata.TagDeserializer
import com.google.samples.apps.iosched.shared.util.testdata.TestData
import com.google.samples.apps.iosched.shared.util.testdata.TestDataTemp

private val FILENAME = "conference_data.json"

/**
 * Loads data from JSON file specified in [FILENAME] to be used for development and testing.
 */
object ConferenceDataJsonParser {

    private val testData by lazy { parseConferenceData() }

    private fun parseConferenceData() : TestData {
        val inputStream = this.javaClass.classLoader.getResource(FILENAME)
                .openStream()

        val jsonReader = com.google.gson.stream.JsonReader(inputStream.bufferedReader())

        val gson = GsonBuilder()
                .registerTypeAdapter(SessionTemp::class.java, SessionDeserializer())
                .registerTypeAdapter(Block::class.java, BlockDeserializer())
                .registerTypeAdapter(Tag::class.java, TagDeserializer())
                .create()

        val tempData: TestDataTemp = gson.fromJson(jsonReader, TestDataTemp::class.java)
        return normalize(tempData)
    }

    /**
     * Adds nested objects like `session.tags`.
     */
    private fun normalize(data: TestDataTemp): TestData {
        val sessions: MutableList<Session> = mutableListOf()

        data.sessions.forEach { session: SessionTemp ->
            val newSession = Session(
                    id = session.id,
                    startTime = session.startTime,
                    endTime = session.endTime,
                    title = session.title,
                    abstract = session.abstract,
                    sessionUrl = session.sessionUrl,
                    liveStreamUrl = session.liveStreamUrl,
                    youTubeUrl = session.youTubeUrl,
                    tags = data.tags.filter { it.id in session.tags },
                    speakers = data.speakers.filter { it.id in session.speakers }.toSet(),
                    photoUrl = session.photoUrl,
                    relatedSessions = session.relatedSessions,
                    room = data.rooms.first { it.id == session.room }
            )
            sessions.add(newSession)
        }

        return TestData(sessions = sessions,
                tags = data.tags,
                speakers = data.speakers,
                blocks = data.blocks,
                rooms = data.rooms)
    }

    fun getSessions() = testData.sessions

    fun getSession(sessionId: String): Session {
        return testData.sessions.firstOrNull { it.id == sessionId } ?: throw IllegalStateException(
                "Session $sessionId does not exist.")
    }

    fun getAgenda() = testData.blocks

    fun getTags() = testData.tags
}
