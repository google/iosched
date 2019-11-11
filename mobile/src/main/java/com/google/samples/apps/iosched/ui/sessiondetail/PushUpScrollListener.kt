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

import android.view.View
import android.widget.TextView
import androidx.annotation.IdRes
import androidx.recyclerview.widget.RecyclerView

/**
 * A [RecyclerView.OnScrollListener] which adjusts the position of the [up] view based on scroll.
 */
class PushUpScrollListener(
    private val up: View,
    recyclerView: View,
    @IdRes titleResId: Int,
    @IdRes imageResId: Int
) : RecyclerView.OnScrollListener() {

    private var pushPointY = -1

    init {
        val title = recyclerView.findViewById<TextView>(titleResId)
        pushPointY = if (title.visibility == View.VISIBLE) {
            // If title is in header, push the up button from the first line of text.
            // Due to using auto-sizing text, the view needs to be a fixed height (not wrap)
            // with gravity bottom so we find the text top using the baseline.
            val textTop = title.baseline - title.textSize.toInt()
            textTop - up.height
        } else {
            // If no title in header, push the up button based on the bottom of the photo
            val photo = recyclerView.findViewById<View>(imageResId)
            photo.height - up.height
        }
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        if (pushPointY < 0) return
        val scrollY =
            recyclerView.findViewHolderForAdapterPosition(0)?.itemView?.top ?: Integer.MIN_VALUE

        val desiredTop = Math.min(pushPointY + scrollY, 0)
        if (desiredTop != up.top) {
            val offset = desiredTop - up.top
            up.offsetTopAndBottom(offset)
        }
    }
}
