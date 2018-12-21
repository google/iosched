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

package com.google.samples.apps.iosched.ui.agenda

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.Rect
import android.graphics.Typeface.BOLD
import android.text.Layout.Alignment.ALIGN_CENTER
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDimensionOrThrow
import androidx.core.content.res.getDimensionPixelSizeOrThrow
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.graphics.withTranslation
import androidx.core.text.inSpans
import androidx.core.view.get
import androidx.core.view.isEmpty
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.State
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.Block
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter

/**
 * A [RecyclerView.ItemDecoration] which draws sticky headers marking the days in a given list of
 * [Block]s. It also inserts gaps between days.
 */
class AgendaHeadersDecoration(
    context: Context,
    blocks: List<Block>
) : ItemDecoration() {

    private val paint: TextPaint
    private val width: Int
    private val paddingTop: Int
    private val dateFormatter = DateTimeFormatter.ofPattern("d")
    private val dayFormatter = DateTimeFormatter.ofPattern("eee")
    private val dayTextSize: Int

    init {
        val attrs = context.obtainStyledAttributes(
            R.style.Widget_IOSched_DateHeaders,
            R.styleable.DateHeader
        )
        paint = TextPaint(ANTI_ALIAS_FLAG).apply {
            color = attrs.getColorOrThrow(R.styleable.DateHeader_android_textColor)
            textSize = attrs.getDimensionOrThrow(R.styleable.DateHeader_dateTextSize)
            try {
                typeface = ResourcesCompat.getFont(
                    context,
                    attrs.getResourceIdOrThrow(R.styleable.DateHeader_android_fontFamily)
                )
            } catch (_: Exception) {
                // ignore
            }
        }
        width = attrs.getDimensionPixelSizeOrThrow(R.styleable.DateHeader_android_width)
        paddingTop = attrs.getDimensionPixelSizeOrThrow(R.styleable.DateHeader_android_paddingTop)
        dayTextSize = attrs.getDimensionPixelSizeOrThrow(R.styleable.DateHeader_dayTextSize)
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
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
        val position = parent.getChildAdapterPosition(view)
        if (position <= 0) return

        if (daySlots.containsKey(position)) {
            // first block of day, pad top
            outRect.top = paddingTop
        } else if (daySlots.containsKey(position + 1)) {
            // last block of day, pad bottom
            outRect.bottom = paddingTop
        }
    }

    /**
     * Loop over each child and draw any corresponding headers i.e. items who's position is a key in
     * [daySlots]. We also look back to see if there are any headers _before_ the first header we
     * found i.e. which needs to be sticky.
     */

    override fun onDraw(c: Canvas, parent: RecyclerView, state: State) {
        if (daySlots.isEmpty() || parent.isEmpty()) return

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
                    paint.alpha = (view.alpha * 255).toInt()
                    val top = (viewTop + paddingTop)
                        .coerceAtLeast(paddingTop)
                        .coerceAtMost(prevHeaderTop - layout.height)
                    c.withTranslation(y = top.toFloat()) {
                        layout.draw(c)
                    }
                    earliestFoundHeaderPos = position
                    prevHeaderTop = viewTop - paddingTop - paddingTop
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
                    val top = (prevHeaderTop - layout.height).coerceAtMost(paddingTop)
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
        val text = SpannableStringBuilder(dateFormatter.format(day)).apply {
            append(System.lineSeparator())
            inSpans(AbsoluteSizeSpan(dayTextSize), StyleSpan(BOLD)) {
                append(dayFormatter.format(day).toUpperCase())
            }
        }
        return StaticLayout(text, paint, width, ALIGN_CENTER, 1f, 0f, false)
    }
}
