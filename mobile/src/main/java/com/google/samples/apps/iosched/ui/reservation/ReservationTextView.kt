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
import androidx.appcompat.widget.AppCompatTextView
import com.google.samples.apps.iosched.R

/**
 * An [AppCompatTextView] extension supporting multiple custom states, representing the status
 * of a user's reservation for an event.
 */
class ReservationTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.textViewStyle
) : AppCompatTextView(context, attrs, defStyleAttr) {

    var status = ReservationViewState.RESERVABLE
        set(value) {
            if (value == field) return
            field = value
            setText(value.text)
            refreshDrawableState()
        }

    init {
        setText(ReservationViewState.RESERVABLE.text)
        val drawable = context.getDrawable(R.drawable.asld_reservation)
        setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
    }

    override fun onCreateDrawableState(extraSpace: Int): IntArray {
        @Suppress("SENSELESS_COMPARISON") // Status is null during super init
        if (status == null) return super.onCreateDrawableState(extraSpace)
        val drawableState = super.onCreateDrawableState(extraSpace + 1)
        mergeDrawableStates(drawableState, status.state)
        return drawableState
    }
}
