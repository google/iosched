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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;

import com.google.samples.apps.iosched.R;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class AddToScheduleFABFrameLayout extends CheckableFrameLayout {
    private View mRevealView;
    private float mHotSpotX, mHotSpotY;
    private int mRevealViewOffColor;

    public AddToScheduleFABFrameLayout(Context context) {
        this(context, null, 0, 0);
    }

    public AddToScheduleFABFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public AddToScheduleFABFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public AddToScheduleFABFrameLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr);

        mRevealView = new View(context);
        mRevealView.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        addView(mRevealView, 0);
        mRevealViewOffColor = getResources().getColor(R.color.theme_accent_1);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            mHotSpotX = event.getX();
            mHotSpotY = event.getY();
        }
        return super.onTouchEvent(event);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        ViewOutlineProvider viewOutlineProvider = new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                outline.setOval(0, 0, view.getWidth(), view.getHeight());
            }
        };
        setOutlineProvider(viewOutlineProvider);
        setClipToOutline(true);
    }

    @Override
    public void setChecked(boolean checked, boolean allowAnimate) {
        super.setChecked(checked, allowAnimate);
        if (allowAnimate) {
            // TODO: switch to mHotSpotX/mHotSpotY/getWidth if/when nested reveals can be clipped
            // by parents. was possible in LPV79 but no longer as of this writing.
            Animator animator = ViewAnimationUtils.createCircularReveal(
                    mRevealView,
                    (int) getWidth() / 2, (int) getHeight() / 2, 0, getWidth() / 2);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    setChecked(mChecked, false);
                }
            });
            animator.start();
            mRevealView.setVisibility(View.VISIBLE);
            mRevealView.setBackgroundColor(mChecked ? Color.WHITE : mRevealViewOffColor);
        } else {
            mRevealView.setVisibility(View.GONE);
            RippleDrawable newBackground = (RippleDrawable) getResources().getDrawable(mChecked
                    ? R.drawable.add_schedule_fab_ripple_background_on
                    : R.drawable.add_schedule_fab_ripple_background_off);
            setBackground(newBackground);
        }
    }
}
