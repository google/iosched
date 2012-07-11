/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.iosched.ui.widget;

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.util.UIUtils;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.FontMetricsInt;
import android.graphics.Paint.Style;
import android.graphics.Typeface;
import android.text.format.Time;
import android.util.AttributeSet;
import android.view.View;

/**
 * Custom view that draws a vertical time "ruler" representing the chronological
 * progression of a single day. Usually shown along with {@link BlockView}
 * instances to give a spatial sense of time.
 */
public class TimeRulerView extends View {

    private int mHeaderWidth = 70;
    private int mHourHeight = 90;
    private boolean mHorizontalDivider = true;
    private int mLabelTextSize = 20;
    private int mLabelPaddingLeft = 8;
    private int mLabelColor = Color.BLACK;
    private int mDividerColor = Color.LTGRAY;
    private int mStartHour = 0;
    private int mEndHour = 23;

    public TimeRulerView(Context context) {
        this(context, null);
    }

    public TimeRulerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TimeRulerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.TimeRulerView,
                defStyle, 0);

        mHeaderWidth = a.getDimensionPixelSize(R.styleable.TimeRulerView_headerWidth,
                mHeaderWidth);
        mHourHeight = a
                .getDimensionPixelSize(R.styleable.TimeRulerView_hourHeight, mHourHeight);
        mHorizontalDivider = a.getBoolean(R.styleable.TimeRulerView_horizontalDivider,
                mHorizontalDivider);
        mLabelTextSize = a.getDimensionPixelSize(R.styleable.TimeRulerView_labelTextSize,
                mLabelTextSize);
        mLabelPaddingLeft = a.getDimensionPixelSize(R.styleable.TimeRulerView_labelPaddingLeft,
                mLabelPaddingLeft);
        mLabelColor = a.getColor(R.styleable.TimeRulerView_labelColor, mLabelColor);
        mDividerColor = a.getColor(R.styleable.TimeRulerView_dividerColor, mDividerColor);
        mStartHour = a.getInt(R.styleable.TimeRulerView_startHour, mStartHour);
        mEndHour = a.getInt(R.styleable.TimeRulerView_endHour, mEndHour);

        a.recycle();
    }

    /**
     * Return the vertical offset (in pixels) for a requested time (in
     * milliseconds since epoch).
     */
    public int getTimeVerticalOffset(long timeMillis) {
        Time time = new Time(UIUtils.CONFERENCE_TIME_ZONE.getID());
        time.set(timeMillis);

        final int minutes = ((time.hour - mStartHour) * 60) + time.minute;
        return (minutes * mHourHeight) / 60;
    }

    @Override
    protected synchronized void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int hours = mEndHour - mStartHour;

        int width = mHeaderWidth;
        int height = mHourHeight * hours;

        setMeasuredDimension(resolveSize(width, widthMeasureSpec),
                resolveSize(height, heightMeasureSpec));
    }

    private Paint mDividerPaint = new Paint();
    private Paint mLabelPaint = new Paint();

    @Override
    protected synchronized void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        final int hourHeight = mHourHeight;

        final Paint dividerPaint = mDividerPaint;
        dividerPaint.setColor(mDividerColor);
        dividerPaint.setStyle(Style.FILL);

        final Paint labelPaint = mLabelPaint;
        labelPaint.setColor(mLabelColor);
        labelPaint.setTextSize(mLabelTextSize);
        labelPaint.setTypeface(Typeface.DEFAULT_BOLD);
        labelPaint.setAntiAlias(true);

        final FontMetricsInt metrics = labelPaint.getFontMetricsInt();
        final int labelHeight = Math.abs(metrics.ascent);
        final int labelOffset = labelHeight + mLabelPaddingLeft;

        final int right = getRight();

        // Walk left side of canvas drawing timestamps
        final int hours = mEndHour - mStartHour;
        for (int i = 0; i < hours; i++) {
            final int dividerY = hourHeight * i;
            final int nextDividerY = hourHeight * (i + 1);
            canvas.drawLine(0, dividerY, right, dividerY, dividerPaint);

            // draw text title for timestamp
            canvas.drawRect(0, dividerY, mHeaderWidth, nextDividerY, dividerPaint);

            // TODO: localize these labels better, including handling
            // 24-hour mode when set in framework.
            final int hour = mStartHour + i;
            String label;
            if (hour == 0) {
                label = "12am";
            } else if (hour <= 11) {
                label = hour + "am";
            } else if (hour == 12) {
                label = "12pm";
            } else {
                label = (hour - 12) + "pm";
            }

            final float labelWidth = labelPaint.measureText(label);

            canvas.drawText(label, 0, label.length(), mHeaderWidth - labelWidth
                    - mLabelPaddingLeft, dividerY + labelOffset, labelPaint);
        }
    }

    public int getHeaderWidth() {
        return mHeaderWidth;
    }
}
