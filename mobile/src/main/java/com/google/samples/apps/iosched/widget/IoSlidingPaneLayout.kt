/*
 * Copyright 2021 Google LLC
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
import androidx.slidingpanelayout.widget.SlidingPaneLayout

// TODO(b/187348546) Remove when SlidingPaneLayout can support all MeasureSpec modes.
class IoSlidingPaneLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : SlidingPaneLayout(context, attrs, defStyleAttr) {

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthSpec = if (MeasureSpec.getMode(widthMeasureSpec) != MeasureSpec.EXACTLY) {
            // SlidingPaneLayout throws an exception when widthMode is not EXACTLY, so change to
            // EXACTLY and continue measuring.
            val widthSize = MeasureSpec.getSize(widthMeasureSpec)
            MeasureSpec.makeMeasureSpec(
                if (widthSize > 0) widthSize else 500,
                MeasureSpec.EXACTLY
            )
        } else {
            widthMeasureSpec
        }

        val heightSpec = if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.UNSPECIFIED) {
            // SlidingPaneLayout throws an exception when heightMode is UNSPECIFIED, so change
            // to AT_MOST and continue measuring.
            val heightSize = MeasureSpec.getSize(widthMeasureSpec)
            MeasureSpec.makeMeasureSpec(
                if (heightSize > 0) heightSize else 500,
                MeasureSpec.AT_MOST
            )
        } else {
            heightMeasureSpec
        }
        super.onMeasure(widthSpec, heightSpec)
    }
}
