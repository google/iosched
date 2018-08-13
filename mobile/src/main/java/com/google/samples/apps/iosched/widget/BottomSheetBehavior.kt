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

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import androidx.annotation.IntDef
import androidx.annotation.VisibleForTesting
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout.Behavior
import androidx.core.view.ViewCompat
import androidx.customview.view.AbsSavedState
import androidx.customview.widget.ViewDragHelper
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.util.readBoolean
import com.google.samples.apps.iosched.shared.util.writeBoolean
import java.lang.ref.WeakReference
import kotlin.math.absoluteValue

/**
 * Copy of material lib's BottomSheetBehavior that includes some bug fixes.
 */
// TODO remove when a fixed version in material lib is released.
class BottomSheetBehavior<V : View> : Behavior<V> {

    companion object {
        /** The bottom sheet is dragging. */
        const val STATE_DRAGGING = 1
        /** The bottom sheet is settling. */
        const val STATE_SETTLING = 2
        /** The bottom sheet is expanded. */
        const val STATE_EXPANDED = 3
        /** The bottom sheet is collapsed. */
        const val STATE_COLLAPSED = 4
        /** The bottom sheet is hidden. */
        const val STATE_HIDDEN = 5
        /** The bottom sheet is half-expanded (used when behavior_fitToContents is false). */
        const val STATE_HALF_EXPANDED = 6

        /**
         * Peek at the 16:9 ratio keyline of its parent. This can be used as a parameter for
         * [setPeekHeight(Int)]. [getPeekHeight()] will return this when the value is set.
         */
        const val PEEK_HEIGHT_AUTO = -1

        private const val HIDE_THRESHOLD = 0.5f
        private const val HIDE_FRICTION = 0.1f

        @IntDef(
            value = [STATE_DRAGGING,
                STATE_SETTLING,
                STATE_EXPANDED,
                STATE_COLLAPSED,
                STATE_HIDDEN,
                STATE_HALF_EXPANDED]
        )
        @Retention(AnnotationRetention.SOURCE)
        annotation class State

        /** Utility to get the [BottomSheetBehavior] from a [view]. */
        @JvmStatic
        fun from(view: View): BottomSheetBehavior<*> {
            val lp = view.layoutParams as? CoordinatorLayout.LayoutParams
                ?: throw IllegalArgumentException("view is not a child of CoordinatorLayout")
            return lp.behavior as? BottomSheetBehavior
                ?: throw IllegalArgumentException("view not associated with this behavior")
        }
    }

    /** Callback for monitoring events about bottom sheets. */
    interface BottomSheetCallback {
        /**
         * Called when the bottom sheet changes its state.
         *
         * @param bottomSheet The bottom sheet view.
         * @param newState The new state. This will be one of link [STATE_DRAGGING],
         * [STATE_SETTLING], [STATE_EXPANDED], [STATE_COLLAPSED], [STATE_HIDDEN], or
         * [STATE_HALF_EXPANDED].
         */
        fun onStateChanged(bottomSheet: View, newState: Int) {}

        /**
         * Called when the bottom sheet is being dragged.
         *
         * @param bottomSheet The bottom sheet view.
         * @param slideOffset The new offset of this bottom sheet within [-1,1] range. Offset
         * increases as this bottom sheet is moving upward. From 0 to 1 the sheet is between
         * collapsed and expanded states and from -1 to 0 it is between hidden and collapsed states.
         */
        fun onSlide(bottomSheet: View, slideOffset: Float) {}
    }

    /** The current state of the bottom sheet, backing property */
    private var _state = STATE_COLLAPSED
    /** The current state of the bottom sheet */
    @State
    var state
        get() = _state
        set(@State value) {
            if (_state == value) {
                return
            }
            if (viewRef == null) {
                // Child is not laid out yet. Set our state and let onLayoutChild() handle it later.
                if (value == STATE_COLLAPSED ||
                    value == STATE_EXPANDED ||
                    value == STATE_HALF_EXPANDED ||
                    (isHideable && value == STATE_HIDDEN)
                ) {
                    _state = value
                }
                return
            }

            viewRef?.get()?.apply {
                // Start the animation; wait until a pending layout if there is one.
                if (parent != null && parent.isLayoutRequested && isAttachedToWindow) {
                    post {
                        startSettlingAnimation(this, value)
                    }
                } else {
                    startSettlingAnimation(this, value)
                }
            }
        }

    /** Whether to fit to contents. If false, the behavior will include [STATE_HALF_EXPANDED]. */
    var isFitToContents = true
        set(value) {
            if (field != value) {
                field = value
                // If sheet is already laid out, recalculate the collapsed offset.
                // Otherwise onLayoutChild will handle this later.
                if (viewRef != null) {
                    collapsedOffset = calculateCollapsedOffset()
                }
                // Fix incorrect expanded settings.
                setStateInternal(
                    if (field && state == STATE_HALF_EXPANDED) STATE_EXPANDED else state
                )
            }
        }

    /** Real peek height in pixels */
    private var _peekHeight = 0
    /** Peek height in pixels, or [PEEK_HEIGHT_AUTO] */
    var peekHeight
        get() = if (peekHeightAuto) PEEK_HEIGHT_AUTO else _peekHeight
        set(value) {
            var needLayout = false
            if (value == PEEK_HEIGHT_AUTO) {
                if (!peekHeightAuto) {
                    peekHeightAuto = true
                    needLayout = true
                }
            } else if (peekHeightAuto || _peekHeight != value) {
                peekHeightAuto = false
                _peekHeight = Math.max(0, value)
                collapsedOffset = parentHeight - value
                needLayout = true
            }
            if (needLayout && (state == STATE_COLLAPSED || state == STATE_HIDDEN)) {
                viewRef?.get()?.requestLayout()
            }
        }

    /** Whether the bottom sheet can be hidden. */
    var isHideable = false
        set(value) {
            if (field != value) {
                field = value
                if (!value && state == STATE_HIDDEN) {
                    // Fix invalid state by moving to collapsed
                    state = STATE_COLLAPSED
                }
            }
        }

    /** Whether the bottom sheet can be dragged or not. */
    var isDraggable = true

    /** Whether the bottom sheet should skip collapsed state after being expanded once. */
    var skipCollapsed = false

    /** Whether animations should be disabled, to be used from UI tests. */
    @VisibleForTesting
    var isAnimationDisabled = false

    /** Whether or not to use automatic peek height */
    private var peekHeightAuto = false
    /** Minimum peek height allowed */
    private var peekHeightMin = 0
    /** The last peek height calculated in onLayoutChild */
    private var lastPeekHeight = 0

    private var parentHeight = 0
    /** Bottom sheet's top offset in [STATE_EXPANDED] state. */
    private var fitToContentsOffset = 0
    /** Bottom sheet's top offset in [STATE_HALF_EXPANDED] state. */
    private var halfExpandedOffset = 0
    /** Bottom sheet's top offset in [STATE_COLLAPSED] state. */
    private var collapsedOffset = 0

    /** Keeps reference to the bottom sheet outside of Behavior callbacks */
    private var viewRef: WeakReference<View>? = null
    /** Controls movement of the bottom sheet */
    private lateinit var dragHelper: ViewDragHelper

    // Touch event handling, etc
    private var lastTouchX = 0
    private var lastTouchY = 0
    private var initialTouchY = 0
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var acceptTouches = true

    private var minimumVelocity = 0
    private var maximumVelocity = 0
    private var velocityTracker: VelocityTracker? = null

    private var nestedScrolled = false
    private var nestedScrollingChildRef: WeakReference<View>? = null

    private val callbacks: MutableSet<BottomSheetCallback> = mutableSetOf()

    constructor() : super()

    @SuppressLint("PrivateResource")
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        // Re-use BottomSheetBehavior's attrs
        val a = context.obtainStyledAttributes(attrs, R.styleable.BottomSheetBehavior_Layout)
        val value = a.peekValue(R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight)
        peekHeight = if (value != null && value.data == PEEK_HEIGHT_AUTO) {
            value.data
        } else {
            a.getDimensionPixelSize(
                R.styleable.BottomSheetBehavior_Layout_behavior_peekHeight, PEEK_HEIGHT_AUTO
            )
        }
        isHideable = a.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_hideable, false)
        isFitToContents =
            a.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_fitToContents, true)
        skipCollapsed =
            a.getBoolean(R.styleable.BottomSheetBehavior_Layout_behavior_skipCollapsed, false)
        a.recycle()
        val configuration = ViewConfiguration.get(context)
        minimumVelocity = configuration.scaledMinimumFlingVelocity
        maximumVelocity = configuration.scaledMaximumFlingVelocity
    }

    override fun onSaveInstanceState(parent: CoordinatorLayout, child: V): Parcelable {
        return SavedState(
            super.onSaveInstanceState(parent, child) ?: Bundle.EMPTY,
            state,
            peekHeight,
            isFitToContents,
            isHideable,
            skipCollapsed,
            isDraggable
        )
    }

    override fun onRestoreInstanceState(parent: CoordinatorLayout, child: V, state: Parcelable) {
        val ss = state as SavedState
        super.onRestoreInstanceState(parent, child, ss.superState ?: Bundle.EMPTY)

        isDraggable = ss.isDraggable
        peekHeight = ss.peekHeight
        isFitToContents = ss.isFitToContents
        isHideable = ss.isHideable
        skipCollapsed = ss.skipCollapsed

        // Set state last. Intermediate states are restored as collapsed state.
        _state = if (ss.state == STATE_DRAGGING || ss.state == STATE_SETTLING) {
            STATE_COLLAPSED
        } else {
            ss.state
        }
    }

    fun addBottomSheetCallback(callback: BottomSheetCallback) {
        callbacks.add(callback)
    }

    fun removeBottomSheetCallback(callback: BottomSheetCallback) {
        callbacks.remove(callback)
    }

    private fun setStateInternal(@State state: Int) {
        if (_state != state) {
            _state = state
            viewRef?.get()?.let { view ->
                callbacks.forEach { callback ->
                    callback.onStateChanged(view, state)
                }
            }
        }
    }

    // -- Layout

    override fun onLayoutChild(
        parent: CoordinatorLayout,
        child: V,
        layoutDirection: Int
    ): Boolean {
        if (parent.fitsSystemWindows && !child.fitsSystemWindows) {
            child.fitsSystemWindows = true
        }
        val savedTop = child.top
        // First let the parent lay it out
        parent.onLayoutChild(child, layoutDirection)
        parentHeight = parent.height

        // Calculate peek and offsets
        if (peekHeightAuto) {
            if (peekHeightMin == 0) {
                // init peekHeightMin
                @SuppressLint("PrivateResource")
                peekHeightMin = parent.resources.getDimensionPixelSize(
                    R.dimen.design_bottom_sheet_peek_height_min
                )
            }
            lastPeekHeight = Math.max(peekHeightMin, parentHeight - parent.width * 9 / 16)
        } else {
            lastPeekHeight = _peekHeight
        }
        fitToContentsOffset = Math.max(0, parentHeight - child.height)
        halfExpandedOffset = parentHeight / 2
        collapsedOffset = calculateCollapsedOffset()

        // Offset the bottom sheet
        when (state) {
            STATE_EXPANDED -> ViewCompat.offsetTopAndBottom(child, getExpandedOffset())
            STATE_HALF_EXPANDED -> ViewCompat.offsetTopAndBottom(child, halfExpandedOffset)
            STATE_HIDDEN -> ViewCompat.offsetTopAndBottom(child, parentHeight)
            STATE_COLLAPSED -> ViewCompat.offsetTopAndBottom(child, collapsedOffset)
            STATE_DRAGGING, STATE_SETTLING -> ViewCompat.offsetTopAndBottom(
                child, savedTop - child.top
            )
        }

        // Init these for later
        viewRef = WeakReference(child)
        if (!::dragHelper.isInitialized) {
            dragHelper = ViewDragHelper.create(parent, dragCallback)
        }
        return true
    }

    private fun calculateCollapsedOffset(): Int {
        return if (isFitToContents) {
            Math.max(parentHeight - lastPeekHeight, fitToContentsOffset)
        } else {
            parentHeight - lastPeekHeight
        }
    }

    private fun getExpandedOffset() = if (isFitToContents) fitToContentsOffset else 0

    // -- Touch events and scrolling

    override fun onInterceptTouchEvent(
        parent: CoordinatorLayout,
        child: V,
        event: MotionEvent
    ): Boolean {
        if (!isDraggable || !child.isShown) {
            acceptTouches = false
            return false
        }

        val action = event.actionMasked
        lastTouchX = event.x.toInt()
        lastTouchY = event.y.toInt()

        // Record velocity
        if (action == MotionEvent.ACTION_DOWN) {
            resetVelocityTracker()
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)

        when (action) {
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
                if (!acceptTouches) {
                    acceptTouches = true
                    return false
                }
            }

            MotionEvent.ACTION_DOWN -> {
                activePointerId = event.getPointerId(event.actionIndex)
                initialTouchY = event.y.toInt()

                clearNestedScroll()

                if (!parent.isPointInChildBounds(child, lastTouchX, initialTouchY)) {
                    // Not touching the sheet
                    acceptTouches = false
                }
            }
        }

        return acceptTouches &&
            // CoordinatorLayout can call us before the view is laid out. >_<
            ::dragHelper.isInitialized &&
            dragHelper.shouldInterceptTouchEvent(event)
    }

    override fun onTouchEvent(
        parent: CoordinatorLayout,
        child: V,
        event: MotionEvent
    ): Boolean {
        if (!isDraggable || !child.isShown) {
            return false
        }

        val action = event.actionMasked
        if (action == MotionEvent.ACTION_DOWN && state == STATE_DRAGGING) {
            return true
        }

        lastTouchX = event.x.toInt()
        lastTouchY = event.y.toInt()

        // Record velocity
        if (action == MotionEvent.ACTION_DOWN) {
            resetVelocityTracker()
        }
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain()
        }
        velocityTracker?.addMovement(event)

        // CoordinatorLayout can call us before the view is laid out. >_<
        if (::dragHelper.isInitialized) {
            dragHelper.processTouchEvent(event)
        }

        if (acceptTouches &&
            action == MotionEvent.ACTION_MOVE &&
            exceedsTouchSlop(initialTouchY, lastTouchY)
        ) {
            // Manually capture the sheet since nothing beneath us is scrolling.
            dragHelper.captureChildView(child, event.getPointerId(event.actionIndex))
        }

        return acceptTouches
    }

    private fun resetVelocityTracker() {
        activePointerId = MotionEvent.INVALID_POINTER_ID
        velocityTracker?.recycle()
        velocityTracker = null
    }

    private fun exceedsTouchSlop(p1: Int, p2: Int) = Math.abs(p1 - p2) >= dragHelper.touchSlop

    // Nested scrolling

    override fun onStartNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        directTargetChild: View,
        target: View,
        axes: Int,
        type: Int
    ): Boolean {
        nestedScrolled = false
        if (isDraggable &&
            viewRef?.get() == directTargetChild &&
            (axes and ViewCompat.SCROLL_AXIS_VERTICAL) != 0
        ) {
            // Scrolling view is a descendent of the sheet and scrolling vertically.
            // Let's follow along!
            nestedScrollingChildRef = WeakReference(target)
            return true
        }
        return false
    }

    override fun onNestedPreScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        dx: Int,
        dy: Int,
        consumed: IntArray,
        type: Int
    ) {
        if (type == ViewCompat.TYPE_NON_TOUCH) {
            return // Ignore fling here
        }
        if (target != nestedScrollingChildRef?.get()) {
            return
        }

        val currentTop = child.top
        val newTop = currentTop - dy
        if (dy > 0) { // Upward
            if (newTop < getExpandedOffset()) {
                consumed[1] = currentTop - getExpandedOffset()
                ViewCompat.offsetTopAndBottom(child, -consumed[1])
                setStateInternal(STATE_EXPANDED)
            } else {
                consumed[1] = dy
                ViewCompat.offsetTopAndBottom(child, -dy)
                setStateInternal(STATE_DRAGGING)
            }
        } else if (dy < 0) { // Downward
            if (!target.canScrollVertically(-1)) {
                if (newTop <= collapsedOffset || isHideable) {
                    consumed[1] = dy
                    ViewCompat.offsetTopAndBottom(child, -dy)
                    setStateInternal(STATE_DRAGGING)
                } else {
                    consumed[1] = currentTop - collapsedOffset
                    ViewCompat.offsetTopAndBottom(child, -consumed[1])
                    setStateInternal(STATE_COLLAPSED)
                }
            }
        }
        dispatchOnSlide(child.top)
        nestedScrolled = true
    }

    override fun onStopNestedScroll(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        type: Int
    ) {
        if (child.top == getExpandedOffset()) {
            setStateInternal(STATE_EXPANDED)
            return
        }
        if (target != nestedScrollingChildRef?.get() || !nestedScrolled) {
            return
        }

        settleBottomSheet(child, getYVelocity(), true)
        clearNestedScroll()
    }

    override fun onNestedPreFling(
        coordinatorLayout: CoordinatorLayout,
        child: V,
        target: View,
        velocityX: Float,
        velocityY: Float
    ): Boolean {
        return isDraggable &&
            target == nestedScrollingChildRef?.get() &&
            (state != STATE_EXPANDED || super.onNestedPreFling(
                coordinatorLayout, child, target, velocityX, velocityY
            ))
    }

    private fun clearNestedScroll() {
        nestedScrolled = false
        nestedScrollingChildRef = null
    }

    // Settling

    private fun getYVelocity(): Float {
        return velocityTracker?.run {
            computeCurrentVelocity(1000, maximumVelocity.toFloat())
            getYVelocity(activePointerId)
        } ?: 0f
    }

    private fun settleBottomSheet(sheet: View, yVelocity: Float, isNestedScroll: Boolean) {
        val top: Int
        @State val targetState: Int

        val flinging = yVelocity.absoluteValue > minimumVelocity
        if (flinging && yVelocity < 0) { // Moving up
            if (isFitToContents) {
                top = fitToContentsOffset
                targetState = STATE_EXPANDED
            } else {
                if (sheet.top > halfExpandedOffset) {
                    top = halfExpandedOffset
                    targetState = STATE_HALF_EXPANDED
                } else {
                    top = 0
                    targetState = STATE_EXPANDED
                }
            }
        } else if (isHideable && shouldHide(sheet, yVelocity)) {
            top = parentHeight
            targetState = STATE_HIDDEN
        } else if (flinging && yVelocity > 0) { // Moving down
            top = collapsedOffset
            targetState = STATE_COLLAPSED
        } else {
            val currentTop = sheet.top
            if (isFitToContents) {
                if (Math.abs(currentTop - fitToContentsOffset)
                    < Math.abs(currentTop - collapsedOffset)
                ) {
                    top = fitToContentsOffset
                    targetState = STATE_EXPANDED
                } else {
                    top = collapsedOffset
                    targetState = STATE_COLLAPSED
                }
            } else {
                if (currentTop < halfExpandedOffset) {
                    if (currentTop < Math.abs(currentTop - collapsedOffset)) {
                        top = 0
                        targetState = STATE_EXPANDED
                    } else {
                        top = halfExpandedOffset
                        targetState = STATE_HALF_EXPANDED
                    }
                } else {
                    if (Math.abs(currentTop - halfExpandedOffset)
                        < Math.abs(currentTop - collapsedOffset)
                    ) {
                        top = halfExpandedOffset
                        targetState = STATE_HALF_EXPANDED
                    } else {
                        top = collapsedOffset
                        targetState = STATE_COLLAPSED
                    }
                }
            }
        }

        val startedSettling = if (isNestedScroll) {
            dragHelper.smoothSlideViewTo(sheet, sheet.left, top)
        } else {
            dragHelper.settleCapturedViewAt(sheet.left, top)
        }

        if (startedSettling) {
            setStateInternal(STATE_SETTLING)
            ViewCompat.postOnAnimation(sheet, SettleRunnable(sheet, targetState))
        } else {
            setStateInternal(targetState)
        }
    }

    private fun shouldHide(child: View, yVelocity: Float): Boolean {
        if (skipCollapsed) {
            return true
        }
        if (child.top < collapsedOffset) {
            return false // it should not hide, but collapse.
        }
        val newTop = child.top + yVelocity * HIDE_FRICTION
        return Math.abs(newTop - collapsedOffset) / _peekHeight.toFloat() > HIDE_THRESHOLD
    }

    private fun startSettlingAnimation(child: View, state: Int) {
        var top: Int
        var finalState = state

        when {
            state == STATE_COLLAPSED -> top = collapsedOffset
            state == STATE_EXPANDED -> top = getExpandedOffset()
            state == STATE_HALF_EXPANDED -> {
                top = halfExpandedOffset
                // Skip to expanded state if we would scroll past the height of the contents.
                if (isFitToContents && top <= fitToContentsOffset) {
                    finalState = STATE_EXPANDED
                    top = fitToContentsOffset
                }
            }
            state == STATE_HIDDEN && isHideable -> top = parentHeight
            else -> throw IllegalArgumentException("Invalid state: " + state)
        }

        if (isAnimationDisabled) {
            // Prevent animations
            ViewCompat.offsetTopAndBottom(child, top - child.top)
        }

        if (dragHelper.smoothSlideViewTo(child, child.left, top)) {
            setStateInternal(STATE_SETTLING)
            ViewCompat.postOnAnimation(child, SettleRunnable(child, finalState))
        } else {
            setStateInternal(finalState)
        }
    }

    private fun dispatchOnSlide(top: Int) {
        viewRef?.get()?.let { sheet ->
            val denom = if (top > collapsedOffset) {
                parentHeight - collapsedOffset
            } else {
                collapsedOffset - getExpandedOffset()
            }
            callbacks.forEach { callback ->
                callback.onSlide(sheet, (collapsedOffset - top).toFloat() / denom)
            }
        }
    }

    private inner class SettleRunnable(
        private val view: View,
        @State private val state: Int
    ) : Runnable {
        override fun run() {
            if (dragHelper.continueSettling(true)) {
                view.postOnAnimation(this)
            } else {
                setStateInternal(state)
            }
        }
    }

    private val dragCallback: ViewDragHelper.Callback = object : ViewDragHelper.Callback() {

        override fun tryCaptureView(child: View, pointerId: Int): Boolean {
            when {
            // Sanity check
                state == STATE_DRAGGING -> return false
            // recapture a settling sheet
                dragHelper.viewDragState == ViewDragHelper.STATE_SETTLING -> return true
            // let nested scroll handle this
                nestedScrollingChildRef?.get() != null -> return false
            }

            val dy = lastTouchY - initialTouchY
            if (dy == 0) {
                // ViewDragHelper tries to capture in onTouch for the ACTION_DOWN event, but there's
                // really no way to check for a scrolling child without a direction, so wait.
                return false
            }

            if (state == STATE_COLLAPSED) {
                if (isHideable) {
                    // Any drag should capture in order to expand or hide the sheet
                    return true
                }
                if (dy < 0) {
                    // Expand on upward movement, even if there's scrolling content underneath
                    return true
                }
            }

            // Check for scrolling content underneath the touch point that can scroll in the
            // appropriate direction.
            val scrollingChild = findScrollingChildUnder(child, lastTouchX, lastTouchY, -dy)
            return scrollingChild == null
        }

        private fun findScrollingChildUnder(view: View, x: Int, y: Int, direction: Int): View? {
            if (view.visibility == View.VISIBLE && dragHelper.isViewUnder(view, x, y)) {
                if (view.canScrollVertically(direction)) {
                    return view
                }
                if (view is ViewGroup) {
                    // TODO this doesn't account for elevation or child drawing order.
                    for (i in (view.childCount - 1) downTo 0) {
                        val child = view.getChildAt(i)
                        val found =
                            findScrollingChildUnder(child, x - child.left, y - child.top, direction)
                        if (found != null) {
                            return found
                        }
                    }
                }
            }
            return null
        }

        override fun getViewVerticalDragRange(child: View): Int {
            return if (isHideable) parentHeight else collapsedOffset
        }

        override fun clampViewPositionVertical(child: View, top: Int, dy: Int): Int {
            val maxOffset = if (isHideable) parentHeight else collapsedOffset
            return top.coerceIn(getExpandedOffset(), maxOffset)
        }

        override fun clampViewPositionHorizontal(child: View, left: Int, dx: Int) = child.left

        override fun onViewDragStateChanged(state: Int) {
            if (state == ViewDragHelper.STATE_DRAGGING) {
                setStateInternal(STATE_DRAGGING)
            }
        }

        override fun onViewPositionChanged(child: View, left: Int, top: Int, dx: Int, dy: Int) {
            dispatchOnSlide(top)
        }

        override fun onViewReleased(releasedChild: View, xvel: Float, yvel: Float) {
            settleBottomSheet(releasedChild, yvel, false)
        }
    }

    /** SavedState implementation */
    internal class SavedState : AbsSavedState {

        @State internal val state: Int
        internal val peekHeight: Int
        internal val isFitToContents: Boolean
        internal val isHideable: Boolean
        internal val skipCollapsed: Boolean
        internal val isDraggable: Boolean

        constructor(source: Parcel) : this(source, null)

        constructor(source: Parcel, loader: ClassLoader?) : super(source, loader) {
            state = source.readInt()
            peekHeight = source.readInt()
            isFitToContents = source.readBoolean()
            isHideable = source.readBoolean()
            skipCollapsed = source.readBoolean()
            isDraggable = source.readBoolean()
        }

        constructor(
            superState: Parcelable,
            @State state: Int,
            peekHeight: Int,
            isFitToContents: Boolean,
            isHideable: Boolean,
            skipCollapsed: Boolean,
            isDraggable: Boolean
        ) : super(superState) {
            this.state = state
            this.peekHeight = peekHeight
            this.isFitToContents = isFitToContents
            this.isHideable = isHideable
            this.skipCollapsed = skipCollapsed
            this.isDraggable = isDraggable
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            super.writeToParcel(dest, flags)
            dest.apply {
                writeInt(state)
                writeInt(peekHeight)
                writeBoolean(isFitToContents)
                writeBoolean(isHideable)
                writeBoolean(skipCollapsed)
                writeBoolean(isDraggable)
            }
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
