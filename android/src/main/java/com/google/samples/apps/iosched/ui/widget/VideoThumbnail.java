/*
 * Copyright (c) 2016 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.samples.apps.iosched.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.ImageView;

import com.google.samples.apps.iosched.R;

/**
 * An extension to {@link ImageView} that draws a play button over the main image and exposes
 * a played state for treating the thumbnail image's presentation.
 */
public class VideoThumbnail extends ImageView {

    // Custom state, used to change the tint attribute
    private static final int[] STATE_PLAYED = { R.attr.state_played };

    private Drawable mPlayIcon;
    private boolean mIsPlayed = false;

    public VideoThumbnail(final Context context, final AttributeSet attrs) {
        super(context, attrs, R.attr.videoThumbnailStyle);

        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.VideoThumbnail, R.attr.videoThumbnailStyle, 0);
        mPlayIcon = a.getDrawable(R.styleable.VideoThumbnail_playIcon);
        a.recycle();
        setScaleType(ScaleType.CENTER_CROP);
    }

    public boolean isPlayed() {
        return mIsPlayed;
    }

    public void setPlayed(final boolean played) {
        if (mIsPlayed != played) {
            mIsPlayed = played;
            refreshDrawableState();
            invalidate();
        }
    }

    @Override
    protected void onMeasure(final int widthMeasureSpec, final int heightMeasureSpec) {
        // Maintain a 16:9 aspect ratio, based on the width
        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = width * 9 / 16;
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
        // Place the play icon in the center of this view
        if (mPlayIcon != null) {
            final int playLeft = (getMeasuredWidth() - mPlayIcon.getIntrinsicWidth()) / 2;
            final int playTop = (getMeasuredHeight() - mPlayIcon.getIntrinsicHeight()) / 2;
            mPlayIcon.setBounds(playLeft, playTop,
                    playLeft + mPlayIcon.getIntrinsicWidth(),
                    playTop + mPlayIcon.getIntrinsicHeight());
        }
    }

    @Override
    public int[] onCreateDrawableState(int extraSpace) {
        final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
        if (mIsPlayed) {
            mergeDrawableStates(drawableState, STATE_PLAYED);
        }
        return drawableState;
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        super.onDraw(canvas);
        if (mPlayIcon != null) {
            mPlayIcon.draw(canvas);
        }
    }
}
