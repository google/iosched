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

import com.google.samples.apps.iosched.shared.model.Room
import com.google.samples.apps.iosched.shared.model.Session
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay
import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDay.DAY_1
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeUtilsTest {

    @Test
    fun conferenceDay_contains() {
        val room1 = Room(id = "1", name = "Tent 1", capacity = 40)

        val inDay1 = Session("1", ConferenceDay.DAY_1.start, ConferenceDay.DAY_1.end,
                "", "", room1, "", "", "", emptyList(), emptySet(), "", emptySet())
        assertTrue(DAY_1.contains(inDay1))

        // Starts before DAY_1
        val day1MinusMinute = ConferenceDay.DAY_1.start.minusMinutes(1)
        val notInDay1 = Session("2", day1MinusMinute, ConferenceDay.DAY_1.end,
                "", "", room1, "", "", "", emptyList(), emptySet(), "", emptySet())
        assertFalse(DAY_1.contains(notInDay1))

        // Ends after DAY_1
        val day1PlusMinute = ConferenceDay.DAY_1.end.plusMinutes(1)
        val alsoNotInDay1 = Session("3", ConferenceDay.DAY_1.start, day1PlusMinute,
                "", "", room1, "", "", "", emptyList(), emptySet(), "", emptySet())
        assertFalse(DAY_1.contains(alsoNotInDay1))
    }
}
