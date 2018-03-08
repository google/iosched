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

package com.google.samples.apps.iosched.shared.data.session.agenda

import com.google.samples.apps.iosched.shared.model.Block
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay

/**
 * TODO: Placeholder
 */
object RemoteAgendaDataSource : AgendaDataSource {

    override fun getAgenda() = AGENDA

    private val AGENDA: List<Block> by lazy {
        listOf(
            // Day 1
            Block(
                title = "Breakfast",
                type = "meal",
                color = 0xff31e7b6.toInt(),
                isDark = false,
                startTime = ConferenceDay.DAY_1.start,
                endTime = ConferenceDay.DAY_1.start.plusMinutes(150L)),
            Block(
                title = "Badge pick-up",
                type = "badge",
                color = 0xffe6e6e6.toInt(),
                isDark = false,
                startTime = ConferenceDay.DAY_1.start,
                endTime = ConferenceDay.DAY_1.start.plusHours(12L)),
            Block(
                title = "Keynote",
                type = "keynote",
                color = 0xfffcd230.toInt(),
                isDark = false,
                startTime = ConferenceDay.DAY_1.start.plusHours(3L),
                endTime = ConferenceDay.DAY_1.start.plusMinutes(270L)),
            Block(
                title = "Lunch",
                type = "meal",
                color = 0xff31e7b6.toInt(),
                isDark = false,
                startTime = ConferenceDay.DAY_1.start.plusMinutes(270L),
                endTime = ConferenceDay.DAY_1.start.plusMinutes(450L)),
            Block(
                title = "Codelabs",
                type = "codelab",
                color = 0xff4768fd.toInt(),
                isDark = true,
                startTime = ConferenceDay.DAY_1.start.plusMinutes(270L),
                endTime = ConferenceDay.DAY_1.start.plusMinutes(750L)),
            Block(
                title = "Sandbox",
                type = "sandbox",
                color = 0xff4768fd.toInt(),
                isDark = true,
                startTime = ConferenceDay.DAY_1.start.plusMinutes(270L),
                endTime = ConferenceDay.DAY_1.start.plusMinutes(750L)),
            Block(
                title = "Office Hours & App Review",
                type = "office_hours",
                color = 0xff4768fd.toInt(),
                isDark = true,
                startTime = ConferenceDay.DAY_1.start.plusMinutes(270L),
                endTime = ConferenceDay.DAY_1.start.plusMinutes(750L)),
            Block(
                title = "I/O Store",
                type = "store",
                color = 0xffffffff.toInt(),
                strokeColor = 0xffff6c00.toInt(),
                isDark = false,
                startTime = ConferenceDay.DAY_1.start.plusMinutes(270L),
                endTime = ConferenceDay.DAY_1.start.plusMinutes(750L)),
            Block(
                title = "Keynote",
                type = "keynote",
                color = 0xfffcd230.toInt(),
                isDark = false,
                startTime = ConferenceDay.DAY_1.start.plusHours(5L),
                endTime = ConferenceDay.DAY_1.start.plusHours(6L)),
            Block(
                title = "Sessions",
                type = "session",
                color = 0xff27e5fd.toInt(),
                isDark = false,
                startTime = ConferenceDay.DAY_1.start.plusHours(7L),
                endTime = ConferenceDay.DAY_1.start.plusHours(12L)),
            Block(
                title = "After hours party",
                type = "after_hours",
                color = 0xff202124.toInt(),
                isDark = true,
                startTime = ConferenceDay.DAY_1.end.minusHours(3L),
                endTime = ConferenceDay.DAY_1.end),

            // Day 2
            Block(
                title = "Breakfast",
                type = "meal",
                color = 0xff31e7b6.toInt(),
                isDark = false,
                startTime = ConferenceDay.DAY_2.start,
                endTime = ConferenceDay.DAY_2.start.plusHours(2L)),
            Block(
                title = "Badge & device pick-up",
                type = "badge",
                color = 0xffe6e6e6.toInt(),
                isDark = false,
                startTime = ConferenceDay.DAY_2.start,
                endTime = ConferenceDay.DAY_2.start.plusHours(11L)),
            Block(
                title = "I/O Store",
                type = "store",
                color = 0xffffffff.toInt(),
                strokeColor = 0xffff6c00.toInt(),
                isDark = false,
                startTime = ConferenceDay.DAY_2.start,
                endTime = ConferenceDay.DAY_2.start.plusHours(12L)),
            Block(
                title = "Sessions",
                type = "session",
                color = 0xff27e5fd.toInt(),
                isDark = false,
                startTime = ConferenceDay.DAY_2.start.plusMinutes(30L),
                endTime = ConferenceDay.DAY_2.end.minusMinutes(150L)),
            Block(
                title = "Codelabs",
                type = "codelab",
                color = 0xff4768fd.toInt(),
                isDark = true,
                startTime = ConferenceDay.DAY_2.start.plusMinutes(30L),
                endTime = ConferenceDay.DAY_2.end.minusHours(2L)),
            Block(
                title = "Sandbox",
                type = "sandbox",
                color = 0xff4768fd.toInt(),
                isDark = true,
                startTime = ConferenceDay.DAY_2.start.plusMinutes(30L),
                endTime = ConferenceDay.DAY_2.end.minusHours(2L)),
            Block(
                title = "Office Hours & App Review",
                type = "office_hours",
                color = 0xff4768fd.toInt(),
                isDark = true,
                startTime = ConferenceDay.DAY_2.start.plusMinutes(30L),
                endTime = ConferenceDay.DAY_2.end.minusHours(2L)),
            Block(
                title = "Lunch",
                type = "meal",
                color = 0xff31e7b6.toInt(),
                isDark = false,
                startTime = ConferenceDay.DAY_2.start.plusMinutes(210L),
                endTime = ConferenceDay.DAY_2.start.plusMinutes(390L)),
            Block(
                title = "Concert",
                type = "concert",
                color = 0xff202124.toInt(),
                isDark = true,
                startTime = ConferenceDay.DAY_2.end.minusMinutes(90L),
                endTime = ConferenceDay.DAY_2.end),

            // Day 3
            Block(
                title = "Breakfast",
                type = "meal",
                color = 0xff31e7b6.toInt(),
                isDark = false,
                startTime = ConferenceDay.DAY_3.start,
                endTime = ConferenceDay.DAY_3.start.plusHours(2L)),
            Block(
                title = "Badge & device pick-up",
                type = "badge",
                color = 0xffe6e6e6.toInt(),
                isDark = false,
                startTime = ConferenceDay.DAY_3.start,
                endTime = ConferenceDay.DAY_3.end),
            Block(
                title = "I/O Store",
                type = "store",
                color = 0xffffffff.toInt(),
                strokeColor = 0xffff6c00.toInt(),
                isDark = false,
                startTime = ConferenceDay.DAY_3.start,
                endTime = ConferenceDay.DAY_3.end.plusHours(1L)),
            Block(
                title = "Sessions",
                type = "session",
                color = 0xff27e5fd.toInt(),
                isDark = false,
                startTime = ConferenceDay.DAY_3.start.plusMinutes(30L),
                endTime = ConferenceDay.DAY_3.end.plusMinutes(30L)),
            Block(
                title = "Codelabs",
                type = "codelab",
                color = 0xff4768fd.toInt(),
                isDark = true,
                startTime = ConferenceDay.DAY_3.start.plusMinutes(30L),
                endTime = ConferenceDay.DAY_3.end),
            Block(
                title = "Sandbox",
                type = "sandbox",
                color = 0xff4768fd.toInt(),
                isDark = true,
                startTime = ConferenceDay.DAY_3.start.plusMinutes(30L),
                endTime = ConferenceDay.DAY_3.end),
            Block(
                title = "Office Hours & App Review",
                type = "office_hours",
                color = 0xff4768fd.toInt(),
                isDark = true,
                startTime = ConferenceDay.DAY_3.start.plusMinutes(30L),
                endTime = ConferenceDay.DAY_3.end),
            Block(
                title = "Lunch",
                type = "meal",
                color = 0xff31e7b6.toInt(),
                isDark = false,
                startTime = ConferenceDay.DAY_3.start.plusMinutes(210L),
                endTime = ConferenceDay.DAY_3.start.plusMinutes(390L))
        )
    }
}
