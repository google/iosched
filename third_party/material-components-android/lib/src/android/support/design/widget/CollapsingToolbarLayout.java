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
import static android.support.design.widget.MathUtils.constrain;
import static android.support.design.widget.ViewUtils.objectEquals;

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntDef;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;
import android.support.annotation.StyleRes;
import android.support.design.R;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * CollapsingToolbarLayout is a wrapper for {@link Toolbar} which implements a collapsing app bar.
 * It is designed to be used as a direct child of a {@link AppBarLayout}. CollapsingToolbarLayout
 * contains the following features:
 *
 * <h4>Collapsing title</h4>
 *
 * A title which is larger when the layout is fully visible but collapses and becomes smaller as the
 * layout is scrolled off screen. You can set the title to display via {@link
 * #setTitle(CharSequence)}. The title appearance can be tweaked via the {@code
 * collapsedTextAppearance} and {@code expandedTextAppearance} attributes.
 *
 * <h4>Content scrim</h4>
 *
 * A full-bleed scrim which is show or hidden when the scroll position has hit a certain threshold.
 * You can change this via {@link #setContentScrim(Drawable)}.
 *
 * <h4>Status bar scrim</h4>
 *
 * A scrim which is show or hidden behind the status bar when the scroll position has hit a certain
 * threshold. You can change this via {@link #setStatusBarScrim(Drawable)}. This only works on
 * {@link android.os.Build.VERSION_CODES#LOLLIPOP LOLLIPOP} devices when we set to fit system
 * windows.
 *
 * <h4>Parallax scrolling children</h4>
 *
 * Child views can opt to be scrolled within this layout in a parallax fashion. See {@link
 * LayoutParams#COLLAPSE_MODE_PARALLAX} and {@link LayoutParams#setParallaxMultiplier(float)}.
 *
 * <h4>Pinned position children</h4>
 *
 * Child views can opt to be pinned in space globally. This is useful when implementing a collapsing
 * as it allows the {@link Toolbar} to be fixed in place even though this layout is moving. See
 * {@link LayoutParams#COLLAPSE_MODE_PIN}.
 *
 * <p><strong>Do not manually add views to the Toolbar at run time</strong>. We will add a 'dummy
 * view' to the Toolbar which allows us to work out the available space for the title. This can
 * interfere with any views which you add.
 *
 * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_collapsedTitleTextAppearance
 * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_expandedTitleTextAppearance
 * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_contentScrim
 * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_expandedTitleMargin
 * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_expandedTitleMarginStart
 * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_expandedTitleMarginEnd
 * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_expandedTitleMarginBottom
 * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_statusBarScrim
 * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_toolbarId
 */
public class CollapsingToolbarLayout extends FrameLayout {

  private static final int DEFAULT_SCRIM_ANIMATION_DURATION = 600;

  private boolean mRefreshToolbar = true;
  private int mToolbarId;
  private Toolbar mToolbar;
  private View mToolbarDirectChild;
  private View mDummyView;

  private int mExpandedMarginStart;
  private int mExpandedMarginTop;
  private int mExpandedMarginEnd;
  private int mExpandedMarginBottom;

  private final Rect mTmpRect = new Rect();
  final CollapsingTextHelper mCollapsingTextHelper;
  private boolean mCollapsingTitleEnabled;
  private boolean mDrawCollapsingTitle;

  private Drawable mContentScrim;
  Drawable mStatusBarScrim;
  private int mScrimAlpha;
  private boolean mScrimsAreShown;
  private ValueAnimator mScrimAnimator;
  private long mScrimAnimationDuration;
  private int mScrimVisibleHeightTrigger = -1;

  private AppBarLayout.OnOffsetChangedListener mOnOffsetChangedListener;

  int mCurrentOffset;

  WindowInsetsCompat mLastInsets;

  public CollapsingToolbarLayout(Context context) {
    this(context, null);
  }

  public CollapsingToolbarLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public CollapsingToolbarLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    ThemeUtils.checkAppCompatTheme(context);

    mCollapsingTextHelper = new CollapsingTextHelper(this);
    mCollapsingTextHelper.setTextSizeInterpolator(AnimationUtils.DECELERATE_INTERPOLATOR);

    TypedArray a =
        context.obtainStyledAttributes(
            attrs,
            R.styleable.CollapsingToolbarLayout,
            defStyleAttr,
            R.style.Widget_Design_CollapsingToolbar);

    mCollapsingTextHelper.setExpandedTextGravity(
        a.getInt(
            R.styleable.CollapsingToolbarLayout_expandedTitleGravity,
            GravityCompat.START | Gravity.BOTTOM));
    mCollapsingTextHelper.setCollapsedTextGravity(
        a.getInt(
            R.styleable.CollapsingToolbarLayout_collapsedTitleGravity,
            GravityCompat.START | Gravity.CENTER_VERTICAL));

    mExpandedMarginStart =
        mExpandedMarginTop =
            mExpandedMarginEnd =
                mExpandedMarginBottom =
                    a.getDimensionPixelSize(
                        R.styleable.CollapsingToolbarLayout_expandedTitleMargin, 0);

    if (a.hasValue(R.styleable.CollapsingToolbarLayout_expandedTitleMarginStart)) {
      mExpandedMarginStart =
          a.getDimensionPixelSize(R.styleable.CollapsingToolbarLayout_expandedTitleMarginStart, 0);
    }
    if (a.hasValue(R.styleable.CollapsingToolbarLayout_expandedTitleMarginEnd)) {
      mExpandedMarginEnd =
          a.getDimensionPixelSize(R.styleable.CollapsingToolbarLayout_expandedTitleMarginEnd, 0);
    }
    if (a.hasValue(R.styleable.CollapsingToolbarLayout_expandedTitleMarginTop)) {
      mExpandedMarginTop =
          a.getDimensionPixelSize(R.styleable.CollapsingToolbarLayout_expandedTitleMarginTop, 0);
    }
    if (a.hasValue(R.styleable.CollapsingToolbarLayout_expandedTitleMarginBottom)) {
      mExpandedMarginBottom =
          a.getDimensionPixelSize(R.styleable.CollapsingToolbarLayout_expandedTitleMarginBottom, 0);
    }

    mCollapsingTitleEnabled = a.getBoolean(R.styleable.CollapsingToolbarLayout_titleEnabled, true);
    setTitle(a.getText(R.styleable.CollapsingToolbarLayout_title));

    // First load the default text appearances
    mCollapsingTextHelper.setExpandedTextAppearance(
        R.style.TextAppearance_Design_CollapsingToolbar_Expanded);
    mCollapsingTextHelper.setCollapsedTextAppearance(
        android.support.v7.appcompat.R.style.TextAppearance_AppCompat_Widget_ActionBar_Title);

    // Now overlay any custom text appearances
    if (a.hasValue(R.styleable.CollapsingToolbarLayout_expandedTitleTextAppearance)) {
      mCollapsingTextHelper.setExpandedTextAppearance(
          a.getResourceId(R.styleable.CollapsingToolbarLayout_expandedTitleTextAppearance, 0));
    }
    if (a.hasValue(R.styleable.CollapsingToolbarLayout_collapsedTitleTextAppearance)) {
      mCollapsingTextHelper.setCollapsedTextAppearance(
          a.getResourceId(R.styleable.CollapsingToolbarLayout_collapsedTitleTextAppearance, 0));
    }

    mScrimVisibleHeightTrigger =
        a.getDimensionPixelSize(R.styleable.CollapsingToolbarLayout_scrimVisibleHeightTrigger, -1);

    mScrimAnimationDuration =
        a.getInt(
            R.styleable.CollapsingToolbarLayout_scrimAnimationDuration,
            DEFAULT_SCRIM_ANIMATION_DURATION);

    setContentScrim(a.getDrawable(R.styleable.CollapsingToolbarLayout_contentScrim));
    setStatusBarScrim(a.getDrawable(R.styleable.CollapsingToolbarLayout_statusBarScrim));

    mToolbarId = a.getResourceId(R.styleable.CollapsingToolbarLayout_toolbarId, -1);

    a.recycle();

    setWillNotDraw(false);

    ViewCompat.setOnApplyWindowInsetsListener(
        this,
        new android.support.v4.view.OnApplyWindowInsetsListener() {
          @Override
          public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
            return onWindowInsetChanged(insets);
          }
        });
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    // Add an OnOffsetChangedListener if possible
    final ViewParent parent = getParent();
    if (parent instanceof AppBarLayout) {
      // Copy over from the ABL whether we should fit system windows
      ViewCompat.setFitsSystemWindows(this, ViewCompat.getFitsSystemWindows((View) parent));

      if (mOnOffsetChangedListener == null) {
        mOnOffsetChangedListener = new OffsetUpdateListener();
      }
      ((AppBarLayout) parent).addOnOffsetChangedListener(mOnOffsetChangedListener);

      // We're attached, so lets request an inset dispatch
      ViewCompat.requestApplyInsets(this);
    }
  }

  @Override
  protected void onDetachedFromWindow() {
    // Remove our OnOffsetChangedListener if possible and it exists
    final ViewParent parent = getParent();
    if (mOnOffsetChangedListener != null && parent instanceof AppBarLayout) {
      ((AppBarLayout) parent).removeOnOffsetChangedListener(mOnOffsetChangedListener);
    }

    super.onDetachedFromWindow();
  }

  WindowInsetsCompat onWindowInsetChanged(final WindowInsetsCompat insets) {
    WindowInsetsCompat newInsets = null;

    if (ViewCompat.getFitsSystemWindows(this)) {
      // If we're set to fit system windows, keep the insets
      newInsets = insets;
    }

    // If our insets have changed, keep them and invalidate the scroll ranges...
    if (!objectEquals(mLastInsets, newInsets)) {
      mLastInsets = newInsets;
      requestLayout();
    }

    // Consume the insets. This is done so that child views with fitSystemWindows=true do not
    // get the default padding functionality from View
    return insets.consumeSystemWindowInsets();
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);

    // If we don't have a toolbar, the scrim will be not be drawn in drawChild() below.
    // Instead, we draw it here, before our collapsing text.
    ensureToolbar();
    if (mToolbar == null && mContentScrim != null && mScrimAlpha > 0) {
      mContentScrim.mutate().setAlpha(mScrimAlpha);
      mContentScrim.draw(canvas);
    }

    // Let the collapsing text helper draw its text
    if (mCollapsingTitleEnabled && mDrawCollapsingTitle) {
      mCollapsingTextHelper.draw(canvas);
    }

    // Now draw the status bar scrim
    if (mStatusBarScrim != null && mScrimAlpha > 0) {
      final int topInset = mLastInsets != null ? mLastInsets.getSystemWindowInsetTop() : 0;
      if (topInset > 0) {
        mStatusBarScrim.setBounds(0, -mCurrentOffset, getWidth(), topInset - mCurrentOffset);
        mStatusBarScrim.mutate().setAlpha(mScrimAlpha);
        mStatusBarScrim.draw(canvas);
      }
    }
  }

  @Override
  protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
    // This is a little weird. Our scrim needs to be behind the Toolbar (if it is present),
    // but in front of any other children which are behind it. To do this we intercept the
    // drawChild() call, and draw our scrim just before the Toolbar is drawn
    boolean invalidated = false;
    if (mContentScrim != null && mScrimAlpha > 0 && isToolbarChild(child)) {
      mContentScrim.mutate().setAlpha(mScrimAlpha);
      mContentScrim.draw(canvas);
      invalidated = true;
    }
    return super.drawChild(canvas, child, drawingTime) || invalidated;
  }

  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    if (mContentScrim != null) {
      mContentScrim.setBounds(0, 0, w, h);
    }
  }

  private void ensureToolbar() {
    if (!mRefreshToolbar) {
      return;
    }

    // First clear out the current Toolbar
    mToolbar = null;
    mToolbarDirectChild = null;

    if (mToolbarId != -1) {
      // If we have an ID set, try and find it and it's direct parent to us
      mToolbar = (Toolbar) findViewById(mToolbarId);
      if (mToolbar != null) {
        mToolbarDirectChild = findDirectChild(mToolbar);
      }
    }

    if (mToolbar == null) {
      // If we don't have an ID, or couldn't find a Toolbar with the correct ID, try and find
      // one from our direct children
      Toolbar toolbar = null;
      for (int i = 0, count = getChildCount(); i < count; i++) {
        final View child = getChildAt(i);
        if (child instanceof Toolbar) {
          toolbar = (Toolbar) child;
          break;
        }
      }
      mToolbar = toolbar;
    }

    updateDummyView();
    mRefreshToolbar = false;
  }

  private boolean isToolbarChild(View child) {
    return (mToolbarDirectChild == null || mToolbarDirectChild == this)
        ? child == mToolbar
        : child == mToolbarDirectChild;
  }

  /** Returns the direct child of this layout, which itself is the ancestor of the given view. */
  private View findDirectChild(final View descendant) {
    View directChild = descendant;
    for (ViewParent p = descendant.getParent(); p != this && p != null; p = p.getParent()) {
      if (p instanceof View) {
        directChild = (View) p;
      }
    }
    return directChild;
  }

  private void updateDummyView() {
    if (!mCollapsingTitleEnabled && mDummyView != null) {
      // If we have a dummy view and we have our title disabled, remove it from its parent
      final ViewParent parent = mDummyView.getParent();
      if (parent instanceof ViewGroup) {
        ((ViewGroup) parent).removeView(mDummyView);
      }
    }
    if (mCollapsingTitleEnabled && mToolbar != null) {
      if (mDummyView == null) {
        mDummyView = new View(getContext());
      }
      if (mDummyView.getParent() == null) {
        mToolbar.addView(mDummyView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
      }
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    ensureToolbar();
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    final int mode = MeasureSpec.getMode(heightMeasureSpec);
    final int topInset = mLastInsets != null ? mLastInsets.getSystemWindowInsetTop() : 0;
    if (mode == MeasureSpec.UNSPECIFIED && topInset > 0) {
      // If we have a top inset and we're set to wrap_content height we need to make sure
      // we add the top inset to our height, therefore we re-measure
      heightMeasureSpec =
          MeasureSpec.makeMeasureSpec(getMeasuredHeight() + topInset, MeasureSpec.EXACTLY);
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);

    if (mLastInsets != null) {
      // Shift down any views which are not set to fit system windows
      final int insetTop = mLastInsets.getSystemWindowInsetTop();
      for (int i = 0, z = getChildCount(); i < z; i++) {
        final View child = getChildAt(i);
        if (!ViewCompat.getFitsSystemWindows(child)) {
          if (child.getTop() < insetTop) {
            // If the child isn't set to fit system windows but is drawing within
            // the inset offset it down
            ViewCompat.offsetTopAndBottom(child, insetTop);
          }
        }
      }
    }

    // Update the collapsed bounds by getting it's transformed bounds
    if (mCollapsingTitleEnabled && mDummyView != null) {
      // We only draw the title if the dummy view is being displayed (Toolbar removes
      // views if there is no space)
      mDrawCollapsingTitle =
          ViewCompat.isAttachedToWindow(mDummyView) && mDummyView.getVisibility() == VISIBLE;

      if (mDrawCollapsingTitle) {
        final boolean isRtl =
            ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL;

        // Update the collapsed bounds
        final int maxOffset =
            getMaxOffsetForPinChild(mToolbarDirectChild != null ? mToolbarDirectChild : mToolbar);
        ViewGroupUtils.getDescendantRect(this, mDummyView, mTmpRect);
        mCollapsingTextHelper.setCollapsedBounds(
            mTmpRect.left + (isRtl ? mToolbar.getTitleMarginEnd() : mToolbar.getTitleMarginStart()),
            mTmpRect.top + maxOffset + mToolbar.getTitleMarginTop(),
            mTmpRect.right
                + (isRtl ? mToolbar.getTitleMarginStart() : mToolbar.getTitleMarginEnd()),
            mTmpRect.bottom + maxOffset - mToolbar.getTitleMarginBottom());

        // Update the expanded bounds
        mCollapsingTextHelper.setExpandedBounds(
            isRtl ? mExpandedMarginEnd : mExpandedMarginStart,
            mTmpRect.top + mExpandedMarginTop,
            right - left - (isRtl ? mExpandedMarginStart : mExpandedMarginEnd),
            bottom - top - mExpandedMarginBottom);
        // Now recalculate using the new bounds
        mCollapsingTextHelper.recalculate();
      }
    }

    // Update our child view offset helpers. This needs to be done after the title has been
    // setup, so that any Toolbars are in their original position
    for (int i = 0, z = getChildCount(); i < z; i++) {
      getViewOffsetHelper(getChildAt(i)).onViewLayout();
    }

    // Finally, set our minimum height to enable proper AppBarLayout collapsing
    if (mToolbar != null) {
      if (mCollapsingTitleEnabled && TextUtils.isEmpty(mCollapsingTextHelper.getText())) {
        // If we do not currently have a title, try and grab it from the Toolbar
        mCollapsingTextHelper.setText(mToolbar.getTitle());
      }
      if (mToolbarDirectChild == null || mToolbarDirectChild == this) {
        setMinimumHeight(getHeightWithMargins(mToolbar));
      } else {
        setMinimumHeight(getHeightWithMargins(mToolbarDirectChild));
      }
    }

    updateScrimVisibility();
  }

  private static int getHeightWithMargins(@NonNull final View view) {
    final ViewGroup.LayoutParams lp = view.getLayoutParams();
    if (lp instanceof MarginLayoutParams) {
      final MarginLayoutParams mlp = (MarginLayoutParams) lp;
      return view.getHeight() + mlp.topMargin + mlp.bottomMargin;
    }
    return view.getHeight();
  }

  static ViewOffsetHelper getViewOffsetHelper(View view) {
    ViewOffsetHelper offsetHelper = (ViewOffsetHelper) view.getTag(R.id.view_offset_helper);
    if (offsetHelper == null) {
      offsetHelper = new ViewOffsetHelper(view);
      view.setTag(R.id.view_offset_helper, offsetHelper);
    }
    return offsetHelper;
  }

  /**
   * Sets the title to be displayed by this view, if enabled.
   *
   * @see #setTitleEnabled(boolean)
   * @see #getTitle()
   * @attr ref R.styleable#CollapsingToolbarLayout_title
   */
  public void setTitle(@Nullable CharSequence title) {
    mCollapsingTextHelper.setText(title);
  }

  /**
   * Returns the title currently being displayed by this view. If the title is not enabled, then
   * this will return {@code null}.
   *
   * @attr ref R.styleable#CollapsingToolbarLayout_title
   */
  @Nullable
  public CharSequence getTitle() {
    return mCollapsingTitleEnabled ? mCollapsingTextHelper.getText() : null;
  }

  /**
   * Sets whether this view should display its own title.
   *
   * <p>The title displayed by this view will shrink and grow based on the scroll offset.
   *
   * @see #setTitle(CharSequence)
   * @see #isTitleEnabled()
   * @attr ref R.styleable#CollapsingToolbarLayout_titleEnabled
   */
  public void setTitleEnabled(boolean enabled) {
    if (enabled != mCollapsingTitleEnabled) {
      mCollapsingTitleEnabled = enabled;
      updateDummyView();
      requestLayout();
    }
  }

  /**
   * Returns whether this view is currently displaying its own title.
   *
   * @see #setTitleEnabled(boolean)
   * @attr ref R.styleable#CollapsingToolbarLayout_titleEnabled
   */
  public boolean isTitleEnabled() {
    return mCollapsingTitleEnabled;
  }

  /**
   * Set whether the content scrim and/or status bar scrim should be shown or not. Any change in the
   * vertical scroll may overwrite this value. Any visibility change will be animated if this view
   * has already been laid out.
   *
   * @param shown whether the scrims should be shown
   * @see #getStatusBarScrim()
   * @see #getContentScrim()
   */
  public void setScrimsShown(boolean shown) {
    setScrimsShown(shown, ViewCompat.isLaidOut(this) && !isInEditMode());
  }

  /**
   * Set whether the content scrim and/or status bar scrim should be shown or not. Any change in the
   * vertical scroll may overwrite this value.
   *
   * @param shown whether the scrims should be shown
   * @param animate whether to animate the visibility change
   * @see #getStatusBarScrim()
   * @see #getContentScrim()
   */
  public void setScrimsShown(boolean shown, boolean animate) {
    if (mScrimsAreShown != shown) {
      if (animate) {
        animateScrim(shown ? 0xFF : 0x0);
      } else {
        setScrimAlpha(shown ? 0xFF : 0x0);
      }
      mScrimsAreShown = shown;
    }
  }

  private void animateScrim(int targetAlpha) {
    ensureToolbar();
    if (mScrimAnimator == null) {
      mScrimAnimator = new ValueAnimator();
      mScrimAnimator.setDuration(mScrimAnimationDuration);
      mScrimAnimator.setInterpolator(
          targetAlpha > mScrimAlpha
              ? AnimationUtils.FAST_OUT_LINEAR_IN_INTERPOLATOR
              : AnimationUtils.LINEAR_OUT_SLOW_IN_INTERPOLATOR);
      mScrimAnimator.addUpdateListener(
          new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
              setScrimAlpha((int) animator.getAnimatedValue());
            }
          });
    } else if (mScrimAnimator.isRunning()) {
      mScrimAnimator.cancel();
    }

    mScrimAnimator.setIntValues(mScrimAlpha, targetAlpha);
    mScrimAnimator.start();
  }

  void setScrimAlpha(int alpha) {
    if (alpha != mScrimAlpha) {
      final Drawable contentScrim = mContentScrim;
      if (contentScrim != null && mToolbar != null) {
        ViewCompat.postInvalidateOnAnimation(mToolbar);
      }
      mScrimAlpha = alpha;
      ViewCompat.postInvalidateOnAnimation(CollapsingToolbarLayout.this);
    }
  }

  int getScrimAlpha() {
    return mScrimAlpha;
  }

  /**
   * Set the drawable to use for the content scrim from resources. Providing null will disable the
   * scrim functionality.
   *
   * @param drawable the drawable to display
   * @attr ref R.styleable#CollapsingToolbarLayout_contentScrim
   * @see #getContentScrim()
   */
  public void setContentScrim(@Nullable Drawable drawable) {
    if (mContentScrim != drawable) {
      if (mContentScrim != null) {
        mContentScrim.setCallback(null);
      }
      mContentScrim = drawable != null ? drawable.mutate() : null;
      if (mContentScrim != null) {
        mContentScrim.setBounds(0, 0, getWidth(), getHeight());
        mContentScrim.setCallback(this);
        mContentScrim.setAlpha(mScrimAlpha);
      }
      ViewCompat.postInvalidateOnAnimation(this);
    }
  }

  /**
   * Set the color to use for the content scrim.
   *
   * @param color the color to display
   * @attr ref R.styleable#CollapsingToolbarLayout_contentScrim
   * @see #getContentScrim()
   */
  public void setContentScrimColor(@ColorInt int color) {
    setContentScrim(new ColorDrawable(color));
  }

  /**
   * Set the drawable to use for the content scrim from resources.
   *
   * @param resId drawable resource id
   * @attr ref R.styleable#CollapsingToolbarLayout_contentScrim
   * @see #getContentScrim()
   */
  public void setContentScrimResource(@DrawableRes int resId) {
    setContentScrim(ContextCompat.getDrawable(getContext(), resId));
  }

  /**
   * Returns the drawable which is used for the foreground scrim.
   *
   * @attr ref R.styleable#CollapsingToolbarLayout_contentScrim
   * @see #setContentScrim(Drawable)
   */
  @Nullable
  public Drawable getContentScrim() {
    return mContentScrim;
  }

  /**
   * Set the drawable to use for the status bar scrim from resources. Providing null will disable
   * the scrim functionality.
   *
   * <p>This scrim is only shown when we have been given a top system inset.
   *
   * @param drawable the drawable to display
   * @attr ref R.styleable#CollapsingToolbarLayout_statusBarScrim
   * @see #getStatusBarScrim()
   */
  public void setStatusBarScrim(@Nullable Drawable drawable) {
    if (mStatusBarScrim != drawable) {
      if (mStatusBarScrim != null) {
        mStatusBarScrim.setCallback(null);
      }
      mStatusBarScrim = drawable != null ? drawable.mutate() : null;
      if (mStatusBarScrim != null) {
        if (mStatusBarScrim.isStateful()) {
          mStatusBarScrim.setState(getDrawableState());
        }
        DrawableCompat.setLayoutDirection(mStatusBarScrim, ViewCompat.getLayoutDirection(this));
        mStatusBarScrim.setVisible(getVisibility() == VISIBLE, false);
        mStatusBarScrim.setCallback(this);
        mStatusBarScrim.setAlpha(mScrimAlpha);
      }
      ViewCompat.postInvalidateOnAnimation(this);
    }
  }

  @Override
  protected void drawableStateChanged() {
    super.drawableStateChanged();

    final int[] state = getDrawableState();
    boolean changed = false;

    Drawable d = mStatusBarScrim;
    if (d != null && d.isStateful()) {
      changed |= d.setState(state);
    }
    d = mContentScrim;
    if (d != null && d.isStateful()) {
      changed |= d.setState(state);
    }
    if (mCollapsingTextHelper != null) {
      changed |= mCollapsingTextHelper.setState(state);
    }

    if (changed) {
      invalidate();
    }
  }

  @Override
  protected boolean verifyDrawable(Drawable who) {
    return super.verifyDrawable(who) || who == mContentScrim || who == mStatusBarScrim;
  }

  @Override
  public void setVisibility(int visibility) {
    super.setVisibility(visibility);

    final boolean visible = visibility == VISIBLE;
    if (mStatusBarScrim != null && mStatusBarScrim.isVisible() != visible) {
      mStatusBarScrim.setVisible(visible, false);
    }
    if (mContentScrim != null && mContentScrim.isVisible() != visible) {
      mContentScrim.setVisible(visible, false);
    }
  }

  /**
   * Set the color to use for the status bar scrim.
   *
   * <p>This scrim is only shown when we have been given a top system inset.
   *
   * @param color the color to display
   * @attr ref R.styleable#CollapsingToolbarLayout_statusBarScrim
   * @see #getStatusBarScrim()
   */
  public void setStatusBarScrimColor(@ColorInt int color) {
    setStatusBarScrim(new ColorDrawable(color));
  }

  /**
   * Set the drawable to use for the content scrim from resources.
   *
   * @param resId drawable resource id
   * @attr ref R.styleable#CollapsingToolbarLayout_statusBarScrim
   * @see #getStatusBarScrim()
   */
  public void setStatusBarScrimResource(@DrawableRes int resId) {
    setStatusBarScrim(ContextCompat.getDrawable(getContext(), resId));
  }

  /**
   * Returns the drawable which is used for the status bar scrim.
   *
   * @attr ref R.styleable#CollapsingToolbarLayout_statusBarScrim
   * @see #setStatusBarScrim(Drawable)
   */
  @Nullable
  public Drawable getStatusBarScrim() {
    return mStatusBarScrim;
  }

  /**
   * Sets the text color and size for the collapsed title from the specified TextAppearance
   * resource.
   *
   * @attr ref
   *     android.support.design.R.styleable#CollapsingToolbarLayout_collapsedTitleTextAppearance
   */
  public void setCollapsedTitleTextAppearance(@StyleRes int resId) {
    mCollapsingTextHelper.setCollapsedTextAppearance(resId);
  }

  /**
   * Sets the text color of the collapsed title.
   *
   * @param color The new text color in ARGB format
   */
  public void setCollapsedTitleTextColor(@ColorInt int color) {
    setCollapsedTitleTextColor(ColorStateList.valueOf(color));
  }

  /**
   * Sets the text colors of the collapsed title.
   *
   * @param colors ColorStateList containing the new text colors
   */
  public void setCollapsedTitleTextColor(@NonNull ColorStateList colors) {
    mCollapsingTextHelper.setCollapsedTextColor(colors);
  }

  /**
   * Sets the horizontal alignment of the collapsed title and the vertical gravity that will be used
   * when there is extra space in the collapsed bounds beyond what is required for the title itself.
   *
   * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_collapsedTitleGravity
   */
  public void setCollapsedTitleGravity(int gravity) {
    mCollapsingTextHelper.setCollapsedTextGravity(gravity);
  }

  /**
   * Returns the horizontal and vertical alignment for title when collapsed.
   *
   * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_collapsedTitleGravity
   */
  public int getCollapsedTitleGravity() {
    return mCollapsingTextHelper.getCollapsedTextGravity();
  }

  /**
   * Sets the text color and size for the expanded title from the specified TextAppearance resource.
   *
   * @attr ref
   *     android.support.design.R.styleable#CollapsingToolbarLayout_expandedTitleTextAppearance
   */
  public void setExpandedTitleTextAppearance(@StyleRes int resId) {
    mCollapsingTextHelper.setExpandedTextAppearance(resId);
  }

  /**
   * Sets the text color of the expanded title.
   *
   * @param color The new text color in ARGB format
   */
  public void setExpandedTitleColor(@ColorInt int color) {
    setExpandedTitleTextColor(ColorStateList.valueOf(color));
  }

  /**
   * Sets the text colors of the expanded title.
   *
   * @param colors ColorStateList containing the new text colors
   */
  public void setExpandedTitleTextColor(@NonNull ColorStateList colors) {
    mCollapsingTextHelper.setExpandedTextColor(colors);
  }

  /**
   * Sets the horizontal alignment of the expanded title and the vertical gravity that will be used
   * when there is extra space in the expanded bounds beyond what is required for the title itself.
   *
   * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_expandedTitleGravity
   */
  public void setExpandedTitleGravity(int gravity) {
    mCollapsingTextHelper.setExpandedTextGravity(gravity);
  }

  /**
   * Returns the horizontal and vertical alignment for title when expanded.
   *
   * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_expandedTitleGravity
   */
  public int getExpandedTitleGravity() {
    return mCollapsingTextHelper.getExpandedTextGravity();
  }

  /**
   * Set the typeface to use for the collapsed title.
   *
   * @param typeface typeface to use, or {@code null} to use the default.
   */
  public void setCollapsedTitleTypeface(@Nullable Typeface typeface) {
    mCollapsingTextHelper.setCollapsedTypeface(typeface);
  }

  /** Returns the typeface used for the collapsed title. */
  @NonNull
  public Typeface getCollapsedTitleTypeface() {
    return mCollapsingTextHelper.getCollapsedTypeface();
  }

  /**
   * Set the typeface to use for the expanded title.
   *
   * @param typeface typeface to use, or {@code null} to use the default.
   */
  public void setExpandedTitleTypeface(@Nullable Typeface typeface) {
    mCollapsingTextHelper.setExpandedTypeface(typeface);
  }

  /** Returns the typeface used for the expanded title. */
  @NonNull
  public Typeface getExpandedTitleTypeface() {
    return mCollapsingTextHelper.getExpandedTypeface();
  }

  /**
   * Sets the expanded title margins.
   *
   * @param start the starting title margin in pixels
   * @param top the top title margin in pixels
   * @param end the ending title margin in pixels
   * @param bottom the bottom title margin in pixels
   * @see #getExpandedTitleMarginStart()
   * @see #getExpandedTitleMarginTop()
   * @see #getExpandedTitleMarginEnd()
   * @see #getExpandedTitleMarginBottom()
   * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_expandedTitleMargin
   */
  public void setExpandedTitleMargin(int start, int top, int end, int bottom) {
    mExpandedMarginStart = start;
    mExpandedMarginTop = top;
    mExpandedMarginEnd = end;
    mExpandedMarginBottom = bottom;
    requestLayout();
  }

  /**
   * @return the starting expanded title margin in pixels
   * @see #setExpandedTitleMarginStart(int)
   * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_expandedTitleMarginStart
   */
  public int getExpandedTitleMarginStart() {
    return mExpandedMarginStart;
  }

  /**
   * Sets the starting expanded title margin in pixels.
   *
   * @param margin the starting title margin in pixels
   * @see #getExpandedTitleMarginStart()
   * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_expandedTitleMarginStart
   */
  public void setExpandedTitleMarginStart(int margin) {
    mExpandedMarginStart = margin;
    requestLayout();
  }

  /**
   * @return the top expanded title margin in pixels
   * @see #setExpandedTitleMarginTop(int)
   * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_expandedTitleMarginTop
   */
  public int getExpandedTitleMarginTop() {
    return mExpandedMarginTop;
  }

  /**
   * Sets the top expanded title margin in pixels.
   *
   * @param margin the top title margin in pixels
   * @see #getExpandedTitleMarginTop()
   * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_expandedTitleMarginTop
   */
  public void setExpandedTitleMarginTop(int margin) {
    mExpandedMarginTop = margin;
    requestLayout();
  }

  /**
   * @return the ending expanded title margin in pixels
   * @see #setExpandedTitleMarginEnd(int)
   * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_expandedTitleMarginEnd
   */
  public int getExpandedTitleMarginEnd() {
    return mExpandedMarginEnd;
  }

  /**
   * Sets the ending expanded title margin in pixels.
   *
   * @param margin the ending title margin in pixels
   * @see #getExpandedTitleMarginEnd()
   * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_expandedTitleMarginEnd
   */
  public void setExpandedTitleMarginEnd(int margin) {
    mExpandedMarginEnd = margin;
    requestLayout();
  }

  /**
   * @return the bottom expanded title margin in pixels
   * @see #setExpandedTitleMarginBottom(int)
   * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_expandedTitleMarginBottom
   */
  public int getExpandedTitleMarginBottom() {
    return mExpandedMarginBottom;
  }

  /**
   * Sets the bottom expanded title margin in pixels.
   *
   * @param margin the bottom title margin in pixels
   * @see #getExpandedTitleMarginBottom()
   * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_expandedTitleMarginBottom
   */
  public void setExpandedTitleMarginBottom(int margin) {
    mExpandedMarginBottom = margin;
    requestLayout();
  }

  /**
   * Set the amount of visible height in pixels used to define when to trigger a scrim visibility
   * change.
   *
   * <p>If the visible height of this view is less than the given value, the scrims will be made
   * visible, otherwise they are hidden.
   *
   * @param height value in pixels used to define when to trigger a scrim visibility change
   * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_scrimVisibleHeightTrigger
   */
  public void setScrimVisibleHeightTrigger(@IntRange(from = 0) final int height) {
    if (mScrimVisibleHeightTrigger != height) {
      mScrimVisibleHeightTrigger = height;
      // Update the scrim visibility
      updateScrimVisibility();
    }
  }

  /**
   * Returns the amount of visible height in pixels used to define when to trigger a scrim
   * visibility change.
   *
   * @see #setScrimVisibleHeightTrigger(int)
   */
  public int getScrimVisibleHeightTrigger() {
    if (mScrimVisibleHeightTrigger >= 0) {
      // If we have one explicitly set, return it
      return mScrimVisibleHeightTrigger;
    }

    // Otherwise we'll use the default computed value
    final int insetTop = mLastInsets != null ? mLastInsets.getSystemWindowInsetTop() : 0;

    final int minHeight = ViewCompat.getMinimumHeight(this);
    if (minHeight > 0) {
      // If we have a minHeight set, lets use 2 * minHeight (capped at our height)
      return Math.min((minHeight * 2) + insetTop, getHeight());
    }

    // If we reach here then we don't have a min height set. Instead we'll take a
    // guess at 1/3 of our height being visible
    return getHeight() / 3;
  }

  /**
   * Set the duration used for scrim visibility animations.
   *
   * @param duration the duration to use in milliseconds
   * @attr ref android.support.design.R.styleable#CollapsingToolbarLayout_scrimAnimationDuration
   */
  public void setScrimAnimationDuration(@IntRange(from = 0) final long duration) {
    mScrimAnimationDuration = duration;
  }

  /** Returns the duration in milliseconds used for scrim visibility animations. */
  public long getScrimAnimationDuration() {
    return mScrimAnimationDuration;
  }

  @Override
  protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
    return p instanceof LayoutParams;
  }

  @Override
  protected LayoutParams generateDefaultLayoutParams() {
    return new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
  }

  @Override
  public FrameLayout.LayoutParams generateLayoutParams(AttributeSet attrs) {
    return new LayoutParams(getContext(), attrs);
  }

  @Override
  protected FrameLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
    return new LayoutParams(p);
  }

  public static class LayoutParams extends FrameLayout.LayoutParams {

    private static final float DEFAULT_PARALLAX_MULTIPLIER = 0.5f;

    /** @hide */
    @RestrictTo(LIBRARY_GROUP)
    @IntDef({COLLAPSE_MODE_OFF, COLLAPSE_MODE_PIN, COLLAPSE_MODE_PARALLAX})
    @Retention(RetentionPolicy.SOURCE)
    @interface CollapseMode {}

    /** The view will act as normal with no collapsing behavior. */
    public static final int COLLAPSE_MODE_OFF = 0;

    /**
     * The view will pin in place until it reaches the bottom of the {@link
     * CollapsingToolbarLayout}.
     */
    public static final int COLLAPSE_MODE_PIN = 1;

    /**
     * The view will scroll in a parallax fashion. See {@link #setParallaxMultiplier(float)} to
     * change the multiplier used.
     */
    public static final int COLLAPSE_MODE_PARALLAX = 2;

    int mCollapseMode = COLLAPSE_MODE_OFF;
    float mParallaxMult = DEFAULT_PARALLAX_MULTIPLIER;

    public LayoutParams(Context c, AttributeSet attrs) {
      super(c, attrs);

      TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.CollapsingToolbarLayout_Layout);
      mCollapseMode =
          a.getInt(
              R.styleable.CollapsingToolbarLayout_Layout_layout_collapseMode, COLLAPSE_MODE_OFF);
      setParallaxMultiplier(
          a.getFloat(
              R.styleable.CollapsingToolbarLayout_Layout_layout_collapseParallaxMultiplier,
              DEFAULT_PARALLAX_MULTIPLIER));
      a.recycle();
    }

    public LayoutParams(int width, int height) {
      super(width, height);
    }

    public LayoutParams(int width, int height, int gravity) {
      super(width, height, gravity);
    }

    public LayoutParams(ViewGroup.LayoutParams p) {
      super(p);
    }

    public LayoutParams(MarginLayoutParams source) {
      super(source);
    }

    @RequiresApi(19)
    public LayoutParams(FrameLayout.LayoutParams source) {
      // The copy constructor called here only exists on API 19+.
      super(source);
    }

    /**
     * Set the collapse mode.
     *
     * @param collapseMode one of {@link #COLLAPSE_MODE_OFF}, {@link #COLLAPSE_MODE_PIN} or {@link
     *     #COLLAPSE_MODE_PARALLAX}.
     */
    public void setCollapseMode(@CollapseMode int collapseMode) {
      mCollapseMode = collapseMode;
    }

    /**
     * Returns the requested collapse mode.
     *
     * @return the current mode. One of {@link #COLLAPSE_MODE_OFF}, {@link #COLLAPSE_MODE_PIN} or
     *     {@link #COLLAPSE_MODE_PARALLAX}.
     */
    @CollapseMode
    public int getCollapseMode() {
      return mCollapseMode;
    }

    /**
     * Set the parallax scroll multiplier used in conjunction with {@link #COLLAPSE_MODE_PARALLAX}.
     * A value of {@code 0.0} indicates no movement at all, {@code 1.0f} indicates normal scroll
     * movement.
     *
     * @param multiplier the multiplier.
     * @see #getParallaxMultiplier()
     */
    public void setParallaxMultiplier(float multiplier) {
      mParallaxMult = multiplier;
    }

    /**
     * Returns the parallax scroll multiplier used in conjunction with {@link
     * #COLLAPSE_MODE_PARALLAX}.
     *
     * @see #setParallaxMultiplier(float)
     */
    public float getParallaxMultiplier() {
      return mParallaxMult;
    }
  }

  /** Show or hide the scrims if needed */
  final void updateScrimVisibility() {
    if (mContentScrim != null || mStatusBarScrim != null) {
      setScrimsShown(getHeight() + mCurrentOffset < getScrimVisibleHeightTrigger());
    }
  }

  final int getMaxOffsetForPinChild(View child) {
    final ViewOffsetHelper offsetHelper = getViewOffsetHelper(child);
    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
    return getHeight() - offsetHelper.getLayoutTop() - child.getHeight() - lp.bottomMargin;
  }

  private class OffsetUpdateListener implements AppBarLayout.OnOffsetChangedListener {
    OffsetUpdateListener() {}

    @Override
    public void onOffsetChanged(AppBarLayout layout, int verticalOffset) {
      mCurrentOffset = verticalOffset;

      final int insetTop = mLastInsets != null ? mLastInsets.getSystemWindowInsetTop() : 0;

      for (int i = 0, z = getChildCount(); i < z; i++) {
        final View child = getChildAt(i);
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        final ViewOffsetHelper offsetHelper = getViewOffsetHelper(child);

        switch (lp.mCollapseMode) {
          case LayoutParams.COLLAPSE_MODE_PIN:
            offsetHelper.setTopAndBottomOffset(
                constrain(-verticalOffset, 0, getMaxOffsetForPinChild(child)));
            break;
          case LayoutParams.COLLAPSE_MODE_PARALLAX:
            offsetHelper.setTopAndBottomOffset(Math.round(-verticalOffset * lp.mParallaxMult));
            break;
        }
      }

      // Show or hide the scrims if needed
      updateScrimVisibility();

      if (mStatusBarScrim != null && insetTop > 0) {
        ViewCompat.postInvalidateOnAnimation(CollapsingToolbarLayout.this);
      }

      // Update the collapsing text's fraction
      final int expandRange =
          getHeight() - ViewCompat.getMinimumHeight(CollapsingToolbarLayout.this) - insetTop;
      mCollapsingTextHelper.setExpansionFraction(Math.abs(verticalOffset) / (float) expandRange);
    }
  }
}
