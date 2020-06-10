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
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.util.AttributeSet
import android.view.Window
import android.view.WindowInsets
import android.widget.FrameLayout
import com.google.samples.apps.iosched.R
import kotlin.LazyThreadSafetyMode.NONE

/**
 * A [FrameLayout] which will draw a divider on the edge of the navigation bar. Similar in
 * functionality to what [Window.setNavigationBarDividerColor] provides, but works with translucent
 * navigation bar colors.
 */
class NavigationBarContentFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    var navigationBarDividerColor: Int = 0
        set(value) {
            field = value
            dividerDrawable.color = value
        }

    var navigationBarDividerSize: Int = 0
        set(value) {
            field = value
            updateDividerBounds()
        }

    private var lastInsets: WindowInsets? = null

    private val dividerDrawable by lazy(NONE) {
        ColorDrawable(navigationBarDividerColor).apply {
            callback = this@NavigationBarContentFrameLayout
            alpha = 255
        }
    }

    init {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.NavigationBarContentFrameLayout,
            defStyleAttr,
            R.style.Widget_IOSched_NavigationBarContentFrameLayout
        )

        navigationBarDividerColor = a.getColor(
            R.styleable.NavigationBarContentFrameLayout_navigationBarBorderColor,
            Color.MAGENTA
        )
        navigationBarDividerSize = a.getDimensionPixelSize(
            R.styleable.NavigationBarContentFrameLayout_navigationBarBorderSize,
            0
        )

        a.recycle()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateDividerBounds()
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)

        val d = dividerDrawable
        if (!d.bounds.isEmpty) {
            d.draw(canvas)
        }
    }

    override fun onApplyWindowInsets(insets: WindowInsets): WindowInsets {
        lastInsets = insets
        updateDividerBounds()
        return insets
    }

    private fun updateDividerBounds() {
        val d = dividerDrawable
        val insets = lastInsets

        val bottomInset = insets?.systemWindowInsetBottom ?: 0
        val leftInset = insets?.systemWindowInsetLeft ?: 0
        val rightInset = insets?.systemWindowInsetRight ?: 0

        when {
            bottomInset > 0 -> {
                // Display divider above bottom inset
                d.setBounds(
                    left,
                    bottom - bottomInset,
                    right,
                    bottom + navigationBarDividerSize - bottomInset
                )
            }
            leftInset > 0 -> {
                // Display divider to the right of the left inset
                d.setBounds(
                    leftInset - navigationBarDividerSize,
                    top,
                    leftInset,
                    bottom
                )
            }
            rightInset > 0 -> {
                // Display divider to the left of the right inset
                d.setBounds(
                    right - rightInset,
                    top,
                    right + navigationBarDividerSize - rightInset,
                    bottom
                )
            }
            else -> {
                // Set an empty size which will remove the drawable
                d.setBounds(0, 0, 0, 0)
            }
        }

        // Update our will-not-draw flag
        setWillNotDraw(d.bounds.isEmpty)
    }
}
