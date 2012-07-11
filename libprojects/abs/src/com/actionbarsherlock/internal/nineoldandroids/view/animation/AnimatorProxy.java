package com.actionbarsherlock.internal.nineoldandroids.view.animation;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Build;
import android.util.FloatMath;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Transformation;

public final class AnimatorProxy extends Animation {
    public static final boolean NEEDS_PROXY = Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB;

    private static final WeakHashMap<View, AnimatorProxy> PROXIES =
            new WeakHashMap<View, AnimatorProxy>();

    public static AnimatorProxy wrap(View view) {
        AnimatorProxy proxy = PROXIES.get(view);
        if (proxy == null) {
            proxy = new AnimatorProxy(view);
            PROXIES.put(view, proxy);
        }
        return proxy;
    }

    private final WeakReference<View> mView;

    private float mAlpha = 1;
    private float mScaleX = 1;
    private float mScaleY = 1;
    private float mTranslationX;
    private float mTranslationY;

    private final RectF mBefore = new RectF();
    private final RectF mAfter = new RectF();
    private final Matrix mTempMatrix = new Matrix();

    private AnimatorProxy(View view) {
        setDuration(0); //perform transformation immediately
        setFillAfter(true); //persist transformation beyond duration
        view.setAnimation(this);
        mView = new WeakReference<View>(view);
    }

    public float getAlpha() {
        return mAlpha;
    }
    public void setAlpha(float alpha) {
        if (mAlpha != alpha) {
            mAlpha = alpha;
            View view = mView.get();
            if (view != null) {
                view.invalidate();
            }
        }
    }
    public float getScaleX() {
        return mScaleX;
    }
    public void setScaleX(float scaleX) {
        if (mScaleX != scaleX) {
            prepareForUpdate();
            mScaleX = scaleX;
            invalidateAfterUpdate();
        }
    }
    public float getScaleY() {
        return mScaleY;
    }
    public void setScaleY(float scaleY) {
        if (mScaleY != scaleY) {
            prepareForUpdate();
            mScaleY = scaleY;
            invalidateAfterUpdate();
        }
    }
    public int getScrollX() {
        View view = mView.get();
        if (view == null) {
            return 0;
        }
        return view.getScrollX();
    }
    public void setScrollX(int value) {
        View view = mView.get();
        if (view != null) {
            view.scrollTo(value, view.getScrollY());
        }
    }
    public int getScrollY() {
        View view = mView.get();
        if (view == null) {
            return 0;
        }
        return view.getScrollY();
    }
    public void setScrollY(int value) {
        View view = mView.get();
        if (view != null) {
            view.scrollTo(view.getScrollY(), value);
        }
    }

    public float getTranslationX() {
        return mTranslationX;
    }
    public void setTranslationX(float translationX) {
        if (mTranslationX != translationX) {
            prepareForUpdate();
            mTranslationX = translationX;
            invalidateAfterUpdate();
        }
    }
    public float getTranslationY() {
        return mTranslationY;
    }
    public void setTranslationY(float translationY) {
        if (mTranslationY != translationY) {
            prepareForUpdate();
            mTranslationY = translationY;
            invalidateAfterUpdate();
        }
    }

    private void prepareForUpdate() {
        View view = mView.get();
        if (view != null) {
            computeRect(mBefore, view);
        }
    }
    private void invalidateAfterUpdate() {
        View view = mView.get();
        if (view == null) {
            return;
        }
        View parent = (View)view.getParent();
        if (parent == null) {
            return;
        }

        view.setAnimation(this);

        final RectF after = mAfter;
        computeRect(after, view);
        after.union(mBefore);

        parent.invalidate(
                (int) FloatMath.floor(after.left),
                (int) FloatMath.floor(after.top),
                (int) FloatMath.ceil(after.right),
                (int) FloatMath.ceil(after.bottom));
    }

    private void computeRect(final RectF r, View view) {
        // compute current rectangle according to matrix transformation
        final float w = view.getWidth();
        final float h = view.getHeight();

        // use a rectangle at 0,0 to make sure we don't run into issues with scaling
        r.set(0, 0, w, h);

        final Matrix m = mTempMatrix;
        m.reset();
        transformMatrix(m, view);
        mTempMatrix.mapRect(r);

        r.offset(view.getLeft(), view.getTop());

        // Straighten coords if rotations flipped them
        if (r.right < r.left) {
            final float f = r.right;
            r.right = r.left;
            r.left = f;
        }
        if (r.bottom < r.top) {
            final float f = r.top;
            r.top = r.bottom;
            r.bottom = f;
        }
    }

    private void transformMatrix(Matrix m, View view) {
        final float w = view.getWidth();
        final float h = view.getHeight();

        final float sX = mScaleX;
        final float sY = mScaleY;
        if ((sX != 1.0f) || (sY != 1.0f)) {
            final float deltaSX = ((sX * w) - w) / 2f;
            final float deltaSY = ((sY * h) - h) / 2f;
            m.postScale(sX, sY);
            m.postTranslate(-deltaSX, -deltaSY);
        }
        m.postTranslate(mTranslationX, mTranslationY);
    }

    @Override
    protected void applyTransformation(float interpolatedTime, Transformation t) {
        View view = mView.get();
        if (view != null) {
            t.setAlpha(mAlpha);
            transformMatrix(t.getMatrix(), view);
        }
    }

    @Override
    public void reset() {
        /* Do nothing. */
    }
}
