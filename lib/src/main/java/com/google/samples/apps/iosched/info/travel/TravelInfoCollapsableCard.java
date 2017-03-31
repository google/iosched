package com.google.samples.apps.iosched.info.travel;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.support.annotation.Nullable;
import android.support.transition.TransitionManager;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.samples.apps.iosched.lib.R;

public class TravelInfoCollapsableCard extends CardView {

    private boolean mExpanded = false;
    private TextView mTravelTitle;
    private TextView mTravelDescription;
    private ImageView mExpandIcon;

    public TravelInfoCollapsableCard(Context context) {
        this(context, null);
    }

    public TravelInfoCollapsableCard(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TravelInfoCollapsableCard(Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.TravelInfo, 0, 0);
        String travelTitle = arr.getString(R.styleable.TravelInfo_travelTitle);
        String travelDescription = arr.getString(R.styleable.TravelInfo_travelDescription);
        arr.recycle();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.info_travel_content_card, this, true);
        mTravelTitle = (TextView) ((RelativeLayout) getChildAt(0)).getChildAt(0);
        mTravelDescription = (TextView) ((RelativeLayout) getChildAt(0)).getChildAt(2);
        mExpandIcon = (ImageView) ((RelativeLayout) getChildAt(0)).getChildAt(1);
        mTravelTitle.setText(travelTitle);
        mTravelDescription.setText(travelDescription);
        updateFromExpandOrCollapse();
        mExpandIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mExpanded = !mExpanded;
                updateFromExpandOrCollapse();
            }
        });
    }

    public void updateFromExpandOrCollapse() {
        if (mExpanded) {
            int matchParentMeasureSpec = View.MeasureSpec.makeMeasureSpec
                    (((View) mTravelDescription.getParent()).getWidth(), View.MeasureSpec.EXACTLY);
            int wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec
                    (0, View.MeasureSpec.UNSPECIFIED);
            mTravelDescription.measure(matchParentMeasureSpec, wrapContentMeasureSpec);
            expand(mTravelDescription, 500, mTravelDescription.getMeasuredHeight());
            mTravelDescription.getMeasuredHeight();
            mExpandIcon.animate().rotation(180f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mExpandIcon.setColorFilter(
                                    new LightingColorFilter(Color.BLUE, Color.BLUE));
                            mTravelTitle.setTextColor(Color.BLUE);
                        }
                    }).start();
        } else {
            collapse(mTravelDescription, 500, 0);
            mExpandIcon.animate().rotation(0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mExpandIcon.setColorFilter(
                                    new LightingColorFilter(Color.BLACK, Color.BLACK));
                            mTravelTitle.setTextColor(Color.BLACK);
                        }
                    }).start();
        }

    }

    private void expand(final View v, int duration, int targetHeight) {
        int prevHeight  = v.getHeight();
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

    private void collapse(final View v, int duration, int targetHeight) {
        int prevHeight  = v.getHeight();
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
}
