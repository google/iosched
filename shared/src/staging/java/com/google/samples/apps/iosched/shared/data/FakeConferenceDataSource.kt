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

import android.graphics.Color
import com.google.samples.apps.iosched.model.Codelab
import com.google.samples.apps.iosched.model.ConferenceData
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.Tag
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDays
import org.threeten.bp.ZonedDateTime

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
        val sessions = data.sessions.toMutableList()
        val speakers = data.speakers.toMutableList()
        val codelabs = data.codelabs.toMutableList()
        val tags = data.tags.toMutableList()

        // Rename the first session of each day
        ConferenceDays.forEachIndexed daysForEach@{ index, day ->
            val firstSessionIndex = sessions.indexOfFirst { it in day }
            if (firstSessionIndex != -1) {
                val firstSession = sessions[firstSessionIndex]
                sessions.removeAt(firstSessionIndex)
                sessions.add(
                    firstSessionIndex,
                    firstSession.copy(title = "First session day ${index + 1}")
                )
            }
        }

        // Create a fake tag
        val stagingTag = Tag(
            displayName = FAKE_SESSION_TAG_NAME, id = "FAKE_TAG", tagName = "topic_staging",
            color = Color.BLUE, fontColor = Color.CYAN, category = "topic",
            orderInCategory = 99
        )
        tags.add(stagingTag)

        // Create a fake speaker
        val stagingSpeaker =
            speakers.first().copy(id = "FAKE_SPEAKER", name = FAKE_SESSION_SPEAKER_NAME)
        speakers.add(stagingSpeaker)

        // Create a fake session with some known fields on the first day.
        val startTime = ConferenceDays.first().start.plusHours(4)
        val stagingSession = Session(
            id = FAKE_SESSION_ID,
            title = FAKE_SESSION_NAME,
            description = "Staging session description",
            room = data.rooms.first(),
            speakers = setOf(stagingSpeaker),
            tags = listOf(stagingTag),
            displayTags = listOf(stagingTag),
            startTime = startTime,
            endTime = startTime.plusHours(1),
            isLivestream = false,
            photoUrl = "",
            relatedSessions = emptySet(),
            sessionUrl = "",
            doryLink = "",
            youTubeUrl = ""
        )
        sessions.add(stagingSession)

        // Add a fake codelab
        val codelab = Codelab(
            id = FAKE_CODELAB_TITLE,
            title = FAKE_CODELAB_TITLE,
            description = FAKE_CODELAB_DESC,
            codelabUrl = "",
            durationMinutes = 1,
            iconUrl = null,
            sortPriority = Int.MAX_VALUE,
            tags = listOf(stagingTag)
        )
        codelabs.add(0, codelab)

        // TODO: Find a way to test alarms without introducing a session with invalid timestamps.
        // addSessionForAlarmsTesting(sessions, stagingSession)

        // Return the new data replacing the modified properties only.
        return data.copy(sessions = sessions, speakers = speakers, tags = tags, codelabs = codelabs)
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

    const val FAKE_SESSION_NAME = "Fake session on day 1"
    const val FAKE_SESSION_ID = "FAKE_SESSION_ID"
    const val FAKE_SESSION_TAG_NAME = "Staging tag"
    const val FAKE_SESSION_SPEAKER_NAME = "Dr. Staging"
    const val ALARM_SESSION_ID = "abcdefg"

    const val FAKE_CODELAB_TITLE = "Baking an Android Cake"
    const val FAKE_CODELAB_DESC = "In this codelab you'll something something something spatula."
}
