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

package com.google.samples.apps.iosched.ui.reservation

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import androidx.annotation.DrawableRes
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.ui.reservation.StarReserveFabMode.RESERVE
import com.google.samples.apps.iosched.ui.reservation.StarReserveFabMode.STAR

/**
 * An extension to the [FloatingActionButton] supporting multiple custom states, representing the
 * status of a user's reservation for an event or whether an event is bookmarked (as modelled by
 * the checked state).
 */
class StarReserveFab(
    context: Context,
    attrs: AttributeSet
) : FloatingActionButton(context, attrs), Checkable {

    private var mode = RESERVE

    private var _checked = false
        set(value) {
            if (field != value || mode != STAR) {
                field = value
                currentDrawable = R.drawable.asld_star_event
                mode = STAR
                val contentDescRes = if (value) R.string.a11y_starred else R.string.a11y_unstarred
                contentDescription = context.getString(contentDescRes)
                refreshDrawableState()
            }
        }

    var reservationStatus = ReservationViewState.RESERVATION_DISABLED
        set(value) {
            if (value != field || mode != RESERVE) {
                field = value
                currentDrawable = R.drawable.asld_reservation
                mode = RESERVE
                contentDescription = context.getString(value.contentDescription)
                refreshDrawableState()
            }
        }

    @DrawableRes
    private var currentDrawable = 0
        set(value) {
            if (field != value) {
                field = value
                setImageResource(value)
            }
        }

    override fun isChecked() = _checked

    override fun setChecked(checked: Boolean) {
        _checked = checked
    }

    override fun toggle() {
        _checked = !_checked
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        if (!isShowingStar() && !isShowingReservation()) {
            return super.onCreateDrawableState(extraSpace)
        }

        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        when {
            isShowingStar() -> {
                val state = if (_checked) stateChecked else stateUnchecked
                mergeDrawableStates(drawableState, state)
            }
            isShowingReservation() -> {
                mergeDrawableStates(drawableState, reservationStatus.state)
            }
        }
        return drawableState
    }

    private fun isShowingReservation() = currentDrawable == R.drawable.asld_reservation

    private fun isShowingStar() = currentDrawable == R.drawable.asld_star_event

    companion object {
        private val stateChecked = intArrayOf(android.R.attr.state_checked)
        private val stateUnchecked = intArrayOf(-android.R.attr.state_checked)
    }
}

/**
 * Enum of the mutually exclusive modes [StarReserveFab] can be in.
 */
private enum class StarReserveFabMode {
    STAR,
    RESERVE
}
