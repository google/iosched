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

package com.google.samples.apps.iosched.ui.sessiondetail

import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.TextView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.SessionType.AFTER_DARK
import com.google.samples.apps.iosched.model.SessionType.APP_REVIEW
import com.google.samples.apps.iosched.model.SessionType.GAME_REVIEW
import com.google.samples.apps.iosched.model.SessionType.KEYNOTE
import com.google.samples.apps.iosched.model.SessionType.OFFICE_HOURS
import com.google.samples.apps.iosched.model.SessionType.SESSION
import com.google.samples.apps.iosched.model.userdata.UserEvent
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.ui.reservation.ReservationViewState
import com.google.samples.apps.iosched.ui.reservation.ReservationViewState.RESERVABLE
import com.google.samples.apps.iosched.ui.reservation.StarReserveFab
import org.threeten.bp.Duration
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

/* Narrow headers used for events that have neither a photo nor video url. */
@BindingAdapter("eventNarrowHeader")
fun eventNarrowHeaderImage(imageView: ImageView, session: Session?) {
    session ?: return

    val resId = when (session.type) {
        KEYNOTE -> R.drawable.event_narrow_keynote
        // For the next few types, we choose a random image, but we should use the same image for a
        // given event, so use the id to pick.
        SESSION -> when (session.id.hashCode() % 4) {
            0 -> R.drawable.event_narrow_session1
            1 -> R.drawable.event_narrow_session2
            2 -> R.drawable.event_narrow_session3
            else -> R.drawable.event_narrow_session4
        }
        OFFICE_HOURS -> when (session.id.hashCode() % 3) {
            0 -> R.drawable.event_narrow_office_hours1
            1 -> R.drawable.event_narrow_office_hours2
            else -> R.drawable.event_narrow_office_hours3
        }
        APP_REVIEW -> when (session.id.hashCode() % 3) {
            0 -> R.drawable.event_narrow_app_reviews1
            1 -> R.drawable.event_narrow_app_reviews2
            else -> R.drawable.event_narrow_app_reviews3
        }
        GAME_REVIEW -> when (session.id.hashCode() % 3) {
            0 -> R.drawable.event_narrow_game_reviews1
            1 -> R.drawable.event_narrow_game_reviews2
            else -> R.drawable.event_narrow_game_reviews3
        }
        AFTER_DARK -> R.drawable.event_narrow_afterhours
        else -> R.drawable.event_narrow_other
    }
    imageView.setImageResource(resId)
}

/* Photos are used if the event has a photo and/or a video url. */
@BindingAdapter("eventPhoto")
fun eventPhoto(imageView: ImageView, session: Session?) {
    session ?: return

    val resId = when (session.type) {
        KEYNOTE -> R.drawable.event_placeholder_keynote
        // Choose a random image, but we should use the same image for a given session, so use ID to
        // pick.
        SESSION -> when (session.id.hashCode() % 4) {
            0 -> R.drawable.event_placeholder_session1
            1 -> R.drawable.event_placeholder_session2
            2 -> R.drawable.event_placeholder_session3
            else -> R.drawable.event_placeholder_session4
        }
        // Other event types probably won't have photos or video, but just in case...
        else -> R.drawable.event_placeholder_keynote
    }

    if (session.hasPhoto) {
        Glide.with(imageView)
            .load(session.photoUrl)
            .apply(RequestOptions().placeholder(resId))
            .into(imageView)
    } else {
        imageView.setImageResource(resId)
    }
}

@BindingAdapter(
    value = ["sessionDetailStartTime", "sessionDetailEndTime", "timeZoneId"], requireAll = true
)
fun timeString(
    view: TextView,
    sessionDetailStartTime: ZonedDateTime?,
    sessionDetailEndTime: ZonedDateTime?,
    timeZoneId: ZoneId?
) {
    if (sessionDetailStartTime == null || sessionDetailEndTime == null || timeZoneId == null) {
        view.text = ""
    } else {
        view.text = TimeUtils.timeString(
            TimeUtils.zonedTime(sessionDetailStartTime, timeZoneId),
            TimeUtils.zonedTime(sessionDetailEndTime, timeZoneId)
        )
    }
}

@BindingAdapter("sessionStartCountdown")
fun sessionStartCountdown(view: TextView, timeUntilStart: Duration?) {
    if (timeUntilStart == null) {
        view.visibility = GONE
    } else {
        view.visibility = VISIBLE
        val minutes = timeUntilStart.toMinutes()
        view.text = view.context.resources.getQuantityString(
            R.plurals.session_starting_in, minutes.toInt(), minutes.toString()
        )
    }
}

@BindingAdapter(
    "userEvent",
    "isSignedIn",
    "isRegistered",
    "isReservable",
    "isReservationDeniedByCutoff",
    "eventListener",
    requireAll = true
)
fun assignFab(
    fab: StarReserveFab,
    userEvent: UserEvent?,
    isSignedIn: Boolean,
    isRegistered: Boolean,
    isReservable: Boolean,
    isReservationDeniedByCutoff: Boolean,
    eventListener: SessionDetailEventListener
) {
    when {
        !isSignedIn -> {
            if (isReservable) {
                fab.reservationStatus = RESERVABLE
            } else {
                fab.isChecked = false
            }
            fab.setOnClickListener { eventListener.onLoginClicked() }
        }
        isRegistered && isReservable -> {
            fab.reservationStatus = ReservationViewState.fromUserEvent(
                userEvent,
                isReservationDeniedByCutoff
            )
            fab.setOnClickListener { eventListener.onReservationClicked() }
        }
        else -> {
            fab.isChecked = userEvent?.isStarred == true
            fab.setOnClickListener { eventListener.onStarClicked() }
        }
    }
}
