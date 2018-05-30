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
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.Session
import com.google.samples.apps.iosched.model.SessionType
import com.google.samples.apps.iosched.model.SessionType.AFTER_HOURS
import com.google.samples.apps.iosched.model.SessionType.APP_REVIEW
import com.google.samples.apps.iosched.model.SessionType.CODELAB
import com.google.samples.apps.iosched.model.SessionType.OFFICE_HOURS
import com.google.samples.apps.iosched.model.SessionType.SANDBOX
import com.google.samples.apps.iosched.model.SessionType.SESSION
import com.google.samples.apps.iosched.model.SessionType.UNKNOWN
import com.google.samples.apps.iosched.model.userdata.UserEvent
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.ui.reservation.ReservationViewState
import com.google.samples.apps.iosched.ui.reservation.ReservationViewState.RESERVABLE
import com.google.samples.apps.iosched.ui.reservation.StarReserveFab
import com.google.samples.apps.iosched.util.drawable.HeaderGridDrawable
import org.threeten.bp.Duration
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

@Suppress("unused")
@BindingAdapter("headerImage")
fun headerImage(imageView: ImageView, photoUrl: String?) {
    if (!photoUrl.isNullOrEmpty()) {
        Glide.with(imageView)
            .load(photoUrl)
            .apply(RequestOptions().placeholder(HeaderGridDrawable(imageView.context)))
            .into(imageView)
    } else {
        imageView.setImageDrawable(HeaderGridDrawable(imageView.context))
    }
}

@Suppress("unused")
@BindingAdapter("eventType")
fun headerLogoImage(imageView: ImageView, eventType: SessionType?) {
    val resId = when (eventType) {
        AFTER_HOURS -> R.drawable.event_header_afterhours
        APP_REVIEW -> R.drawable.event_header_office_hours
        CODELAB -> R.drawable.event_header_codelabs
        OFFICE_HOURS -> R.drawable.event_header_office_hours
        SANDBOX -> R.drawable.event_header_sandbox
        SESSION -> R.drawable.event_header_sessions
        UNKNOWN -> R.drawable.event_header_sessions
        else -> R.drawable.event_header_sessions
    }
    imageView.setImageDrawable(imageView.context.getDrawable(resId))
}

@Suppress("unused")
@BindingAdapter("eventHeaderAnim")
fun eventHeaderAnim(lottieView: LottieAnimationView, session: Session?) {
    val anim = when (session?.type) {
        AFTER_HOURS -> "anim/event_details_after_hours.json"
        CODELAB -> "anim/event_details_codelabs.json"
        OFFICE_HOURS -> "anim/event_details_office_hours.json"
        SANDBOX -> "anim/event_details_sandbox.json"
        else -> "anim/event_details_session.json" // default to session anim
    }
    lottieView.setAnimation(anim)
}

@Suppress("unused")
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
    "isReservationDisabled",
    "eventListener",
    requireAll = true
)
fun assignFab(
    fab: StarReserveFab,
    userEvent: UserEvent?,
    isSignedIn: Boolean,
    isRegistered: Boolean,
    isReservable: Boolean,
    isReservationDisabled: Boolean,
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
                isReservationDisabled
            )
            fab.setOnClickListener { eventListener.onReservationClicked() }
        }
        else -> {
            fab.isChecked = userEvent?.isStarred == true
            fab.setOnClickListener { eventListener.onStarClicked() }
        }
    }
}
