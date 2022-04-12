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

package com.google.samples.apps.iosched.ui.schedule

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.text.Layout.Alignment.ALIGN_CENTER
import android.text.StaticLayout
import android.text.TextPaint
import android.util.SparseArray
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.res.getDimensionPixelSizeOrThrow
import androidx.core.content.res.getResourceIdOrThrow
import androidx.core.graphics.withTranslation
import androidx.core.util.containsKey
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import androidx.recyclerview.widget.RecyclerView.State
import com.google.samples.apps.iosched.R
import com.google.samples.apps.iosched.shared.domain.sessions.ConferenceDayIndexer
import com.google.samples.apps.iosched.shared.util.TimeUtils
import com.google.samples.apps.iosched.util.newStaticLayout
import kotlin.math.ceil
import org.threeten.bp.ZoneId

class DaySeparatorItemDecoration(
    context: Context,
    indexer: ConferenceDayIndexer,
    zoneId: ZoneId
) : ItemDecoration() {

    private val labels: SparseArray<StaticLayout>

    private val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG)

    private val textWidth: Int

    private val decorHeight: Int

    private val verticalBias: Float

    init {
        val attrs = context.obtainStyledAttributes(
            R.style.Widget_IOSched_DaySeparatorDecoration,
            R.styleable.DaySeparatorDecoration
        )

        val textSize =
            attrs.getDimension(R.styleable.DaySeparatorDecoration_android_textSize, paint.textSize)
        paint.textSize = textSize
        try {
            paint.typeface = ResourcesCompat.getFont(
                context,
                attrs.getResourceIdOrThrow(R.styleable.DaySeparatorDecoration_android_fontFamily)
            )
        } catch (ignored: Exception) {
        }

        val textColor =
            attrs.getColor(R.styleable.DaySeparatorDecoration_android_textColor, Color.BLACK)
        paint.color = textColor

        textWidth =
            attrs.getDimensionPixelSizeOrThrow(R.styleable.DaySeparatorDecoration_android_width)
        val height =
            attrs.getDimensionPixelSizeOrThrow(R.styleable.DaySeparatorDecoration_android_height)
        val minHeight = ceil(textSize).toInt()
        decorHeight = Math.max(height, minHeight)

        verticalBias = attrs.getFloat(R.styleable.DaySeparatorDecoration_verticalBias, 0.5f)
            .coerceIn(0f, 1f)

        attrs.recycle()

        labels = buildLabels(context, indexer, zoneId)
    }

    private fun buildLabels(
        context: Context,
        indexer: ConferenceDayIndexer,
        zoneId: ZoneId
    ): SparseArray<StaticLayout> {
        val isInConferenceZone = TimeUtils.isConferenceTimeZone(zoneId)
        val sparseArray = SparseArray<StaticLayout>()
        for (day in indexer.days) {
            val position = indexer.positionForDay(day)
            val text = context.getString(TimeUtils.getLabelResForDay(day, isInConferenceZone))
            val label = newStaticLayout(text, paint, textWidth, ALIGN_CENTER, 1f, 0f, false)
            sparseArray.put(position, label)
        }
        return sparseArray
    }

    override fun getItemOffsets(outRect: Rect, child: View, parent: RecyclerView, state: State) {
        val position = parent.getChildAdapterPosition(child)
        outRect.top = if (labels.containsKey(position)) decorHeight else 0
    }

    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: State) {
        val layoutManager = parent.layoutManager ?: return
        val centerX = parent.width / 2f

        parent.forEach { child ->
            if (child.top < parent.height && child.bottom > 0) {
                // Child is visible
                val layout = labels[parent.getChildAdapterPosition(child)]
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
}
