/*
 * Copyright 2019 Google LLC
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

package com.google.samples.apps.iosched.shared.data.agenda

import com.google.samples.apps.iosched.model.Block
import org.threeten.bp.ZonedDateTime

/**
 * Single point of access to agenda data for the presentation layer.
 */
interface AgendaRepository {
    fun getAgenda(): List<Block>
}

class DefaultAgendaRepository : AgendaRepository {
    private val blocks by lazy {
        listOf(
            Block(
                title = "Badge pick-up",
                type = "badge",
                color = 0xffe6e6e6.toInt(),
                startTime = ZonedDateTime.parse("2019-05-06T07:00-07:00"),
                endTime = ZonedDateTime.parse("2019-05-06T19:00-07:00")
            ),
            Block(
                title = "Badge pick-up",
                type = "badge",
                color = 0xffe6e6e6.toInt(),
                startTime = ZonedDateTime.parse("2019-05-07T07:00-07:00"),
                endTime = ZonedDateTime.parse("2019-05-07T19:00-07:00")
            ),
            Block(
                title = "Breakfast",
                type = "meal",
                color = 0xfffad2ce.toInt(),
                startTime = ZonedDateTime.parse("2019-05-07T07:00-07:00"),
                endTime = ZonedDateTime.parse("2019-05-07T09:30-07:00")
            ),
            Block(
                title = "Google Keynote",
                type = "keynote",
                color = 0xfffbbc05.toInt(),
                startTime = ZonedDateTime.parse("2019-05-07T10:00-07:00"),
                endTime = ZonedDateTime.parse("2019-05-07T11:30-07:00")
            ),
            Block(
                title = "I/O Store",
                type = "store",
                color = 0xffffffff.toInt(),
                strokeColor = 0xffff6c00.toInt(),
                startTime = ZonedDateTime.parse("2019-05-07T11:30-07:00"),
                endTime = ZonedDateTime.parse("2019-05-07T19:30-07:00")
            ),
            Block(
                title = "Lunch",
                type = "meal",
                color = 0xfffad2ce.toInt(),
                startTime = ZonedDateTime.parse("2019-05-07T11:30-07:00"),
                endTime = ZonedDateTime.parse("2019-05-07T12:45-07:00")
            ),
            Block(
                title = "Developer Keynote",
                type = "keynote",
                color = 0xfffbbc05.toInt(),
                startTime = ZonedDateTime.parse("2019-05-07T12:45-07:00"),
                endTime = ZonedDateTime.parse("2019-05-07T14:00-07:00")
            ),
            Block(
                title = "Sessions",
                type = "session",
                isDark = true,
                color = 0xff5bb975.toInt(),
                startTime = ZonedDateTime.parse("2019-05-07T14:00-07:00"),
                endTime = ZonedDateTime.parse("2019-05-07T19:00-07:00")
            ),
            Block(
                title = "Codelabs",
                type = "codelab",
                color = 0xff4285f4.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse("2019-05-07T14:00-07:00"),
                endTime = ZonedDateTime.parse("2019-05-07T19:00-07:00")
            ),
            Block(
                title = "Office Hours & App Review",
                type = "office_hours",
                color = 0xff4285f4.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse("2019-05-07T14:00-07:00"),
                endTime = ZonedDateTime.parse("2019-05-07T19:00-07:00")
            ),
            Block(
                title = "Sandboxes",
                type = "sandbox",
                color = 0xff4285f4.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse("2019-05-07T14:00-07:00"),
                endTime = ZonedDateTime.parse("2019-05-07T19:30-07:00")
            ),
            Block(
                title = "After Dark",
                type = "after_hours",
                color = 0xff164fa5.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse("2019-05-07T18:30-07:00"),
                endTime = ZonedDateTime.parse("2019-05-07T22:00-07:00")
            ),
            Block(
                title = "Badge & device pick-up",
                type = "badge",
                color = 0xffe6e6e6.toInt(),
                startTime = ZonedDateTime.parse("2019-05-08T08:00-07:00"),
                endTime = ZonedDateTime.parse("2019-05-08T19:00-07:00")
            ),
            Block(
                title = "Breakfast",
                type = "meal",
                color = 0xfffad2ce.toInt(),
                startTime = ZonedDateTime.parse("2019-05-08T08:00-07:00"),
                endTime = ZonedDateTime.parse("2019-05-08T10:00-07:00")
            ),
            Block(
                title = "I/O Store",
                type = "store",
                color = 0xffffffff.toInt(),
                strokeColor = 0xffff6c00.toInt(),
                startTime = ZonedDateTime.parse("2019-05-08T08:00-07:00"),
                endTime = ZonedDateTime.parse("2019-05-08T20:00-07:00")
            ),
            Block(
                title = "Lunch",
                type = "meal",
                color = 0xfffad2ce.toInt(),
                startTime = ZonedDateTime.parse("2019-05-08T11:30-07:00"),
                endTime = ZonedDateTime.parse("2019-05-08T14:30-07:00")
            ),
            Block(
                title = "Sessions",
                type = "session",
                isDark = true,
                color = 0xff5bb975.toInt(),
                startTime = ZonedDateTime.parse("2019-05-08T08:30-07:00"),
                endTime = ZonedDateTime.parse("2019-05-08T19:30-07:00")
            ),
            Block(
                title = "Codelabs",
                type = "codelab",
                color = 0xff4285f4.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse("2019-05-08T08:30-07:00"),
                endTime = ZonedDateTime.parse("2019-05-08T19:15-07:00")
            ),
            Block(
                title = "Office Hours & App Reviews",
                type = "office_hours",
                color = 0xff4285f4.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse("2019-05-08T08:30-07:00"),
                endTime = ZonedDateTime.parse("2019-05-08T19:30-07:00")
            ),
            Block(
                title = "Sandboxes",
                type = "sandbox",
                color = 0xff4285f4.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse("2019-05-08T08:30-07:00"),
                endTime = ZonedDateTime.parse("2019-05-08T19:15-07:00")
            ),
            Block(
                title = "Concert",
                type = "concert",
                color = 0xff164fa5.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse("2019-05-08T19:45-07:00"),
                endTime = ZonedDateTime.parse("2019-05-08T22:00-07:00")
            ),
            Block(
                title = "Badge & device pick-up",
                type = "badge",
                color = 0xffe6e6e6.toInt(),
                startTime = ZonedDateTime.parse("2019-05-09T08:00-07:00"),
                endTime = ZonedDateTime.parse("2019-05-09T16:00-07:00")
            ),
            Block(
                title = "Breakfast",
                type = "meal",
                color = 0xfffad2ce.toInt(),
                startTime = ZonedDateTime.parse("2019-05-09T08:00-07:00"),
                endTime = ZonedDateTime.parse("2019-05-09T10:00-07:00")
            ),
            Block(
                title = "I/O Store",
                type = "store",
                color = 0xffffffff.toInt(),
                strokeColor = 0xffff6c00.toInt(),
                startTime = ZonedDateTime.parse("2019-05-09T08:00-07:00"),
                endTime = ZonedDateTime.parse("2019-05-09T17:00-07:00")
            ),
            Block(
                title = "Lunch",
                type = "meal",
                color = 0xfffad2ce.toInt(),
                startTime = ZonedDateTime.parse("2019-05-09T11:30-07:00"),
                endTime = ZonedDateTime.parse("2019-05-09T14:30-07:00")
            ),
            Block(
                title = "Sessions",
                type = "session",
                isDark = true,
                color = 0xff5bb975.toInt(),
                startTime = ZonedDateTime.parse("2019-05-09T08:30-07:00"),
                endTime = ZonedDateTime.parse("2019-05-09T16:30-07:00")
            ),
            Block(
                title = "Codelabs",
                type = "codelab",
                color = 0xff4285f4.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse("2019-05-09T08:30-07:00"),
                endTime = ZonedDateTime.parse("2019-05-09T16:00-07:00")
            ),
            Block(
                title = "Office Hours & App Reviews",
                type = "office_hours",
                color = 0xff4285f4.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse("2019-05-09T08:30-07:00"),
                endTime = ZonedDateTime.parse("2019-05-09T16:00-07:00")
            ),
            Block(
                title = "Sandboxes",
                type = "sandbox",
                color = 0xff4285f4.toInt(),
                isDark = true,
                startTime = ZonedDateTime.parse("2019-05-09T08:30-07:00"),
                endTime = ZonedDateTime.parse("2019-05-09T16:00-07:00")
            )
        )
    }

    override fun getAgenda(): List<Block> = blocks
}
