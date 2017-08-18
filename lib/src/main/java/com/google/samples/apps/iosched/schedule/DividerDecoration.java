/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.samples.apps.iosched.schedule;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ItemDecoration;
import android.support.v7.widget.RecyclerView.ViewHolder;

import static android.graphics.Paint.ANTI_ALIAS_FLAG;

/**
 * An {@link ItemDecoration} which only draws <strong>between</strong> {@link ViewHolder}s that
 * require it as marked by implementing {@link Divided}. i.e. draws a divider line at the top
 * of an item if both it and the previous item are {@link Divided}. Currently requires a
 * {@link LinearLayoutManager}.
 */
public class DividerDecoration extends ItemDecoration {

    private static final int[] ATTRS =
            new int[] { android.R.attr.dividerHeight, android.R.attr.divider };
    private final Paint mDividerPaint = new Paint(ANTI_ALIAS_FLAG);
    private final float mHalfHeight;

    public DividerDecoration(float dividerHeight, @ColorInt int dividerColor) {
        mDividerPaint.setStrokeWidth(dividerHeight);
        mHalfHeight = dividerHeight / 2f;
        mDividerPaint.setColor(dividerColor);
    }

    public DividerDecoration(@NonNull Context context) {
        final TypedArray a = context.obtainStyledAttributes(ATTRS);
        float dividerHeight = a.getDimension(0, 0f);
        mDividerPaint.setStrokeWidth(dividerHeight);
        mHalfHeight = dividerHeight / 2f;
        mDividerPaint.setColor(a.getColor(1, 0xff00ff));
        a.recycle();
    }

    @Override
    public void onDraw(Canvas canvas, RecyclerView rv, RecyclerView.State state) {
        int count = rv.getChildCount();
        if (count < 2) return;
        float[] points = new float[count * 4];
        boolean previousItemNeedsDivider = false;

        LinearLayoutManager layoutManager = (LinearLayoutManager) rv.getLayoutManager();
        int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
        for (int i = 0; i < count; i++) {
            ViewHolder holder = rv.findViewHolderForAdapterPosition(firstVisibleItemPosition + i);
            boolean needsDivider = holder instanceof Divided;
            if (previousItemNeedsDivider && needsDivider) {
                points[4 * i] = layoutManager.getDecoratedLeft(holder.itemView);
                float top = layoutManager.getDecoratedTop(holder.itemView)
                        + holder.itemView.getTranslationY() + mHalfHeight;
                points[(4 * i) + 1] = top;
                points[(4 * i) + 2] = layoutManager.getDecoratedRight(holder.itemView);
                points[(4 * i) + 3] = top;
            }
            previousItemNeedsDivider = needsDivider;
        }
        canvas.drawLines(points, mDividerPaint);
    }

    /**
     * Empty marker interface, used to denote a {@link ViewHolder} as requiring a divider.
     */
    interface Divided { }
}
