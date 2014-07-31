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

import com.google.samples.apps.iosched.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * A simple {@link TextView} subclass that uses {@link TextUtils#ellipsize(CharSequence,
 * android.text.TextPaint, float, android.text.TextUtils.TruncateAt, boolean,
 * android.text.TextUtils.EllipsizeCallback)} to truncate the displayed text. This is used in
 * {@link com.google.samples.apps.iosched.ui.PlusStreamRowViewBinder} when displaying G+ post text
 * which is converted from HTML to a {@link android.text.SpannableString} and sometimes causes
 * issues for the built-in TextView ellipsize function.
 */
public class EllipsizedTextView extends TextView {
    private static final int MAX_ELLIPSIZE_LINES = 100;

    private int mMaxLines;

    public EllipsizedTextView(Context context) {
        this(context, null, 0);
    }

    public EllipsizedTextView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EllipsizedTextView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        // Attribute initialization
        final TypedArray a = context.obtainStyledAttributes(attrs, new int[]{
                android.R.attr.maxLines
        }, defStyle, 0);

        mMaxLines = a.getInteger(0, 1);
        a.recycle();
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        CharSequence newText = getWidth() == 0 || mMaxLines > MAX_ELLIPSIZE_LINES ? text :
                TextUtils.ellipsize(text, getPaint(), getWidth() * mMaxLines,
                        TextUtils.TruncateAt.END, false, null);
        super.setText(newText, type);
    }

    @Override
    protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        super.onSizeChanged(width, height, oldWidth, oldHeight);
        if (width > 0 && oldWidth != width) {
            setText(getText());
        }
    }

    @Override
    public void setMaxLines(int maxlines) {
        super.setMaxLines(maxlines);
        mMaxLines = maxlines;
    }
}
