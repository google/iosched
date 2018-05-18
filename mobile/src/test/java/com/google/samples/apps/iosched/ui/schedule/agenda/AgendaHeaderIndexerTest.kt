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

package com.google.samples.apps.iosched.ui.schedule.agenda

import com.google.samples.apps.iosched.test.data.TestData
import org.junit.Assert.assertEquals
import org.junit.Test
import org.threeten.bp.ZonedDateTime

class AgendaHeaderIndexerTest {

    @Test
    fun indexAgenda_groupsCorrectly() {
        // Given a list of test blocks starting over 3 days
        val block = TestData.block1
        val start = ZonedDateTime.parse("2018-05-08T07:00:00-07:00")
        val sessions = listOf(
            block.copy(startTime = start),
            block.copy(startTime = start),
            block.copy(startTime = start.plusDays(1)),
            block.copy(startTime = start.plusDays(1)),
            block.copy(startTime = start.plusDays(2)),
            block.copy(startTime = start.plusDays(2))
        )

        // Process this list to group by start day, keyed on index
        val grouped = indexAgendaHeaders(sessions).toMap()

        // Then verify that the correct groupings are made and indexes used
        assertEquals(3, grouped.size)
        assertEquals(setOf(0, 2, 4), grouped.keys)
        assertEquals(start, grouped[0])
        assertEquals(start.plusDays(1), grouped[2])
        assertEquals(start.plusDays(2), grouped[4])
    }

    @Test
    fun indexAgenda_roundsDayDown() {
        // Given a list of test blocks where a block straddles two days
        val block = TestData.block1
        val dayOneSevenAM = ZonedDateTime.parse("2018-05-08T07:00:00-07:00")
        val dayOneEightAM = ZonedDateTime.parse("2018-05-08T08:00:00-07:00")
        val dayOneElevenPM = ZonedDateTime.parse("2018-05-08T23:00:00-07:00")
        val dayTwoOneAM = ZonedDateTime.parse("2018-05-09T01:00:00-07:00")
        val dayTwoSevenAM = ZonedDateTime.parse("2018-05-09T07:00:00-07:00")
        val dayTwoEightAM = ZonedDateTime.parse("2018-05-09T08:00:00-07:00")

        val sessions = listOf(
            block.copy(startTime = dayOneSevenAM, endTime = dayOneEightAM),
            block.copy(startTime = dayOneElevenPM, endTime = dayTwoOneAM), // straddling day
            block.copy(startTime = dayTwoSevenAM, endTime = dayTwoEightAM)
        )

        // Process this list to group by start time, keyed on index
        val grouped = indexAgendaHeaders(sessions).toMap()

        // Then verify that the correct groupings are made; the straddling block should be grouped
        // with the day it starts in.
        assertEquals(2, grouped.size)
        assertEquals(setOf(0, 2), grouped.keys)
        assertEquals(dayOneSevenAM, grouped[0])
        assertEquals(dayTwoSevenAM, grouped[2])
    }
}
