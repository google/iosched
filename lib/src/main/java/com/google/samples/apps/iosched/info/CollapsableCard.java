package com.google.samples.apps.iosched.info;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.support.annotation.Nullable;
import android.support.v7.widget.CardView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.samples.apps.iosched.lib.R;

public class CollapsableCard extends CardView {

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

    public CollapsableCard(Context context, @Nullable AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray arr = context.obtainStyledAttributes(attrs, R.styleable.CollapsableCard, 0, 0);
        String cardTitle = arr.getString(R.styleable.CollapsableCard_cardTitle);
        String cardDescription = arr.getString(R.styleable.CollapsableCard_cardDescription);
        arr.recycle();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.collapsable_card_content, this, true);
        mCardTitle = (TextView) view.findViewById(R.id.card_title);
        mCardDescription = (TextView) view.findViewById(R.id.card_description);
        mExpandIcon = (ImageView) view.findViewById(R.id.expand_icon);
        mCardTitle.setText(cardTitle);
        mCardDescription.setText(cardDescription);
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
                    (((View) mCardDescription.getParent()).getWidth(), View.MeasureSpec.EXACTLY);
            int wrapContentMeasureSpec = View.MeasureSpec.makeMeasureSpec
                    (0, View.MeasureSpec.UNSPECIFIED);
            mCardDescription.measure(matchParentMeasureSpec, wrapContentMeasureSpec);
            //TODO find way to use TransitionManager
            expand(mCardDescription, 500, mCardDescription.getMeasuredHeight());
            mExpandIcon.animate().rotation(180f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mExpandIcon.setColorFilter(
                                    new LightingColorFilter(Color.BLUE, Color.BLUE));
                            mCardTitle.setTextColor(Color.BLUE);
                        }
                    }).start();
        } else {
            //TODO find way to use TransitionManager
            collapse(mCardDescription, 500, 0);
            mExpandIcon.animate().rotation(0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            mExpandIcon.setColorFilter(
                                    new LightingColorFilter(Color.BLACK, Color.BLACK));
                            mCardTitle.setTextColor(Color.BLACK);
                        }
                    }).start();
        }

    }

    private void expand(final View v, int duration, int targetHeight) {
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

    private void collapse(final View v, int duration, int targetHeight) {
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
}
