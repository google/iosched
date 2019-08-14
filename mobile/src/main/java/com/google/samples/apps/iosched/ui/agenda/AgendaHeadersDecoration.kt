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
import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout.Alignment.ALIGN_CENTER
import android.text.StaticLayout
import android.text.TextPaint
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.getColorOrThrow
import androidx.core.content.res.getDimensionOrThrow
import androidx.core.content.res.getDimensionPixelSizeOrThrow
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.graphics.withTranslation
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.State
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.model.Block
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.util.newStaticLayout
import org.threeten.bp.ZonedDateTime
import kotlin.math.ceil

/**
 * A [RecyclerView.ItemDecoration] which draws sticky headers marking the days in a given list of
 * [Block]s. It also inserts gaps between days.
 */
class AgendaHeadersDecoration(
    context: Context,
    blocks: List<Block>,
    inConferenceTimeZone: Boolean
) : ItemDecoration() {

    private val paint: TextPaint
    private val textWidth: Int
    private val decorHeight: Int
    private val verticalBias: Float

    init {
        val attrs = context.obtainStyledAttributes(
            R.style.Widget_IOSched_DateHeaders,
            R.styleable.DateHeader
        )
        paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG).apply {
            color = attrs.getColorOrThrow(R.styleable.DateHeader_android_textColor)
            textSize = attrs.getDimensionOrThrow(R.styleable.DateHeader_android_textSize)
            try {
                typeface = ResourcesCompat.getFont(
                    context,
                    attrs.getResourceIdOrThrow(R.styleable.DateHeader_android_fontFamily)
                )
            } catch (_: Exception) {
                // ignore
            }
        }

        textWidth = attrs.getDimensionPixelSizeOrThrow(R.styleable.DateHeader_android_width)
        val height = attrs.getDimensionPixelSizeOrThrow(R.styleable.DateHeader_android_height)
        val minHeight = ceil(paint.textSize).toInt()
        decorHeight = Math.max(height, minHeight)

        verticalBias = attrs.getFloat(R.styleable.DateHeader_verticalBias, 0.5f).coerceIn(0f, 1f)

        attrs.recycle()
    }

    // Get the block index:day and create header layouts for each
    private val daySlots: Map<Int, StaticLayout> =
        indexAgendaHeaders(blocks).map {
            it.first to createHeader(context, it.second, inConferenceTimeZone)
        }.toMap()

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: State) {
        val position = parent.getChildAdapterPosition(view)
        outRect.top = if (daySlots.containsKey(position)) decorHeight else 0
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: State) {
        val layoutManager = parent.layoutManager ?: return
        val centerX = parent.width / 2f

        parent.forEach { child ->
            if (child.top < parent.height && child.bottom > 0) {
                // Child is visible
                val layout = daySlots[parent.getChildAdapterPosition(child)]
                if (layout != null) {
                    val dx = centerX - (layout.width / 2)
                    val dy = layoutManager.getDecoratedTop(child) +
                        child.translationY +
                        // offset vertically within the space according to the bias
                        (decorHeight - layout.height) * verticalBias
                    canvas.withTranslation(x = dx, y = dy) {
                        layout.draw(this)
                    }
                }
            }
        }
    }

    /**
     * Create a header layout for the given [time]
     */
    private fun createHeader(
        context: Context,
        time: ZonedDateTime,
        inConferenceTimeZone: Boolean
    ): StaticLayout {
        val labelRes = TimeUtils.getLabelResForTime(time, inConferenceTimeZone)
        val text = context.getText(labelRes)
        return newStaticLayout(text, paint, textWidth, ALIGN_CENTER, 1f, 0f, false)
    }
}
