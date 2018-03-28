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

import android.content.Context
import android.support.annotation.IdRes
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.CoordinatorLayout.Behavior
import android.support.v4.view.ViewCompat.SCROLL_AXIS_VERTICAL
import android.support.v4.widget.NestedScrollView
import android.support.v7.widget.AppCompatImageButton
import android.util.AttributeSet
import android.view.View
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.content.res.getResourceIdOrThrow
import com.google.samples.apps.iosched.R

/**
 * A custom [Behavior] which adjusts the position of the view it is applied to, based upon the
 * position of another view, as identified by attributes.
 *
 * Note that we can't use `anchor`s here as the view we depend upon is not a direct child of the
 * [CoordinatorLayout]. Instead we listed for nested scrolls and check the dependant view's
 * position.
 */
class PushUpBehavior(
    context: Context,
    attrs: AttributeSet
) : Behavior<AppCompatImageButton>(context, attrs) {

    @IdRes private val titleId: Int
    @IdRes private val photoId: Int
    private var pushPointY = -1

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.PushUpBehavior)
        titleId = a.getResourceIdOrThrow(R.styleable.PushUpBehavior_behavior_pushTitle)
        photoId = a.getResourceIdOrThrow(R.styleable.PushUpBehavior_behavior_pushPhoto)
        a.recycle()
    }

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        up: AppCompatImageButton,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        if (axes == SCROLL_AXIS_VERTICAL && target is NestedScrollView) {
            if (pushPointY < 0) setupPushPoint(target, up)
            return true
        }
        return false
    }

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: AppCompatImageButton,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
        updatePosition(target as NestedScrollView, child)
    }

    override fun onNestedFling(
        coordinatorLayout: CoordinatorLayout,
        child: AppCompatImageButton,
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        updatePosition(target as NestedScrollView, child)
        return false
    }

    private fun setupPushPoint(scrollView: View, up: AppCompatImageButton) {
        val title = scrollView.findViewById<TextView>(titleId)
        pushPointY = if (title.visibility == VISIBLE) {
            // If title is in header, push the up button from the first line of text.
            // Due to using auto-sizing text, the view needs to be a fixed height (not wrap)
            // with gravity bottom so we find the text top using the baseline.
            val textTop = title.baseline - title.textSize.toInt()
            textTop - up.height
        } else {
            // If no title in header, push the up button based on the bottom of the photo
            val photo = scrollView.findViewById<View>(photoId)
            photo.height - up.height
        }
    }

    private fun updatePosition(scrollView: NestedScrollView, up: AppCompatImageButton) {
        val desiredTop = Math.min(pushPointY - scrollView.scrollY, 0)
        if (desiredTop != up.top) {
            val offset = desiredTop - up.top
            up.offsetTopAndBottom(offset)
        }
    }
}
