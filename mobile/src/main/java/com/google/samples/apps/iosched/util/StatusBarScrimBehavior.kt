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
import androidx.core.view.children
import com.google.android.material.appbar.AppBarLayout

@Keep
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

        // Find a AppBarLayout sibling and copy it's elevation
        val appBarLayout = parent.children.first { it is AppBarLayout }
        (appBarLayout as? AppBarLayout)?.let {
            child.elevation = appBarLayout.elevation
        }

        // Return false so that the child is laid out by the parent
        return false
    }

    override fun onApplyWindowInsets(
        parent: CoordinatorLayout,
        child: View,
        insets: WindowInsetsCompat
    ): WindowInsetsCompat {
        child.layoutParams.height = insets.systemWindowInsetTop
        child.requestLayout()
        return insets
    }
}