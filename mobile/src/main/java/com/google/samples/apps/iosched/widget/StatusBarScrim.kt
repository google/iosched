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

package com.google.samples.apps.iosched.widget

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.WindowInsets

class StatusBarScrim @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var lastWindowInsets: WindowInsets? = null

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        if (insets != lastWindowInsets) {
            lastWindowInsets = insets
            // Set this view as invisible if there isn't a top inset
            visibility = if (insets.systemWindowInsetTop > 0) VISIBLE else INVISIBLE
            // Request a layout to change size
            requestLayout()
        }
        return insets
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val hMode = MeasureSpec.getMode(heightMeasureSpec)

        if (hMode != MeasureSpec.EXACTLY) {
            val newHeightSpec = MeasureSpec.makeMeasureSpec(
                    lastWindowInsets?.systemWindowInsetTop ?: 0,
                    MeasureSpec.EXACTLY
            )
            super.onMeasure(widthMeasureSpec, newHeightSpec)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }
}
