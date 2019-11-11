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

package com.google.samples.apps.iosched.shared.util

import com.google.samples.apps.iosched.model.Block
import com.google.samples.apps.iosched.shared.data.agenda.AgendaRepository

class FakeAgendaRepository(blocks: List<Block>) : AgendaRepository {

    constructor(vararg blocks: Block) : this(blocks.toList())

    val agenda = blocks.toMutableList()

    override suspend fun getAgenda(forceRefresh: Boolean): List<Block> = agenda
}

val afterConferenceBlock = Block(
    title = "I'm disabled",
    type = "meal",
    color = 0xffff00ff.toInt(),
    startTime = TimeUtils.ConferenceDays[0].start,
    endTime = TimeUtils.ConferenceDays.last().end.plusHours(15)
)

val beforeConferenceBlock = Block(
    title = "I'm disabled",
    type = "meal",
    color = 0xffff00ff.toInt(),
    startTime = TimeUtils.ConferenceDays[0].start.minusDays(1L),
    endTime = TimeUtils.ConferenceDays.last().end
)

val disabledBlock = Block(
    title = "I'm disabled",
    type = "meal",
    color = 0xffff00ff.toInt(),
    startTime = TimeUtils.ConferenceDays[0].start.plusHours(1L),
    endTime = TimeUtils.ConferenceDays[0].start.plusHours(1L)
)

val validBlock1 = Block(
    title = "I'm not disabled",
    type = "meal",
    color = 0xffff00ff.toInt(),
    startTime = TimeUtils.ConferenceDays[0].start.plusHours(1L),
    endTime = TimeUtils.ConferenceDays[0].start.plusHours(2L)
)

val validBlock2 = Block(
    title = "I'm not disabled",
    type = "meal",
    color = 0xffff00ff.toInt(),
    startTime = TimeUtils.ConferenceDays[0].start.plusHours(2L),
    endTime = TimeUtils.ConferenceDays[0].start.plusHours(3L)
)

object AgendaTestData {
    val agenda = listOf(validBlock1, validBlock2)
}
