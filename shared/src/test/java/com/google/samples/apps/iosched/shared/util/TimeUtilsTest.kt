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

import com.google.samples.apps.iosched.shared.util.TimeUtils.ConferenceDays
import com.google.samples.apps.iosched.test.data.TestData
import com.google.samples.apps.iosched.test.data.TestData.session0
import org.junit.Assert
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.threeten.bp.ZonedDateTime
import java.util.regex.Pattern

class TimeUtilsTest {

    private lateinit var time0800: ZonedDateTime
    private lateinit var time1000: ZonedDateTime
    private lateinit var time1300: ZonedDateTime

    @Before
    fun setup() {
        time0800 = ZonedDateTime.parse("2018-05-08T08:00:00.000-07:00[America/Los_Angeles]")
        time1000 = ZonedDateTime.parse("2018-05-08T10:00:00.000-07:00[America/Los_Angeles]")
        time1300 = ZonedDateTime.parse("2018-05-08T13:00:00.000-07:00[America/Los_Angeles]")
    }

    @Test
    fun conferenceDay_contains() {
        val inDay1 = session0.copy(
            startTime = ConferenceDays.first().start,
            endTime = ConferenceDays.first().end
        )
        assertTrue(ConferenceDays.first().contains(inDay1))

        // Starts before DAY_1
        val notInDay1 = session0.copy(
            startTime = ConferenceDays.first().start.minusMinutes(1),
            endTime = ConferenceDays.first().end
        )
        assertFalse(ConferenceDays.first().contains(notInDay1))

        // Ends after DAY_1
        val alsoNotInDay1 = session0.copy(
            startTime = ConferenceDays.first().start,
            endTime = ConferenceDays.first().end.plusMinutes(1)
        )
        assertFalse(ConferenceDays.first().contains(alsoNotInDay1))
    }

    @Test
    fun conferenceDay_formatMonthDay() {
        val pattern = Pattern.compile("""0\d""") // zero followed by any digit
        ConferenceDays.forEach {
            assertFalse(pattern.matcher(it.formatMonthDay()).find())
        }
    }

    @Test
    fun getSessionState_unknown() {
        Assert.assertEquals(
            TimeUtils.SessionRelativeTimeState.UNKNOWN,
            TimeUtils.getSessionState(null)
        )
        Assert.assertEquals(
            TimeUtils.SessionRelativeTimeState.UNKNOWN,
            TimeUtils.getSessionState(null, time0800)
        )
    }

    @Test
    fun getSessionState_before() {
        val session = TestData.session0
        Assert.assertEquals(
            TimeUtils.SessionRelativeTimeState.BEFORE,
            TimeUtils.getSessionState(session, session.startTime.minusHours(1L))
        )
    }

    @Test
    fun getSessionState_after() {
        val session = TestData.session0
        Assert.assertEquals(
            TimeUtils.SessionRelativeTimeState.AFTER,
            TimeUtils.getSessionState(session, session.endTime.plusHours(1L))
        )
    }

    @Test
    fun getSessionState_during() {
        val session = TestData.session0
        Assert.assertEquals(
            TimeUtils.SessionRelativeTimeState.DURING,
            TimeUtils.getSessionState(session, session.startTime)
        )
        Assert.assertEquals(
            TimeUtils.SessionRelativeTimeState.DURING,
            TimeUtils.getSessionState(session, session.startTime.plusMinutes(1L))
        )
        Assert.assertEquals(
            TimeUtils.SessionRelativeTimeState.DURING,
            TimeUtils.getSessionState(session, session.endTime)
        )
    }

    @Test
    fun abbreviatedTimeString() {
        val a = ZonedDateTime.parse("2018-04-01T12:30:40Z[GMT]")
        Assert.assertEquals(TimeUtils.abbreviatedTimeString(a), "Sun, Apr 1")
    }

    @Test
    fun timeString_sameMeridiem() {
        Assert.assertEquals(
            "Tue, May 8, 10:00 - 11:00 AM",
            TimeUtils.timeString(time1000, time1000.plusHours(1))
        )
        Assert.assertEquals(
            "Tue, May 8, 1:00 - 2:00 PM",
            TimeUtils.timeString(time1300, time1300.plusHours(1))
        )
    }

    @Test
    fun timeString_differentMeridiem() {
        Assert.assertEquals(
            "Tue, May 8, 10:00 AM - 12:00 PM",
            TimeUtils.timeString(time1000, time1000.plusHours(2))
        )
    }

    @Test
    fun timeString_omitsLeadingZeroInDate() {
        Assert.assertEquals(
            "Tue, May 8, 8:00 - 9:00 AM",
            TimeUtils.timeString(time0800, time0800.plusHours(1))
        )
        val timeMay10 = ZonedDateTime.parse("2018-05-10T13:00:00.000-07:00[America/Los_Angeles]")
        Assert.assertEquals(
            "Thu, May 10, 1:00 - 2:00 PM",
            TimeUtils.timeString(timeMay10, timeMay10.plusHours(1))
        )
    }

    @Test
    fun timeString_omitsLeadingZeroInTime() {
        Assert.assertEquals(
            "Tue, May 8, 8:00 - 9:00 AM",
            TimeUtils.timeString(time0800, time0800.plusHours(1))
        )
        Assert.assertEquals(
            "Tue, May 8, 8:00 - 10:00 AM",
            TimeUtils.timeString(time0800, time1000)
        )
        Assert.assertEquals(
            "Tue, May 8, 12:00 - 1:00 PM",
            TimeUtils.timeString(time1300.minusHours(1), time1300)
        )
    }
}
