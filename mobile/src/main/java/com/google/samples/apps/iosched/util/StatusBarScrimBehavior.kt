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
import android.util.AttributeSet
import android.view.View
import androidx.annotation.Keep
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.google.android.material.appbar.AppBarLayout

/**
 * A [CoordinatorLayout.Behavior] which which automatically update the height of any attached view
 * to the height of the system status bar. It also automatically updates the elevation of any
 * attached views to match any sibling [AppBarLayout]s.
 */
@Keep
@Suppress("UNUSED")
class StatusBarScrimBehavior(
    context: Context,
    attrs: AttributeSet
) : CoordinatorLayout.Behavior<View>(context, attrs) {
    override fun onLayoutChild(
        parent: CoordinatorLayout,
        child: View,
        layoutDirection: Int
    ): Boolean {
        child.setOnApplyWindowInsetsListener(NoopWindowInsetsListener)
        // Return false so that the child is laid out by the parent
        return false
    }

    override fun layoutDependsOn(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {
        if (dependency is AppBarLayout) {
            // Jump the drawable state in case the elevation is animating
            dependency.jumpDrawablesToCurrentState()
            // Copy over the elevation value
            child.elevation = dependency.elevation
            return true
        }
        return false
    }

    override fun onDependentViewChanged(
        parent: CoordinatorLayout,
        child: View,
        dependency: View
    ): Boolean {
        child.elevation = dependency.elevation
        return false
    }

    override fun onApplyWindowInsets(
        parent: CoordinatorLayout,
        child: View,
        insets: WindowInsetsCompat
    ): WindowInsetsCompat {
        child.updateLayoutParams<CoordinatorLayout.LayoutParams> {
            height = insets.systemWindowInsetTop
        }
        return insets
    }
}