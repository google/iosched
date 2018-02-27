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

package com.google.samples.apps.iosched.ui.schedule.agenda

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Rect
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.RecyclerView
import android.text.Layout.Alignment.ALIGN_OPPOSITE
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.view.View
import androidx.content.res.getColorOrThrow
import androidx.content.res.getDimensionOrThrow
import androidx.content.res.getDimensionPixelSizeOrThrow
import androidx.content.res.getResourceIdOrThrow
import androidx.graphics.withTranslation
import androidx.text.inSpans
import androidx.view.get
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.model.Block
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

/**
 * A [RecyclerView.ItemDecoration] which draws sticky headers marking the days in a given list of
 * [blocks]. It also inserts gaps & a dividing line between days.
 */
class ScheduleAgendaHeadersDecoration(
    context: Context,
    blocks: List<Block>
) : RecyclerView.ItemDecoration() {

    private val textPaint: TextPaint
    private val dividerPaint: Paint
    private val width: Int
    private val padding: Int
    private val margin: Int
    private val dayFormatter = DateTimeFormatter.ofPattern("eee")
    private val dateFormatter = DateTimeFormatter.ofPattern("d")
    private val dateTextSize: Int

    init {
        val attrs = context.obtainStyledAttributes(
            R.style.Widget_IOSched_DateHeaders,
            R.styleable.DateHeader
        )
        textPaint = TextPaint(ANTI_ALIAS_FLAG).apply {
            color = attrs.getColorOrThrow(R.styleable.DateHeader_android_textColor)
            textSize = attrs.getDimensionOrThrow(R.styleable.DateHeader_dayTextSize)
            try {
                typeface = ResourcesCompat.getFont(
                    context,
                    attrs.getResourceIdOrThrow(R.styleable.DateHeader_android_fontFamily)
                )
            } catch (nfe: Resources.NotFoundException) {
            }
        }
        dividerPaint = Paint().apply {
            color = attrs.getColorOrThrow(R.styleable.DateHeader_android_divider)
            strokeWidth = attrs.getDimensionOrThrow(R.styleable.DateHeader_android_dividerHeight)
        }
        width = attrs.getDimensionPixelSizeOrThrow(R.styleable.DateHeader_android_width)
        padding = attrs.getDimensionPixelSizeOrThrow(R.styleable.DateHeader_android_padding)
        margin = attrs.getDimensionPixelSizeOrThrow(R.styleable.DateHeader_android_layout_margin)
        dateTextSize = attrs.getDimensionPixelSizeOrThrow(R.styleable.DateHeader_dateTextSize)
        attrs.recycle()
    }

    // Get the block index:day and create header layouts for each
    private val daySlots: Map<Int, StaticLayout> =
        indexAgendaHeaders(blocks).map {
            it.first to createHeader(it.second)
        }.toMap()

    /**
     *  Add gaps between days, split over the last and first block of a day.
     */
    override fun getItemOffsets(
        outRect: Rect, view: View, parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        if (position <= 0) return

        if (daySlots.containsKey(position)) {
            // first block of day, pad top
            outRect.top = padding
        } else if (daySlots.containsKey(position + 1)) {
            // last block of day, pad bottom
            outRect.bottom = padding
        }
    }

    /**
     * Loop over each child and draw any corresponding headers i.e. items who's position is a key in
     * [daySlots]. We also look back to see if there are any headers _before_ the first header we
     * found i.e. which needs to be sticky.
     */
    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State?) {
        if (daySlots.isEmpty()) return

        var earliestFoundHeaderPos = -1
        var prevHeaderTop = Int.MAX_VALUE

        // Loop over each attached view looking for header items.
        // Loop backwards as a lower header can push another higher one upward.
        for (i in parent.childCount - 1 downTo 0) {
            val view = parent[i]
            val viewTop = view.top + view.translationY.toInt()
            if (view.bottom > 0 && viewTop < parent.height) {
                val position = parent.getChildAdapterPosition(view)
                daySlots[position]?.let { layout ->
                    val top = (viewTop + padding)
                        .coerceAtLeast(padding)
                        .coerceAtMost(prevHeaderTop - layout.height)
                    c.withTranslation(y = top.toFloat()) {
                        layout.draw(c)

                        // draw a divider line above day headers (except the first)
                        if (position != 0) {
                            val dividerY = padding * -2f
                            c.drawLine(0f, dividerY, parent.width.toFloat(), dividerY, dividerPaint)
                        }
                    }
                    earliestFoundHeaderPos = position
                    prevHeaderTop = viewTop - padding - padding
                }
            }
        }

        // If no headers found, ensure header of the first shown item is drawn.
        if (earliestFoundHeaderPos < 0) {
            earliestFoundHeaderPos = parent.getChildAdapterPosition(parent[0]) + 1
        }

        // Look back over headers to see if a prior item should be drawn sticky.
        for (headerPos in daySlots.keys.reversed()) {
            if (headerPos < earliestFoundHeaderPos) {
                daySlots[headerPos]?.let { layout ->
                    val top = (prevHeaderTop - layout.height).coerceAtMost(padding)
                    c.withTranslation(y = top.toFloat()) {
                        layout.draw(c)
                    }
                }
                break
            }
        }
    }

    /**
     * Create a header layout for the given [day]
     */
    private fun createHeader(day: ZonedDateTime): StaticLayout {
        val text = SpannableStringBuilder(dayFormatter.format(day).toUpperCase()).apply {
            append(System.lineSeparator())
            inSpans(AbsoluteSizeSpan(dateTextSize)) {
                append(dateFormatter.format(day))
            }
        }
        return StaticLayout(text, textPaint, width - margin, ALIGN_OPPOSITE, 1f, 0f, false)
    }
}
