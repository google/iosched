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

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.design.R;
import android.support.design.widget.FloatingActionButtonImpl.InternalVisibilityChangedListener;
import android.support.v4.view.ViewCompat;
import android.support.v7.widget.AppCompatImageHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

/**
 * Floating action buttons are used for a special type of promoted action. They are distinguished by
 * a circled icon floating above the UI and have special motion behaviors related to morphing,
 * launching, and the transferring anchor point.
 *
 * <p>Floating action buttons come in two sizes: the default and the mini. The size can be
 * controlled with the {@code fabSize} attribute.
 *
 * <p>As this class descends from {@link ImageView}, you can control the icon which is displayed via
 * {@link #setImageDrawable(Drawable)}.
 *
 * <p>The background color of this view defaults to the your theme's {@code colorAccent}. If you
 * wish to change this at runtime then you can do so via {@link
 * #setBackgroundTintList(ColorStateList)}.
 */
@CoordinatorLayout.DefaultBehavior(FloatingActionButton.Behavior.class)
public class FloatingActionButton extends VisibilityAwareImageButton {

  private static final String LOG_TAG = "FloatingActionButton";

  /** Callback to be invoked when the visibility of a FloatingActionButton changes. */
  public abstract static class OnVisibilityChangedListener {
    /**
     * Called when a FloatingActionButton has been {@link #show(OnVisibilityChangedListener) shown}.
     *
     * @param fab the FloatingActionButton that was shown.
     */
    public void onShown(FloatingActionButton fab) {}

    /**
     * Called when a FloatingActionButton has been {@link #hide(OnVisibilityChangedListener)
     * hidden}.
     *
     * @param fab the FloatingActionButton that was hidden.
     */
    public void onHidden(FloatingActionButton fab) {}
  }

  // These values must match those in the attrs declaration

  /**
   * The mini sized button. Will always been smaller than {@link #SIZE_NORMAL}.
   *
   * @see #setSize(int)
   */
  public static final int SIZE_MINI = 1;

  /**
   * The normal sized button. Will always been larger than {@link #SIZE_MINI}.
   *
   * @see #setSize(int)
   */
  public static final int SIZE_NORMAL = 0;

  /**
   * Size which will change based on the window size. For small sized windows (largest screen
   * dimension < 470dp) this will select a small sized button, and for larger sized windows it will
   * select a larger size.
   *
   * @see #setSize(int)
   */
  public static final int SIZE_AUTO = -1;

  /** The switch point for the largest screen edge where SIZE_AUTO switches from mini to normal. */
  private static final int AUTO_MINI_LARGEST_SCREEN_WIDTH = 470;

  /** @hide */
  @RestrictTo(LIBRARY_GROUP)
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({SIZE_MINI, SIZE_NORMAL, SIZE_AUTO})
  public @interface Size {}

  private ColorStateList mBackgroundTint;
  private PorterDuff.Mode mBackgroundTintMode;

  private int mBorderWidth;
  private int mRippleColor;
  private int mSize;
  int mImagePadding;
  private int mMaxImageSize;

  boolean mCompatPadding;
  final Rect mShadowPadding = new Rect();
  private final Rect mTouchArea = new Rect();

  private AppCompatImageHelper mImageHelper;

  private FloatingActionButtonImpl mImpl;

  public FloatingActionButton(Context context) {
    this(context, null);
  }

  public FloatingActionButton(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public FloatingActionButton(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    ThemeUtils.checkAppCompatTheme(context);

    TypedArray a =
        context.obtainStyledAttributes(
            attrs,
            R.styleable.FloatingActionButton,
            defStyleAttr,
            R.style.Widget_Design_FloatingActionButton);
    mBackgroundTint = a.getColorStateList(R.styleable.FloatingActionButton_backgroundTint);
    mBackgroundTintMode =
        ViewUtils.parseTintMode(
            a.getInt(R.styleable.FloatingActionButton_backgroundTintMode, -1), null);
    mRippleColor = a.getColor(R.styleable.FloatingActionButton_rippleColor, 0);
    mSize = a.getInt(R.styleable.FloatingActionButton_fabSize, SIZE_AUTO);
    mBorderWidth = a.getDimensionPixelSize(R.styleable.FloatingActionButton_borderWidth, 0);
    final float elevation = a.getDimension(R.styleable.FloatingActionButton_elevation, 0f);
    final float pressedTranslationZ =
        a.getDimension(R.styleable.FloatingActionButton_pressedTranslationZ, 0f);
    mCompatPadding = a.getBoolean(R.styleable.FloatingActionButton_useCompatPadding, false);
    a.recycle();

    mImageHelper = new AppCompatImageHelper(this);
    mImageHelper.loadFromAttributes(attrs, defStyleAttr);

    mMaxImageSize = (int) getResources().getDimension(R.dimen.design_fab_image_size);

    getImpl()
        .setBackgroundDrawable(mBackgroundTint, mBackgroundTintMode, mRippleColor, mBorderWidth);
    getImpl().setElevation(elevation);
    getImpl().setPressedTranslationZ(pressedTranslationZ);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int preferredSize = getSizeDimension();

    mImagePadding = (preferredSize - mMaxImageSize) / 2;
    getImpl().updatePadding();

    final int w = resolveAdjustedSize(preferredSize, widthMeasureSpec);
    final int h = resolveAdjustedSize(preferredSize, heightMeasureSpec);

    // As we want to stay circular, we set both dimensions to be the
    // smallest resolved dimension
    final int d = Math.min(w, h);

    // We add the shadow's padding to the measured dimension
    setMeasuredDimension(
        d + mShadowPadding.left + mShadowPadding.right,
        d + mShadowPadding.top + mShadowPadding.bottom);
  }

  /**
   * Returns the ripple color for this button.
   *
   * @return the ARGB color used for the ripple
   * @see #setRippleColor(int)
   */
  @ColorInt
  public int getRippleColor() {
    return mRippleColor;
  }

  /**
   * Sets the ripple color for this button.
   *
   * <p>When running on devices with KitKat or below, we draw this color as a filled circle rather
   * than a ripple.
   *
   * @param color ARGB color to use for the ripple
   * @attr ref android.support.design.R.styleable#FloatingActionButton_rippleColor
   * @see #getRippleColor()
   */
  public void setRippleColor(@ColorInt int color) {
    if (mRippleColor != color) {
      mRippleColor = color;
      getImpl().setRippleColor(color);
    }
  }

  /**
   * Returns the tint applied to the background drawable, if specified.
   *
   * @return the tint applied to the background drawable
   * @see #setBackgroundTintList(ColorStateList)
   */
  @Nullable
  @Override
  public ColorStateList getBackgroundTintList() {
    return mBackgroundTint;
  }

  /**
   * Applies a tint to the background drawable. Does not modify the current tint mode, which is
   * {@link PorterDuff.Mode#SRC_IN} by default.
   *
   * @param tint the tint to apply, may be {@code null} to clear tint
   */
  @Override
  public void setBackgroundTintList(@Nullable ColorStateList tint) {
    if (mBackgroundTint != tint) {
      mBackgroundTint = tint;
      getImpl().setBackgroundTintList(tint);
    }
  }

  /**
   * Returns the blending mode used to apply the tint to the background drawable, if specified.
   *
   * @return the blending mode used to apply the tint to the background drawable
   * @see #setBackgroundTintMode(PorterDuff.Mode)
   */
  @Nullable
  @Override
  public PorterDuff.Mode getBackgroundTintMode() {
    return mBackgroundTintMode;
  }

  /**
   * Specifies the blending mode used to apply the tint specified by {@link
   * #setBackgroundTintList(ColorStateList)}} to the background drawable. The default mode is {@link
   * PorterDuff.Mode#SRC_IN}.
   *
   * @param tintMode the blending mode used to apply the tint, may be {@code null} to clear tint
   */
  @Override
  public void setBackgroundTintMode(@Nullable PorterDuff.Mode tintMode) {
    if (mBackgroundTintMode != tintMode) {
      mBackgroundTintMode = tintMode;
      getImpl().setBackgroundTintMode(tintMode);
    }
  }

  @Override
  public void setBackgroundDrawable(Drawable background) {
    Log.i(LOG_TAG, "Setting a custom background is not supported.");
  }

  @Override
  public void setBackgroundResource(int resid) {
    Log.i(LOG_TAG, "Setting a custom background is not supported.");
  }

  @Override
  public void setBackgroundColor(int color) {
    Log.i(LOG_TAG, "Setting a custom background is not supported.");
  }

  @Override
  public void setImageResource(@DrawableRes int resId) {
    // Intercept this call and instead retrieve the Drawable via the image helper
    mImageHelper.setImageResource(resId);
  }

  /**
   * Shows the button.
   *
   * <p>This method will animate the button show if the view has already been laid out.
   */
  public void show() {
    show(null);
  }

  /**
   * Shows the button.
   *
   * <p>This method will animate the button show if the view has already been laid out.
   *
   * @param listener the listener to notify when this view is shown
   */
  public void show(@Nullable final OnVisibilityChangedListener listener) {
    show(listener, true);
  }

  void show(OnVisibilityChangedListener listener, boolean fromUser) {
    getImpl().show(wrapOnVisibilityChangedListener(listener), fromUser);
  }

  /**
   * Hides the button.
   *
   * <p>This method will animate the button hide if the view has already been laid out.
   */
  public void hide() {
    hide(null);
  }

  /**
   * Hides the button.
   *
   * <p>This method will animate the button hide if the view has already been laid out.
   *
   * @param listener the listener to notify when this view is hidden
   */
  public void hide(@Nullable OnVisibilityChangedListener listener) {
    hide(listener, true);
  }

  void hide(@Nullable OnVisibilityChangedListener listener, boolean fromUser) {
    getImpl().hide(wrapOnVisibilityChangedListener(listener), fromUser);
  }

  /**
   * Set whether FloatingActionButton should add inner padding on platforms Lollipop and after, to
   * ensure consistent dimensions on all platforms.
   *
   * @param useCompatPadding true if FloatingActionButton is adding inner padding on platforms
   *     Lollipop and after, to ensure consistent dimensions on all platforms.
   * @attr ref android.support.design.R.styleable#FloatingActionButton_useCompatPadding
   * @see #getUseCompatPadding()
   */
  public void setUseCompatPadding(boolean useCompatPadding) {
    if (mCompatPadding != useCompatPadding) {
      mCompatPadding = useCompatPadding;
      getImpl().onCompatShadowChanged();
    }
  }

  /**
   * Returns whether FloatingActionButton will add inner padding on platforms Lollipop and after.
   *
   * @return true if FloatingActionButton is adding inner padding on platforms Lollipop and after,
   *     to ensure consistent dimensions on all platforms.
   * @attr ref android.support.design.R.styleable#FloatingActionButton_useCompatPadding
   * @see #setUseCompatPadding(boolean)
   */
  public boolean getUseCompatPadding() {
    return mCompatPadding;
  }

  /**
   * Sets the size of the button.
   *
   * <p>The options relate to the options available on the material design specification. {@link
   * #SIZE_NORMAL} is larger than {@link #SIZE_MINI}. {@link #SIZE_AUTO} will choose an appropriate
   * size based on the screen size.
   *
   * @param size one of {@link #SIZE_NORMAL}, {@link #SIZE_MINI} or {@link #SIZE_AUTO}
   * @attr ref android.support.design.R.styleable#FloatingActionButton_fabSize
   */
  public void setSize(@Size int size) {
    if (size != mSize) {
      mSize = size;
      requestLayout();
    }
  }

  /**
   * Returns the chosen size for this button.
   *
   * @return one of {@link #SIZE_NORMAL}, {@link #SIZE_MINI} or {@link #SIZE_AUTO}
   * @see #setSize(int)
   */
  @Size
  public int getSize() {
    return mSize;
  }

  @Nullable
  private InternalVisibilityChangedListener wrapOnVisibilityChangedListener(
      @Nullable final OnVisibilityChangedListener listener) {
    if (listener == null) {
      return null;
    }

    return new InternalVisibilityChangedListener() {
      @Override
      public void onShown() {
        listener.onShown(FloatingActionButton.this);
      }

      @Override
      public void onHidden() {
        listener.onHidden(FloatingActionButton.this);
      }
    };
  }

  int getSizeDimension() {
    return getSizeDimension(mSize);
  }

  private int getSizeDimension(@Size final int size) {
    final Resources res = getResources();
    switch (size) {
      case SIZE_AUTO:
        // If we're set to auto, grab the size from resources and refresh
        final int width = res.getConfiguration().screenWidthDp;
        final int height = res.getConfiguration().screenHeightDp;
        return Math.max(width, height) < AUTO_MINI_LARGEST_SCREEN_WIDTH
            ? getSizeDimension(SIZE_MINI)
            : getSizeDimension(SIZE_NORMAL);
      case SIZE_MINI:
        return res.getDimensionPixelSize(R.dimen.design_fab_size_mini);
      case SIZE_NORMAL:
      default:
        return res.getDimensionPixelSize(R.dimen.design_fab_size_normal);
    }
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    getImpl().onAttachedToWindow();
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    getImpl().onDetachedFromWindow();
  }

  @Override
  protected void drawableStateChanged() {
    super.drawableStateChanged();
    getImpl().onDrawableStateChanged(getDrawableState());
  }

  @Override
  public void jumpDrawablesToCurrentState() {
    super.jumpDrawablesToCurrentState();
    getImpl().jumpDrawableToCurrentState();
  }

  /**
   * Return in {@code rect} the bounds of the actual floating action button content in view-local
   * coordinates. This is defined as anything within any visible shadow.
   *
   * @return true if this view actually has been laid out and has a content rect, else false.
   */
  public boolean getContentRect(@NonNull Rect rect) {
    if (ViewCompat.isLaidOut(this)) {
      rect.set(0, 0, getWidth(), getHeight());
      rect.left += mShadowPadding.left;
      rect.top += mShadowPadding.top;
      rect.right -= mShadowPadding.right;
      rect.bottom -= mShadowPadding.bottom;
      return true;
    } else {
      return false;
    }
  }

  /** Returns the FloatingActionButton's background, minus any compatible shadow implementation. */
  @NonNull
  public Drawable getContentBackground() {
    return getImpl().getContentBackground();
  }

  private static int resolveAdjustedSize(int desiredSize, int measureSpec) {
    int result = desiredSize;
    int specMode = MeasureSpec.getMode(measureSpec);
    int specSize = MeasureSpec.getSize(measureSpec);
    switch (specMode) {
      case MeasureSpec.UNSPECIFIED:
        // Parent says we can be as big as we want. Just don't be larger
        // than max size imposed on ourselves.
        result = desiredSize;
        break;
      case MeasureSpec.AT_MOST:
        // Parent says we can be as big as we want, up to specSize.
        // Don't be larger than specSize, and don't be larger than
        // the max size imposed on ourselves.
        result = Math.min(desiredSize, specSize);
        break;
      case MeasureSpec.EXACTLY:
        // No choice. Do what we are told.
        result = specSize;
        break;
    }
    return result;
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    switch (ev.getAction()) {
      case MotionEvent.ACTION_DOWN:
        // Skipping the gesture if it doesn't start in in the FAB 'content' area
        if (getContentRect(mTouchArea) && !mTouchArea.contains((int) ev.getX(), (int) ev.getY())) {
          return false;
        }
        break;
    }
    return super.onTouchEvent(ev);
  }

  /**
   * Behavior designed for use with {@link FloatingActionButton} instances. Its main function is to
   * move {@link FloatingActionButton} views so that any displayed {@link Snackbar}s do not cover
   * them.
   */
  public static class Behavior extends CoordinatorLayout.Behavior<FloatingActionButton> {
    private static final boolean AUTO_HIDE_DEFAULT = true;

    private Rect mTmpRect;
    private OnVisibilityChangedListener mInternalAutoHideListener;
    private boolean mAutoHideEnabled;

    public Behavior() {
      super();
      mAutoHideEnabled = AUTO_HIDE_DEFAULT;
    }

    public Behavior(Context context, AttributeSet attrs) {
      super(context, attrs);
      TypedArray a =
          context.obtainStyledAttributes(attrs, R.styleable.FloatingActionButton_Behavior_Layout);
      mAutoHideEnabled =
          a.getBoolean(
              R.styleable.FloatingActionButton_Behavior_Layout_behavior_autoHide,
              AUTO_HIDE_DEFAULT);
      a.recycle();
    }

    /**
     * Sets whether the associated FloatingActionButton automatically hides when there is not enough
     * space to be displayed. This works with {@link AppBarLayout} and {@link BottomSheetBehavior}.
     *
     * @attr ref
     *     android.support.design.R.styleable#FloatingActionButton_Behavior_Layout_behavior_autoHide
     * @param autoHide true to enable automatic hiding
     */
    public void setAutoHideEnabled(boolean autoHide) {
      mAutoHideEnabled = autoHide;
    }

    /**
     * Returns whether the associated FloatingActionButton automatically hides when there is not
     * enough space to be displayed.
     *
     * @attr ref
     *     android.support.design.R.styleable#FloatingActionButton_Behavior_Layout_behavior_autoHide
     * @return true if enabled
     */
    public boolean isAutoHideEnabled() {
      return mAutoHideEnabled;
    }

    @Override
    public void onAttachedToLayoutParams(@NonNull CoordinatorLayout.LayoutParams lp) {
      if (lp.dodgeInsetEdges == Gravity.NO_GRAVITY) {
        // If the developer hasn't set dodgeInsetEdges, lets set it to BOTTOM so that
        // we dodge any Snackbars
        lp.dodgeInsetEdges = Gravity.BOTTOM;
      }
    }

    @Override
    public boolean onDependentViewChanged(
        CoordinatorLayout parent, FloatingActionButton child, View dependency) {
      if (dependency instanceof AppBarLayout) {
        // If we're depending on an AppBarLayout we will show/hide it automatically
        // if the FAB is anchored to the AppBarLayout
        updateFabVisibilityForAppBarLayout(parent, (AppBarLayout) dependency, child);
      } else if (isBottomSheet(dependency)) {
        updateFabVisibilityForBottomSheet(dependency, child);
      }
      return false;
    }

    private static boolean isBottomSheet(@NonNull View view) {
      final ViewGroup.LayoutParams lp = view.getLayoutParams();
      if (lp instanceof CoordinatorLayout.LayoutParams) {
        return ((CoordinatorLayout.LayoutParams) lp).getBehavior() instanceof BottomSheetBehavior;
      }
      return false;
    }

    @VisibleForTesting
    void setInternalAutoHideListener(OnVisibilityChangedListener listener) {
      mInternalAutoHideListener = listener;
    }

    private boolean shouldUpdateVisibility(View dependency, FloatingActionButton child) {
      final CoordinatorLayout.LayoutParams lp =
          (CoordinatorLayout.LayoutParams) child.getLayoutParams();
      if (!mAutoHideEnabled) {
        return false;
      }

      if (lp.getAnchorId() != dependency.getId()) {
        // The anchor ID doesn't match the dependency, so we won't automatically
        // show/hide the FAB
        return false;
      }

      //noinspection RedundantIfStatement
      if (child.getUserSetVisibility() != VISIBLE) {
        // The view isn't set to be visible so skip changing its visibility
        return false;
      }

      return true;
    }

    private boolean updateFabVisibilityForAppBarLayout(
        CoordinatorLayout parent, AppBarLayout appBarLayout, FloatingActionButton child) {
      if (!shouldUpdateVisibility(appBarLayout, child)) {
        return false;
      }

      if (mTmpRect == null) {
        mTmpRect = new Rect();
      }

      // First, let's get the visible rect of the dependency
      final Rect rect = mTmpRect;
      ViewGroupUtils.getDescendantRect(parent, appBarLayout, rect);

      if (rect.bottom <= appBarLayout.getMinimumHeightForVisibleOverlappingContent()) {
        // If the anchor's bottom is below the seam, we'll animate our FAB out
        child.hide(mInternalAutoHideListener, false);
      } else {
        // Else, we'll animate our FAB back in
        child.show(mInternalAutoHideListener, false);
      }
      return true;
    }

    private boolean updateFabVisibilityForBottomSheet(
        View bottomSheet, FloatingActionButton child) {
      if (!shouldUpdateVisibility(bottomSheet, child)) {
        return false;
      }
      CoordinatorLayout.LayoutParams lp = (CoordinatorLayout.LayoutParams) child.getLayoutParams();
      if (bottomSheet.getTop() < child.getHeight() / 2 + lp.topMargin) {
        child.hide(mInternalAutoHideListener, false);
      } else {
        child.show(mInternalAutoHideListener, false);
      }
      return true;
    }

    @Override
    public boolean onLayoutChild(
        CoordinatorLayout parent, FloatingActionButton child, int layoutDirection) {
      // First, let's make sure that the visibility of the FAB is consistent
      final List<View> dependencies = parent.getDependencies(child);
      for (int i = 0, count = dependencies.size(); i < count; i++) {
        final View dependency = dependencies.get(i);
        if (dependency instanceof AppBarLayout) {
          if (updateFabVisibilityForAppBarLayout(parent, (AppBarLayout) dependency, child)) {
            break;
          }
        } else if (isBottomSheet(dependency)) {
          if (updateFabVisibilityForBottomSheet(dependency, child)) {
            break;
          }
        }
      }
      // Now let the CoordinatorLayout lay out the FAB
      parent.onLayoutChild(child, layoutDirection);
      // Now offset it if needed
      offsetIfNeeded(parent, child);
      return true;
    }

    @Override
    public boolean getInsetDodgeRect(
        @NonNull CoordinatorLayout parent,
        @NonNull FloatingActionButton child,
        @NonNull Rect rect) {
      // Since we offset so that any internal shadow padding isn't shown, we need to make
      // sure that the shadow isn't used for any dodge inset calculations
      final Rect shadowPadding = child.mShadowPadding;
      rect.set(
          child.getLeft() + shadowPadding.left,
          child.getTop() + shadowPadding.top,
          child.getRight() - shadowPadding.right,
          child.getBottom() - shadowPadding.bottom);
      return true;
    }

    /**
     * Pre-Lollipop we use padding so that the shadow has enough space to be drawn. This method
     * offsets our layout position so that we're positioned correctly if we're on one of our
     * parent's edges.
     */
    private void offsetIfNeeded(CoordinatorLayout parent, FloatingActionButton fab) {
      final Rect padding = fab.mShadowPadding;

      if (padding != null && padding.centerX() > 0 && padding.centerY() > 0) {
        final CoordinatorLayout.LayoutParams lp =
            (CoordinatorLayout.LayoutParams) fab.getLayoutParams();

        int offsetTB = 0;
        int offsetLR = 0;

        if (fab.getRight() >= parent.getWidth() - lp.rightMargin) {
          // If we're on the right edge, shift it the right
          offsetLR = padding.right;
        } else if (fab.getLeft() <= lp.leftMargin) {
          // If we're on the left edge, shift it the left
          offsetLR = -padding.left;
        }
        if (fab.getBottom() >= parent.getHeight() - lp.bottomMargin) {
          // If we're on the bottom edge, shift it down
          offsetTB = padding.bottom;
        } else if (fab.getTop() <= lp.topMargin) {
          // If we're on the top edge, shift it up
          offsetTB = -padding.top;
        }

        if (offsetTB != 0) {
          ViewCompat.offsetTopAndBottom(fab, offsetTB);
        }
        if (offsetLR != 0) {
          ViewCompat.offsetLeftAndRight(fab, offsetLR);
        }
      }
    }
  }

  /**
   * Returns the backward compatible elevation of the FloatingActionButton.
   *
   * @return the backward compatible elevation in pixels.
   * @attr ref android.support.design.R.styleable#FloatingActionButton_elevation
   * @see #setCompatElevation(float)
   */
  public float getCompatElevation() {
    return getImpl().getElevation();
  }

  /**
   * Updates the backward compatible elevation of the FloatingActionButton.
   *
   * @param elevation The backward compatible elevation in pixels.
   * @attr ref android.support.design.R.styleable#FloatingActionButton_elevation
   * @see #getCompatElevation()
   * @see #setUseCompatPadding(boolean)
   */
  public void setCompatElevation(float elevation) {
    getImpl().setElevation(elevation);
  }

  private FloatingActionButtonImpl getImpl() {
    if (mImpl == null) {
      mImpl = createImpl();
    }
    return mImpl;
  }

  private FloatingActionButtonImpl createImpl() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      return new FloatingActionButtonLollipop(this, new ShadowDelegateImpl());
    } else {
      return new FloatingActionButtonImpl(this, new ShadowDelegateImpl());
    }
  }

  private class ShadowDelegateImpl implements ShadowViewDelegate {
    ShadowDelegateImpl() {}

    @Override
    public float getRadius() {
      return getSizeDimension() / 2f;
    }

    @Override
    public void setShadowPadding(int left, int top, int right, int bottom) {
      mShadowPadding.set(left, top, right, bottom);
      setPadding(
          left + mImagePadding, top + mImagePadding, right + mImagePadding, bottom + mImagePadding);
    }

    @Override
    public void setBackgroundDrawable(Drawable background) {
      FloatingActionButton.super.setBackgroundDrawable(background);
    }

    @Override
    public boolean isCompatPaddingEnabled() {
      return mCompatPadding;
    }
  }
}
