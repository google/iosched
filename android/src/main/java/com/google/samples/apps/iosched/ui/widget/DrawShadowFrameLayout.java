
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

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Property;
import android.widget.Checkable;
import android.widget.FrameLayout;

import com.google.samples.apps.iosched.R;

public class DrawShadowFrameLayout extends FrameLayout {
    private Drawable mShadowDrawable;
    private NinePatchDrawable mShadowNinePatchDrawable;
    private int mShadowTopOffset;
    private boolean mShadowVisible;
    private int mWidth, mHeight;
    private ObjectAnimator mAnimator;
    private float mAlpha = 1f;

    public DrawShadowFrameLayout(Context context) {
        this(context, null, 0);
    }

    public DrawShadowFrameLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawShadowFrameLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        final TypedArray a = context.obtainStyledAttributes(attrs,
                R.styleable.DrawShadowFrameLayout, 0, 0);

        mShadowDrawable = a.getDrawable(R.styleable.DrawShadowFrameLayout_shadowDrawable);
        if (mShadowDrawable != null) {
            mShadowDrawable.setCallback(this);
            if (mShadowDrawable instanceof NinePatchDrawable) {
                mShadowNinePatchDrawable = (NinePatchDrawable) mShadowDrawable;
            }
        }

        mShadowVisible = a.getBoolean(R.styleable.DrawShadowFrameLayout_shadowVisible, true);
        setWillNotDraw(!mShadowVisible || mShadowDrawable == null);

        a.recycle();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mWidth = w;
        mHeight = h;
        updateShadowBounds();
    }

    private void updateShadowBounds() {
        if (mShadowDrawable != null) {
            mShadowDrawable.setBounds(0, mShadowTopOffset, mWidth, mHeight);
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (mShadowDrawable != null && mShadowVisible) {
            if (mShadowNinePatchDrawable != null) {
                mShadowNinePatchDrawable.getPaint().setAlpha((int) (255 * mAlpha));
            }
            mShadowDrawable.draw(canvas);
        }
    }

    public void setShadowTopOffset(int shadowTopOffset) {
        this.mShadowTopOffset = shadowTopOffset;
        updateShadowBounds();
        ViewCompat.postInvalidateOnAnimation(this);
    }

    public void setShadowVisible(boolean shadowVisible, boolean animate) {
        this.mShadowVisible = shadowVisible;
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }

        if (animate && mShadowDrawable != null) {
            mAnimator = ObjectAnimator.ofFloat(this, SHADOW_ALPHA,
                    shadowVisible ? 0f : 1f,
                    shadowVisible ? 1f : 0f);
            mAnimator.setDuration(1000);
            mAnimator.start();
        }

        ViewCompat.postInvalidateOnAnimation(this);
        setWillNotDraw(!mShadowVisible || mShadowDrawable == null);
    }

    private static Property<DrawShadowFrameLayout, Float> SHADOW_ALPHA
            = new Property<DrawShadowFrameLayout, Float>(Float.class, "shadowAlpha") {
        @Override
        public Float get(DrawShadowFrameLayout dsfl) {
            return dsfl.mAlpha;
        }

        @Override
        public void set(DrawShadowFrameLayout dsfl, Float value) {
            dsfl.mAlpha = value;
            ViewCompat.postInvalidateOnAnimation(dsfl);
        }
    };
}
