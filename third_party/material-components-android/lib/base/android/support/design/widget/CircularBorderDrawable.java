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
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.support.v4.graphics.ColorUtils;

/** A drawable which draws an oval 'border'. */
class CircularBorderDrawable extends Drawable {

  /**
   * We actually draw the stroke wider than the border size given. This is to reduce any potential
   * transparent space caused by anti-aliasing and padding rounding. This value defines the
   * multiplier used to determine to draw stroke width.
   */
  private static final float DRAW_STROKE_WIDTH_MULTIPLE = 1.3333f;

  final Paint mPaint;
  final Rect mRect = new Rect();
  final RectF mRectF = new RectF();

  float mBorderWidth;

  private int mTopOuterStrokeColor;
  private int mTopInnerStrokeColor;
  private int mBottomOuterStrokeColor;
  private int mBottomInnerStrokeColor;

  private ColorStateList mBorderTint;
  private int mCurrentBorderTintColor;

  private boolean mInvalidateShader = true;

  private float mRotation;

  public CircularBorderDrawable() {
    mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    mPaint.setStyle(Paint.Style.STROKE);
  }

  void setGradientColors(
      int topOuterStrokeColor,
      int topInnerStrokeColor,
      int bottomOuterStrokeColor,
      int bottomInnerStrokeColor) {
    mTopOuterStrokeColor = topOuterStrokeColor;
    mTopInnerStrokeColor = topInnerStrokeColor;
    mBottomOuterStrokeColor = bottomOuterStrokeColor;
    mBottomInnerStrokeColor = bottomInnerStrokeColor;
  }

  /** Set the border width */
  void setBorderWidth(float width) {
    if (mBorderWidth != width) {
      mBorderWidth = width;
      mPaint.setStrokeWidth(width * DRAW_STROKE_WIDTH_MULTIPLE);
      mInvalidateShader = true;
      invalidateSelf();
    }
  }

  @Override
  public void draw(Canvas canvas) {
    if (mInvalidateShader) {
      mPaint.setShader(createGradientShader());
      mInvalidateShader = false;
    }

    final float halfBorderWidth = mPaint.getStrokeWidth() / 2f;
    final RectF rectF = mRectF;

    // We need to inset the oval bounds by half the border width. This is because stroke draws
    // the center of the border on the dimension. Whereas we want the stroke on the inside.
    copyBounds(mRect);
    rectF.set(mRect);
    rectF.left += halfBorderWidth;
    rectF.top += halfBorderWidth;
    rectF.right -= halfBorderWidth;
    rectF.bottom -= halfBorderWidth;

    canvas.save();
    canvas.rotate(mRotation, rectF.centerX(), rectF.centerY());
    // Draw the oval
    canvas.drawOval(rectF, mPaint);
    canvas.restore();
  }

  @Override
  public boolean getPadding(Rect padding) {
    final int borderWidth = Math.round(mBorderWidth);
    padding.set(borderWidth, borderWidth, borderWidth, borderWidth);
    return true;
  }

  @Override
  public void setAlpha(int alpha) {
    mPaint.setAlpha(alpha);
    invalidateSelf();
  }

  void setBorderTint(ColorStateList tint) {
    if (tint != null) {
      mCurrentBorderTintColor = tint.getColorForState(getState(), mCurrentBorderTintColor);
    }
    mBorderTint = tint;
    mInvalidateShader = true;
    invalidateSelf();
  }

  @Override
  public void setColorFilter(ColorFilter colorFilter) {
    mPaint.setColorFilter(colorFilter);
    invalidateSelf();
  }

  @Override
  public int getOpacity() {
    return mBorderWidth > 0 ? PixelFormat.TRANSLUCENT : PixelFormat.TRANSPARENT;
  }

  final void setRotation(float rotation) {
    if (rotation != mRotation) {
      mRotation = rotation;
      invalidateSelf();
    }
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    mInvalidateShader = true;
  }

  @Override
  public boolean isStateful() {
    return (mBorderTint != null && mBorderTint.isStateful()) || super.isStateful();
  }

  @Override
  protected boolean onStateChange(int[] state) {
    if (mBorderTint != null) {
      final int newColor = mBorderTint.getColorForState(state, mCurrentBorderTintColor);
      if (newColor != mCurrentBorderTintColor) {
        mInvalidateShader = true;
        mCurrentBorderTintColor = newColor;
      }
    }
    if (mInvalidateShader) {
      invalidateSelf();
    }
    return mInvalidateShader;
  }

  /**
   * Creates a vertical {@link LinearGradient}
   *
   * @return
   */
  private Shader createGradientShader() {
    final Rect rect = mRect;
    copyBounds(rect);

    final float borderRatio = mBorderWidth / rect.height();

    final int[] colors = new int[6];
    colors[0] = ColorUtils.compositeColors(mTopOuterStrokeColor, mCurrentBorderTintColor);
    colors[1] = ColorUtils.compositeColors(mTopInnerStrokeColor, mCurrentBorderTintColor);
    colors[2] =
        ColorUtils.compositeColors(
            ColorUtils.setAlphaComponent(mTopInnerStrokeColor, 0), mCurrentBorderTintColor);
    colors[3] =
        ColorUtils.compositeColors(
            ColorUtils.setAlphaComponent(mBottomInnerStrokeColor, 0), mCurrentBorderTintColor);
    colors[4] = ColorUtils.compositeColors(mBottomInnerStrokeColor, mCurrentBorderTintColor);
    colors[5] = ColorUtils.compositeColors(mBottomOuterStrokeColor, mCurrentBorderTintColor);

    final float[] positions = new float[6];
    positions[0] = 0f;
    positions[1] = borderRatio;
    positions[2] = 0.5f;
    positions[3] = 0.5f;
    positions[4] = 1f - borderRatio;
    positions[5] = 1f;

    return new LinearGradient(
        0, rect.top, 0, rect.bottom, colors, positions, Shader.TileMode.CLAMP);
  }
}
