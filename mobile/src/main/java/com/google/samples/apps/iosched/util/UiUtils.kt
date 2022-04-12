/*
 * Copyright 2019 Google LLC
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

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import com.google.samples.apps.iosched.R

fun navigationItemBackground(context: Context): Drawable? {
    // Need to inflate the drawable and CSL via AppCompatResources to work on Lollipop
    var background =
        AppCompatResources.getDrawable(context, R.drawable.navigation_item_background)
    if (background != null) {
        val tint = AppCompatResources.getColorStateList(
            context, R.color.navigation_item_background_tint
        )
        background = DrawableCompat.wrap(background.mutate())
        background.setTintList(tint)
    }
    return background
}

/**
 * Map a slideOffset (in the range `[-1, 1]`) to an alpha value based on the desired range.
 * For example, `slideOffsetToAlpha(0.5, 0.25, 1) = 0.33` because 0.5 is 1/3 of the way between
 * 0.25 and 1. The result value is additionally clamped to the range `[0, 1]`.
 */
fun slideOffsetToAlpha(value: Float, rangeMin: Float, rangeMax: Float): Float {
    return ((value - rangeMin) / (rangeMax - rangeMin)).coerceIn(0f, 1f)
}
