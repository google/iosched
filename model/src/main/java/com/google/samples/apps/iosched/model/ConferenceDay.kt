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

package com.google.samples.apps.iosched.model

import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

private const val formatPattern = "MMMM d"

val FORMATTER_MONTH_DAY: DateTimeFormatter =
    DateTimeFormatter.ofPattern(formatPattern, Locale.getDefault())

data class ConferenceDay(val start: ZonedDateTime, val end: ZonedDateTime) {
    fun contains(session: Session) = start <= session.startTime && end >= session.endTime

    fun formatMonthDay(): String = FORMATTER_MONTH_DAY.format(start)
}