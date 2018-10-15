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

package com.google.samples.apps.adssched.shared.data.session.agenda

import com.google.samples.apps.adssched.model.Block
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single point of access to agenda data for the presentation layer.
 */
@Singleton
open class AgendaRepository @Inject constructor() {

    companion object {
        private const val LABEL_REGISTRATION = "Registration"
        private const val LABEL_KEYNOTE = "Keynote"
        private const val LABEL_SESSIONS = "Sessions"
        private const val LABEL_BREAKFAST = "Breakfast"
        private const val LABEL_LUNCH = "Lunch"
        private const val LABEL_TEA_BREAK = "Tea Break"
        private const val LABEL_PARTY = "Party"

        private const val TYPE_REGISTRATION = "badge"
        private const val TYPE_KEYNOTE = "keynote"
        private const val TYPE_SESSIONS = "session"
        private const val TYPE_MEAL = "meal"
        private const val TYPE_AFTER_HOURS = "after_hours"

        private const val COLOR_REGISTRATION = 0xffe6e6e6.toInt()
        private const val COLOR_KEYNOTE = 0xfffdc93e.toInt()
        private const val COLOR_SESSIONS = 0xff73bbf5.toInt()
        private const val COLOR_MEAL = 0xff9bdd7c.toInt()
        private const val COLOR_AFTER_HOURS = 0xff202124.toInt()
    }

    private val blocks by lazy {
        listOf(
            Block(
                title = LABEL_BREAKFAST,
                type = TYPE_MEAL,
                color = COLOR_MEAL,
                startTime = ZonedDateTime.parse("2018-11-07T08:00-08:00"),
                endTime = ZonedDateTime.parse("2018-11-07T10:00-08:00")
            ),
            Block(
                title = LABEL_REGISTRATION,
                type = TYPE_REGISTRATION,
                color = COLOR_REGISTRATION,
                startTime = ZonedDateTime.parse("2018-11-07T08:00-08:00"),
                endTime = ZonedDateTime.parse("2018-11-07T10:00-08:00")
            ),
            Block(
                title = LABEL_KEYNOTE,
                type = TYPE_KEYNOTE,
                color = COLOR_KEYNOTE,
                startTime = ZonedDateTime.parse("2018-11-07T10:00-08:00"),
                endTime = ZonedDateTime.parse("2018-11-07T11:00-08:00")
            ),
            Block(
                title = LABEL_SESSIONS,
                type = TYPE_SESSIONS,
                color = COLOR_SESSIONS,
                startTime = ZonedDateTime.parse("2018-11-07T11:15-08:00"),
                endTime = ZonedDateTime.parse("2018-11-07T12:45-08:00")
            ),
            Block(
                title = LABEL_LUNCH,
                type = TYPE_MEAL,
                color = COLOR_MEAL,
                startTime = ZonedDateTime.parse("2018-11-07T12:45-08:00"),
                endTime = ZonedDateTime.parse("2018-11-07T14:00-08:00")
            ),
            Block(
                title = LABEL_SESSIONS,
                type = TYPE_SESSIONS,
                color = COLOR_SESSIONS,
                startTime = ZonedDateTime.parse("2018-11-07T14:00-08:00"),
                endTime = ZonedDateTime.parse("2018-11-07T15:30-08:00")
            ),
            Block(
                title = LABEL_TEA_BREAK,
                type = TYPE_MEAL,
                color = COLOR_MEAL,
                startTime = ZonedDateTime.parse("2018-11-07T15:30-08:00"),
                endTime = ZonedDateTime.parse("2018-11-07T16:00-08:00")
            ),
            Block(
                title = LABEL_SESSIONS,
                type = TYPE_SESSIONS,
                color = COLOR_SESSIONS,
                startTime = ZonedDateTime.parse("2018-11-07T16:00-08:00"),
                endTime = ZonedDateTime.parse("2018-11-07T18:20-08:00")
            ),
            Block(
                title = LABEL_PARTY,
                type = TYPE_AFTER_HOURS,
                color = COLOR_AFTER_HOURS,
                isDark = true,
                startTime = ZonedDateTime.parse("2018-11-07T18:20-08:00"),
                endTime = ZonedDateTime.parse("2018-11-07T22:20-08:00")
            ),

            Block(
                title = LABEL_BREAKFAST,
                type = TYPE_MEAL,
                color = COLOR_MEAL,
                startTime = ZonedDateTime.parse("2018-11-08T08:00-08:00"),
                endTime = ZonedDateTime.parse("2018-11-08T09:30-08:00")
            ),
            Block(
                title = LABEL_REGISTRATION,
                type = TYPE_REGISTRATION,
                color = COLOR_REGISTRATION,
                startTime = ZonedDateTime.parse("2018-11-08T08:00-08:00"),
                endTime = ZonedDateTime.parse("2018-11-08T09:30-08:00")
            ),
            Block(
                title = LABEL_SESSIONS,
                type = TYPE_SESSIONS,
                color = COLOR_SESSIONS,
                startTime = ZonedDateTime.parse("2018-11-08T09:30-08:00"),
                endTime = ZonedDateTime.parse("2018-11-08T11:50-08:00")
            ),
            Block(
                title = LABEL_LUNCH,
                type = TYPE_MEAL,
                color = COLOR_MEAL,
                startTime = ZonedDateTime.parse("2018-11-08T11:50-08:00"),
                endTime = ZonedDateTime.parse("2018-11-08T13:05-08:00")
            ),
            Block(
                title = LABEL_SESSIONS,
                type = TYPE_SESSIONS,
                color = COLOR_SESSIONS,
                startTime = ZonedDateTime.parse("2018-11-08T13:05-08:00"),
                endTime = ZonedDateTime.parse("2018-11-08T15:25-08:00")
            ),
            Block(
                title = LABEL_TEA_BREAK,
                type = TYPE_MEAL,
                color = COLOR_MEAL,
                startTime = ZonedDateTime.parse("2018-11-08T15:25-08:00"),
                endTime = ZonedDateTime.parse("2018-11-08T15:55-08:00")
            ),
            Block(
                title = LABEL_SESSIONS,
                type = TYPE_SESSIONS,
                color = COLOR_SESSIONS,
                startTime = ZonedDateTime.parse("2018-11-08T15:55-08:00"),
                endTime = ZonedDateTime.parse("2018-11-08T17:25-08:00")
            )
        )
    }

    fun getAgenda(): List<Block> = blocks
}
