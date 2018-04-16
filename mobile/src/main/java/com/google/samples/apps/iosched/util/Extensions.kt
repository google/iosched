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

package com.google.samples.apps.iosched.util

import android.app.Activity
import android.databinding.ObservableBoolean
import android.databinding.ViewDataBinding
import android.graphics.drawable.Drawable
import android.support.annotation.DrawableRes
import android.text.StaticLayout
import android.view.View
import androidx.view.postDelayed
import com.google.samples.apps.iosched.shared.domain.internal.DefaultScheduler

fun ObservableBoolean.hasSameValue(other: ObservableBoolean) = get() == other.get()

fun Int.isEven() = this % 2 == 0

fun View.isRtl() = layoutDirection == View.LAYOUT_DIRECTION_RTL

inline fun <T: ViewDataBinding> T.executeAfter(block: T.() -> Unit) {
    block()
    executePendingBindings()
}

/**
 * Loads a drawable asynchronously and passes it into the callback function.
 */
inline fun loadDrawableAsync(view: View, @DrawableRes id: Int, crossinline f: (Drawable) -> Unit) {
    DefaultScheduler.execute {
        val drawable = view.context.getDrawable(id)
        view.post {
            f(drawable)
        }
    }
}

/**
 * An extension to `postponeEnterTransition` which will resume after a timeout.
 */
fun Activity.postponeEnterTransition(timeout: Long) {
    postponeEnterTransition()
    window.decorView.postDelayed(timeout) {
        startPostponedEnterTransition()
    }
}

/**
 * Calculated the widest line in a [StaticLayout].
 */
fun StaticLayout.textWidth() : Int {
    var width = 0f
    for (i in 0 until lineCount) {
        width = width.coerceAtLeast(getLineWidth(i))
    }
    return width.toInt()
}

/**
 * Linearly interpolate between two values.
 */
fun lerp(a: Float, b: Float, t: Float): Float {
    return a + (b - a) * t
}
