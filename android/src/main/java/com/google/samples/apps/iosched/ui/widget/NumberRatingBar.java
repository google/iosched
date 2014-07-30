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
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

public class NumberRatingBar extends SeekBar {
    private OnSeekBarChangeListener mUserSeekBarChangeListener;

    public NumberRatingBar(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        super.setOnSeekBarChangeListener(mSeekBarChangeListener);
        updateSecondaryProgress();
        setThumb(null);
    }

    public NumberRatingBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public NumberRatingBar(Context context) {
        this(context, null, 0);
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        mUserSeekBarChangeListener = listener;
    }

    private OnSeekBarChangeListener mSeekBarChangeListener = new OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            updateSecondaryProgress();
            if (mUserSeekBarChangeListener != null) {
                mUserSeekBarChangeListener.onProgressChanged(seekBar, progress, fromUser);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            if (mUserSeekBarChangeListener != null) {
                mUserSeekBarChangeListener.onStartTrackingTouch(seekBar);
            }
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            if (mUserSeekBarChangeListener != null) {
                mUserSeekBarChangeListener.onStopTrackingTouch(seekBar);
            }
        }
    };

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // A delectable hack.
        event.offsetLocation(getWidth() / 5, 0);
        return super.onTouchEvent(event);
    }

    private void updateSecondaryProgress() {
        // Another delectable hack.
        setSecondaryProgress(getProgress() - 1);
    }
}
