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

import com.google.samples.apps.iosched.shared.BuildConfig
import com.google.samples.apps.iosched.shared.model.Session
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

object TimeUtils {
    val CONFERENCE_TIMEZONE = ZoneId.of(BuildConfig.CONFERENCE_TIMEZONE)
    val CONFERENCE_DAYS = ConferenceDay.values()

    private val formatPattern = "MMMM d"
    val FORMATTER_MONTH_DAY = DateTimeFormatter.ofPattern(formatPattern, Locale.getDefault())

    enum class ConferenceDay(val start: ZonedDateTime, val end: ZonedDateTime) {
        PRECONFERENCE_DAY(ZonedDateTime.parse(BuildConfig.PRECONFERENCE_DAY_START),
                ZonedDateTime.parse(BuildConfig.PRECONFERENCE_DAY_END)),
        DAY_1(ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY1_START),
                ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY1_END)),
        DAY_2(ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY2_START),
                ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY2_END)),
        DAY_3(ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY3_START),
                ZonedDateTime.parse(BuildConfig.CONFERENCE_DAY3_END));

        fun contains(session: Session) = start <= session.startTime && end >= session.endTime
    }

    fun timeString(startTime: ZonedDateTime, endTime: ZonedDateTime): String {
        val sb = StringBuilder()
        sb.append(DateTimeFormatter.ofPattern("EEE, MMM d, h:mm ").format(startTime))

        val startTimeMeridiem: String = DateTimeFormatter.ofPattern("a").format(startTime)
        val endTimeMeridiem: String = DateTimeFormatter.ofPattern("a").format(endTime)
        if (startTimeMeridiem != endTimeMeridiem) sb.append(startTimeMeridiem).append(" ")

        sb.append(DateTimeFormatter.ofPattern("- h:mm a").format(endTime))
        return sb.toString()
    }
}
