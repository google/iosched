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

package com.google.samples.apps.iosched.widget

import android.content.Context
import android.support.design.widget.CoordinatorLayout
import android.support.v4.view.NestedScrollingChild2
import android.support.v4.view.NestedScrollingChildHelper
import android.util.AttributeSet
import android.view.View

/**
 * A CoordinatorLayout that simultaneously acts as a nested scrolling child; that is, it passes
 * nested scrolling events to its parent. This allows it to be nested inside another
 * CoordinatorLayout and have the behaviors of the children of both react to nested scroll events.
 */
class NestedScrollingCoordinatorLayout @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : CoordinatorLayout(context, attrs, defStyleAttr), NestedScrollingChild2 {

    private val scrollingChildHelper =
        NestedScrollingChildHelper(this).apply { isNestedScrollingEnabled = true }

    override fun hasNestedScrollingParent(): Boolean {
        return scrollingChildHelper.hasNestedScrollingParent()
    }

    override fun hasNestedScrollingParent(type: Int): Boolean {
        return scrollingChildHelper.hasNestedScrollingParent(type)
    }

    override fun setNestedScrollingEnabled(enabled: Boolean) {
        scrollingChildHelper.isNestedScrollingEnabled = enabled
    }

    override fun startNestedScroll(axes: Int): Boolean {
        // calls ViewParentCompat.onStartNestedScroll
        return scrollingChildHelper.startNestedScroll(axes)
    }

    override fun startNestedScroll(axes: Int, type: Int): Boolean {
        // calls ViewParentCompat.onStartNestedScroll
        return scrollingChildHelper.startNestedScroll(axes, type)
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int): Boolean {
        val handled = super.onStartNestedScroll(child, target, axes)
        return handled or
                // calls ViewParentCompat.onStartNestedScroll
                scrollingChildHelper.startNestedScroll(axes)
    }

    override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
        val handled = super.onStartNestedScroll(child, target, axes, type)
        return handled or
                // calls ViewParentCompat.onStartNestedScroll
                scrollingChildHelper.startNestedScroll(axes, type)
    }

    override fun stopNestedScroll() {
        // calls ViewParentCompat.onStopNestedScroll
        scrollingChildHelper.stopNestedScroll()
    }

    override fun stopNestedScroll(type: Int) {
        // calls ViewParentCompat.onStopNestedScroll
        scrollingChildHelper.stopNestedScroll(type)
    }

    override fun onStopNestedScroll(target: View) {
        super.onStopNestedScroll(target)
        // calls ViewCompat.stopNestedScroll
        scrollingChildHelper.onStopNestedScroll(target)
    }

    override fun onStopNestedScroll(target: View, type: Int) {
        super.onStopNestedScroll(target, type)
        // calls ViewCompat.stopNestedScroll
        scrollingChildHelper.onStopNestedScroll(target)
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray) {
        dispatchNestedPreScroll(dx, dy, consumed, null)
        super.onNestedPreScroll(target, dx, dy, consumed)
    }

    override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
        dispatchNestedPreScroll(dx, dy, consumed, null, type)
        super.onNestedPreScroll(target, dx, dy, consumed, type)
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?
    ): Boolean {
        // calls ViewParentCompat.onNestedPreScroll
        return scrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow)
    }

    override fun dispatchNestedPreScroll(
        dx: Int,
        dy: Int,
        consumed: IntArray?,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        // calls ViewParentCompat.onNestedPreScroll
        return scrollingChildHelper.dispatchNestedPreScroll(dx, dy, consumed, offsetInWindow, type)
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int
    ) {
        super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed)
        // calls ViewParentCompat.onNestedScroll
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, null)
    }

    override fun onNestedScroll(
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
        super.onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, type)
        // calls ViewParentCompat.onNestedScroll
        dispatchNestedScroll(dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, null, type)
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?
    ): Boolean {
        // calls ViewParentCompat.onNestedScroll
        return scrollingChildHelper.dispatchNestedScroll(
            dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, offsetInWindow
        )
    }

    override fun dispatchNestedScroll(
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        offsetInWindow: IntArray?,
        type: Int
    ): Boolean {
        // calls ViewParentCompat.onNestedScroll
        return scrollingChildHelper.dispatchNestedScroll(
            dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed, null, type
        )
    }

    override fun onNestedPreFling(target: View, velocityX: Float, velocityY: Float): Boolean {
        val handled = dispatchNestedPreFling(velocityX, velocityY)
        return handled or super.onNestedPreFling(target, velocityX, velocityY)
    }

    override fun dispatchNestedPreFling(velocityX: Float, velocityY: Float): Boolean {
        // calls ViewParentCompat.onNestedPreFling
        return scrollingChildHelper.dispatchNestedPreFling(velocityX, velocityY)
    }

    override fun onNestedFling(
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        val handled = super.onNestedFling(target, velocityX, velocityY, consumed)
        return handled or dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun dispatchNestedFling(
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        // calls ViewParentCompat.onNestedFling
        return scrollingChildHelper.dispatchNestedFling(velocityX, velocityY, consumed)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scrollingChildHelper.onDetachedFromWindow()
    }
}
