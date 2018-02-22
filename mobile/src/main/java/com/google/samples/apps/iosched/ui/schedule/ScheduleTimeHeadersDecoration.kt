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
import android.graphics.Typeface
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.RecyclerView
import android.text.Layout.Alignment.ALIGN_CENTER
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import androidx.content.res.getColorOrThrow
import androidx.content.res.getDimensionOrThrow
import androidx.content.res.getDimensionPixelSizeOrThrow
import androidx.graphics.withTranslation
import androidx.text.inSpans
import androidx.view.get
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.model.Session
import org.threeten.bp.format.DateTimeFormatter

/**
 * A [RecyclerView.ItemDecoration] which draws sticky headers for a given list of sessions.
 */
class ScheduleTimeHeadersDecoration(
    context: Context,
    sessions: List<Session>
) : RecyclerView.ItemDecoration() {

    private val paint: TextPaint
    private val width: Int
    private val paddingTop: Int
    private val meridiemTextSize: Int
    private val hourFormatter = DateTimeFormatter.ofPattern("h")
    private val meridiemFormatter = DateTimeFormatter.ofPattern("a")

    init {
        val attrs = context.obtainStyledAttributes(R.style.Widget_IOSched_TimeHeaders,
            R.styleable.TimeHeader) // TODO replace with use block from ktx when released
        paint = TextPaint(ANTI_ALIAS_FLAG).apply {
            color = attrs.getColorOrThrow(R.styleable.TimeHeader_android_textColor)
            textSize = attrs.getDimensionOrThrow(R.styleable.TimeHeader_hourTextSize)
            try {
                typeface = ResourcesCompat.getFont(context,
                        attrs.getResourceId(R.styleable.TimeHeader_android_fontFamily, 0))
            } catch (e:Exception) {
                //TODO: fix fonts for AIA on O+
            }
        }
        width = attrs.getDimensionPixelSizeOrThrow(R.styleable.TimeHeader_android_width)
        paddingTop = attrs.getDimensionPixelSizeOrThrow(R.styleable.TimeHeader_android_paddingTop)
        meridiemTextSize =
            attrs.getDimensionPixelSizeOrThrow(R.styleable.TimeHeader_meridiemTextSize)
        attrs.recycle()
    }

    // Get the sessions index:start time and create header layouts for each
    private val timeSlots: Map<Int, StaticLayout> =
        indexSessionHeaders(sessions).map {
            it.first to it.second.let {
                val ssb = SpannableStringBuilder(hourFormatter.format(it)).apply {
                    append('\n')
                    inSpans(AbsoluteSizeSpan(meridiemTextSize), StyleSpan(Typeface.BOLD)) {
                        append(meridiemFormatter.format(it).toUpperCase())
                    }
                }
                StaticLayout(ssb, paint, width, ALIGN_CENTER, 1f, 0f, false)
            }
        }.toMap()

    /**
     * Loop over each child and draw any corresponding headers i.e. items who's position is a key in
     * [timeSlots]. We also look back to see if there are any headers _before_ the first header we
     * found i.e. which needs to be sticky.
     */
    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State?) {
        if (timeSlots.isEmpty()) return

        var earliestFoundHeaderPos = -1
        var prevHeaderTop = Int.MAX_VALUE

        // Loop over each attached view looking for header items.
        // Loop backwards as a lower header can push another higher one upward.
        for (i in parent.childCount - 1 downTo 0) {
            val view = parent[i]
            val viewTop = view.top + view.translationY.toInt()
            if (view.bottom > 0 && viewTop < parent.height) {
                val position = parent.getChildAdapterPosition(view)
                timeSlots[position]?.let {
                val top = (viewTop + paddingTop)
                    .coerceAtLeast(paddingTop)
                    .coerceAtMost(prevHeaderTop - it.height)
                    c.withTranslation(y = top.toFloat()) {
                        it.draw(c)
                    }
                    earliestFoundHeaderPos = position
                    prevHeaderTop = viewTop
                }
            }
        }

        // If no headers found, ensure header of the first shown item is drawn.
        if (earliestFoundHeaderPos < 0) {
            earliestFoundHeaderPos = parent.getChildAdapterPosition(parent[0]) + 1
        }

        // Look back over headers to see if a prior item should be drawn sticky.
        for (headerPos in timeSlots.keys.reversed()) {
            if (headerPos < earliestFoundHeaderPos) {
                timeSlots[headerPos]?.let {
                    val top = (prevHeaderTop - it.height).coerceAtMost(paddingTop)
                    c.withTranslation(y = top.toFloat()) {
                        it.draw(c)
                    }
                }
                break
            }
        }
    }
}
