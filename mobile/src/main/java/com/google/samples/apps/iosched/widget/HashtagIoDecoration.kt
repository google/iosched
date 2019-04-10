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
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import androidx.core.graphics.withTranslation
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.State
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.util.isRtl
import kotlin.math.max
import kotlin.math.roundToInt

class HashtagIoDecoration(context: Context) : ItemDecoration() {

    private val drawable: Drawable?
    private val margin: Int

    private var decorBottom = 0

    init {
        val attrs = context.obtainStyledAttributes(
            R.style.Widget_IOSched_HashtagIoDecoration,
            R.styleable.HashtagIoDecoration
        )
        drawable = attrs.getDrawable(R.styleable.HashtagIoDecoration_android_drawable)?.apply {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
        }
        margin = attrs.getDimensionPixelSize(R.styleable.HashtagIoDecoration_margin, 0)
        attrs.recycle()

        decorBottom = 2 * margin + (drawable?.intrinsicHeight ?: 0)
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
        // Decorate the last child only.
        if (drawable == null || parent.getChildAdapterPosition(view) != state.itemCount - 1) {
            super.getItemOffsets(outRect, view, parent, state)
        } else {
            outRect.set(0, 0, 0, decorBottom)
        }
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: State) {
        if (drawable == null) {
            return
        }

        val x = if (parent.isRtl()) {
            parent.paddingEnd + margin
        } else {
            parent.width - parent.paddingEnd - margin - drawable.intrinsicWidth
        }

        val yFromParentBottom =
            parent.height - parent.paddingBottom - margin - drawable.intrinsicHeight
        if (state.itemCount < 1) {
            // No children. Just draw at the bottom of the parent.
            drawDecoration(canvas, x, yFromParentBottom)
            return
        }

        // Find the decorated view or bust.
        val child = findTargetChild(parent, state.itemCount - 1) ?: return
        val yFromChildBottom = child.bottom + child.translationY.roundToInt() + margin
        if (yFromChildBottom > parent.height) {
            // There's no room below the child, so the decoration is not visible.
            return
        }

        // Pin the decoration to the bottom of the parent if there's excess space.
        val y = max(yFromChildBottom, yFromParentBottom)
        drawDecoration(canvas, x, y)
    }

    private fun findTargetChild(parent: RecyclerView, adapterPosition: Int): View? {
        parent.forEach { child ->
            if (parent.getChildAdapterPosition(child) == adapterPosition) {
                return child
            }
        }
        return null
    }

    private fun drawDecoration(canvas: Canvas, x: Int, y: Int) {
        canvas.withTranslation(x = x.toFloat(), y = y.toFloat()) {
            drawable?.draw(canvas)
        }
    }
}
