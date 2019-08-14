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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView.State

/**
 * [LinearSmoothScroller] implementation that first jumps closer to the target position if necessary
 * in order to avoid really long scrolling animations.
 */
class JumpSmoothScroller(
    context: Context,
    /** Maximum position difference to scroll normally without jumping first. */
    private val maxDifference: Int = 5
) : LinearSmoothScroller(context) {

    override fun getVerticalSnapPreference() = SNAP_TO_START

    override fun getHorizontalSnapPreference() = SNAP_TO_START

    override fun onSeekTargetStep(dx: Int, dy: Int, state: State, action: Action) {
        val layoutManager = layoutManager as? LinearLayoutManager
        if (layoutManager != null) {
            // If we're far enough away from the target position, jump closer before scrolling
            if (targetPosition + maxDifference < layoutManager.findFirstVisibleItemPosition()) {
                action.jumpTo(targetPosition + maxDifference)
                return
            }
            if (targetPosition - maxDifference > layoutManager.findLastVisibleItemPosition()) {
                action.jumpTo(targetPosition - maxDifference)
                return
            }
        }
        // Otherwise let superclass handle scrolling normally.
        super.onSeekTargetStep(dx, dy, state, action)
    }
}
