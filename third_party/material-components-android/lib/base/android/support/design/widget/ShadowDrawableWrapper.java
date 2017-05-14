/*
 * Copyright (C) 2014 The Android Open Source Project
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

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.support.design.R;
import android.support.v4.content.ContextCompat;
import android.support.v7.graphics.drawable.DrawableWrapper;

/**
 * A {@link android.graphics.drawable.Drawable} which wraps another drawable and draws a shadow
 * around it.
 */
class ShadowDrawableWrapper extends DrawableWrapper {
  // used to calculate content padding
  static final double COS_45 = Math.cos(Math.toRadians(45));

  static final float SHADOW_MULTIPLIER = 1.5f;

  static final float SHADOW_TOP_SCALE = 0.25f;
  static final float SHADOW_HORIZ_SCALE = 0.5f;
  static final float SHADOW_BOTTOM_SCALE = 1f;

  final Paint mCornerShadowPaint;
  final Paint mEdgeShadowPaint;

  final RectF mContentBounds;

  float mCornerRadius;

  Path mCornerShadowPath;

  // updated value with inset
  float mMaxShadowSize;
  // actual value set by developer
  float mRawMaxShadowSize;

  // multiplied value to account for shadow offset
  float mShadowSize;
  // actual value set by developer
  float mRawShadowSize;

  private boolean mDirty = true;

  private final int mShadowStartColor;
  private final int mShadowMiddleColor;
  private final int mShadowEndColor;

  private boolean mAddPaddingForCorners = true;

  private float mRotation;

  /** If shadow size is set to a value above max shadow, we print a warning */
  private boolean mPrintedShadowClipWarning = false;

  public ShadowDrawableWrapper(
      Context context, Drawable content, float radius, float shadowSize, float maxShadowSize) {
    super(content);

    mShadowStartColor = ContextCompat.getColor(context, R.color.design_fab_shadow_start_color);
    mShadowMiddleColor = ContextCompat.getColor(context, R.color.design_fab_shadow_mid_color);
    mShadowEndColor = ContextCompat.getColor(context, R.color.design_fab_shadow_end_color);

    mCornerShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.DITHER_FLAG);
    mCornerShadowPaint.setStyle(Paint.Style.FILL);
    mCornerRadius = Math.round(radius);
    mContentBounds = new RectF();
    mEdgeShadowPaint = new Paint(mCornerShadowPaint);
    mEdgeShadowPaint.setAntiAlias(false);
    setShadowSize(shadowSize, maxShadowSize);
  }

  /** Casts the value to an even integer. */
  private static int toEven(float value) {
    int i = Math.round(value);
    return (i % 2 == 1) ? i - 1 : i;
  }

  public void setAddPaddingForCorners(boolean addPaddingForCorners) {
    mAddPaddingForCorners = addPaddingForCorners;
    invalidateSelf();
  }

  @Override
  public void setAlpha(int alpha) {
    super.setAlpha(alpha);
    mCornerShadowPaint.setAlpha(alpha);
    mEdgeShadowPaint.setAlpha(alpha);
  }

  @Override
  protected void onBoundsChange(Rect bounds) {
    mDirty = true;
  }

  void setShadowSize(float shadowSize, float maxShadowSize) {
    if (shadowSize < 0 || maxShadowSize < 0) {
      throw new IllegalArgumentException("invalid shadow size");
    }
    shadowSize = toEven(shadowSize);
    maxShadowSize = toEven(maxShadowSize);
    if (shadowSize > maxShadowSize) {
      shadowSize = maxShadowSize;
      if (!mPrintedShadowClipWarning) {
        mPrintedShadowClipWarning = true;
      }
    }
    if (mRawShadowSize == shadowSize && mRawMaxShadowSize == maxShadowSize) {
      return;
    }
    mRawShadowSize = shadowSize;
    mRawMaxShadowSize = maxShadowSize;
    mShadowSize = Math.round(shadowSize * SHADOW_MULTIPLIER);
    mMaxShadowSize = maxShadowSize;
    mDirty = true;
    invalidateSelf();
  }

  @Override
  public boolean getPadding(Rect padding) {
    int vOffset =
        (int)
            Math.ceil(
                calculateVerticalPadding(mRawMaxShadowSize, mCornerRadius, mAddPaddingForCorners));
    int hOffset =
        (int)
            Math.ceil(
                calculateHorizontalPadding(
                    mRawMaxShadowSize, mCornerRadius, mAddPaddingForCorners));
    padding.set(hOffset, vOffset, hOffset, vOffset);
    return true;
  }

  public static float calculateVerticalPadding(
      float maxShadowSize, float cornerRadius, boolean addPaddingForCorners) {
    if (addPaddingForCorners) {
      return (float) (maxShadowSize * SHADOW_MULTIPLIER + (1 - COS_45) * cornerRadius);
    } else {
      return maxShadowSize * SHADOW_MULTIPLIER;
    }
  }

  public static float calculateHorizontalPadding(
      float maxShadowSize, float cornerRadius, boolean addPaddingForCorners) {
    if (addPaddingForCorners) {
      return (float) (maxShadowSize + (1 - COS_45) * cornerRadius);
    } else {
      return maxShadowSize;
    }
  }

  @Override
  public int getOpacity() {
    return PixelFormat.TRANSLUCENT;
  }

  public void setCornerRadius(float radius) {
    radius = Math.round(radius);
    if (mCornerRadius == radius) {
      return;
    }
    mCornerRadius = radius;
    mDirty = true;
    invalidateSelf();
  }

  @Override
  public void draw(Canvas canvas) {
    if (mDirty) {
      buildComponents(getBounds());
      mDirty = false;
    }
    drawShadow(canvas);

    super.draw(canvas);
  }

  final void setRotation(float rotation) {
    if (mRotation != rotation) {
      mRotation = rotation;
      invalidateSelf();
    }
  }

  private void drawShadow(Canvas canvas) {
    final int rotateSaved = canvas.save();
    canvas.rotate(mRotation, mContentBounds.centerX(), mContentBounds.centerY());

    final float edgeShadowTop = -mCornerRadius - mShadowSize;
    final float shadowOffset = mCornerRadius;
    final boolean drawHorizontalEdges = mContentBounds.width() - 2 * shadowOffset > 0;
    final boolean drawVerticalEdges = mContentBounds.height() - 2 * shadowOffset > 0;

    final float shadowOffsetTop = mRawShadowSize - (mRawShadowSize * SHADOW_TOP_SCALE);
    final float shadowOffsetHorizontal = mRawShadowSize - (mRawShadowSize * SHADOW_HORIZ_SCALE);
    final float shadowOffsetBottom = mRawShadowSize - (mRawShadowSize * SHADOW_BOTTOM_SCALE);

    final float shadowScaleHorizontal = shadowOffset / (shadowOffset + shadowOffsetHorizontal);
    final float shadowScaleTop = shadowOffset / (shadowOffset + shadowOffsetTop);
    final float shadowScaleBottom = shadowOffset / (shadowOffset + shadowOffsetBottom);

    // LT
    int saved = canvas.save();
    canvas.translate(mContentBounds.left + shadowOffset, mContentBounds.top + shadowOffset);
    canvas.scale(shadowScaleHorizontal, shadowScaleTop);
    canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
    if (drawHorizontalEdges) {
      // TE
      canvas.scale(1f / shadowScaleHorizontal, 1f);
      canvas.drawRect(
          0,
          edgeShadowTop,
          mContentBounds.width() - 2 * shadowOffset,
          -mCornerRadius,
          mEdgeShadowPaint);
    }
    canvas.restoreToCount(saved);
    // RB
    saved = canvas.save();
    canvas.translate(mContentBounds.right - shadowOffset, mContentBounds.bottom - shadowOffset);
    canvas.scale(shadowScaleHorizontal, shadowScaleBottom);
    canvas.rotate(180f);
    canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
    if (drawHorizontalEdges) {
      // BE
      canvas.scale(1f / shadowScaleHorizontal, 1f);
      canvas.drawRect(
          0,
          edgeShadowTop,
          mContentBounds.width() - 2 * shadowOffset,
          -mCornerRadius + mShadowSize,
          mEdgeShadowPaint);
    }
    canvas.restoreToCount(saved);
    // LB
    saved = canvas.save();
    canvas.translate(mContentBounds.left + shadowOffset, mContentBounds.bottom - shadowOffset);
    canvas.scale(shadowScaleHorizontal, shadowScaleBottom);
    canvas.rotate(270f);
    canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
    if (drawVerticalEdges) {
      // LE
      canvas.scale(1f / shadowScaleBottom, 1f);
      canvas.drawRect(
          0,
          edgeShadowTop,
          mContentBounds.height() - 2 * shadowOffset,
          -mCornerRadius,
          mEdgeShadowPaint);
    }
    canvas.restoreToCount(saved);
    // RT
    saved = canvas.save();
    canvas.translate(mContentBounds.right - shadowOffset, mContentBounds.top + shadowOffset);
    canvas.scale(shadowScaleHorizontal, shadowScaleTop);
    canvas.rotate(90f);
    canvas.drawPath(mCornerShadowPath, mCornerShadowPaint);
    if (drawVerticalEdges) {
      // RE
      canvas.scale(1f / shadowScaleTop, 1f);
      canvas.drawRect(
          0,
          edgeShadowTop,
          mContentBounds.height() - 2 * shadowOffset,
          -mCornerRadius,
          mEdgeShadowPaint);
    }
    canvas.restoreToCount(saved);

    canvas.restoreToCount(rotateSaved);
  }

  private void buildShadowCorners() {
    RectF innerBounds = new RectF(-mCornerRadius, -mCornerRadius, mCornerRadius, mCornerRadius);
    RectF outerBounds = new RectF(innerBounds);
    outerBounds.inset(-mShadowSize, -mShadowSize);

    if (mCornerShadowPath == null) {
      mCornerShadowPath = new Path();
    } else {
      mCornerShadowPath.reset();
    }
    mCornerShadowPath.setFillType(Path.FillType.EVEN_ODD);
    mCornerShadowPath.moveTo(-mCornerRadius, 0);
    mCornerShadowPath.rLineTo(-mShadowSize, 0);
    // outer arc
    mCornerShadowPath.arcTo(outerBounds, 180f, 90f, false);
    // inner arc
    mCornerShadowPath.arcTo(innerBounds, 270f, -90f, false);
    mCornerShadowPath.close();

    float shadowRadius = -outerBounds.top;
    if (shadowRadius > 0f) {
      float startRatio = mCornerRadius / shadowRadius;
      float midRatio = startRatio + ((1f - startRatio) / 2f);
      mCornerShadowPaint.setShader(
          new RadialGradient(
              0,
              0,
              shadowRadius,
              new int[] {0, mShadowStartColor, mShadowMiddleColor, mShadowEndColor},
              new float[] {0f, startRatio, midRatio, 1f},
              Shader.TileMode.CLAMP));
    }

    // we offset the content shadowSize/2 pixels up to make it more realistic.
    // this is why edge shadow shader has some extra space
    // When drawing bottom edge shadow, we use that extra space.
    mEdgeShadowPaint.setShader(
        new LinearGradient(
            0,
            innerBounds.top,
            0,
            outerBounds.top,
            new int[] {mShadowStartColor, mShadowMiddleColor, mShadowEndColor},
            new float[] {0f, .5f, 1f},
            Shader.TileMode.CLAMP));
    mEdgeShadowPaint.setAntiAlias(false);
  }

  private void buildComponents(Rect bounds) {
    // Card is offset SHADOW_MULTIPLIER * maxShadowSize to account for the shadow shift.
    // We could have different top-bottom offsets to avoid extra gap above but in that case
    // center aligning Views inside the CardView would be problematic.
    final float verticalOffset = mRawMaxShadowSize * SHADOW_MULTIPLIER;
    mContentBounds.set(
        bounds.left + mRawMaxShadowSize,
        bounds.top + verticalOffset,
        bounds.right - mRawMaxShadowSize,
        bounds.bottom - verticalOffset);

    getWrappedDrawable()
        .setBounds(
            (int) mContentBounds.left,
            (int) mContentBounds.top,
            (int) mContentBounds.right,
            (int) mContentBounds.bottom);

    buildShadowCorners();
  }

  public float getCornerRadius() {
    return mCornerRadius;
  }

  public void setShadowSize(float size) {
    setShadowSize(size, mRawMaxShadowSize);
  }

  public void setMaxShadowSize(float size) {
    setShadowSize(mRawShadowSize, size);
  }

  public float getShadowSize() {
    return mRawShadowSize;
  }

  public float getMaxShadowSize() {
    return mRawMaxShadowSize;
  }

  public float getMinWidth() {
    final float content = 2 * Math.max(mRawMaxShadowSize, mCornerRadius + mRawMaxShadowSize / 2);
    return content + mRawMaxShadowSize * 2;
  }

  public float getMinHeight() {
    final float content =
        2 * Math.max(mRawMaxShadowSize, mCornerRadius + mRawMaxShadowSize * SHADOW_MULTIPLIER / 2);
    return content + (mRawMaxShadowSize * SHADOW_MULTIPLIER) * 2;
  }
}
