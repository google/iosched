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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.support.annotation.Keep
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.CoordinatorLayout.Behavior
import android.support.v4.view.ViewCompat
import android.support.v4.view.animation.FastOutLinearInInterpolator
import android.support.v4.view.animation.LinearOutSlowInInterpolator
import android.util.AttributeSet
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewPropertyAnimator
import android.view.animation.Interpolator

/**
 * Alternate version of material lib's HideBottomViewOnScrollBehavior. This one has various bugfixes
 * and additional features.
 */
// TODO remove if/when version in material lib that meets our needs.
@Keep
class HideBottomViewOnScrollBehavior<V : View> : Behavior<V> {

    /** Callback interface for scroll events */
    interface ScrollListener {
        /** Called when the [view]'s position is changed by a scroll event. */
        fun onBottomViewScrolled(view: View)
    }

    private var height = 0
    private var scrollState = SCROLLED_UP
    private var currentAnimation: ViewPropertyAnimator? = null
    private var minScaledFlingVelocity = 0

    private var listeners = mutableSetOf<ScrollListener>()

    constructor() : super()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        minScaledFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    }

    override fun onLayoutChild(
        parent: CoordinatorLayout?,
        child: V,
        layoutDirection: Int
    ): Boolean {
        height = child.measuredHeight
        return super.onLayoutChild(parent, child, layoutDirection)
    }

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ) = axes == ViewCompat.SCROLL_AXIS_VERTICAL

    override fun onNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        dxConsumed: Int,
        dyConsumed: Int,
        dxUnconsumed: Int,
        dyUnconsumed: Int,
        type: Int
    ) {
        if (dyConsumed > 0) {
            slideDown(child)
        } else if (dyConsumed < 0) {
            slideUp(child)
        }
    }

    override fun onNestedFling(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        velocityX: Float,
        velocityY: Float,
        consumed: Boolean
    ): Boolean {
        if (velocityY > minScaledFlingVelocity) {
            slideDown(child)
        } else if (velocityY < minScaledFlingVelocity) {
            slideUp(child)
        }
        return false
    }

    private fun slideDown(child: View) {
        if (scrollState != SCROLLED_DOWN) {
            currentAnimation?.cancel()
            child.clearAnimation()
            scrollState = SCROLLED_DOWN
            animateChildTo(child, height, FAST_OUT_LINEAR_IN_INTERPOLATOR)
        }
    }
    private fun slideUp(child: View) {
        if (scrollState != SCROLLED_UP) {
            currentAnimation?.cancel()
            child.clearAnimation()
            scrollState = SCROLLED_UP
            animateChildTo(child, 0, LINEAR_OUT_SLOW_IN_INTERPOLATOR)
        }
    }
    private fun animateChildTo(child: View, y: Int, interpolator: Interpolator) {
        currentAnimation = child.animate().apply {
            translationY(y.toFloat())
            duration = ANIMATION_DURATION
            setInterpolator(interpolator)
            setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    currentAnimation = null
                }
            })
            setUpdateListener {
                listeners.forEach { it.onBottomViewScrolled(child) }
            }
        }
    }

    fun addScrollListener(listener: ScrollListener) {
        listeners.add(listener)
    }

    fun removeScrollListener(listener: ScrollListener) {
        listeners.remove(listener)
    }

    companion object {
        private const val ANIMATION_DURATION = 200L
        private const val SCROLLED_DOWN = 0
        private const val SCROLLED_UP = 1

        private val LINEAR_OUT_SLOW_IN_INTERPOLATOR = LinearOutSlowInInterpolator()
        private val FAST_OUT_LINEAR_IN_INTERPOLATOR = FastOutLinearInInterpolator()

        /** Utility for getting a HideBottomViewOnScrollBehavior from a [view]. */
        @JvmStatic
        fun from(view: View): HideBottomViewOnScrollBehavior<*> {
            val lp = view.layoutParams as? CoordinatorLayout.LayoutParams
                    ?: throw IllegalArgumentException("view is not a child of CoordinatorLayout")
            return lp.behavior as? HideBottomViewOnScrollBehavior
                    ?: throw IllegalArgumentException("view is not associated with this behavior")
        }
    }
}
