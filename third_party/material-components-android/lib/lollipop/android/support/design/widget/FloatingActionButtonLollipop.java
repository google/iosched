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

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.StateListAnimator;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.View;

@RequiresApi(21)
class FloatingActionButtonLollipop extends FloatingActionButtonImpl {

  private InsetDrawable mInsetDrawable;

  FloatingActionButtonLollipop(
      VisibilityAwareImageButton view, ShadowViewDelegate shadowViewDelegate) {
    super(view, shadowViewDelegate);
  }

  @Override
  void setBackgroundDrawable(
      ColorStateList backgroundTint,
      PorterDuff.Mode backgroundTintMode,
      int rippleColor,
      int borderWidth) {
    // Now we need to tint the shape background with the tint
    mShapeDrawable = DrawableCompat.wrap(createShapeDrawable());
    DrawableCompat.setTintList(mShapeDrawable, backgroundTint);
    if (backgroundTintMode != null) {
      DrawableCompat.setTintMode(mShapeDrawable, backgroundTintMode);
    }

    final Drawable rippleContent;
    if (borderWidth > 0) {
      mBorderDrawable = createBorderDrawable(borderWidth, backgroundTint);
      rippleContent = new LayerDrawable(new Drawable[] {mBorderDrawable, mShapeDrawable});
    } else {
      mBorderDrawable = null;
      rippleContent = mShapeDrawable;
    }

    mRippleDrawable = new RippleDrawable(ColorStateList.valueOf(rippleColor), rippleContent, null);

    mContentBackground = mRippleDrawable;

    mShadowViewDelegate.setBackgroundDrawable(mRippleDrawable);
  }

  @Override
  void setRippleColor(int rippleColor) {
    if (mRippleDrawable instanceof RippleDrawable) {
      ((RippleDrawable) mRippleDrawable).setColor(ColorStateList.valueOf(rippleColor));
    } else {
      super.setRippleColor(rippleColor);
    }
  }

  @Override
  void onElevationsChanged(final float elevation, final float pressedTranslationZ) {
    if (Build.VERSION.SDK_INT == 21) {
      // Animations produce NPE in version 21. Bluntly set the values instead (matching the
      // logic in the animations below).
      if (mView.isEnabled()) {
        mView.setElevation(elevation);
        if (mView.isFocused() || mView.isPressed()) {
          mView.setTranslationZ(pressedTranslationZ);
        } else {
          mView.setTranslationZ(0);
        }
      } else {
        mView.setElevation(0);
        mView.setTranslationZ(0);
      }
    } else {
      final StateListAnimator stateListAnimator = new StateListAnimator();

      // Animate elevation and translationZ to our values when pressed
      AnimatorSet set = new AnimatorSet();
      set.play(ObjectAnimator.ofFloat(mView, "elevation", elevation).setDuration(0))
          .with(
              ObjectAnimator.ofFloat(mView, View.TRANSLATION_Z, pressedTranslationZ)
                  .setDuration(PRESSED_ANIM_DURATION));
      set.setInterpolator(ANIM_INTERPOLATOR);
      stateListAnimator.addState(PRESSED_ENABLED_STATE_SET, set);

      // Same deal for when we're focused
      set = new AnimatorSet();
      set.play(ObjectAnimator.ofFloat(mView, "elevation", elevation).setDuration(0))
          .with(
              ObjectAnimator.ofFloat(mView, View.TRANSLATION_Z, pressedTranslationZ)
                  .setDuration(PRESSED_ANIM_DURATION));
      set.setInterpolator(ANIM_INTERPOLATOR);
      stateListAnimator.addState(FOCUSED_ENABLED_STATE_SET, set);

      // Animate translationZ to 0 if not pressed
      set = new AnimatorSet();
      set.playSequentially(
          ObjectAnimator.ofFloat(mView, "elevation", elevation).setDuration(0),
          // This is a no-op animation which exists here only for introducing the duration
          // because setting the delay (on the next animation) via "setDelay" or "after"
          // can trigger a NPE between android versions 22 and 24 (due to a framework
          // bug). The issue has been fixed in version 25.
          ObjectAnimator.ofFloat(mView, View.TRANSLATION_Z, mView.getTranslationZ())
              .setDuration(PRESSED_ANIM_DELAY),
          ObjectAnimator.ofFloat(mView, View.TRANSLATION_Z, 0f).setDuration(PRESSED_ANIM_DURATION));
      set.setInterpolator(ANIM_INTERPOLATOR);
      stateListAnimator.addState(ENABLED_STATE_SET, set);

      // Animate everything to 0 when disabled
      set = new AnimatorSet();
      set.play(ObjectAnimator.ofFloat(mView, "elevation", 0f).setDuration(0))
          .with(ObjectAnimator.ofFloat(mView, View.TRANSLATION_Z, 0f).setDuration(0));
      set.setInterpolator(ANIM_INTERPOLATOR);
      stateListAnimator.addState(EMPTY_STATE_SET, set);

      mView.setStateListAnimator(stateListAnimator);
    }

    if (mShadowViewDelegate.isCompatPaddingEnabled()) {
      updatePadding();
    }
  }

  @Override
  public float getElevation() {
    return mView.getElevation();
  }

  @Override
  void onCompatShadowChanged() {
    updatePadding();
  }

  @Override
  void onPaddingUpdated(Rect padding) {
    if (mShadowViewDelegate.isCompatPaddingEnabled()) {
      mInsetDrawable =
          new InsetDrawable(
              mRippleDrawable, padding.left, padding.top, padding.right, padding.bottom);
      mShadowViewDelegate.setBackgroundDrawable(mInsetDrawable);
    } else {
      mShadowViewDelegate.setBackgroundDrawable(mRippleDrawable);
    }
  }

  @Override
  void onDrawableStateChanged(int[] state) {
    // no-op
  }

  @Override
  void jumpDrawableToCurrentState() {
    // no-op
  }

  @Override
  boolean requirePreDrawListener() {
    return false;
  }

  @Override
  CircularBorderDrawable newCircularDrawable() {
    return new CircularBorderDrawableLollipop();
  }

  @Override
  GradientDrawable newGradientDrawableForShape() {
    return new AlwaysStatefulGradientDrawable();
  }

  @Override
  void getPadding(Rect rect) {
    if (mShadowViewDelegate.isCompatPaddingEnabled()) {
      final float radius = mShadowViewDelegate.getRadius();
      final float maxShadowSize = getElevation() + mPressedTranslationZ;
      final int hPadding =
          (int)
              Math.ceil(
                  ShadowDrawableWrapper.calculateHorizontalPadding(maxShadowSize, radius, false));
      final int vPadding =
          (int)
              Math.ceil(
                  ShadowDrawableWrapper.calculateVerticalPadding(maxShadowSize, radius, false));
      rect.set(hPadding, vPadding, hPadding, vPadding);
    } else {
      rect.set(0, 0, 0, 0);
    }
  }

  /**
   * LayerDrawable on L+ caches its isStateful() state and doesn't refresh it, meaning that if we
   * apply a tint to one of its children, the parent doesn't become stateful and the tint doesn't
   * work for state changes. We workaround it by saying that we are always stateful. If we don't
   * have a stateful tint, the change is ignored anyway.
   */
  static class AlwaysStatefulGradientDrawable extends GradientDrawable {
    @Override
    public boolean isStateful() {
      return true;
    }
  }
}
