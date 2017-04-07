/*
 * Copyright (c) 2017 Google Inc.
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
package com.google.samples.apps.iosched.info;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.samples.apps.iosched.lib.R;

public class CollapsableCard extends FrameLayout {

    private boolean mExpanded = false;
    private TextView mCardTitle;
    private TextView mCardDescription;
    private ImageView mExpandIcon;

    public CollapsableCard(Context context) {
        this(context, null);
    }

    public CollapsableCard(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CollapsableCard(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.CollapsableCard, 0, 0);
        String cardTitle = arr.getString(R.styleable.CollapsableCard_cardTitle);
        String cardDescription = arr.getString(R.styleable.CollapsableCard_cardDescription);
        arr.recycle();
        View view = LayoutInflater.from(context)
                .inflate(R.layout.collapsable_card_content, this, true);
        mCardTitle = (TextView) view.findViewById(R.id.card_title);
        mCardDescription = (TextView) view.findViewById(R.id.card_description);
        mExpandIcon = (ImageView) view.findViewById(R.id.expand_icon);
        mCardTitle.setText(cardTitle);
        mCardDescription.setText(cardDescription);
        updateFromExpandOrCollapse();
        OnClickListener expandClick = new OnClickListener() {
            @Override
            public void onClick(View v) {
                mExpanded = !mExpanded;
                updateFromExpandOrCollapse();
            }
        };
        mExpandIcon.setOnClickListener(expandClick);
        mCardTitle.setOnClickListener(expandClick);
    }

    public void updateFromExpandOrCollapse() {
        if (mExpanded) {
            int matchParentMeasureSpec = View.MeasureSpec.makeMeasureSpec
                    (((View) mCardDescription.getParent()).getWidth(), View.MeasureSpec.EXACTLY);
            int wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec
                    (0, View.MeasureSpec.UNSPECIFIED);
            mCardDescription.measure(matchParentMeasureSpec, wrapContentMeasureSpec);
            //TODO find way to use TransitionManager
            expand(mCardDescription, 300L, mCardDescription.getMeasuredHeight());
            mExpandIcon.animate().rotation(180f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mExpandIcon.setActivated(true);
                            mCardTitle.setActivated(true);
                        }
                    }).start();
        } else {
            //TODO find way to use TransitionManager
            collapse(mCardDescription, 200L, 0);
            mExpandIcon.animate().rotation(0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mExpandIcon.setActivated(false);
                            mCardTitle.setActivated(false);
                        }
                    }).start();
        }
    }

    private void expand(final View v, long duration, int targetHeight) {
        int prevHeight = v.getHeight();
        ValueAnimator valueAnimator = ValueAnimator.ofInt(prevHeight, targetHeight);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                v.getLayoutParams().height = (int) animation.getAnimatedValue();
                v.requestLayout();
            }
        });
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    private void collapse(final View v, long duration, int targetHeight) {
        int prevHeight = v.getHeight();
        ValueAnimator valueAnimator = ValueAnimator.ofInt(prevHeight, targetHeight);
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                v.getLayoutParams().height = (int) animation.getAnimatedValue();
                v.requestLayout();
            }
        });
        valueAnimator.setInterpolator(new DecelerateInterpolator());
        valueAnimator.setDuration(duration);
        valueAnimator.start();
    }

    public void setCardDescription(CharSequence description) {
        mCardDescription.setText(description);
    }
}
