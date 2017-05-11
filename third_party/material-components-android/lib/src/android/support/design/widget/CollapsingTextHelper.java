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

import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.v4.text.TextDirectionHeuristicsCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.TintTypedArray;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Interpolator;

final class CollapsingTextHelper {

  // Pre-JB-MR2 doesn't support HW accelerated canvas scaled text so we will workaround it
  // by using our own texture
  private static final boolean USE_SCALING_TEXTURE = Build.VERSION.SDK_INT < 18;

  private static final boolean DEBUG_DRAW = false;
  private static final Paint DEBUG_DRAW_PAINT;

  static {
    DEBUG_DRAW_PAINT = DEBUG_DRAW ? new Paint() : null;
    if (DEBUG_DRAW_PAINT != null) {
      DEBUG_DRAW_PAINT.setAntiAlias(true);
      DEBUG_DRAW_PAINT.setColor(Color.MAGENTA);
    }
  }

  private final View mView;

  private boolean mDrawTitle;
  private float mExpandedFraction;

  private final Rect mExpandedBounds;
  private final Rect mCollapsedBounds;
  private final RectF mCurrentBounds;
  private int mExpandedTextGravity = Gravity.CENTER_VERTICAL;
  private int mCollapsedTextGravity = Gravity.CENTER_VERTICAL;
  private float mExpandedTextSize = 15;
  private float mCollapsedTextSize = 15;
  private ColorStateList mExpandedTextColor;
  private ColorStateList mCollapsedTextColor;

  private float mExpandedDrawY;
  private float mCollapsedDrawY;
  private float mExpandedDrawX;
  private float mCollapsedDrawX;
  private float mCurrentDrawX;
  private float mCurrentDrawY;
  private Typeface mCollapsedTypeface;
  private Typeface mExpandedTypeface;
  private Typeface mCurrentTypeface;

  private CharSequence mText;
  private CharSequence mTextToDraw;
  private boolean mIsRtl;

  private boolean mUseTexture;
  private Bitmap mExpandedTitleTexture;
  private Paint mTexturePaint;
  private float mTextureAscent;
  private float mTextureDescent;

  private float mScale;
  private float mCurrentTextSize;

  private int[] mState;

  private boolean mBoundsChanged;

  private final TextPaint mTextPaint;

  private Interpolator mPositionInterpolator;
  private Interpolator mTextSizeInterpolator;

  private float mCollapsedShadowRadius, mCollapsedShadowDx, mCollapsedShadowDy;
  private int mCollapsedShadowColor;

  private float mExpandedShadowRadius, mExpandedShadowDx, mExpandedShadowDy;
  private int mExpandedShadowColor;

  public CollapsingTextHelper(View view) {
    mView = view;

    mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);

    mCollapsedBounds = new Rect();
    mExpandedBounds = new Rect();
    mCurrentBounds = new RectF();
  }

  void setTextSizeInterpolator(Interpolator interpolator) {
    mTextSizeInterpolator = interpolator;
    recalculate();
  }

  void setPositionInterpolator(Interpolator interpolator) {
    mPositionInterpolator = interpolator;
    recalculate();
  }

  void setExpandedTextSize(float textSize) {
    if (mExpandedTextSize != textSize) {
      mExpandedTextSize = textSize;
      recalculate();
    }
  }

  void setCollapsedTextSize(float textSize) {
    if (mCollapsedTextSize != textSize) {
      mCollapsedTextSize = textSize;
      recalculate();
    }
  }

  void setCollapsedTextColor(ColorStateList textColor) {
    if (mCollapsedTextColor != textColor) {
      mCollapsedTextColor = textColor;
      recalculate();
    }
  }

  void setExpandedTextColor(ColorStateList textColor) {
    if (mExpandedTextColor != textColor) {
      mExpandedTextColor = textColor;
      recalculate();
    }
  }

  void setExpandedBounds(int left, int top, int right, int bottom) {
    if (!rectEquals(mExpandedBounds, left, top, right, bottom)) {
      mExpandedBounds.set(left, top, right, bottom);
      mBoundsChanged = true;
      onBoundsChanged();
    }
  }

  void setCollapsedBounds(int left, int top, int right, int bottom) {
    if (!rectEquals(mCollapsedBounds, left, top, right, bottom)) {
      mCollapsedBounds.set(left, top, right, bottom);
      mBoundsChanged = true;
      onBoundsChanged();
    }
  }

  void onBoundsChanged() {
    mDrawTitle =
        mCollapsedBounds.width() > 0
            && mCollapsedBounds.height() > 0
            && mExpandedBounds.width() > 0
            && mExpandedBounds.height() > 0;
  }

  void setExpandedTextGravity(int gravity) {
    if (mExpandedTextGravity != gravity) {
      mExpandedTextGravity = gravity;
      recalculate();
    }
  }

  int getExpandedTextGravity() {
    return mExpandedTextGravity;
  }

  void setCollapsedTextGravity(int gravity) {
    if (mCollapsedTextGravity != gravity) {
      mCollapsedTextGravity = gravity;
      recalculate();
    }
  }

  int getCollapsedTextGravity() {
    return mCollapsedTextGravity;
  }

  void setCollapsedTextAppearance(int resId) {
    TintTypedArray a =
        TintTypedArray.obtainStyledAttributes(
            mView.getContext(), resId, android.support.v7.appcompat.R.styleable.TextAppearance);
    if (a.hasValue(android.support.v7.appcompat.R.styleable.TextAppearance_android_textColor)) {
      mCollapsedTextColor =
          a.getColorStateList(
              android.support.v7.appcompat.R.styleable.TextAppearance_android_textColor);
    }
    if (a.hasValue(android.support.v7.appcompat.R.styleable.TextAppearance_android_textSize)) {
      mCollapsedTextSize =
          a.getDimensionPixelSize(
              android.support.v7.appcompat.R.styleable.TextAppearance_android_textSize,
              (int) mCollapsedTextSize);
    }
    mCollapsedShadowColor =
        a.getInt(android.support.v7.appcompat.R.styleable.TextAppearance_android_shadowColor, 0);
    mCollapsedShadowDx =
        a.getFloat(android.support.v7.appcompat.R.styleable.TextAppearance_android_shadowDx, 0);
    mCollapsedShadowDy =
        a.getFloat(android.support.v7.appcompat.R.styleable.TextAppearance_android_shadowDy, 0);
    mCollapsedShadowRadius =
        a.getFloat(android.support.v7.appcompat.R.styleable.TextAppearance_android_shadowRadius, 0);
    a.recycle();

    if (Build.VERSION.SDK_INT >= 16) {
      mCollapsedTypeface = readFontFamilyTypeface(resId);
    }

    recalculate();
  }

  void setExpandedTextAppearance(int resId) {
    TintTypedArray a =
        TintTypedArray.obtainStyledAttributes(
            mView.getContext(), resId, android.support.v7.appcompat.R.styleable.TextAppearance);
    if (a.hasValue(android.support.v7.appcompat.R.styleable.TextAppearance_android_textColor)) {
      mExpandedTextColor =
          a.getColorStateList(
              android.support.v7.appcompat.R.styleable.TextAppearance_android_textColor);
    }
    if (a.hasValue(android.support.v7.appcompat.R.styleable.TextAppearance_android_textSize)) {
      mExpandedTextSize =
          a.getDimensionPixelSize(
              android.support.v7.appcompat.R.styleable.TextAppearance_android_textSize,
              (int) mExpandedTextSize);
    }
    mExpandedShadowColor =
        a.getInt(android.support.v7.appcompat.R.styleable.TextAppearance_android_shadowColor, 0);
    mExpandedShadowDx =
        a.getFloat(android.support.v7.appcompat.R.styleable.TextAppearance_android_shadowDx, 0);
    mExpandedShadowDy =
        a.getFloat(android.support.v7.appcompat.R.styleable.TextAppearance_android_shadowDy, 0);
    mExpandedShadowRadius =
        a.getFloat(android.support.v7.appcompat.R.styleable.TextAppearance_android_shadowRadius, 0);
    a.recycle();

    if (Build.VERSION.SDK_INT >= 16) {
      mExpandedTypeface = readFontFamilyTypeface(resId);
    }

    recalculate();
  }

  private Typeface readFontFamilyTypeface(int resId) {
    final TypedArray a =
        mView.getContext().obtainStyledAttributes(resId, new int[] {android.R.attr.fontFamily});
    try {
      final String family = a.getString(0);
      if (family != null) {
        return Typeface.create(family, Typeface.NORMAL);
      }
    } finally {
      a.recycle();
    }
    return null;
  }

  void setCollapsedTypeface(Typeface typeface) {
    if (mCollapsedTypeface != typeface) {
      mCollapsedTypeface = typeface;
      recalculate();
    }
  }

  void setExpandedTypeface(Typeface typeface) {
    if (mExpandedTypeface != typeface) {
      mExpandedTypeface = typeface;
      recalculate();
    }
  }

  void setTypefaces(Typeface typeface) {
    mCollapsedTypeface = mExpandedTypeface = typeface;
    recalculate();
  }

  Typeface getCollapsedTypeface() {
    return mCollapsedTypeface != null ? mCollapsedTypeface : Typeface.DEFAULT;
  }

  Typeface getExpandedTypeface() {
    return mExpandedTypeface != null ? mExpandedTypeface : Typeface.DEFAULT;
  }

  /**
   * Set the value indicating the current scroll value. This decides how much of the background will
   * be displayed, as well as the title metrics/positioning.
   *
   * <p>A value of {@code 0.0} indicates that the layout is fully expanded. A value of {@code 1.0}
   * indicates that the layout is fully collapsed.
   */
  void setExpansionFraction(float fraction) {
    fraction = MathUtils.constrain(fraction, 0f, 1f);

    if (fraction != mExpandedFraction) {
      mExpandedFraction = fraction;
      calculateCurrentOffsets();
    }
  }

  final boolean setState(final int[] state) {
    mState = state;

    if (isStateful()) {
      recalculate();
      return true;
    }

    return false;
  }

  final boolean isStateful() {
    return (mCollapsedTextColor != null && mCollapsedTextColor.isStateful())
        || (mExpandedTextColor != null && mExpandedTextColor.isStateful());
  }

  float getExpansionFraction() {
    return mExpandedFraction;
  }

  float getCollapsedTextSize() {
    return mCollapsedTextSize;
  }

  float getExpandedTextSize() {
    return mExpandedTextSize;
  }

  private void calculateCurrentOffsets() {
    calculateOffsets(mExpandedFraction);
  }

  private void calculateOffsets(final float fraction) {
    interpolateBounds(fraction);
    mCurrentDrawX = lerp(mExpandedDrawX, mCollapsedDrawX, fraction, mPositionInterpolator);
    mCurrentDrawY = lerp(mExpandedDrawY, mCollapsedDrawY, fraction, mPositionInterpolator);

    setInterpolatedTextSize(
        lerp(mExpandedTextSize, mCollapsedTextSize, fraction, mTextSizeInterpolator));

    if (mCollapsedTextColor != mExpandedTextColor) {
      // If the collapsed and expanded text colors are different, blend them based on the
      // fraction
      mTextPaint.setColor(
          blendColors(getCurrentExpandedTextColor(), getCurrentCollapsedTextColor(), fraction));
    } else {
      mTextPaint.setColor(getCurrentCollapsedTextColor());
    }

    mTextPaint.setShadowLayer(
        lerp(mExpandedShadowRadius, mCollapsedShadowRadius, fraction, null),
        lerp(mExpandedShadowDx, mCollapsedShadowDx, fraction, null),
        lerp(mExpandedShadowDy, mCollapsedShadowDy, fraction, null),
        blendColors(mExpandedShadowColor, mCollapsedShadowColor, fraction));

    ViewCompat.postInvalidateOnAnimation(mView);
  }

  @ColorInt
  private int getCurrentExpandedTextColor() {
    if (mState != null) {
      return mExpandedTextColor.getColorForState(mState, 0);
    } else {
      return mExpandedTextColor.getDefaultColor();
    }
  }

  @ColorInt
  private int getCurrentCollapsedTextColor() {
    if (mState != null) {
      return mCollapsedTextColor.getColorForState(mState, 0);
    } else {
      return mCollapsedTextColor.getDefaultColor();
    }
  }

  private void calculateBaseOffsets() {
    final float currentTextSize = mCurrentTextSize;

    // We then calculate the collapsed text size, using the same logic
    calculateUsingTextSize(mCollapsedTextSize);
    float width =
        mTextToDraw != null ? mTextPaint.measureText(mTextToDraw, 0, mTextToDraw.length()) : 0;
    final int collapsedAbsGravity =
        GravityCompat.getAbsoluteGravity(
            mCollapsedTextGravity,
            mIsRtl ? ViewCompat.LAYOUT_DIRECTION_RTL : ViewCompat.LAYOUT_DIRECTION_LTR);
    switch (collapsedAbsGravity & Gravity.VERTICAL_GRAVITY_MASK) {
      case Gravity.BOTTOM:
        mCollapsedDrawY = mCollapsedBounds.bottom;
        break;
      case Gravity.TOP:
        mCollapsedDrawY = mCollapsedBounds.top - mTextPaint.ascent();
        break;
      case Gravity.CENTER_VERTICAL:
      default:
        float textHeight = mTextPaint.descent() - mTextPaint.ascent();
        float textOffset = (textHeight / 2) - mTextPaint.descent();
        mCollapsedDrawY = mCollapsedBounds.centerY() + textOffset;
        break;
    }
    switch (collapsedAbsGravity & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
      case Gravity.CENTER_HORIZONTAL:
        mCollapsedDrawX = mCollapsedBounds.centerX() - (width / 2);
        break;
      case Gravity.RIGHT:
        mCollapsedDrawX = mCollapsedBounds.right - width;
        break;
      case Gravity.LEFT:
      default:
        mCollapsedDrawX = mCollapsedBounds.left;
        break;
    }

    calculateUsingTextSize(mExpandedTextSize);
    width = mTextToDraw != null ? mTextPaint.measureText(mTextToDraw, 0, mTextToDraw.length()) : 0;
    final int expandedAbsGravity =
        GravityCompat.getAbsoluteGravity(
            mExpandedTextGravity,
            mIsRtl ? ViewCompat.LAYOUT_DIRECTION_RTL : ViewCompat.LAYOUT_DIRECTION_LTR);
    switch (expandedAbsGravity & Gravity.VERTICAL_GRAVITY_MASK) {
      case Gravity.BOTTOM:
        mExpandedDrawY = mExpandedBounds.bottom;
        break;
      case Gravity.TOP:
        mExpandedDrawY = mExpandedBounds.top - mTextPaint.ascent();
        break;
      case Gravity.CENTER_VERTICAL:
      default:
        float textHeight = mTextPaint.descent() - mTextPaint.ascent();
        float textOffset = (textHeight / 2) - mTextPaint.descent();
        mExpandedDrawY = mExpandedBounds.centerY() + textOffset;
        break;
    }
    switch (expandedAbsGravity & GravityCompat.RELATIVE_HORIZONTAL_GRAVITY_MASK) {
      case Gravity.CENTER_HORIZONTAL:
        mExpandedDrawX = mExpandedBounds.centerX() - (width / 2);
        break;
      case Gravity.RIGHT:
        mExpandedDrawX = mExpandedBounds.right - width;
        break;
      case Gravity.LEFT:
      default:
        mExpandedDrawX = mExpandedBounds.left;
        break;
    }

    // The bounds have changed so we need to clear the texture
    clearTexture();
    // Now reset the text size back to the original
    setInterpolatedTextSize(currentTextSize);
  }

  private void interpolateBounds(float fraction) {
    mCurrentBounds.left =
        lerp(mExpandedBounds.left, mCollapsedBounds.left, fraction, mPositionInterpolator);
    mCurrentBounds.top = lerp(mExpandedDrawY, mCollapsedDrawY, fraction, mPositionInterpolator);
    mCurrentBounds.right =
        lerp(mExpandedBounds.right, mCollapsedBounds.right, fraction, mPositionInterpolator);
    mCurrentBounds.bottom =
        lerp(mExpandedBounds.bottom, mCollapsedBounds.bottom, fraction, mPositionInterpolator);
  }

  public void draw(Canvas canvas) {
    final int saveCount = canvas.save();

    if (mTextToDraw != null && mDrawTitle) {
      float x = mCurrentDrawX;
      float y = mCurrentDrawY;

      final boolean drawTexture = mUseTexture && mExpandedTitleTexture != null;

      final float ascent;
      final float descent;
      if (drawTexture) {
        ascent = mTextureAscent * mScale;
        descent = mTextureDescent * mScale;
      } else {
        ascent = mTextPaint.ascent() * mScale;
        descent = mTextPaint.descent() * mScale;
      }

      if (DEBUG_DRAW) {
        // Just a debug tool, which drawn a magenta rect in the text bounds
        canvas.drawRect(
            mCurrentBounds.left, y + ascent, mCurrentBounds.right, y + descent, DEBUG_DRAW_PAINT);
      }

      if (drawTexture) {
        y += ascent;
      }

      if (mScale != 1f) {
        canvas.scale(mScale, mScale, x, y);
      }

      if (drawTexture) {
        // If we should use a texture, draw it instead of text
        canvas.drawBitmap(mExpandedTitleTexture, x, y, mTexturePaint);
      } else {
        canvas.drawText(mTextToDraw, 0, mTextToDraw.length(), x, y, mTextPaint);
      }
    }

    canvas.restoreToCount(saveCount);
  }

  private boolean calculateIsRtl(CharSequence text) {
    final boolean defaultIsRtl =
        ViewCompat.getLayoutDirection(mView) == ViewCompat.LAYOUT_DIRECTION_RTL;
    return (defaultIsRtl
            ? TextDirectionHeuristicsCompat.FIRSTSTRONG_RTL
            : TextDirectionHeuristicsCompat.FIRSTSTRONG_LTR)
        .isRtl(text, 0, text.length());
  }

  private void setInterpolatedTextSize(float textSize) {
    calculateUsingTextSize(textSize);

    // Use our texture if the scale isn't 1.0
    mUseTexture = USE_SCALING_TEXTURE && mScale != 1f;

    if (mUseTexture) {
      // Make sure we have an expanded texture if needed
      ensureExpandedTexture();
    }

    ViewCompat.postInvalidateOnAnimation(mView);
  }

  private void calculateUsingTextSize(final float textSize) {
    if (mText == null) return;

    final float collapsedWidth = mCollapsedBounds.width();
    final float expandedWidth = mExpandedBounds.width();

    final float availableWidth;
    final float newTextSize;
    boolean updateDrawText = false;

    if (isClose(textSize, mCollapsedTextSize)) {
      newTextSize = mCollapsedTextSize;
      mScale = 1f;
      if (mCurrentTypeface != mCollapsedTypeface) {
        mCurrentTypeface = mCollapsedTypeface;
        updateDrawText = true;
      }
      availableWidth = collapsedWidth;
    } else {
      newTextSize = mExpandedTextSize;
      if (mCurrentTypeface != mExpandedTypeface) {
        mCurrentTypeface = mExpandedTypeface;
        updateDrawText = true;
      }
      if (isClose(textSize, mExpandedTextSize)) {
        // If we're close to the expanded text size, snap to it and use a scale of 1
        mScale = 1f;
      } else {
        // Else, we'll scale down from the expanded text size
        mScale = textSize / mExpandedTextSize;
      }

      final float textSizeRatio = mCollapsedTextSize / mExpandedTextSize;
      // This is the size of the expanded bounds when it is scaled to match the
      // collapsed text size
      final float scaledDownWidth = expandedWidth * textSizeRatio;

      if (scaledDownWidth > collapsedWidth) {
        // If the scaled down size is larger than the actual collapsed width, we need to
        // cap the available width so that when the expanded text scales down, it matches
        // the collapsed width
        availableWidth = Math.min(collapsedWidth / textSizeRatio, expandedWidth);
      } else {
        // Otherwise we'll just use the expanded width
        availableWidth = expandedWidth;
      }
    }

    if (availableWidth > 0) {
      updateDrawText = (mCurrentTextSize != newTextSize) || mBoundsChanged || updateDrawText;
      mCurrentTextSize = newTextSize;
      mBoundsChanged = false;
    }

    if (mTextToDraw == null || updateDrawText) {
      mTextPaint.setTextSize(mCurrentTextSize);
      mTextPaint.setTypeface(mCurrentTypeface);
      // Use linear text scaling if we're scaling the canvas
      mTextPaint.setLinearText(mScale != 1f);

      // If we don't currently have text to draw, or the text size has changed, ellipsize...
      final CharSequence title =
          TextUtils.ellipsize(mText, mTextPaint, availableWidth, TextUtils.TruncateAt.END);
      if (!TextUtils.equals(title, mTextToDraw)) {
        mTextToDraw = title;
        mIsRtl = calculateIsRtl(mTextToDraw);
      }
    }
  }

  private void ensureExpandedTexture() {
    if (mExpandedTitleTexture != null
        || mExpandedBounds.isEmpty()
        || TextUtils.isEmpty(mTextToDraw)) {
      return;
    }

    calculateOffsets(0f);
    mTextureAscent = mTextPaint.ascent();
    mTextureDescent = mTextPaint.descent();

    final int w = Math.round(mTextPaint.measureText(mTextToDraw, 0, mTextToDraw.length()));
    final int h = Math.round(mTextureDescent - mTextureAscent);

    if (w <= 0 || h <= 0) {
      return; // If the width or height are 0, return
    }

    mExpandedTitleTexture = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);

    Canvas c = new Canvas(mExpandedTitleTexture);
    c.drawText(mTextToDraw, 0, mTextToDraw.length(), 0, h - mTextPaint.descent(), mTextPaint);

    if (mTexturePaint == null) {
      // Make sure we have a paint
      mTexturePaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    }
  }

  public void recalculate() {
    if (mView.getHeight() > 0 && mView.getWidth() > 0) {
      // If we've already been laid out, calculate everything now otherwise we'll wait
      // until a layout
      calculateBaseOffsets();
      calculateCurrentOffsets();
    }
  }

  /**
   * Set the title to display
   *
   * @param text
   */
  void setText(CharSequence text) {
    if (text == null || !text.equals(mText)) {
      mText = text;
      mTextToDraw = null;
      clearTexture();
      recalculate();
    }
  }

  CharSequence getText() {
    return mText;
  }

  private void clearTexture() {
    if (mExpandedTitleTexture != null) {
      mExpandedTitleTexture.recycle();
      mExpandedTitleTexture = null;
    }
  }

  /**
   * Returns true if {@code value} is 'close' to it's closest decimal value. Close is currently
   * defined as it's difference being < 0.001.
   */
  private static boolean isClose(float value, float targetValue) {
    return Math.abs(value - targetValue) < 0.001f;
  }

  ColorStateList getExpandedTextColor() {
    return mExpandedTextColor;
  }

  ColorStateList getCollapsedTextColor() {
    return mCollapsedTextColor;
  }

  /**
   * Blend {@code color1} and {@code color2} using the given ratio.
   *
   * @param ratio of which to blend. 0.0 will return {@code color1}, 0.5 will give an even blend,
   *     1.0 will return {@code color2}.
   */
  private static int blendColors(int color1, int color2, float ratio) {
    final float inverseRatio = 1f - ratio;
    float a = (Color.alpha(color1) * inverseRatio) + (Color.alpha(color2) * ratio);
    float r = (Color.red(color1) * inverseRatio) + (Color.red(color2) * ratio);
    float g = (Color.green(color1) * inverseRatio) + (Color.green(color2) * ratio);
    float b = (Color.blue(color1) * inverseRatio) + (Color.blue(color2) * ratio);
    return Color.argb((int) a, (int) r, (int) g, (int) b);
  }

  private static float lerp(
      float startValue, float endValue, float fraction, Interpolator interpolator) {
    if (interpolator != null) {
      fraction = interpolator.getInterpolation(fraction);
    }
    return AnimationUtils.lerp(startValue, endValue, fraction);
  }

  private static boolean rectEquals(Rect r, int left, int top, int right, int bottom) {
    return !(r.left != left || r.top != top || r.right != right || r.bottom != bottom);
  }
}
