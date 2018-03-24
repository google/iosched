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
import android.databinding.BindingAdapter
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.firestore.entity.UserEvent
import com.google.samples.apps.iosched.widget.ReservationStatus.RESERVABLE
import com.google.samples.apps.iosched.widget.ReservationStatus.RESERVATION_DISABLED
import com.google.samples.apps.iosched.widget.ReservationStatus.RESERVATION_PENDING
import com.google.samples.apps.iosched.widget.ReservationStatus.RESERVED
import com.google.samples.apps.iosched.widget.ReservationStatus.WAIT_LISTED
import com.google.samples.apps.iosched.widget.ReserveButton
import org.threeten.bp.Duration
import org.threeten.bp.ZonedDateTime

@BindingAdapter(value = ["sessionStart", "sessionEnd", "sessionRoom"], requireAll = true)
fun sessionLengthLocation(
    textView: TextView,
    startTime: ZonedDateTime,
    endTime: ZonedDateTime,
    room: String
) {
    textView.text = textView.context.getString(
        R.string.session_duration_location,
        durationString(textView.context, Duration.between(startTime, endTime)), room
    )
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

@BindingAdapter("reservationStatus")
fun reservationStatus(
    reserveButton: ReserveButton,
    userEvent: UserEvent?
) {
    val reservationUnavailable = false // TODO determine this condition
    reserveButton.status = when {
        userEvent == null -> RESERVABLE
        userEvent.isReserved() == true -> RESERVED
        userEvent.isWaitlisted() == true -> WAIT_LISTED
        userEvent.isReservationPending() == true || userEvent.isCancelPending() == true -> {
            // Treat both pending reservations & cancellations the same. This is important as the
            // icon animations all expect to do through the same pending state.
            RESERVATION_PENDING
        }
        // TODO ?? -> WAIT_LIST_AVAILABLE
        reservationUnavailable -> RESERVATION_DISABLED
        else -> RESERVABLE
    }
    reserveButton.isEnabled = !reservationUnavailable
}

@BindingAdapter(value = ["showReservations", "isReservable"], requireAll = true)
fun showReserveButton(
    reserveButton: ReserveButton,
    showReservations: Boolean,
    eventIsReservable: Boolean
) {
    reserveButton.visibility = if (showReservations && eventIsReservable) VISIBLE else GONE
}
