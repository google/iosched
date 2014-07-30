/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.ui.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.widget.FrameLayout;

import com.google.samples.apps.iosched.R;

/**
 * A view that draws fancy horizontal lines to frame it's content
 */
public class HashtagView extends FrameLayout {

    private static final int LINE_COLOUR = 0x59ffffff;

    private Paint mLinesPaint;
    private float[] mLinePoints;

    public HashtagView(Context context) {
        this(context, null, 0);
    }

    public HashtagView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public HashtagView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mLinesPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mLinesPaint.setColor(LINE_COLOUR);
        mLinesPaint.setStyle(Paint.Style.STROKE);
        mLinesPaint.setStrokeWidth(context.getResources().getDimensionPixelSize(
                R.dimen.hashtag_line_height));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // calculate points for two horizontal lines spaced at 1/3 & 2/3 of the height, occupying
        // 2/3 of the width (centered).
        final int thirdHeight = getMeasuredHeight() / 3;
        final int sixthWidth = getMeasuredWidth() / 6;
        mLinePoints = new float[]{
                // line 1
                sixthWidth, thirdHeight, 5 * sixthWidth, thirdHeight,
                // line 2
                sixthWidth, 2 * thirdHeight, 5 * sixthWidth, 2 * thirdHeight};
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawLines(mLinePoints, mLinesPaint);
    }
}
