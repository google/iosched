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

import androidx.core.graphics.toColorInt
import com.google.samples.apps.iosched.model.ConferenceData
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.Tag
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDays
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime
import timber.log.Timber

/**
 * ConferenceDataSource data source that never touches the network.
 *
 * This class is only available with the staging variant. It's used for UI tests and
 * faster development.
 */
object FakeConferenceDataSource : ConferenceDataSource {

    override fun getRemoteConferenceData() = getOfflineConferenceData()

    override fun getOfflineConferenceData(): ConferenceData? {
        val bootstrapContent = BootstrapConferenceDataSource.getOfflineConferenceData()

        return transformDataForStaging(bootstrapContent ?: throw Exception("Couldn't load data"))
    }

    private fun transformDataForStaging(data: ConferenceData): ConferenceData {
        val sessions = moveAllSessionsToToday(data.sessions.toMutableList())
        val speakers = data.speakers.toMutableSet()
        val tags = data.tags.toMutableList()

        var lastFirstSession: Session? = null
        // Rename the first sessions of each day

        ConferenceDays.forEachIndexed daysForEach@{ index, day ->
            val firstSessionIndex =
                sessions.indexOfFirst { it.startTime >= day.start && it.endTime <= day.end }
            if (firstSessionIndex == -1) {
                Timber.e("Some sessions are set outside of the conference days")
                return@daysForEach
            }
            val firstSession = sessions[firstSessionIndex]

            sessions.removeAt(firstSessionIndex)
            sessions.add(
                firstSessionIndex,
                firstSession.copy(title = "First session day ${index + 1}")
            )
            lastFirstSession = firstSession
        }

        if (lastFirstSession == null) {
            throw Exception("Conference data does not have sessions.")
        }

        // Give a known ID to an arbitrary session (the second session with tags and speakers)
        val sessionsInRange = sessions
                .filter {
                    it.startTime >= ConferenceDays.first().start &&
                            it.endTime <= ConferenceDays.last().end
                }
        val secondSession = sessionsInRange[2]
        val secondSessionIndex = sessions.indexOf(secondSession)

        // ...also, change its title, id, speaker, related sessions and tags

        val speaker = speakers.first()

        val newTag = Tag(
            displayName = FAKE_SESSION_TAG_NAME, id = "FAKE_TAG", tagName = "topic_staging",
            color = "#39C79D".toColorInt(), fontColor = "#202124".toColorInt(), category = "topic",
            orderInCategory = 13
        )

        tags.add(newTag)

        sessions.removeAt(secondSessionIndex)
        sessions.add(
            secondSessionIndex,
            secondSession.copy(
                id = FAKE_SESSION_ID,
                title = FAKE_SESSION_NAME,
                relatedSessions = setOf(lastFirstSession!!.id),
                speakers = setOf(speaker.copy(name = FAKE_SESSION_SPEAKER_NAME)),
                tags = listOf(newTag),
                displayTags = listOf(newTag)
            )
        )
        addSessionForAlarmsTesting(sessions, secondSession)

        // Return the new data replacing the modified properties only.
        return data.copy(sessions = sessions, speakers = speakers.toList(), tags = tags)
    }

    private fun addSessionForAlarmsTesting(
        sessions: MutableList<Session>,
        exampleSession: Session
    ) {

        val timeIn5Minutes = ZonedDateTime.now().plusMinutes(5).plusSeconds(10)

        sessions.add(
            exampleSession.copy(
                id = ALARM_SESSION_ID,
                title = "Fake session that starts in 5:10 minutes",
                relatedSessions = emptySet(),
                startTime = timeIn5Minutes,
                endTime = timeIn5Minutes.plusMinutes(30)
            )
        )
    }

    private fun moveAllSessionsToToday(sessions: MutableList<Session>): MutableList<Session> {
        val conferenceStart = ConferenceDays.first().start

        sessions.sortBy { it.startTime }

        val firstSessionStartTime = sessions.first().startTime

        val result = mutableListOf<Session>()

        sessions.forEach { session ->
            val delta = Duration.between(firstSessionStartTime, session.startTime)
            val duration = Duration.between(session.startTime, session.endTime)
            result.add(session.copy(startTime = conferenceStart + delta,
                    endTime = conferenceStart + delta + duration))
        }
        return result
    }

    const val FAKE_SESSION_NAME = "Second session on day 1"
    const val FAKE_SESSION_ID = "FAKE_SESSION_ID"
    const val FAKE_SESSION_TAG_NAME = "Staging tag"
    const val FAKE_SESSION_SPEAKER_NAME = "Dr. Staging"
    const val ALARM_SESSION_ID = "abcdefg"
}
