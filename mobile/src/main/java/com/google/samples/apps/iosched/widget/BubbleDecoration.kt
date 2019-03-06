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

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.Style.FILL
import android.graphics.RectF
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.State
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.util.lerp
import kotlin.math.max
import kotlin.math.min

/**
 * ItemDecoration that draws a bubble background around items in a specified range.
 */
class BubbleDecoration(context: Context) : ItemDecoration() {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = FILL
    }

    private val insetHorizontal: Float
    private val insetVertical: Float

    private val currentRect = RectF()
    private val previousRect = RectF()
    private val temp = RectF() // to avoid object allocations

    private var animator: ValueAnimator? = null
    private var pendingAnimation = false

    private var progress = 1f

    /** The adapter positions to decorate. */
    var bubbleRange: IntRange = -1..-1
        set(value) {
            field = value
            pendingAnimation = true
        }

    init {
        val attrs = context.obtainStyledAttributes(
            R.style.Widget_IOSched_DayIndicatorDecoration,
            R.styleable.DayIndicatorDecoration
        )
        paint.color = attrs.getColor(R.styleable.DayIndicatorDecoration_android_color, 0)
        insetHorizontal = attrs.getDimension(R.styleable.DayIndicatorDecoration_insetHorizontal, 0f)
        insetVertical = attrs.getDimension(R.styleable.DayIndicatorDecoration_insetVertical, 0f)
        attrs.recycle()
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: State) {
        if (pendingAnimation) {
            pendingAnimation = false

            animator?.cancel()

            // Update rects
            computeTargetRect(parent, state, bubbleRange, temp)
            previousRect.set(currentRect)
            currentRect.set(temp)

            startAnimatorIfNeeded(previousRect, currentRect, parent)
        }

        val rect = getDrawingRect(previousRect, currentRect)
        drawBubble(rect, canvas)
    }

    private fun drawBubble(rect: RectF, canvas: Canvas) {
        if (rect.isEmpty) return

        val radius = min(rect.width(), rect.height()) / 2
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    private fun getDrawingRect(initial: RectF, target: RectF): RectF {
        when {
            initial.isEmpty -> computeScalingRect(target, progress)
            target.isEmpty -> computeScalingRect(initial, progress)
            else -> computeMovingRect(initial, target, progress)
        }
        return temp
    }

    private fun computeScalingRect(rect: RectF, progress: Float) {
        // Create the effect of growing/shrinking from the center.
        val dx = rect.width() * progress / 2
        val dy = rect.height() * progress / 2
        temp.set(
            rect.centerX() - dx,
            rect.centerY() - dy,
            rect.centerX() + dx,
            rect.centerY() + dy
        )
    }

    private fun computeMovingRect(initial: RectF, target: RectF, progress: Float) {
        temp.set(
            lerp(initial.left, target.left, progress),
            lerp(initial.top, target.top, progress),
            lerp(initial.right, target.right, progress),
            lerp(initial.bottom, target.bottom, progress)
        )
    }

    private fun computeTargetRect(
        parent: RecyclerView,
        state: State,
        range: IntRange,
        outRectF: RectF
    ) {
        if (state.itemCount < 1 || range.isEmpty()) {
            outRectF.setEmpty()
            return
        }

        var minLeft = parent.width.toFloat()
        var minTop = parent.height.toFloat()
        var maxRight = 0f
        var maxBottom = 0f

        // Compose the rect using views whose adapter positions are in range. There may be extra
        // views due to item animations, so only use the first view found for each position.
        val seenPositions = hashSetOf<Int>()
        parent.forEach { view ->
            val position = parent.getChildViewHolder(view).adapterPosition
            if (position != -1 && position in range && seenPositions.add(position)) {
                minLeft = min(minLeft, view.left.toFloat())
                minTop = min(minTop, view.top.toFloat())
                maxRight = max(maxRight, view.right.toFloat())
                maxBottom = max(maxBottom, view.bottom.toFloat())
            }
        }

        outRectF.set(minLeft, minTop, maxRight, maxBottom)
        outRectF.inset(insetHorizontal, insetVertical)
    }

    private fun startAnimatorIfNeeded(initial: RectF, target: RectF, parent: RecyclerView) {
        if ((initial.isEmpty && target.isEmpty) || initial == target) {
            return
        }

        animator = if (target.isEmpty) {
            ValueAnimator.ofFloat(1f, 0f) // disappearing animation
        } else {
            ValueAnimator.ofFloat(0f, 1f) // appearing or moving animation
        }.apply {
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    animator = null
                }
            })
            addUpdateListener {
                progress = animatedValue as Float
                // Invalidate so we get to draw on the next frame.
                parent.invalidateItemDecorations()
            }

            start()
        }
    }
}
