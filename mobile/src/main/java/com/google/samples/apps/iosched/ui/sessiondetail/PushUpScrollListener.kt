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
import androidx.core.view.isVisible
import androidx.core.view.marginTop
import androidx.recyclerview.widget.RecyclerView

/**
 * A [RecyclerView.OnScrollListener] which adjusts the position of the [up] view based on scroll.
 */
class PushUpScrollListener(
    private val up: View,
    private val recyclerView: RecyclerView,
    @IdRes private val titleResId: Int,
    @IdRes private val imageResId: Int
) : RecyclerView.OnScrollListener() {

    private var pushPointY = -1
    private var upMarginTop = 0

    fun syncPushUpPoint() {
        upMarginTop = up.marginTop

        pushPointY = -1

        val title = recyclerView.findViewById<TextView>(titleResId)
        if (title != null && title.isVisible) {
            // If title is in header, push the up button from the first line of text.
            // Due to using auto-sizing text, the view needs to be a fixed height (not wrap)
            // with gravity bottom so we find the text top using the baseline.
            val textTop = title.baseline - title.textSize.toInt()
            pushPointY = textTop - up.height
        }

        val photo = if (imageResId == 0) null else recyclerView.findViewById<View>(imageResId)
        if (photo != null) {
            // If no title in header, push the up button based on the bottom of the photo
            pushPointY = photo.height - up.height
        }

        offsetView()
    }

    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) = offsetView()

    private fun offsetView() {
        if (pushPointY < 0) return
        val scrollY = recyclerView.findViewHolderForAdapterPosition(0)
                ?.itemView?.top ?: Integer.MIN_VALUE

        val desiredTop = Math.min(pushPointY + scrollY, upMarginTop)
        if (desiredTop != up.top) {
            val offset = desiredTop - up.top
            up.offsetTopAndBottom(offset)
        }
    }
}
