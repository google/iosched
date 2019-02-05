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

package com.google.samples.apps.iosched.ui.schedule

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint.ANTI_ALIAS_FLAG
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
import androidx.core.view.isEmpty
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.State
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.Session
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber

/**
 * A [RecyclerView.ItemDecoration] which draws sticky headers for a given list of sessions.
 */
class ScheduleTimeHeadersDecoration(
    context: Context,
    sessions: List<Session>,
    zoneId: ZoneId
) : ItemDecoration() {

    private val paint: TextPaint
    private val width: Int
    private val paddingTop: Int
    private val hourMinTextSize: Int
    private val meridiemTextSize: Int
    private val hourFormatter = DateTimeFormatter.ofPattern("h")
    private val hourMinFormatter = DateTimeFormatter.ofPattern("h:m")
    private val meridiemFormatter = DateTimeFormatter.ofPattern("a")

    init {
        val attrs = context.obtainStyledAttributes(
            R.style.Widget_IOSched_TimeHeaders,
            R.styleable.TimeHeader
        )
        paint = TextPaint(ANTI_ALIAS_FLAG).apply {
            color = attrs.getColorOrThrow(R.styleable.TimeHeader_android_textColor)
            textSize = attrs.getDimensionOrThrow(R.styleable.TimeHeader_hourTextSize)
            try {
                typeface = ResourcesCompat.getFont(
                    context,
                    attrs.getResourceIdOrThrow(R.styleable.TimeHeader_android_fontFamily)
                )
            } catch (_: Exception) {
                // ignore
            }
        }
        width = attrs.getDimensionPixelSizeOrThrow(R.styleable.TimeHeader_android_width)
        paddingTop = attrs.getDimensionPixelSize(R.styleable.TimeHeader_android_paddingTop, 0)
        hourMinTextSize = attrs.getDimensionPixelSizeOrThrow(R.styleable.TimeHeader_hourMinTextSize)
        meridiemTextSize =
            attrs.getDimensionPixelSizeOrThrow(R.styleable.TimeHeader_meridiemTextSize)
        attrs.recycle()
    }

    // Get the sessions index:start time and create header layouts for each
    // TODO: let user pick between local or conference time zone (b/77606102). Show local for now.
    private val timeSlots: Map<Int, StaticLayout> =
        indexSessionHeaders(sessions, zoneId).map {
            it.first to createHeader(it.second)
        }.toMap()

    /**
     * Loop over each child and draw any corresponding headers i.e. items who's position is a key in
     * [timeSlots]. We also look back to see if there are any headers _before_ the first header we
     * found i.e. which needs to be sticky.
     */
    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: State) {
        if (timeSlots.isEmpty() || parent.isEmpty()) return

        var earliestPosition = Int.MAX_VALUE
        var previousHeaderPosition = -1
        var previousHasHeader = false
        var earliestChild: View? = null
        for (i in parent.childCount - 1 downTo 0) {
            val child = parent.getChildAt(i)
            if (child == null) {
                // This should not be null, but observed null at times.
                // Guard against it to avoid crash and log the state.
                Timber.w(
                    """View is null. Index: $i, childCount: ${parent.childCount},
                        |RecyclerView.State: $state""".trimMargin()
                )
                continue
            }

            if (child.y > parent.height || (child.y + child.height) < 0) {
                // Can't see this child
                continue
            }

            val position = parent.getChildAdapterPosition(child)
            if (position < 0) {
                continue
            }
            if (position < earliestPosition) {
                earliestPosition = position
                earliestChild = child
            }

            val header = timeSlots[position]
            if (header != null) {
                drawHeader(c, child, header, child.alpha, previousHasHeader)
                previousHeaderPosition = position
                previousHasHeader = true
            } else {
                previousHasHeader = false
            }
        }

        if (earliestChild != null && earliestPosition != previousHeaderPosition) {
            // This child needs a sicky header
            val stickyHeader = findHeaderBeforePosition(earliestPosition) ?: return
            previousHasHeader = previousHeaderPosition - earliestPosition == 1
            drawHeader(c, earliestChild, stickyHeader, 1f, previousHasHeader)
        }
    }

    private fun findHeaderBeforePosition(position: Int): StaticLayout? {
        for (headerPos in timeSlots.keys.reversed()) {
            if (headerPos < position) {
                return timeSlots[headerPos]
            }
        }
        return null
    }

    private fun drawHeader(
        canvas: Canvas,
        child: View,
        header: StaticLayout,
        headerAlpha: Float,
        previousHasHeader: Boolean
    ) {
        val childTop = child.y.toInt()
        val childBottom = childTop + child.height
        var top = childTop.coerceAtLeast(paddingTop)
        if (previousHasHeader) {
            top = top.coerceAtMost(childBottom - header.height - paddingTop)
        }
        paint.alpha = (headerAlpha * 255).toInt()
        canvas.withTranslation(y = top.toFloat()) {
            header.draw(canvas)
        }
    }

    /**
     * Create a header layout for the given [startTime].
     */
    private fun createHeader(startTime: ZonedDateTime): StaticLayout {
        val text = if (startTime.minute == 0) {
            SpannableStringBuilder(hourFormatter.format(startTime))
        } else {
            // Use a smaller text size and different pattern if event does not start on the hour
            SpannableStringBuilder().apply {
                inSpans(AbsoluteSizeSpan(hourMinTextSize)) {
                    append(hourMinFormatter.format(startTime))
                }
            }
        }.apply {
            append(System.lineSeparator())
            inSpans(AbsoluteSizeSpan(meridiemTextSize), StyleSpan(BOLD)) {
                append(meridiemFormatter.format(startTime).toUpperCase())
            }
        }
        return StaticLayout(text, paint, width, ALIGN_CENTER, 1f, 0f, false)
    }
}
