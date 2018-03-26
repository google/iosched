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
import android.animation.TimeInterpolator
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.support.annotation.IntDef
import android.support.annotation.Keep
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.CoordinatorLayout.Behavior
import android.support.design.widget.CoordinatorLayout.LayoutParams
import android.support.v4.view.AbsSavedState
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewPropertyAnimator
import com.google.android.material.animation.AnimationUtils
import java.lang.ref.WeakReference
import kotlin.annotation.AnnotationRetention.SOURCE

/**
 * Alternate version of material lib's HideBottomViewOnScrollBehavior. This one has various bugfixes
 * and additional features.
 */
// TODO remove if/when version in material lib that meets our needs.
@Keep
class HideBottomViewOnScrollBehavior<V : View> : Behavior<V> {

    companion object {
        /** The bottom view is fully visible. */
        const val STATE_SHOWN = 0
        /** The bottom view is fully hidden. */
        const val STATE_HIDDEN = 1
        /** The bottom view is settling to a new state. */
        const val STATE_SETTLING = 2

        /** The bottom view can hide and show freely. */
        const val LOCK_MODE_UNLOCKED = 0
        /** The bottom view is shown and cannot hide. */
        const val LOCK_MODE_LOCKED_SHOWN = 1
        /** The bottom view is hidden and cannot show. */
        const val LOCK_MODE_LOCKED_HIDDEN = 2

        private const val ANIMATION_DURATION = 200L

        /** Utility for getting a HideBottomViewOnScrollBehavior from a [view]. */
        @JvmStatic
        fun from(view: View): HideBottomViewOnScrollBehavior<*> {
            val lp = view.layoutParams as? CoordinatorLayout.LayoutParams
                    ?: throw IllegalArgumentException("view is not a child of CoordinatorLayout")
            return lp.behavior as? HideBottomViewOnScrollBehavior
                    ?: throw IllegalArgumentException("view is not associated with this behavior")
        }
    }

    /** Callback interface for scroll events */
    interface BottomViewCallback {
        /**
         * Called when the [view]'s state changes.
         *
         * @param view The bottom view
         * @param newState One of [STATE_SHOWN], [STATE_HIDDEN], or [STATE_SETTLING].
         */
        fun onStateChanged(view: View, newState: Int)

        /**
         * Called when the [view]'s position is changed during aimation.
         *
         * @param view The bottom view
         * @param slideOffset The new offset of the bottom view within [0,1] range. At 0, the view
         * is fully shown; at 1, the view is fully hidden.
         */
        fun onSlide(view: View, slideOffset: Float)
    }

    @IntDef(STATE_SHOWN, STATE_HIDDEN, STATE_SETTLING)
    @Retention(SOURCE)
    annotation class State

    @IntDef(LOCK_MODE_UNLOCKED, LOCK_MODE_LOCKED_SHOWN, LOCK_MODE_LOCKED_HIDDEN)
    @Retention(SOURCE)
    annotation class LockMode

    /**
     * The current lock mode. One of [LOCK_MODE_UNLOCKED], [LOCK_MODE_LOCKED_SHOWN], or
     * [LOCK_MODE_LOCKED_HIDDEN].
     */
    @LockMode var lockMode = LOCK_MODE_UNLOCKED
        set(value) {
            if (field != value) {
                field = value
                if (value == LOCK_MODE_LOCKED_SHOWN) {
                    show()
                } else if (value == LOCK_MODE_LOCKED_HIDDEN) {
                    hide()
                }
            }
        }

    /** The current state. One of [STATE_SHOWN], [STATE_HIDDEN], or [STATE_SETTLING]. */
    @State private var state = STATE_SHOWN
    /** Target state when settling, always one of [STATE_SHOWN], [STATE_HIDDEN]. */
    @State private var settlingHint = STATE_SHOWN

    private var childRef: WeakReference<View>? = null
    private var currentAnimation: ViewPropertyAnimator? = null
    private var minScaledFlingVelocity = 0

    private var callbacks = mutableSetOf<BottomViewCallback>()

    constructor() : super()

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        minScaledFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    }

    override fun onAttachedToLayoutParams(params: LayoutParams) {
        super.onAttachedToLayoutParams(params)
        childRef = null
    }

    override fun onDetachedFromLayoutParams() {
        super.onDetachedFromLayoutParams()
        childRef = null
    }

    override fun onLayoutChild(
        parent: CoordinatorLayout,
        child: V,
        layoutDirection: Int
    ): Boolean {
        // Let the parent lay out the child normally
        parent.onLayoutChild(child, layoutDirection)

        if (childRef == null) { // First layout
            childRef = WeakReference(child)
            if (state == STATE_HIDDEN) {
                settlingHint = STATE_HIDDEN
                child.translationY = child.height.toFloat()
            }
        }
        return true
    }

    // API

    /** Add a callback for bottom view events. */
    fun addBottomViewCallback(callback: BottomViewCallback) {
        callbacks.add(callback)
    }

    /** Remove a callback for bottom view events. */
    fun removeBottomViewCallback(callback: BottomViewCallback) {
        callbacks.remove(callback)
    }

    /**
     * Expand the bottom view. Has no effect if the current [lockMode] is not [LOCK_MODE_UNLOCKED].
     */
    fun show() {
        if (state != STATE_SHOWN
            && settlingHint != STATE_SHOWN
            && lockMode != LOCK_MODE_LOCKED_HIDDEN
        ) {
            animateToState(STATE_SHOWN)
        }
    }

    /**
     * Hide the bottom view. Has no effect if the current [lockMode] is not [LOCK_MODE_UNLOCKED].
     */
    fun hide() {
        if (state != STATE_HIDDEN
            && settlingHint != STATE_HIDDEN
            && lockMode != LOCK_MODE_LOCKED_SHOWN
        ) {
            animateToState(STATE_HIDDEN)
        }
    }

    @State
    fun getState() = state

    // Nested Scrolling

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        return lockMode == LOCK_MODE_UNLOCKED && (axes and ViewCompat.SCROLL_AXIS_VERTICAL) != 0
    }

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
            hide()
        } else if (dyConsumed < 0) {
            show()
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
            hide()
        } else if (velocityY < -minScaledFlingVelocity) {
            show()
        }
        return false
    }

    private fun animateToState(@State finalState: Int) {
        val child = childRef?.get()
        if (child == null) {
            // Not laid out yet. Set the state directly and let onLayoutChild handle it later.
            setStateInteral(finalState)
            return
        }

        currentAnimation?.cancel()
        child.clearAnimation()

        val height = child.height
        val targetY = if (finalState == STATE_SHOWN) 0 else height
        val interpolator: TimeInterpolator = if (finalState == STATE_SHOWN) {
            AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR
        } else {
            AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR
        }

        settlingHint = finalState
        setStateInteral(STATE_SETTLING)
        currentAnimation = child.animate().apply {
            translationY(targetY.toFloat())
            duration = ANIMATION_DURATION
            setInterpolator(interpolator)
            setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    currentAnimation = null
                    setStateInteral(finalState)
                }
            })
            setUpdateListener {
                val offset = child.translationY / height
                callbacks.forEach { it.onSlide(child, offset) }
            }
        }
    }

    private fun setStateInteral(@State newState: Int) {
        if (state != newState) {
            state = newState
            childRef?.get()?.let { child ->
                callbacks.forEach { it.onStateChanged(child, newState) }
            }
        }
    }

    // Saved state

    override fun onSaveInstanceState(parent: CoordinatorLayout, child: V): Parcelable {
        val stateToSave = if (state != STATE_SETTLING) state else settlingHint
        return SavedState(super.onSaveInstanceState(parent, child), stateToSave, lockMode)
    }

    override fun onRestoreInstanceState(
        parent: CoordinatorLayout,
        child: V,
        savedState: Parcelable
    ) {
        (savedState as SavedState).let {
            super.onRestoreInstanceState(parent, child, it.superState)
            state = it.state
            lockMode = it.lockMode
        }
    }

    /** SavedState implementation */
    internal class SavedState : AbsSavedState {

        @State internal val state: Int
        @LockMode internal val lockMode: Int

        constructor(source: Parcel) : this(source, null)

        constructor(source: Parcel, loader: ClassLoader?) : super(source, loader) {
            state = source.readInt()
            lockMode = source.readInt()
        }

        constructor(
            superState: Parcelable,
            @State state: Int,
            @LockMode lockMode: Int
        ) : super(superState) {
            this.state = state
            this.lockMode = lockMode
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.writeInt(state)
            dest.writeInt(lockMode)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<SavedState> =
                object : Parcelable.ClassLoaderCreator<SavedState> {
                    override fun createFromParcel(source: Parcel): SavedState {
                        return SavedState(source, null)
                    }

                    override fun createFromParcel(source: Parcel, loader: ClassLoader): SavedState {
                        return SavedState(source, loader)
                    }

                    override fun newArray(size: Int): Array<SavedState?> {
                        return arrayOfNulls(size)
                    }
                }
        }
    }
}
