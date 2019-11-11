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

package com.google.samples.apps.iosched.ui.schedule.day

import android.content.Context
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.Room
import com.google.samples.apps.iosched.shared.util.TimeUtils
import org.threeten.bp.Duration
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

@BindingAdapter(
    "sessionStart",
    "sessionEnd",
    "alwaysShowDate",
    "sessionRoom",
    "timeZoneId",
    requireAll = true
)
fun sessionLengthLocation(
    textView: TextView,
    startTime: ZonedDateTime,
    endTime: ZonedDateTime,
    alwaysShowDate: Boolean,
    room: Room,
    timeZoneId: ZoneId?
) {
    val finalTimeZoneId = timeZoneId ?: TimeUtils.CONFERENCE_TIMEZONE
    val localStartTime = TimeUtils.zonedTime(startTime, finalTimeZoneId)
    val localEndTime = TimeUtils.zonedTime(endTime, finalTimeZoneId)
    textView.text = if (alwaysShowDate) {
        // In places where sessions are shown without day/time labels, show the full date & time
        // (respecting timezone) plus location. Example: "Wed, May 9, 9 – 10 am / Stage 1"
        fullDateTime(localStartTime, localEndTime, textView, room)
    } else if (finalTimeZoneId != TimeUtils.CONFERENCE_TIMEZONE) {
        // Show the local time, the duration, and the abbreviated room name.
        // Example: "Tue, May 8 / 1 hour / Stage 1"
        textView.context.getString(
            R.string.session_date_duration_location,
            TimeUtils.abbreviatedTimeString(localStartTime),
            durationString(textView.context, Duration.between(localStartTime, localEndTime)),
            room.abbreviatedName
        )
    } else {
        // Assume user is at the conference and show the duration and the full room name
        // Example: "1 hour / Stage2||Hydra"
        textView.context.getString(
            R.string.session_duration_location,
            durationString(textView.context, Duration.between(startTime, endTime)), room.name
        )
    }
    // For accessibility, always use the full date time; without this, sticky headers will confuse
    // a Talkback user.
    textView.contentDescription = fullDateTime(localStartTime, localEndTime, textView, room)
}

private fun fullDateTime(
    localStartTime: ZonedDateTime,
    localEndTime: ZonedDateTime,
    textView: TextView,
    room: Room
): String {
    val timeString = TimeUtils.timeString(localStartTime, localEndTime)
    return textView.context.getString(R.string.session_duration_location, timeString, room.name)
}

private fun durationString(context: Context, duration: Duration): String {
    val hours = duration.toHours()
    return if (hours > 0L) {
        context.resources.getQuantityString(R.plurals.duration_hours, hours.toInt(), hours)
    } else {
        val minutes = duration.toMinutes()
        context.resources.getQuantityString(R.plurals.duration_minutes, minutes.toInt(), minutes)
    }
}