/*
 * Copyright (C) 2015 The Android Open Source Project
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

package android.support.design.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.R;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.ViewCompat;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Interpolator;

class FloatingActionButtonImpl {
  static final Interpolator ANIM_INTERPOLATOR = AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR;
  static final long PRESSED_ANIM_DURATION = 100;
  static final long PRESSED_ANIM_DELAY = 100;

  static final int ANIM_STATE_NONE = 0;
  static final int ANIM_STATE_HIDING = 1;
  static final int ANIM_STATE_SHOWING = 2;

  int mAnimState = ANIM_STATE_NONE;

  private final StateListAnimator mStateListAnimator;

  ShadowDrawableWrapper mShadowDrawable;

  private float mRotation;

  Drawable mShapeDrawable;
  Drawable mRippleDrawable;
  CircularBorderDrawable mBorderDrawable;
  Drawable mContentBackground;

  float mElevation;
  float mPressedTranslationZ;

  interface InternalVisibilityChangedListener {
    void onShown();

    void onHidden();
  }

  static final int SHOW_HIDE_ANIM_DURATION = 200;

  static final int[] PRESSED_ENABLED_STATE_SET = {
    android.R.attr.state_pressed, android.R.attr.state_enabled
  };
  static final int[] FOCUSED_ENABLED_STATE_SET = {
    android.R.attr.state_focused, android.R.attr.state_enabled
  };
  static final int[] ENABLED_STATE_SET = {android.R.attr.state_enabled};
  static final int[] EMPTY_STATE_SET = new int[0];

  final VisibilityAwareImageButton mView;
  final ShadowViewDelegate mShadowViewDelegate;

  private final Rect mTmpRect = new Rect();
  private ViewTreeObserver.OnPreDrawListener mPreDrawListener;

  FloatingActionButtonImpl(VisibilityAwareImageButton view, ShadowViewDelegate shadowViewDelegate) {
    mView = view;
    mShadowViewDelegate = shadowViewDelegate;

    mStateListAnimator = new StateListAnimator();

    // Elevate with translationZ when pressed or focused
    mStateListAnimator.addState(
        PRESSED_ENABLED_STATE_SET, createAnimator(new ElevateToTranslationZAnimation()));
    mStateListAnimator.addState(
        FOCUSED_ENABLED_STATE_SET, createAnimator(new ElevateToTranslationZAnimation()));
    // Reset back to elevation by default
    mStateListAnimator.addState(ENABLED_STATE_SET, createAnimator(new ResetElevationAnimation()));
    // Set to 0 when disabled
    mStateListAnimator.addState(EMPTY_STATE_SET, createAnimator(new DisabledElevationAnimation()));

    mRotation = mView.getRotation();
  }

  void setBackgroundDrawable(
      ColorStateList backgroundTint,
      PorterDuff.Mode backgroundTintMode,
      int rippleColor,
      int borderWidth) {
    // Now we need to tint the original background with the tint, using
    // an InsetDrawable if we have a border width
    mShapeDrawable = DrawableCompat.wrap(createShapeDrawable());
    DrawableCompat.setTintList(mShapeDrawable, backgroundTint);
    if (backgroundTintMode != null) {
      DrawableCompat.setTintMode(mShapeDrawable, backgroundTintMode);
    }

    // Now we created a mask Drawable which will be used for touch feedback.
    GradientDrawable touchFeedbackShape = createShapeDrawable();

    // We'll now wrap that touch feedback mask drawable with a ColorStateList. We do not need
    // to inset for any border here as LayerDrawable will nest the padding for us
    mRippleDrawable = DrawableCompat.wrap(touchFeedbackShape);
    DrawableCompat.setTintList(mRippleDrawable, createColorStateList(rippleColor));

    final Drawable[] layers;
    if (borderWidth > 0) {
      mBorderDrawable = createBorderDrawable(borderWidth, backgroundTint);
      layers = new Drawable[] {mBorderDrawable, mShapeDrawable, mRippleDrawable};
    } else {
      mBorderDrawable = null;
      layers = new Drawable[] {mShapeDrawable, mRippleDrawable};
    }

    mContentBackground = new LayerDrawable(layers);

    mShadowDrawable =
        new ShadowDrawableWrapper(
            mView.getContext(),
            mContentBackground,
            mShadowViewDelegate.getRadius(),
            mElevation,
            mElevation + mPressedTranslationZ);
    mShadowDrawable.setAddPaddingForCorners(false);
    mShadowViewDelegate.setBackgroundDrawable(mShadowDrawable);
  }

  void setBackgroundTintList(ColorStateList tint) {
    if (mShapeDrawable != null) {
      DrawableCompat.setTintList(mShapeDrawable, tint);
    }
    if (mBorderDrawable != null) {
      mBorderDrawable.setBorderTint(tint);
    }
  }

  void setBackgroundTintMode(PorterDuff.Mode tintMode) {
    if (mShapeDrawable != null) {
      DrawableCompat.setTintMode(mShapeDrawable, tintMode);
    }
  }

  void setRippleColor(int rippleColor) {
    if (mRippleDrawable != null) {
      DrawableCompat.setTintList(mRippleDrawable, createColorStateList(rippleColor));
    }
  }

  final void setElevation(float elevation) {
    if (mElevation != elevation) {
      mElevation = elevation;
      onElevationsChanged(elevation, mPressedTranslationZ);
    }
  }

  float getElevation() {
    return mElevation;
  }

  final void setPressedTranslationZ(float translationZ) {
    if (mPressedTranslationZ != translationZ) {
      mPressedTranslationZ = translationZ;
      onElevationsChanged(mElevation, translationZ);
    }
  }

  void onElevationsChanged(float elevation, float pressedTranslationZ) {
    if (mShadowDrawable != null) {
      mShadowDrawable.setShadowSize(elevation, elevation + mPressedTranslationZ);
      updatePadding();
    }
  }

  void onDrawableStateChanged(int[] state) {
    mStateListAnimator.setState(state);
  }

  void jumpDrawableToCurrentState() {
    mStateListAnimator.jumpToCurrentState();
  }

  void hide(@Nullable final InternalVisibilityChangedListener listener, final boolean fromUser) {
    if (isOrWillBeHidden()) {
      // We either are or will soon be hidden, skip the call
      return;
    }

    mView.animate().cancel();

    if (shouldAnimateVisibilityChange()) {
      mAnimState = ANIM_STATE_HIDING;

      mView
          .animate()
          .scaleX(0f)
          .scaleY(0f)
          .alpha(0f)
          .setDuration(SHOW_HIDE_ANIM_DURATION)
          .setInterpolator(AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR)
          .setListener(
              new AnimatorListenerAdapter() {
                private boolean mCancelled;

                @Override
                public void onAnimationStart(Animator animation) {
                  mView.internalSetVisibility(View.VISIBLE, fromUser);
                  mCancelled = false;
                }

                @Override
                public void onAnimationCancel(Animator animation) {
                  mCancelled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                  mAnimState = ANIM_STATE_NONE;

                  if (!mCancelled) {
                    mView.internalSetVisibility(fromUser ? View.GONE : View.INVISIBLE, fromUser);
                    if (listener != null) {
                      listener.onHidden();
                    }
                  }
                }
              });
    } else {
      // If the view isn't laid out, or we're in the editor, don't run the animation
      mView.internalSetVisibility(fromUser ? View.GONE : View.INVISIBLE, fromUser);
      if (listener != null) {
        listener.onHidden();
      }
    }
  }

  void show(@Nullable final InternalVisibilityChangedListener listener, final boolean fromUser) {
    if (isOrWillBeShown()) {
      // We either are or will soon be visible, skip the call
      return;
    }

    mView.animate().cancel();

    if (shouldAnimateVisibilityChange()) {
      mAnimState = ANIM_STATE_SHOWING;

      if (mView.getVisibility() != View.VISIBLE) {
        // If the view isn't visible currently, we'll animate it from a single pixel
        mView.setAlpha(0f);
        mView.setScaleY(0f);
        mView.setScaleX(0f);
      }

      mView
          .animate()
          .scaleX(1f)
          .scaleY(1f)
          .alpha(1f)
          .setDuration(SHOW_HIDE_ANIM_DURATION)
          .setInterpolator(AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR)
          .setListener(
              new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                  mView.internalSetVisibility(View.VISIBLE, fromUser);
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                  mAnimState = ANIM_STATE_NONE;
                  if (listener != null) {
                    listener.onShown();
                  }
                }
              });
    } else {
      mView.internalSetVisibility(View.VISIBLE, fromUser);
      mView.setAlpha(1f);
      mView.setScaleY(1f);
      mView.setScaleX(1f);
      if (listener != null) {
        listener.onShown();
      }
    }
  }

  final Drawable getContentBackground() {
    return mContentBackground;
  }

  void onCompatShadowChanged() {
    // Ignore pre-v21
  }

  final void updatePadding() {
    Rect rect = mTmpRect;
    getPadding(rect);
    onPaddingUpdated(rect);
    mShadowViewDelegate.setShadowPadding(rect.left, rect.top, rect.right, rect.bottom);
  }

  void getPadding(Rect rect) {
    mShadowDrawable.getPadding(rect);
  }

  void onPaddingUpdated(Rect padding) {}

  void onAttachedToWindow() {
    if (requirePreDrawListener()) {
      ensurePreDrawListener();
      mView.getViewTreeObserver().addOnPreDrawListener(mPreDrawListener);
    }
  }

  void onDetachedFromWindow() {
    if (mPreDrawListener != null) {
      mView.getViewTreeObserver().removeOnPreDrawListener(mPreDrawListener);
      mPreDrawListener = null;
    }
  }

  boolean requirePreDrawListener() {
    return true;
  }

  CircularBorderDrawable createBorderDrawable(int borderWidth, ColorStateList backgroundTint) {
    final Context context = mView.getContext();
    CircularBorderDrawable borderDrawable = newCircularDrawable();
    borderDrawable.setGradientColors(
        ContextCompat.getColor(context, R.color.design_fab_stroke_top_outer_color),
        ContextCompat.getColor(context, R.color.design_fab_stroke_top_inner_color),
        ContextCompat.getColor(context, R.color.design_fab_stroke_end_inner_color),
        ContextCompat.getColor(context, R.color.design_fab_stroke_end_outer_color));
    borderDrawable.setBorderWidth(borderWidth);
    borderDrawable.setBorderTint(backgroundTint);
    return borderDrawable;
  }

  CircularBorderDrawable newCircularDrawable() {
    return new CircularBorderDrawable();
  }

  void onPreDraw() {
    final float rotation = mView.getRotation();
    if (mRotation != rotation) {
      mRotation = rotation;
      updateFromViewRotation();
    }
  }

  private void ensurePreDrawListener() {
    if (mPreDrawListener == null) {
      mPreDrawListener =
          new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
              FloatingActionButtonImpl.this.onPreDraw();
              return true;
            }
          };
    }
  }

  GradientDrawable createShapeDrawable() {
    GradientDrawable d = newGradientDrawableForShape();
    d.setShape(GradientDrawable.OVAL);
    d.setColor(Color.WHITE);
    return d;
  }

  GradientDrawable newGradientDrawableForShape() {
    return new GradientDrawable();
  }

  boolean isOrWillBeShown() {
    if (mView.getVisibility() != View.VISIBLE) {
      // If we not currently visible, return true if we're animating to be shown
      return mAnimState == ANIM_STATE_SHOWING;
    } else {
      // Otherwise if we're visible, return true if we're not animating to be hidden
      return mAnimState != ANIM_STATE_HIDING;
    }
  }

  boolean isOrWillBeHidden() {
    if (mView.getVisibility() == View.VISIBLE) {
      // If we currently visible, return true if we're animating to be hidden
      return mAnimState == ANIM_STATE_HIDING;
    } else {
      // Otherwise if we're not visible, return true if we're not animating to be shown
      return mAnimState != ANIM_STATE_SHOWING;
    }
  }

  private ValueAnimator createAnimator(@NonNull ShadowAnimatorImpl impl) {
    final ValueAnimator animator = new ValueAnimator();
    animator.setInterpolator(ANIM_INTERPOLATOR);
    animator.setDuration(PRESSED_ANIM_DURATION);
    animator.addListener(impl);
    animator.addUpdateListener(impl);
    animator.setFloatValues(0, 1);
    return animator;
  }

  private abstract class ShadowAnimatorImpl extends AnimatorListenerAdapter
      implements ValueAnimator.AnimatorUpdateListener {
    private boolean mValidValues;
    private float mShadowSizeStart;
    private float mShadowSizeEnd;

    @Override
    public void onAnimationUpdate(ValueAnimator animator) {
      if (!mValidValues) {
        mShadowSizeStart = mShadowDrawable.getShadowSize();
        mShadowSizeEnd = getTargetShadowSize();
        mValidValues = true;
      }

      mShadowDrawable.setShadowSize(
          mShadowSizeStart
              + ((mShadowSizeEnd - mShadowSizeStart) * animator.getAnimatedFraction()));
    }

    @Override
    public void onAnimationEnd(Animator animator) {
      mShadowDrawable.setShadowSize(mShadowSizeEnd);
      mValidValues = false;
    }

    /** @return the shadow size we want to animate to. */
    protected abstract float getTargetShadowSize();
  }

  private class ResetElevationAnimation extends ShadowAnimatorImpl {
    ResetElevationAnimation() {}

    @Override
    protected float getTargetShadowSize() {
      return mElevation;
    }
  }

  private class ElevateToTranslationZAnimation extends ShadowAnimatorImpl {
    ElevateToTranslationZAnimation() {}

    @Override
    protected float getTargetShadowSize() {
      return mElevation + mPressedTranslationZ;
    }
  }

  private class DisabledElevationAnimation extends ShadowAnimatorImpl {
    DisabledElevationAnimation() {}

    @Override
    protected float getTargetShadowSize() {
      return 0f;
    }
  }

  private static ColorStateList createColorStateList(int selectedColor) {
    final int[][] states = new int[3][];
    final int[] colors = new int[3];
    int i = 0;

    states[i] = FOCUSED_ENABLED_STATE_SET;
    colors[i] = selectedColor;
    i++;

    states[i] = PRESSED_ENABLED_STATE_SET;
    colors[i] = selectedColor;
    i++;

    // Default enabled state
    states[i] = new int[0];
    colors[i] = Color.TRANSPARENT;
    i++;

    return new ColorStateList(states, colors);
  }

  private boolean shouldAnimateVisibilityChange() {
    return ViewCompat.isLaidOut(mView) && !mView.isInEditMode();
  }

  private void updateFromViewRotation() {
    if (Build.VERSION.SDK_INT == 19) {
      // KitKat seems to have an issue with views which are rotated with angles which are
      // not divisible by 90. Worked around by moving to software rendering in these cases.
      if ((mRotation % 90) != 0) {
        if (mView.getLayerType() != View.LAYER_TYPE_SOFTWARE) {
          mView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
      } else {
        if (mView.getLayerType() != View.LAYER_TYPE_NONE) {
          mView.setLayerType(View.LAYER_TYPE_NONE, null);
        }
      }
    }

    // Offset any View rotation
    if (mShadowDrawable != null) {
      mShadowDrawable.setRotation(-mRotation);
    }
    if (mBorderDrawable != null) {
      mBorderDrawable.setRotation(-mRotation);
    }
  }
}
