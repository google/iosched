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

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.core.os.BuildCompat
import androidx.core.view.forEach
import androidx.drawerlayout.widget.DrawerLayout
import com.google.samples.apps.iosched.util.ViewGestureUtils
import kotlin.math.roundToInt

/**
 * Extension of [DrawerLayout] which sets gesture exclusion zones, to work with Android Q's
 * gesture navigation
 */
class IoschedDrawerLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : DrawerLayout(context, attrs, defStyle) {
    init {
        // Add a listener, so that we can update the exclusion rects for when a drawer is
        // closed and opened
        addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {}

            override fun onDrawerClosed(drawerView: View) {
                updateGestureExclusion()
            }

            override fun onDrawerOpened(drawerView: View) {
                updateGestureExclusion()
            }

            override fun onDrawerStateChanged(newState: Int) {}
        })
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        super.onLayout(changed, l, t, r, b)
        updateGestureExclusion()
    }

    @SuppressLint("RtlHardcoded")
    private fun updateGestureExclusion() {
        // If we're not running on Q, we can skip this method entirely
        if (!BuildCompat.isAtLeastQ()) return

        var rects: MutableList<Rect>? = null

        if (hasClosedDrawer(Gravity.LEFT)) {
            // We have a closed drawer on the left, add an exclusion rect so that the user
            // can swipe it open
            if (rects == null) {
                rects = mutableListOf()
            }
            rects.add(Rect(0, 0, dpToPx(EDGE_SIZE_DP), height))
        }
        if (hasClosedDrawer(Gravity.RIGHT)) {
            // We have a closed drawer on the right, add an exclusion rect so that the user
            // can swipe it open
            if (rects == null) {
                rects = mutableListOf()
            }
            rects.add(Rect(width - dpToPx(EDGE_SIZE_DP), 0, width, height))
        }

        ViewGestureUtils.setSystemGestureExclusionRects(this, rects ?: emptyList())
    }

    private fun dpToPx(dp: Int) = (dp * resources.displayMetrics.density).roundToInt()

    companion object {
        // 20dp is the value ViewDragHelper (and DrawerLayout) uses as the touch edge detection
        // zone. Hacky but this file will be removed once we can move to the updated
        // AndroidX version
        const val EDGE_SIZE_DP = 20
    }
}

fun DrawerLayout.hasClosedDrawer(gravity: Int): Boolean {
    forEach { view ->
        val lp = view.layoutParams as DrawerLayout.LayoutParams
        val absGravity = Gravity.getAbsoluteGravity(lp.gravity, layoutDirection)
        if ((absGravity and Gravity.HORIZONTAL_GRAVITY_MASK) == gravity) {
            return !isDrawerOpen(view)
        }
    }
    return false
}
