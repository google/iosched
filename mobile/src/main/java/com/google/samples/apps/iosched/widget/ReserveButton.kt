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

package com.google.samples.apps.iosched.widget

import android.content.Context
import android.support.annotation.StringRes
import android.support.v7.widget.AppCompatImageButton
import android.util.AttributeSet
import com.google.samples.apps.iosched.R

/**
 * An [AppCompatImageButton] extension supporting multiple custom states, representing the status
 * of a user's reservation for an event.
 */
class ReserveButton(context: Context, attrs: AttributeSet) : AppCompatImageButton(context, attrs) {

    var status = ReservationStatus.RESERVATION_DISABLED
        set(value) {
            if (value == field) return
            field = value
            contentDescription = context.getString(value.contentDescription)
            refreshDrawableState()
        }

    init {
        // Drawable defining drawables for each reservation state
        setImageResource(R.drawable.asld_reservation)
        status = ReservationStatus.RESERVABLE
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        // Status is null during super init
        if (status == null) return super.onCreateDrawableState(extraSpace)
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        mergeDrawableStates(drawableState, status.state)
        return drawableState
    }
}

/**
 * Models the different states of a reservation and a corresponding content description.
 */
enum class ReservationStatus(val state: IntArray, @StringRes val contentDescription: Int) {
    RESERVABLE(
        intArrayOf(R.attr.state_reservable),
        R.string.a11y_reservation_available
    ),
    WAIT_LIST_AVAILABLE(
        intArrayOf(R.attr.state_wait_list_available),
        R.string.a11y_reservation_reserved
    ),
    WAIT_LISTED(
        intArrayOf(R.attr.state_wait_listed),
        R.string.a11y_reservation_disabled
    ),
    RESERVED(
        intArrayOf(R.attr.state_reserved),
        R.string.a11y_reservation_wait_list_available
    ),
    RESERVATION_PENDING(
        intArrayOf(R.attr.state_reservation_pending),
        R.string.a11y_reservation_wait_listed
    ),
    RESERVATION_DISABLED(
        intArrayOf(R.attr.state_reservation_disabled),
        R.string.a11y_reservation_pending
    )
}
