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

package com.google.samples.apps.iosched.ui.filters

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Paint.Style.STROKE
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.text.Layout.Alignment.ALIGN_CENTER
import android.text.StaticLayout
import android.text.TextPaint
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.AnimationUtils
import android.widget.Checkable
import androidx.annotation.ColorInt
import androidx.core.animation.doOnEnd
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDimensionOrThrow
import androidx.core.content.res.getDimensionPixelSizeOrThrow
import androidx.core.content.res.getDrawableOrThrow
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.withScale
import androidx.core.graphics.withTranslation
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.util.isRtl
import com.google.samples.apps.iosched.util.lerp
import com.google.samples.apps.iosched.util.newStaticLayout
import com.google.samples.apps.iosched.util.textWidth

/**
 * A custom view for displaying filters. Allows a custom presentation of the tag color and selection
 * state.
 */
class FilterChipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr), Checkable {

    var color: Int = 0
        set(value) {
            if (field != value) {
                field = value
                dotPaint.color = value
                postInvalidateOnAnimation()
            }
        }

    var selectedTextColor: Int = 0
        set(value) {
            field = value
            if (value != 0) {
                clear.mutate().setTint(value)
            }
        }

    var text: CharSequence = ""
        set(value) {
            field = value
            updateContentDescription()
            requestLayout()
        }

    private var showIcons: Boolean = true
        set(value) {
            if (field != value) {
                field = value
                requestLayout()
            }
        }

    private var progress = 0f
        set(value) {
            if (field != value) {
                field = value
                postInvalidateOnAnimation()
                if (value == 0f || value == 1f) {
                    updateContentDescription()
                }
            }
        }

    private val padding: Int

    private val outlinePaint: Paint

    private val textPaint: TextPaint

    private val dotPaint: Paint

    private val clear: Drawable

    private val touchFeedback: Drawable

    private val cornerRadius: Float

    private lateinit var textLayout: StaticLayout

    private var progressAnimator: ValueAnimator? = null

    private val interp =
        AnimationUtils.loadInterpolator(context, android.R.interpolator.fast_out_slow_in)

    private var chipHeight = 0

    @ColorInt private val defaultTextColor: Int

    init {
        val a = context.obtainStyledAttributes(
            attrs,
            R.styleable.FilterChipView,
            R.attr.filterChipViewStyle,
            R.style.Widget_IOSched_FilterChipView
        )
        outlinePaint = Paint(ANTI_ALIAS_FLAG).apply {
            color = a.getColorOrThrow(R.styleable.FilterChipView_android_strokeColor)
            strokeWidth = a.getDimensionOrThrow(R.styleable.FilterChipView_outlineWidth)
            style = STROKE
        }
        defaultTextColor = a.getColorOrThrow(R.styleable.FilterChipView_android_textColor)
        textPaint = TextPaint(ANTI_ALIAS_FLAG).apply {
            color = defaultTextColor
            textSize = a.getDimensionOrThrow(R.styleable.FilterChipView_android_textSize)
            typeface = Typeface.MONOSPACE
            letterSpacing = a.getFloat(R.styleable.FilterChipView_android_letterSpacing, 0f)
        }
        dotPaint = Paint(ANTI_ALIAS_FLAG)
        clear = a.getDrawableOrThrow(R.styleable.FilterChipView_clearIcon).apply {
            setBounds(
                -intrinsicWidth / 2, -intrinsicHeight / 2, intrinsicWidth / 2, intrinsicHeight / 2
            )
        }
        touchFeedback = a.getDrawableOrThrow(R.styleable.FilterChipView_foreground).apply {
            callback = this@FilterChipView
        }
        padding = a.getDimensionPixelSizeOrThrow(R.styleable.FilterChipView_android_padding)
        isChecked = a.getBoolean(R.styleable.FilterChipView_android_checked, false)
        showIcons = a.getBoolean(R.styleable.FilterChipView_showIcons, true)
        cornerRadius = a.getDimension(R.styleable.FilterChipView_cornerRadius, 0f)
        a.recycle()
        clipToOutline = true
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val nonTextWidth = (4 * padding) +
            (2 * outlinePaint.strokeWidth).toInt() +
            if (showIcons) clear.intrinsicWidth else 0
        val availableTextWidth = when (MeasureSpec.getMode(widthMeasureSpec)) {
            MeasureSpec.EXACTLY -> MeasureSpec.getSize(widthMeasureSpec) - nonTextWidth
            MeasureSpec.AT_MOST -> MeasureSpec.getSize(widthMeasureSpec) - nonTextWidth
            // StaticLayout breaks when given extremely large values. 1000 pixels should be enough.
            MeasureSpec.UNSPECIFIED -> 1000
            else -> 1000
        }

        createLayout(availableTextWidth)
        chipHeight = padding + textLayout.height + padding

        val w = nonTextWidth + textLayout.textWidth()
        val h = chipHeight.coerceAtLeast(suggestedMinimumHeight)
        setMeasuredDimension(w, h)

        touchFeedback.setBounds(0, 0, w, chipHeight)
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                val rounding = cornerRadius.coerceAtMost(chipHeight / 2f)
                val top = ((view.height - chipHeight) / 2).coerceAtLeast(0)
                outline.setRoundRect(0, top, view.width, top + chipHeight, rounding)
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        val ty = ((height - chipHeight) / 2f).coerceAtLeast(0f)
        canvas.withTranslation(y = ty) {
            drawChip(canvas)
        }
    }

    private fun drawChip(canvas: Canvas) {
        val strokeWidth = outlinePaint.strokeWidth
        val iconRadius = clear.intrinsicWidth / 2f
        val halfStroke = strokeWidth / 2f
        val rounding = cornerRadius.coerceAtMost((chipHeight - strokeWidth) / 2f)
        val isRtl = isRtl()

        // Outline
        if (progress < 1f) {
            canvas.drawRoundRect(
                halfStroke,
                halfStroke,
                width - halfStroke,
                chipHeight - halfStroke,
                rounding,
                rounding,
                outlinePaint
            )
        }

        // Tag color dot/background
        if (showIcons) {
            // Draws beyond bounds and relies on clipToOutline to enforce pill shape
            val dotRadius = lerp(
                strokeWidth + iconRadius,
                width.toFloat(),
                progress
            )
            val dotCenterX = strokeWidth + padding + iconRadius
            canvas.drawCircle(
                if (isRtl) width - dotCenterX else dotCenterX,
                chipHeight / 2f,
                dotRadius,
                dotPaint
            )
        } else {
            canvas.drawRoundRect(
                halfStroke,
                halfStroke,
                width - halfStroke,
                chipHeight - halfStroke,
                rounding,
                rounding,
                dotPaint
            )
        }

        // Text
        val textLayoutDiff = (textLayout.width - textLayout.textWidth()) / 2
        val textBaseOffset = strokeWidth + padding * 2f
        val textAnimOffset = if (showIcons) {
            val offsetProgress = if (isRtl) progress else 1f - progress
            clear.intrinsicWidth * offsetProgress
        } else {
            0f
        }
        val textX = textBaseOffset + textAnimOffset - textLayoutDiff

        val selectedColor = selectedTextColor
        textPaint.color = if (selectedColor != 0 && progress > 0) {
            ColorUtils.blendARGB(defaultTextColor, selectedColor, progress)
        } else {
            defaultTextColor
        }
        canvas.withTranslation(
            x = textX,
            y = (chipHeight - textLayout.height) / 2f
        ) {
            textLayout.draw(canvas)
        }

        // Clear icon
        if (showIcons && progress > 0f) {
            val iconX = width - strokeWidth - padding - iconRadius
            canvas.withTranslation(
                x = if (isRtl) width - iconX else iconX,
                y = chipHeight / 2f
            ) {
                canvas.withScale(progress, progress) {
                    clear.draw(canvas)
                }
            }
        }

        // Touch feedback
        touchFeedback.draw(canvas)
    }

    /**
     * Starts the animation to enable/disable a filter and invokes a function when done.
     */
    fun animateCheckedAndInvoke(checked: Boolean, onEnd: (() -> Unit)?) {
        val newProgress = if (checked) 1f else 0f
        if (newProgress != progress) {
            progressAnimator?.cancel()
            progressAnimator = ValueAnimator.ofFloat(progress, newProgress).apply {
                addUpdateListener {
                    progress = it.animatedValue as Float
                }
                doOnEnd {
                    progress = newProgress
                    onEnd?.invoke()
                }
                interpolator = interp
                duration = if (checked) SELECTING_DURATION else DESELECTING_DURATION
                start()
            }
        }
    }

    override fun isChecked() = progress == 1f

    override fun toggle() {
        progress = if (progress == 0f) 1f else 0f
    }

    override fun setChecked(checked: Boolean) {
        progress = if (checked) 1f else 0f
    }

    override fun verifyDrawable(who: Drawable): Boolean {
        return super.verifyDrawable(who) || who == touchFeedback
    }

    override fun drawableStateChanged() {
        super.drawableStateChanged()
        touchFeedback.state = drawableState
    }

    override fun jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState()
        touchFeedback.jumpToCurrentState()
    }

    override fun drawableHotspotChanged(x: Float, y: Float) {
        super.drawableHotspotChanged(x, y)
        touchFeedback.setHotspot(x, y)
    }

    private fun createLayout(textWidth: Int) {
        textLayout = newStaticLayout(text, textPaint, textWidth, ALIGN_CENTER, 1f, 0f, true)
    }

    private fun updateContentDescription() {
        val desc = if (isChecked) R.string.a11y_filter_applied else R.string.a11y_filter_not_applied
        contentDescription = resources.getString(desc, text)
    }

    companion object {
        private const val SELECTING_DURATION = 350L
        private const val DESELECTING_DURATION = 200L
    }
}
