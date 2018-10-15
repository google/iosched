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
    private val blocks by lazy {
        listOf(
            Block(
                title = "Breakfast",
                type = "meal",
                color = 0xff31e7b6.toInt(),
                startTime = ZonedDateTime.parse("2018-11-07T08:00-08:00"),
                endTime = ZonedDateTime.parse("2018-11-07T10:00-08:00")
            ),
            Block(
                title = "Registration",
                type = "badge",
                color = 0xffe6e6e6.toInt(),
                startTime = ZonedDateTime.parse("2018-11-07T08:00-08:00"),
                endTime = ZonedDateTime.parse("2018-11-07T10:00-08:00")
            ),
            Block(
                title = "Keynote",
                type = "keynote",
                color = 0xfffcd230.toInt(),
                startTime = ZonedDateTime.parse("2018-11-07T10:00-08:00"),
                endTime = ZonedDateTime.parse("2018-11-07T11:00-08:00")
            ),
            Block(
                title = "Sessions",
                type = "session",
                color = 0xff27e5fd.toInt(),
                startTime = ZonedDateTime.parse("2018-11-07T11:15-08:00"),
                endTime = ZonedDateTime.parse("2018-11-07T12:45-08:00")
            ),
            Block(
                title = "Lunch",
                type = "meal",
                color = 0xff31e7b6.toInt(),
                startTime = ZonedDateTime.parse("2018-11-07T12:45-08:00"),
                endTime = ZonedDateTime.parse("2018-11-07T14:00-08:00")
            ),
            Block(
                title = "Sessions",
                type = "session",
                color = 0xff27e5fd.toInt(),
                startTime = ZonedDateTime.parse("2018-11-07T14:00-08:00"),
                endTime = ZonedDateTime.parse("2018-11-07T15:30-08:00")
            ),
            Block(
                title = "Tea Break",
                type = "meal",
                color = 0xff31e7b6.toInt(),
                startTime = ZonedDateTime.parse("2018-11-07T15:30-08:00"),
                endTime = ZonedDateTime.parse("2018-11-07T16:00-08:00")
            ),
            Block(
                title = "Sessions",
                type = "session",
                color = 0xff27e5fd.toInt(),
                startTime = ZonedDateTime.parse("2018-11-07T16:00-08:00"),
                endTime = ZonedDateTime.parse("2018-11-07T18:20-08:00")
            ),
            Block(
                title = "Party",
                type = "after_hours",
                color = 0xff202124.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse("2018-11-07T18:20-08:00"),
                endTime = ZonedDateTime.parse("2018-11-07T22:20-08:00")
            ),
            
            Block(
                title = "Breakfast",
                type = "meal",
                color = 0xff31e7b6.toInt(),
                startTime = ZonedDateTime.parse("2018-11-08T08:00-08:00"),
                endTime = ZonedDateTime.parse("2018-11-08T09:30-08:00")
            ),
            Block(
                title = "Registration",
                type = "badge",
                color = 0xffe6e6e6.toInt(),
                startTime = ZonedDateTime.parse("2018-11-08T08:00-08:00"),
                endTime = ZonedDateTime.parse("2018-11-08T09:30-08:00")
            ),
            Block(
                title = "Sessions",
                type = "session",
                color = 0xff27e5fd.toInt(),
                startTime = ZonedDateTime.parse("2018-11-08T09:30-08:00"),
                endTime = ZonedDateTime.parse("2018-11-08T11:50-08:00")
            ),
            Block(
                title = "Lunch",
                type = "meal",
                color = 0xff31e7b6.toInt(),
                startTime = ZonedDateTime.parse("2018-11-08T11:50-08:00"),
                endTime = ZonedDateTime.parse("2018-11-08T13:05-08:00")
            ),
            Block(
                title = "Sessions",
                type = "session",
                color = 0xff27e5fd.toInt(),
                startTime = ZonedDateTime.parse("2018-11-08T13:05-08:00"),
                endTime = ZonedDateTime.parse("2018-11-08T15:25-08:00")
            ),
            Block(
                title = "Tea Break",
                type = "meal",
                color = 0xff31e7b6.toInt(),
                startTime = ZonedDateTime.parse("2018-11-08T15:25-08:00"),
                endTime = ZonedDateTime.parse("2018-11-08T15:55-08:00")
            ),
            Block(
                title = "Sessions",
                type = "session",
                color = 0xff27e5fd.toInt(),
                startTime = ZonedDateTime.parse("2018-11-08T15:55-08:00"),
                endTime = ZonedDateTime.parse("2018-11-08T17:25-08:00")
            )
        )
    }

    fun getAgenda(): List<Block> = blocks
}
