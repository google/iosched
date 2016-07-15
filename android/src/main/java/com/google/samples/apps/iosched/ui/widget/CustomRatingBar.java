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
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.content.res.ResourcesCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.util.AccessibilityUtils;

/**
 * Replacement for a {@link android.widget.RatingBar} widget for providing ratings for sessions.
 * Provides ease of use for accessibility for use with Talkback and other assistive technologies.
 * <p/>
 * TODO: add explanation of attributes.
 */
public class CustomRatingBar extends LinearLayout implements View.OnClickListener {

    /**
     * The default maximum rating value if no specific value is specified.
     */
    private static final int DEFAULT_MAX_RATING = 5;

    /**
     * The drawable used if no unfilled drawable resource is specified.
     */
    private static final int DEFAULT_UNFILLED_DRAWABLE_ID = R.drawable.ratingbar_star_unfilled;

    /**
     * The drawable used if no filled drawable resource is specified.
     */
    private static final int DEFAULT_FILLED_DRAWABLE_ID = R.drawable.ratingbar_star_filled;

    /**
     * The maximum permitted rating.
     */
    private int mMaxRating;

    /**
     * The {@link Drawable} used to show a filled value. A rating of M out of N stars is
     * represented by M filled drawables and N - M unfilled drawables.
     */
    private Drawable mFilledDrawable;

    /**
     * The {@link Drawable} used to show an unfilled value. A rating of M out of N stars is
     * represented by M filled drawables and N - M unfilled drawables.
     */
    private Drawable mUnfilledDrawable;

    /**
     * The rating specified by the user.
     */
    private int mRating = 0;

    public CustomRatingBar(Context context) {
        this(context, null, 0);
    }

    public CustomRatingBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomRatingBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray typedArray = context.getTheme().obtainStyledAttributes(
                attrs,
                R.styleable.CustomRatingBar,
                0, 0);

        try {
            mMaxRating = typedArray.getInt(R.styleable.CustomRatingBar_maxRating,
                    DEFAULT_MAX_RATING);
            mFilledDrawable = typedArray.getDrawable(R.styleable.CustomRatingBar_filledDrawable);
            if (mFilledDrawable == null) {
                mFilledDrawable = ResourcesCompat.getDrawable(getResources(),
                        DEFAULT_FILLED_DRAWABLE_ID, null);
            }
            mUnfilledDrawable = typedArray.getDrawable(R.styleable.CustomRatingBar_unfilledDrawable);
            if (mUnfilledDrawable == null) {
                mUnfilledDrawable = ResourcesCompat.getDrawable(getResources(),
                        DEFAULT_UNFILLED_DRAWABLE_ID, null);
            }
        } finally {
            typedArray.recycle();
        }
        setSaveEnabled(true);
    }

    public int getRating() {
        return mRating;
    }

    @Override
    public void onClick(final View v) {
        mRating = (int) v.getTag();
        drawRatingViews();
        int eventType = Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN ?
                AccessibilityEvent.TYPE_ANNOUNCEMENT : AccessibilityEvent.TYPE_VIEW_FOCUSED;
        sendAccessibilityEvent(eventType);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        drawRatingViews();
    }

    @Override
    public void sendAccessibilityEvent(final int eventType) {
        if (!AccessibilityUtils.isAccessibilityEnabled(getContext())) {
            return;
        }
        super.sendAccessibilityEvent(eventType);

        AccessibilityEvent event = AccessibilityEvent.obtain(eventType);
        // Get the Talkback text.
        event.getText().add(getContext().getResources().getString(
                R.string.feedback_rating_confirmation, mRating, mMaxRating));
        event.setEnabled(true);
        AccessibilityManager accessibilityManager = (AccessibilityManager) getContext()
                .getSystemService(Context.ACCESSIBILITY_SERVICE);
        accessibilityManager.sendAccessibilityEvent(event);
    }

    /**
     * Creates or updates the views used for creating a rating.
     */
    private void drawRatingViews() {
        if (this.getChildCount() == 0) {
            createRatingViews();
        } else {
            updateRatingViews();
        }
    }

    /**
     * Creates ({@link ImageView}s) used to submit a rating using unfilled drawables and adds them to
     * the layout.
     */
    private void createRatingViews() {
        for (int i = 0; i < mMaxRating; i++) {
            ImageView imageView = new ImageView(getContext());
            imageView.setLayoutParams(
                    new android.view.ViewGroup.LayoutParams(LayoutParams.WRAP_CONTENT,
                            LayoutParams.WRAP_CONTENT));
            int tagValue = i + 1;
            imageView.setTag(tagValue);
            imageView.setContentDescription(getContext().getString(
                    R.string.feedback_rating_value, tagValue));
            imageView.setImageDrawable(mUnfilledDrawable);
            imageView.setOnClickListener(this);
            addView(imageView);
        }
    }

    /**
     * Updates ({@link ImageView}s) used to submit a rating, using filled drawables to denote the
     * rating created by the user.
     */
    private void updateRatingViews() {
        for (int i = 0; i < mMaxRating; i++) {
            ImageView view = (ImageView) this.getChildAt(i);
            view.setImageDrawable(i + 1 <= mRating ? mFilledDrawable : mUnfilledDrawable);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.rating = mRating;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(final Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());

        mRating = ss.rating;
        drawRatingViews();
    }

    /**
     * Helper class to help retain state during orientation change.
     */
    private static class SavedState extends BaseSavedState {
        int rating;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            rating = in.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(rating);
        }

        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}