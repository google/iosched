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
import static android.support.design.widget.ViewUtils.objectEquals;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.support.annotation.ColorInt;
import android.support.annotation.DrawableRes;
import android.support.annotation.FloatRange;
import android.support.annotation.IdRes;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.support.design.R;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.util.Pools;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.NestedScrollingParent;
import android.support.v4.view.NestedScrollingParentHelper;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.ViewTreeObserver;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CoordinatorLayout is a super-powered {@link android.widget.FrameLayout FrameLayout}.
 *
 * <p>CoordinatorLayout is intended for two primary use cases:
 *
 * <ol>
 *   <li>As a top-level application decor or chrome layout
 *   <li>As a container for a specific interaction with one or more child views
 * </ol>
 *
 * <p>By specifying {@link CoordinatorLayout.Behavior Behaviors} for child views of a
 * CoordinatorLayout you can provide many different interactions within a single parent and those
 * views can also interact with one another. View classes can specify a default behavior when used
 * as a child of a CoordinatorLayout using the {@link CoordinatorLayout.DefaultBehavior
 * DefaultBehavior} annotation.
 *
 * <p>Behaviors may be used to implement a variety of interactions and additional layout
 * modifications ranging from sliding drawers and panels to swipe-dismissable elements and buttons
 * that stick to other elements as they move and animate.
 *
 * <p>Children of a CoordinatorLayout may have an {@link
 * CoordinatorLayout.LayoutParams#setAnchorId(int) anchor}. This view id must correspond to an
 * arbitrary descendant of the CoordinatorLayout, but it may not be the anchored child itself or a
 * descendant of the anchored child. This can be used to place floating views relative to other
 * arbitrary content panes.
 *
 * <p>Children can specify {@link CoordinatorLayout.LayoutParams#insetEdge} to describe how the view
 * insets the CoordinatorLayout. Any child views which are set to dodge the same inset edges by
 * {@link CoordinatorLayout.LayoutParams#dodgeInsetEdges} will be moved appropriately so that the
 * views do not overlap.
 */
public class CoordinatorLayout extends ViewGroup implements NestedScrollingParent {
  static final String TAG = "CoordinatorLayout";
  static final String WIDGET_PACKAGE_NAME;

  static {
    final Package pkg = CoordinatorLayout.class.getPackage();
    WIDGET_PACKAGE_NAME = pkg != null ? pkg.getName() : null;
  }

  private static final int TYPE_ON_INTERCEPT = 0;
  private static final int TYPE_ON_TOUCH = 1;

  static {
    if (Build.VERSION.SDK_INT >= 21) {
      TOP_SORTED_CHILDREN_COMPARATOR = new ViewElevationComparator();
    } else {
      TOP_SORTED_CHILDREN_COMPARATOR = null;
    }
  }

  static final Class<?>[] CONSTRUCTOR_PARAMS = new Class<?>[] {Context.class, AttributeSet.class};

  static final ThreadLocal<Map<String, Constructor<Behavior>>> sConstructors = new ThreadLocal<>();

  static final int EVENT_PRE_DRAW = 0;
  static final int EVENT_NESTED_SCROLL = 1;
  static final int EVENT_VIEW_REMOVED = 2;

  /** @hide */
  @RestrictTo(LIBRARY_GROUP)
  @Retention(RetentionPolicy.SOURCE)
  @IntDef({EVENT_PRE_DRAW, EVENT_NESTED_SCROLL, EVENT_VIEW_REMOVED})
  public @interface DispatchChangeEvent {}

  static final Comparator<View> TOP_SORTED_CHILDREN_COMPARATOR;
  private static final Pools.Pool<Rect> sRectPool = new Pools.SynchronizedPool<>(12);

  @NonNull
  private static Rect acquireTempRect() {
    Rect rect = sRectPool.acquire();
    if (rect == null) {
      rect = new Rect();
    }
    return rect;
  }

  private static void releaseTempRect(@NonNull Rect rect) {
    rect.setEmpty();
    sRectPool.release(rect);
  }

  private final List<View> mDependencySortedChildren = new ArrayList<>();
  private final DirectedAcyclicGraph<View> mChildDag = new DirectedAcyclicGraph<>();

  private final List<View> mTempList1 = new ArrayList<>();
  private final List<View> mTempDependenciesList = new ArrayList<>();
  private final int[] mTempIntPair = new int[2];
  private Paint mScrimPaint;

  private boolean mDisallowInterceptReset;

  private boolean mIsAttachedToWindow;

  private int[] mKeylines;

  private View mBehaviorTouchView;
  private View mNestedScrollingDirectChild;
  private View mNestedScrollingTarget;

  private OnPreDrawListener mOnPreDrawListener;
  private boolean mNeedsPreDrawListener;

  private WindowInsetsCompat mLastInsets;
  private boolean mDrawStatusBarBackground;
  private Drawable mStatusBarBackground;

  OnHierarchyChangeListener mOnHierarchyChangeListener;
  private android.support.v4.view.OnApplyWindowInsetsListener mApplyWindowInsetsListener;

  private final NestedScrollingParentHelper mNestedScrollingParentHelper =
      new NestedScrollingParentHelper(this);

  public CoordinatorLayout(Context context) {
    this(context, null);
  }

  public CoordinatorLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public CoordinatorLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    ThemeUtils.checkAppCompatTheme(context);

    final TypedArray a =
        context.obtainStyledAttributes(
            attrs,
            R.styleable.CoordinatorLayout,
            defStyleAttr,
            R.style.Widget_Design_CoordinatorLayout);
    final int keylineArrayRes = a.getResourceId(R.styleable.CoordinatorLayout_keylines, 0);
    if (keylineArrayRes != 0) {
      final Resources res = context.getResources();
      mKeylines = res.getIntArray(keylineArrayRes);
      final float density = res.getDisplayMetrics().density;
      final int count = mKeylines.length;
      for (int i = 0; i < count; i++) {
        mKeylines[i] *= density;
      }
    }
    mStatusBarBackground = a.getDrawable(R.styleable.CoordinatorLayout_statusBarBackground);
    a.recycle();

    setupForInsets();
    super.setOnHierarchyChangeListener(new HierarchyChangeListener());
  }

  @Override
  public void setOnHierarchyChangeListener(OnHierarchyChangeListener onHierarchyChangeListener) {
    mOnHierarchyChangeListener = onHierarchyChangeListener;
  }

  @Override
  public void onAttachedToWindow() {
    super.onAttachedToWindow();
    resetTouchBehaviors();
    if (mNeedsPreDrawListener) {
      if (mOnPreDrawListener == null) {
        mOnPreDrawListener = new OnPreDrawListener();
      }
      final ViewTreeObserver vto = getViewTreeObserver();
      vto.addOnPreDrawListener(mOnPreDrawListener);
    }
    if (mLastInsets == null && ViewCompat.getFitsSystemWindows(this)) {
      // We're set to fitSystemWindows but we haven't had any insets yet...
      // We should request a new dispatch of window insets
      ViewCompat.requestApplyInsets(this);
    }
    mIsAttachedToWindow = true;
  }

  @Override
  public void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    resetTouchBehaviors();
    if (mNeedsPreDrawListener && mOnPreDrawListener != null) {
      final ViewTreeObserver vto = getViewTreeObserver();
      vto.removeOnPreDrawListener(mOnPreDrawListener);
    }
    if (mNestedScrollingTarget != null) {
      onStopNestedScroll(mNestedScrollingTarget);
    }
    mIsAttachedToWindow = false;
  }

  /**
   * Set a drawable to draw in the insets area for the status bar. Note that this will only be
   * activated if this DrawerLayout fitsSystemWindows.
   *
   * @param bg Background drawable to draw behind the status bar
   */
  public void setStatusBarBackground(@Nullable final Drawable bg) {
    if (mStatusBarBackground != bg) {
      if (mStatusBarBackground != null) {
        mStatusBarBackground.setCallback(null);
      }
      mStatusBarBackground = bg != null ? bg.mutate() : null;
      if (mStatusBarBackground != null) {
        if (mStatusBarBackground.isStateful()) {
          mStatusBarBackground.setState(getDrawableState());
        }
        DrawableCompat.setLayoutDirection(
            mStatusBarBackground, ViewCompat.getLayoutDirection(this));
        mStatusBarBackground.setVisible(getVisibility() == VISIBLE, false);
        mStatusBarBackground.setCallback(this);
      }
      ViewCompat.postInvalidateOnAnimation(this);
    }
  }

  /**
   * Gets the drawable used to draw in the insets area for the status bar.
   *
   * @return The status bar background drawable, or null if none set
   */
  @Nullable
  public Drawable getStatusBarBackground() {
    return mStatusBarBackground;
  }

  @Override
  protected void drawableStateChanged() {
    super.drawableStateChanged();

    final int[] state = getDrawableState();
    boolean changed = false;

    Drawable d = mStatusBarBackground;
    if (d != null && d.isStateful()) {
      changed |= d.setState(state);
    }

    if (changed) {
      invalidate();
    }
  }

  @Override
  protected boolean verifyDrawable(Drawable who) {
    return super.verifyDrawable(who) || who == mStatusBarBackground;
  }

  @Override
  public void setVisibility(int visibility) {
    super.setVisibility(visibility);

    final boolean visible = visibility == VISIBLE;
    if (mStatusBarBackground != null && mStatusBarBackground.isVisible() != visible) {
      mStatusBarBackground.setVisible(visible, false);
    }
  }

  /**
   * Set a drawable to draw in the insets area for the status bar. Note that this will only be
   * activated if this DrawerLayout fitsSystemWindows.
   *
   * @param resId Resource id of a background drawable to draw behind the status bar
   */
  public void setStatusBarBackgroundResource(@DrawableRes int resId) {
    setStatusBarBackground(resId != 0 ? ContextCompat.getDrawable(getContext(), resId) : null);
  }

  /**
   * Set a drawable to draw in the insets area for the status bar. Note that this will only be
   * activated if this DrawerLayout fitsSystemWindows.
   *
   * @param color Color to use as a background drawable to draw behind the status bar in 0xAARRGGBB
   *     format.
   */
  public void setStatusBarBackgroundColor(@ColorInt int color) {
    setStatusBarBackground(new ColorDrawable(color));
  }

  final WindowInsetsCompat setWindowInsets(WindowInsetsCompat insets) {
    if (!objectEquals(mLastInsets, insets)) {
      mLastInsets = insets;
      mDrawStatusBarBackground = insets != null && insets.getSystemWindowInsetTop() > 0;
      setWillNotDraw(!mDrawStatusBarBackground && getBackground() == null);

      // Now dispatch to the Behaviors
      insets = dispatchApplyWindowInsetsToBehaviors(insets);
      requestLayout();
    }
    return insets;
  }

  final WindowInsetsCompat getLastWindowInsets() {
    return mLastInsets;
  }

  /**
   * Reset all Behavior-related tracking records either to clean up or in preparation for a new
   * event stream. This should be called when attached or detached from a window, in response to an
   * UP or CANCEL event, when intercept is request-disallowed and similar cases where an event
   * stream in progress will be aborted.
   */
  private void resetTouchBehaviors() {
    if (mBehaviorTouchView != null) {
      final Behavior b = ((LayoutParams) mBehaviorTouchView.getLayoutParams()).getBehavior();
      if (b != null) {
        final long now = SystemClock.uptimeMillis();
        final MotionEvent cancelEvent =
            MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
        b.onTouchEvent(this, mBehaviorTouchView, cancelEvent);
        cancelEvent.recycle();
      }
      mBehaviorTouchView = null;
    }

    final int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      final View child = getChildAt(i);
      final LayoutParams lp = (LayoutParams) child.getLayoutParams();
      lp.resetTouchBehaviorTracking();
    }
    mDisallowInterceptReset = false;
  }

  /**
   * Populate a list with the current child views, sorted such that the topmost views in z-order are
   * at the front of the list. Useful for hit testing and event dispatch.
   */
  private void getTopSortedChildren(List<View> out) {
    out.clear();

    final boolean useCustomOrder = isChildrenDrawingOrderEnabled();
    final int childCount = getChildCount();
    for (int i = childCount - 1; i >= 0; i--) {
      final int childIndex = useCustomOrder ? getChildDrawingOrder(childCount, i) : i;
      final View child = getChildAt(childIndex);
      out.add(child);
    }

    if (TOP_SORTED_CHILDREN_COMPARATOR != null) {
      Collections.sort(out, TOP_SORTED_CHILDREN_COMPARATOR);
    }
  }

  private boolean performIntercept(MotionEvent ev, final int type) {
    boolean intercepted = false;
    boolean newBlock = false;

    MotionEvent cancelEvent = null;

    final int action = ev.getActionMasked();

    final List<View> topmostChildList = mTempList1;
    getTopSortedChildren(topmostChildList);

    // Let topmost child views inspect first
    final int childCount = topmostChildList.size();
    for (int i = 0; i < childCount; i++) {
      final View child = topmostChildList.get(i);
      final LayoutParams lp = (LayoutParams) child.getLayoutParams();
      final Behavior b = lp.getBehavior();

      if ((intercepted || newBlock) && action != MotionEvent.ACTION_DOWN) {
        // Cancel all behaviors beneath the one that intercepted.
        // If the event is "down" then we don't have anything to cancel yet.
        if (b != null) {
          if (cancelEvent == null) {
            final long now = SystemClock.uptimeMillis();
            cancelEvent = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
          }
          switch (type) {
            case TYPE_ON_INTERCEPT:
              b.onInterceptTouchEvent(this, child, cancelEvent);
              break;
            case TYPE_ON_TOUCH:
              b.onTouchEvent(this, child, cancelEvent);
              break;
          }
        }
        continue;
      }

      if (!intercepted && b != null) {
        switch (type) {
          case TYPE_ON_INTERCEPT:
            intercepted = b.onInterceptTouchEvent(this, child, ev);
            break;
          case TYPE_ON_TOUCH:
            intercepted = b.onTouchEvent(this, child, ev);
            break;
        }
        if (intercepted) {
          mBehaviorTouchView = child;
        }
      }

      // Don't keep going if we're not allowing interaction below this.
      // Setting newBlock will make sure we cancel the rest of the behaviors.
      final boolean wasBlocking = lp.didBlockInteraction();
      final boolean isBlocking = lp.isBlockingInteractionBelow(this, child);
      newBlock = isBlocking && !wasBlocking;
      if (isBlocking && !newBlock) {
        // Stop here since we don't have anything more to cancel - we already did
        // when the behavior first started blocking things below this point.
        break;
      }
    }

    topmostChildList.clear();

    return intercepted;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    MotionEvent cancelEvent = null;

    final int action = ev.getActionMasked();

    // Make sure we reset in case we had missed a previous important event.
    if (action == MotionEvent.ACTION_DOWN) {
      resetTouchBehaviors();
    }

    final boolean intercepted = performIntercept(ev, TYPE_ON_INTERCEPT);

    if (cancelEvent != null) {
      cancelEvent.recycle();
    }

    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
      resetTouchBehaviors();
    }

    return intercepted;
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    boolean handled = false;
    boolean cancelSuper = false;
    MotionEvent cancelEvent = null;

    final int action = ev.getActionMasked();

    if (mBehaviorTouchView != null || (cancelSuper = performIntercept(ev, TYPE_ON_TOUCH))) {
      // Safe since performIntercept guarantees that
      // mBehaviorTouchView != null if it returns true
      final LayoutParams lp = (LayoutParams) mBehaviorTouchView.getLayoutParams();
      final Behavior b = lp.getBehavior();
      if (b != null) {
        handled = b.onTouchEvent(this, mBehaviorTouchView, ev);
      }
    }

    // Keep the super implementation correct
    if (mBehaviorTouchView == null) {
      handled |= super.onTouchEvent(ev);
    } else if (cancelSuper) {
      if (cancelEvent == null) {
        final long now = SystemClock.uptimeMillis();
        cancelEvent = MotionEvent.obtain(now, now, MotionEvent.ACTION_CANCEL, 0.0f, 0.0f, 0);
      }
      super.onTouchEvent(cancelEvent);
    }

    if (!handled && action == MotionEvent.ACTION_DOWN) {}

    if (cancelEvent != null) {
      cancelEvent.recycle();
    }

    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
      resetTouchBehaviors();
    }

    return handled;
  }

  @Override
  public void requestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    super.requestDisallowInterceptTouchEvent(disallowIntercept);
    if (disallowIntercept && !mDisallowInterceptReset) {
      resetTouchBehaviors();
      mDisallowInterceptReset = true;
    }
  }

  private int getKeyline(int index) {
    if (mKeylines == null) {
      Log.e(TAG, "No keylines defined for " + this + " - attempted index lookup " + index);
      return 0;
    }

    if (index < 0 || index >= mKeylines.length) {
      Log.e(TAG, "Keyline index " + index + " out of range for " + this);
      return 0;
    }

    return mKeylines[index];
  }

  static Behavior parseBehavior(Context context, AttributeSet attrs, String name) {
    if (TextUtils.isEmpty(name)) {
      return null;
    }

    final String fullName;
    if (name.startsWith(".")) {
      // Relative to the app package. Prepend the app package name.
      fullName = context.getPackageName() + name;
    } else if (name.indexOf('.') >= 0) {
      // Fully qualified package name.
      fullName = name;
    } else {
      // Assume stock behavior in this package (if we have one)
      fullName =
          !TextUtils.isEmpty(WIDGET_PACKAGE_NAME) ? (WIDGET_PACKAGE_NAME + '.' + name) : name;
    }

    try {
      Map<String, Constructor<Behavior>> constructors = sConstructors.get();
      if (constructors == null) {
        constructors = new HashMap<>();
        sConstructors.set(constructors);
      }
      Constructor<Behavior> c = constructors.get(fullName);
      if (c == null) {
        final Class<Behavior> clazz =
            (Class<Behavior>) Class.forName(fullName, true, context.getClassLoader());
        c = clazz.getConstructor(CONSTRUCTOR_PARAMS);
        c.setAccessible(true);
        constructors.put(fullName, c);
      }
      return c.newInstance(context, attrs);
    } catch (Exception e) {
      throw new RuntimeException("Could not inflate Behavior subclass " + fullName, e);
    }
  }

  LayoutParams getResolvedLayoutParams(View child) {
    final LayoutParams result = (LayoutParams) child.getLayoutParams();
    if (!result.mBehaviorResolved) {
      Class<?> childClass = child.getClass();
      DefaultBehavior defaultBehavior = null;
      while (childClass != null
          && (defaultBehavior = childClass.getAnnotation(DefaultBehavior.class)) == null) {
        childClass = childClass.getSuperclass();
      }
      if (defaultBehavior != null) {
        try {
          result.setBehavior(defaultBehavior.value().newInstance());
        } catch (Exception e) {
          Log.e(
              TAG,
              "Default behavior class "
                  + defaultBehavior.value().getName()
                  + " could not be instantiated. Did you forget a default constructor?",
              e);
        }
      }
      result.mBehaviorResolved = true;
    }
    return result;
  }

  private void prepareChildren() {
    mDependencySortedChildren.clear();
    mChildDag.clear();

    for (int i = 0, count = getChildCount(); i < count; i++) {
      final View view = getChildAt(i);

      final LayoutParams lp = getResolvedLayoutParams(view);
      lp.findAnchorView(this, view);

      mChildDag.addNode(view);

      // Now iterate again over the other children, adding any dependencies to the graph
      for (int j = 0; j < count; j++) {
        if (j == i) {
          continue;
        }
        final View other = getChildAt(j);
        final LayoutParams otherLp = getResolvedLayoutParams(other);
        if (otherLp.dependsOn(this, other, view)) {
          if (!mChildDag.contains(other)) {
            // Make sure that the other node is added
            mChildDag.addNode(other);
          }
          // Now add the dependency to the graph
          mChildDag.addEdge(view, other);
        }
      }
    }

    // Finally add the sorted graph list to our list
    mDependencySortedChildren.addAll(mChildDag.getSortedList());
    // We also need to reverse the result since we want the start of the list to contain
    // Views which have no dependencies, then dependent views after that
    Collections.reverse(mDependencySortedChildren);
  }

  /**
   * Retrieve the transformed bounding rect of an arbitrary descendant view. This does not need to
   * be a direct child.
   *
   * @param descendant descendant view to reference
   * @param out rect to set to the bounds of the descendant view
   */
  void getDescendantRect(View descendant, Rect out) {
    ViewGroupUtils.getDescendantRect(this, descendant, out);
  }

  @Override
  protected int getSuggestedMinimumWidth() {
    return Math.max(super.getSuggestedMinimumWidth(), getPaddingLeft() + getPaddingRight());
  }

  @Override
  protected int getSuggestedMinimumHeight() {
    return Math.max(super.getSuggestedMinimumHeight(), getPaddingTop() + getPaddingBottom());
  }

  /**
   * Called to measure each individual child view unless a {@link CoordinatorLayout.Behavior
   * Behavior} is present. The Behavior may choose to delegate child measurement to this method.
   *
   * @param child the child to measure
   * @param parentWidthMeasureSpec the width requirements for this view
   * @param widthUsed extra space that has been used up by the parent horizontally (possibly by
   *     other children of the parent)
   * @param parentHeightMeasureSpec the height requirements for this view
   * @param heightUsed extra space that has been used up by the parent vertically (possibly by other
   *     children of the parent)
   */
  public void onMeasureChild(
      View child,
      int parentWidthMeasureSpec,
      int widthUsed,
      int parentHeightMeasureSpec,
      int heightUsed) {
    measureChildWithMargins(
        child, parentWidthMeasureSpec, widthUsed, parentHeightMeasureSpec, heightUsed);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    prepareChildren();
    ensurePreDrawListener();

    final int paddingLeft = getPaddingLeft();
    final int paddingTop = getPaddingTop();
    final int paddingRight = getPaddingRight();
    final int paddingBottom = getPaddingBottom();
    final int layoutDirection = ViewCompat.getLayoutDirection(this);
    final boolean isRtl = layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL;
    final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    final int heightSize = MeasureSpec.getSize(heightMeasureSpec);

    final int widthPadding = paddingLeft + paddingRight;
    final int heightPadding = paddingTop + paddingBottom;
    int widthUsed = getSuggestedMinimumWidth();
    int heightUsed = getSuggestedMinimumHeight();
    int childState = 0;

    final boolean applyInsets = mLastInsets != null && ViewCompat.getFitsSystemWindows(this);

    final int childCount = mDependencySortedChildren.size();
    for (int i = 0; i < childCount; i++) {
      final View child = mDependencySortedChildren.get(i);
      if (child.getVisibility() == GONE) {
        // If the child is GONE, skip...
        continue;
      }

      final LayoutParams lp = (LayoutParams) child.getLayoutParams();

      int keylineWidthUsed = 0;
      if (lp.keyline >= 0 && widthMode != MeasureSpec.UNSPECIFIED) {
        final int keylinePos = getKeyline(lp.keyline);
        final int keylineGravity =
            GravityCompat.getAbsoluteGravity(resolveKeylineGravity(lp.gravity), layoutDirection)
                & Gravity.HORIZONTAL_GRAVITY_MASK;
        if ((keylineGravity == Gravity.LEFT && !isRtl)
            || (keylineGravity == Gravity.RIGHT && isRtl)) {
          keylineWidthUsed = Math.max(0, widthSize - paddingRight - keylinePos);
        } else if ((keylineGravity == Gravity.RIGHT && !isRtl)
            || (keylineGravity == Gravity.LEFT && isRtl)) {
          keylineWidthUsed = Math.max(0, keylinePos - paddingLeft);
        }
      }

      int childWidthMeasureSpec = widthMeasureSpec;
      int childHeightMeasureSpec = heightMeasureSpec;
      if (applyInsets && !ViewCompat.getFitsSystemWindows(child)) {
        // We're set to handle insets but this child isn't, so we will measure the
        // child as if there are no insets
        final int horizInsets =
            mLastInsets.getSystemWindowInsetLeft() + mLastInsets.getSystemWindowInsetRight();
        final int vertInsets =
            mLastInsets.getSystemWindowInsetTop() + mLastInsets.getSystemWindowInsetBottom();

        childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(widthSize - horizInsets, widthMode);
        childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(heightSize - vertInsets, heightMode);
      }

      final Behavior b = lp.getBehavior();
      if (b == null
          || !b.onMeasureChild(
              this, child, childWidthMeasureSpec, keylineWidthUsed, childHeightMeasureSpec, 0)) {
        onMeasureChild(child, childWidthMeasureSpec, keylineWidthUsed, childHeightMeasureSpec, 0);
      }

      widthUsed =
          Math.max(
              widthUsed, widthPadding + child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin);

      heightUsed =
          Math.max(
              heightUsed,
              heightPadding + child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin);
      childState = View.combineMeasuredStates(childState, child.getMeasuredState());
    }

    final int width =
        View.resolveSizeAndState(
            widthUsed, widthMeasureSpec, childState & View.MEASURED_STATE_MASK);
    final int height =
        View.resolveSizeAndState(
            heightUsed, heightMeasureSpec, childState << View.MEASURED_HEIGHT_STATE_SHIFT);
    setMeasuredDimension(width, height);
  }

  private WindowInsetsCompat dispatchApplyWindowInsetsToBehaviors(WindowInsetsCompat insets) {
    if (insets.isConsumed()) {
      return insets;
    }

    for (int i = 0, z = getChildCount(); i < z; i++) {
      final View child = getChildAt(i);
      if (ViewCompat.getFitsSystemWindows(child)) {
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        final Behavior b = lp.getBehavior();

        if (b != null) {
          // If the view has a behavior, let it try first
          insets = b.onApplyWindowInsets(this, child, insets);
          if (insets.isConsumed()) {
            // If it consumed the insets, break
            break;
          }
        }
      }
    }

    return insets;
  }

  /**
   * Called to lay out each individual child view unless a {@link CoordinatorLayout.Behavior
   * Behavior} is present. The Behavior may choose to delegate child measurement to this method.
   *
   * @param child child view to lay out
   * @param layoutDirection the resolved layout direction for the CoordinatorLayout, such as {@link
   *     ViewCompat#LAYOUT_DIRECTION_LTR} or {@link ViewCompat#LAYOUT_DIRECTION_RTL}.
   */
  public void onLayoutChild(View child, int layoutDirection) {
    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
    if (lp.checkAnchorChanged()) {
      throw new IllegalStateException(
          "An anchor may not be changed after CoordinatorLayout"
              + " measurement begins before layout is complete.");
    }
    if (lp.mAnchorView != null) {
      layoutChildWithAnchor(child, lp.mAnchorView, layoutDirection);
    } else if (lp.keyline >= 0) {
      layoutChildWithKeyline(child, lp.keyline, layoutDirection);
    } else {
      layoutChild(child, layoutDirection);
    }
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    final int layoutDirection = ViewCompat.getLayoutDirection(this);
    final int childCount = mDependencySortedChildren.size();
    for (int i = 0; i < childCount; i++) {
      final View child = mDependencySortedChildren.get(i);
      if (child.getVisibility() == GONE) {
        // If the child is GONE, skip...
        continue;
      }

      final LayoutParams lp = (LayoutParams) child.getLayoutParams();
      final Behavior behavior = lp.getBehavior();

      if (behavior == null || !behavior.onLayoutChild(this, child, layoutDirection)) {
        onLayoutChild(child, layoutDirection);
      }
    }
  }

  @Override
  public void onDraw(Canvas c) {
    super.onDraw(c);
    if (mDrawStatusBarBackground && mStatusBarBackground != null) {
      final int inset = mLastInsets != null ? mLastInsets.getSystemWindowInsetTop() : 0;
      if (inset > 0) {
        mStatusBarBackground.setBounds(0, 0, getWidth(), inset);
        mStatusBarBackground.draw(c);
      }
    }
  }

  @Override
  public void setFitsSystemWindows(boolean fitSystemWindows) {
    super.setFitsSystemWindows(fitSystemWindows);
    setupForInsets();
  }

  /**
   * Mark the last known child position rect for the given child view. This will be used when
   * checking if a child view's position has changed between frames. The rect used here should be
   * one returned by {@link #getChildRect(android.view.View, boolean, android.graphics.Rect)}, with
   * translation disabled.
   *
   * @param child child view to set for
   * @param r rect to set
   */
  void recordLastChildRect(View child, Rect r) {
    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
    lp.setLastChildRect(r);
  }

  /**
   * Get the last known child rect recorded by {@link #recordLastChildRect(android.view.View,
   * android.graphics.Rect)}.
   *
   * @param child child view to retrieve from
   * @param out rect to set to the outpur values
   */
  void getLastChildRect(View child, Rect out) {
    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
    out.set(lp.getLastChildRect());
  }

  /**
   * Get the position rect for the given child. If the child has currently requested layout or has a
   * visibility of GONE.
   *
   * @param child child view to check
   * @param transform true to include transformation in the output rect, false to only account for
   *     the base position
   * @param out rect to set to the output values
   */
  void getChildRect(View child, boolean transform, Rect out) {
    if (child.isLayoutRequested() || child.getVisibility() == View.GONE) {
      out.setEmpty();
      return;
    }
    if (transform) {
      getDescendantRect(child, out);
    } else {
      out.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
    }
  }

  private void getDesiredAnchoredChildRectWithoutConstraints(
      View child,
      int layoutDirection,
      Rect anchorRect,
      Rect out,
      LayoutParams lp,
      int childWidth,
      int childHeight) {
    final int absGravity =
        GravityCompat.getAbsoluteGravity(resolveAnchoredChildGravity(lp.gravity), layoutDirection);
    final int absAnchorGravity =
        GravityCompat.getAbsoluteGravity(resolveGravity(lp.anchorGravity), layoutDirection);

    final int hgrav = absGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
    final int vgrav = absGravity & Gravity.VERTICAL_GRAVITY_MASK;
    final int anchorHgrav = absAnchorGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
    final int anchorVgrav = absAnchorGravity & Gravity.VERTICAL_GRAVITY_MASK;

    int left;
    int top;

    // Align to the anchor. This puts us in an assumed right/bottom child view gravity.
    // If this is not the case we will subtract out the appropriate portion of
    // the child size below.
    switch (anchorHgrav) {
      default:
      case Gravity.LEFT:
        left = anchorRect.left;
        break;
      case Gravity.RIGHT:
        left = anchorRect.right;
        break;
      case Gravity.CENTER_HORIZONTAL:
        left = anchorRect.left + anchorRect.width() / 2;
        break;
    }

    switch (anchorVgrav) {
      default:
      case Gravity.TOP:
        top = anchorRect.top;
        break;
      case Gravity.BOTTOM:
        top = anchorRect.bottom;
        break;
      case Gravity.CENTER_VERTICAL:
        top = anchorRect.top + anchorRect.height() / 2;
        break;
    }

    // Offset by the child view's gravity itself. The above assumed right/bottom gravity.
    switch (hgrav) {
      default:
      case Gravity.LEFT:
        left -= childWidth;
        break;
      case Gravity.RIGHT:
        // Do nothing, we're already in position.
        break;
      case Gravity.CENTER_HORIZONTAL:
        left -= childWidth / 2;
        break;
    }

    switch (vgrav) {
      default:
      case Gravity.TOP:
        top -= childHeight;
        break;
      case Gravity.BOTTOM:
        // Do nothing, we're already in position.
        break;
      case Gravity.CENTER_VERTICAL:
        top -= childHeight / 2;
        break;
    }

    out.set(left, top, left + childWidth, top + childHeight);
  }

  private void constrainChildRect(LayoutParams lp, Rect out, int childWidth, int childHeight) {
    final int width = getWidth();
    final int height = getHeight();

    // Obey margins and padding
    int left =
        Math.max(
            getPaddingLeft() + lp.leftMargin,
            Math.min(out.left, width - getPaddingRight() - childWidth - lp.rightMargin));
    int top =
        Math.max(
            getPaddingTop() + lp.topMargin,
            Math.min(out.top, height - getPaddingBottom() - childHeight - lp.bottomMargin));

    out.set(left, top, left + childWidth, top + childHeight);
  }

  /**
   * Calculate the desired child rect relative to an anchor rect, respecting both gravity and
   * anchorGravity.
   *
   * @param child child view to calculate a rect for
   * @param layoutDirection the desired layout direction for the CoordinatorLayout
   * @param anchorRect rect in CoordinatorLayout coordinates of the anchor view area
   * @param out rect to set to the output values
   */
  void getDesiredAnchoredChildRect(View child, int layoutDirection, Rect anchorRect, Rect out) {
    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
    final int childWidth = child.getMeasuredWidth();
    final int childHeight = child.getMeasuredHeight();
    getDesiredAnchoredChildRectWithoutConstraints(
        child, layoutDirection, anchorRect, out, lp, childWidth, childHeight);
    constrainChildRect(lp, out, childWidth, childHeight);
  }

  /**
   * CORE ASSUMPTION: anchor has been laid out by the time this is called for a given child view.
   *
   * @param child child to lay out
   * @param anchor view to anchor child relative to; already laid out.
   * @param layoutDirection ViewCompat constant for layout direction
   */
  private void layoutChildWithAnchor(View child, View anchor, int layoutDirection) {
    final LayoutParams lp = (LayoutParams) child.getLayoutParams();

    final Rect anchorRect = acquireTempRect();
    final Rect childRect = acquireTempRect();
    try {
      getDescendantRect(anchor, anchorRect);
      getDesiredAnchoredChildRect(child, layoutDirection, anchorRect, childRect);
      child.layout(childRect.left, childRect.top, childRect.right, childRect.bottom);
    } finally {
      releaseTempRect(anchorRect);
      releaseTempRect(childRect);
    }
  }

  /**
   * Lay out a child view with respect to a keyline.
   *
   * <p>The keyline represents a horizontal offset from the unpadded starting edge of the
   * CoordinatorLayout. The child's gravity will affect how it is positioned with respect to the
   * keyline.
   *
   * @param child child to lay out
   * @param keyline offset from the starting edge in pixels of the keyline to align with
   * @param layoutDirection ViewCompat constant for layout direction
   */
  private void layoutChildWithKeyline(View child, int keyline, int layoutDirection) {
    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
    final int absGravity =
        GravityCompat.getAbsoluteGravity(resolveKeylineGravity(lp.gravity), layoutDirection);

    final int hgrav = absGravity & Gravity.HORIZONTAL_GRAVITY_MASK;
    final int vgrav = absGravity & Gravity.VERTICAL_GRAVITY_MASK;
    final int width = getWidth();
    final int height = getHeight();
    final int childWidth = child.getMeasuredWidth();
    final int childHeight = child.getMeasuredHeight();

    if (layoutDirection == ViewCompat.LAYOUT_DIRECTION_RTL) {
      keyline = width - keyline;
    }

    int left = getKeyline(keyline) - childWidth;
    int top = 0;

    switch (hgrav) {
      default:
      case Gravity.LEFT:
        // Nothing to do.
        break;
      case Gravity.RIGHT:
        left += childWidth;
        break;
      case Gravity.CENTER_HORIZONTAL:
        left += childWidth / 2;
        break;
    }

    switch (vgrav) {
      default:
      case Gravity.TOP:
        // Do nothing, we're already in position.
        break;
      case Gravity.BOTTOM:
        top += childHeight;
        break;
      case Gravity.CENTER_VERTICAL:
        top += childHeight / 2;
        break;
    }

    // Obey margins and padding
    left =
        Math.max(
            getPaddingLeft() + lp.leftMargin,
            Math.min(left, width - getPaddingRight() - childWidth - lp.rightMargin));
    top =
        Math.max(
            getPaddingTop() + lp.topMargin,
            Math.min(top, height - getPaddingBottom() - childHeight - lp.bottomMargin));

    child.layout(left, top, left + childWidth, top + childHeight);
  }

  /**
   * Lay out a child view with no special handling. This will position the child as if it were
   * within a FrameLayout or similar simple frame.
   *
   * @param child child view to lay out
   * @param layoutDirection ViewCompat constant for the desired layout direction
   */
  private void layoutChild(View child, int layoutDirection) {
    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
    final Rect parent = acquireTempRect();
    parent.set(
        getPaddingLeft() + lp.leftMargin,
        getPaddingTop() + lp.topMargin,
        getWidth() - getPaddingRight() - lp.rightMargin,
        getHeight() - getPaddingBottom() - lp.bottomMargin);

    if (mLastInsets != null
        && ViewCompat.getFitsSystemWindows(this)
        && !ViewCompat.getFitsSystemWindows(child)) {
      // If we're set to handle insets but this child isn't, then it has been measured as
      // if there are no insets. We need to lay it out to match.
      parent.left += mLastInsets.getSystemWindowInsetLeft();
      parent.top += mLastInsets.getSystemWindowInsetTop();
      parent.right -= mLastInsets.getSystemWindowInsetRight();
      parent.bottom -= mLastInsets.getSystemWindowInsetBottom();
    }

    final Rect out = acquireTempRect();
    GravityCompat.apply(
        resolveGravity(lp.gravity),
        child.getMeasuredWidth(),
        child.getMeasuredHeight(),
        parent,
        out,
        layoutDirection);
    child.layout(out.left, out.top, out.right, out.bottom);

    releaseTempRect(parent);
    releaseTempRect(out);
  }

  /**
   * Return the given gravity value, but if either or both of the axes doesn't have any gravity
   * specified, the default value (start or top) is specified. This should be used for children that
   * are not anchored to another view or a keyline.
   */
  private static int resolveGravity(int gravity) {
    if ((gravity & Gravity.HORIZONTAL_GRAVITY_MASK) == Gravity.NO_GRAVITY) {
      gravity |= GravityCompat.START;
    }
    if ((gravity & Gravity.VERTICAL_GRAVITY_MASK) == Gravity.NO_GRAVITY) {
      gravity |= Gravity.TOP;
    }
    return gravity;
  }

  /**
   * Return the given gravity value or the default if the passed value is NO_GRAVITY. This should be
   * used for children that are positioned relative to a keyline.
   */
  private static int resolveKeylineGravity(int gravity) {
    return gravity == Gravity.NO_GRAVITY ? GravityCompat.END | Gravity.TOP : gravity;
  }

  /**
   * Return the given gravity value or the default if the passed value is NO_GRAVITY. This should be
   * used for children that are anchored to another view.
   */
  private static int resolveAnchoredChildGravity(int gravity) {
    return gravity == Gravity.NO_GRAVITY ? Gravity.CENTER : gravity;
  }

  @Override
  protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
    if (lp.mBehavior != null) {
      final float scrimAlpha = lp.mBehavior.getScrimOpacity(this, child);
      if (scrimAlpha > 0f) {
        if (mScrimPaint == null) {
          mScrimPaint = new Paint();
        }
        mScrimPaint.setColor(lp.mBehavior.getScrimColor(this, child));
        mScrimPaint.setAlpha(MathUtils.constrain(Math.round(255 * scrimAlpha), 0, 255));

        final int saved = canvas.save();
        if (child.isOpaque()) {
          // If the child is opaque, there is no need to draw behind it so we'll inverse
          // clip the canvas
          canvas.clipRect(
              child.getLeft(),
              child.getTop(),
              child.getRight(),
              child.getBottom(),
              Region.Op.DIFFERENCE);
        }
        // Now draw the rectangle for the scrim
        canvas.drawRect(
            getPaddingLeft(),
            getPaddingTop(),
            getWidth() - getPaddingRight(),
            getHeight() - getPaddingBottom(),
            mScrimPaint);
        canvas.restoreToCount(saved);
      }
    }
    return super.drawChild(canvas, child, drawingTime);
  }

  /**
   * Dispatch any dependent view changes to the relevant {@link Behavior} instances.
   *
   * <p>Usually run as part of the pre-draw step when at least one child view has a reported
   * dependency on another view. This allows CoordinatorLayout to account for layout changes and
   * animations that occur outside of the normal layout pass.
   *
   * <p>It can also be ran as part of the nested scrolling dispatch to ensure that any offsetting is
   * completed within the correct coordinate window.
   *
   * <p>The offsetting behavior implemented here does not store the computed offset in the
   * LayoutParams; instead it expects that the layout process will always reconstruct the proper
   * positioning.
   *
   * @param type the type of event which has caused this call
   */
  final void onChildViewsChanged(@DispatchChangeEvent final int type) {
    final int layoutDirection = ViewCompat.getLayoutDirection(this);
    final int childCount = mDependencySortedChildren.size();
    final Rect inset = acquireTempRect();
    final Rect drawRect = acquireTempRect();
    final Rect lastDrawRect = acquireTempRect();

    for (int i = 0; i < childCount; i++) {
      final View child = mDependencySortedChildren.get(i);
      final LayoutParams lp = (LayoutParams) child.getLayoutParams();
      if (type == EVENT_PRE_DRAW && child.getVisibility() == View.GONE) {
        // Do not try to update GONE child views in pre draw updates.
        continue;
      }

      // Check child views before for anchor
      for (int j = 0; j < i; j++) {
        final View checkChild = mDependencySortedChildren.get(j);

        if (lp.mAnchorDirectChild == checkChild) {
          offsetChildToAnchor(child, layoutDirection);
        }
      }

      // Get the current draw rect of the view
      getChildRect(child, true, drawRect);

      // Accumulate inset sizes
      if (lp.insetEdge != Gravity.NO_GRAVITY && !drawRect.isEmpty()) {
        final int absInsetEdge = GravityCompat.getAbsoluteGravity(lp.insetEdge, layoutDirection);
        switch (absInsetEdge & Gravity.VERTICAL_GRAVITY_MASK) {
          case Gravity.TOP:
            inset.top = Math.max(inset.top, drawRect.bottom);
            break;
          case Gravity.BOTTOM:
            inset.bottom = Math.max(inset.bottom, getHeight() - drawRect.top);
            break;
        }
        switch (absInsetEdge & Gravity.HORIZONTAL_GRAVITY_MASK) {
          case Gravity.LEFT:
            inset.left = Math.max(inset.left, drawRect.right);
            break;
          case Gravity.RIGHT:
            inset.right = Math.max(inset.right, getWidth() - drawRect.left);
            break;
        }
      }

      // Dodge inset edges if necessary
      if (lp.dodgeInsetEdges != Gravity.NO_GRAVITY && child.getVisibility() == View.VISIBLE) {
        offsetChildByInset(child, inset, layoutDirection);
      }

      if (type != EVENT_VIEW_REMOVED) {
        // Did it change? if not continue
        getLastChildRect(child, lastDrawRect);
        if (lastDrawRect.equals(drawRect)) {
          continue;
        }
        recordLastChildRect(child, drawRect);
      }

      // Update any behavior-dependent views for the change
      for (int j = i + 1; j < childCount; j++) {
        final View checkChild = mDependencySortedChildren.get(j);
        final LayoutParams checkLp = (LayoutParams) checkChild.getLayoutParams();
        final Behavior b = checkLp.getBehavior();

        if (b != null && b.layoutDependsOn(this, checkChild, child)) {
          if (type == EVENT_PRE_DRAW && checkLp.getChangedAfterNestedScroll()) {
            // If this is from a pre-draw and we have already been changed
            // from a nested scroll, skip the dispatch and reset the flag
            checkLp.resetChangedAfterNestedScroll();
            continue;
          }

          final boolean handled;
          switch (type) {
            case EVENT_VIEW_REMOVED:
              // EVENT_VIEW_REMOVED means that we need to dispatch
              // onDependentViewRemoved() instead
              b.onDependentViewRemoved(this, checkChild, child);
              handled = true;
              break;
            default:
              // Otherwise we dispatch onDependentViewChanged()
              handled = b.onDependentViewChanged(this, checkChild, child);
              break;
          }

          if (type == EVENT_NESTED_SCROLL) {
            // If this is from a nested scroll, set the flag so that we may skip
            // any resulting onPreDraw dispatch (if needed)
            checkLp.setChangedAfterNestedScroll(handled);
          }
        }
      }
    }

    releaseTempRect(inset);
    releaseTempRect(drawRect);
    releaseTempRect(lastDrawRect);
  }

  private void offsetChildByInset(final View child, final Rect inset, final int layoutDirection) {
    if (!ViewCompat.isLaidOut(child)) {
      // The view has not been laid out yet, so we can't obtain its bounds.
      return;
    }

    if (child.getWidth() <= 0 || child.getHeight() <= 0) {
      // Bounds are empty so there is nothing to dodge against, skip...
      return;
    }

    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
    final Behavior behavior = lp.getBehavior();
    final Rect dodgeRect = acquireTempRect();
    final Rect bounds = acquireTempRect();
    bounds.set(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());

    if (behavior != null && behavior.getInsetDodgeRect(this, child, dodgeRect)) {
      // Make sure that the rect is within the view's bounds
      if (!bounds.contains(dodgeRect)) {
        throw new IllegalArgumentException(
            "Rect should be within the child's bounds."
                + " Rect:"
                + dodgeRect.toShortString()
                + " | Bounds:"
                + bounds.toShortString());
      }
    } else {
      dodgeRect.set(bounds);
    }

    // We can release the bounds rect now
    releaseTempRect(bounds);

    if (dodgeRect.isEmpty()) {
      // Rect is empty so there is nothing to dodge against, skip...
      releaseTempRect(dodgeRect);
      return;
    }

    final int absDodgeInsetEdges =
        GravityCompat.getAbsoluteGravity(lp.dodgeInsetEdges, layoutDirection);

    boolean offsetY = false;
    if ((absDodgeInsetEdges & Gravity.TOP) == Gravity.TOP) {
      int distance = dodgeRect.top - lp.topMargin - lp.mInsetOffsetY;
      if (distance < inset.top) {
        setInsetOffsetY(child, inset.top - distance);
        offsetY = true;
      }
    }
    if ((absDodgeInsetEdges & Gravity.BOTTOM) == Gravity.BOTTOM) {
      int distance = getHeight() - dodgeRect.bottom - lp.bottomMargin + lp.mInsetOffsetY;
      if (distance < inset.bottom) {
        setInsetOffsetY(child, distance - inset.bottom);
        offsetY = true;
      }
    }
    if (!offsetY) {
      setInsetOffsetY(child, 0);
    }

    boolean offsetX = false;
    if ((absDodgeInsetEdges & Gravity.LEFT) == Gravity.LEFT) {
      int distance = dodgeRect.left - lp.leftMargin - lp.mInsetOffsetX;
      if (distance < inset.left) {
        setInsetOffsetX(child, inset.left - distance);
        offsetX = true;
      }
    }
    if ((absDodgeInsetEdges & Gravity.RIGHT) == Gravity.RIGHT) {
      int distance = getWidth() - dodgeRect.right - lp.rightMargin + lp.mInsetOffsetX;
      if (distance < inset.right) {
        setInsetOffsetX(child, distance - inset.right);
        offsetX = true;
      }
    }
    if (!offsetX) {
      setInsetOffsetX(child, 0);
    }

    releaseTempRect(dodgeRect);
  }

  private void setInsetOffsetX(View child, int offsetX) {
    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
    if (lp.mInsetOffsetX != offsetX) {
      final int dx = offsetX - lp.mInsetOffsetX;
      ViewCompat.offsetLeftAndRight(child, dx);
      lp.mInsetOffsetX = offsetX;
    }
  }

  private void setInsetOffsetY(View child, int offsetY) {
    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
    if (lp.mInsetOffsetY != offsetY) {
      final int dy = offsetY - lp.mInsetOffsetY;
      ViewCompat.offsetTopAndBottom(child, dy);
      lp.mInsetOffsetY = offsetY;
    }
  }

  /**
   * Allows the caller to manually dispatch {@link
   * Behavior#onDependentViewChanged(CoordinatorLayout, View, View)} to the associated {@link
   * Behavior} instances of views which depend on the provided {@link View}.
   *
   * <p>You should not normally need to call this method as the it will be automatically done when
   * the view has changed.
   *
   * @param view the View to find dependents of to dispatch the call.
   */
  public void dispatchDependentViewsChanged(View view) {
    final List<View> dependents = mChildDag.getIncomingEdges(view);
    if (dependents != null && !dependents.isEmpty()) {
      for (int i = 0; i < dependents.size(); i++) {
        final View child = dependents.get(i);
        CoordinatorLayout.LayoutParams lp =
            (CoordinatorLayout.LayoutParams) child.getLayoutParams();
        CoordinatorLayout.Behavior b = lp.getBehavior();
        if (b != null) {
          b.onDependentViewChanged(this, child, view);
        }
      }
    }
  }

  /**
   * Returns the list of views which the provided view depends on. Do not store this list as its
   * contents may not be valid beyond the caller.
   *
   * @param child the view to find dependencies for.
   * @return the list of views which {@code child} depends on.
   */
  @NonNull
  public List<View> getDependencies(@NonNull View child) {
    final List<View> dependencies = mChildDag.getOutgoingEdges(child);
    mTempDependenciesList.clear();
    if (dependencies != null) {
      mTempDependenciesList.addAll(dependencies);
    }
    return mTempDependenciesList;
  }

  /**
   * Returns the list of views which depend on the provided view. Do not store this list as its
   * contents may not be valid beyond the caller.
   *
   * @param child the view to find dependents of.
   * @return the list of views which depend on {@code child}.
   */
  @NonNull
  public List<View> getDependents(@NonNull View child) {
    final List<View> edges = mChildDag.getIncomingEdges(child);
    mTempDependenciesList.clear();
    if (edges != null) {
      mTempDependenciesList.addAll(edges);
    }
    return mTempDependenciesList;
  }

  @VisibleForTesting
  final List<View> getDependencySortedChildren() {
    prepareChildren();
    return Collections.unmodifiableList(mDependencySortedChildren);
  }

  /** Add or remove the pre-draw listener as necessary. */
  void ensurePreDrawListener() {
    boolean hasDependencies = false;
    final int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      final View child = getChildAt(i);
      if (hasDependencies(child)) {
        hasDependencies = true;
        break;
      }
    }

    if (hasDependencies != mNeedsPreDrawListener) {
      if (hasDependencies) {
        addPreDrawListener();
      } else {
        removePreDrawListener();
      }
    }
  }

  /** Check if the given child has any layout dependencies on other child views. */
  private boolean hasDependencies(View child) {
    return mChildDag.hasOutgoingEdges(child);
  }

  /**
   * Add the pre-draw listener if we're attached to a window and mark that we currently need it when
   * attached.
   */
  void addPreDrawListener() {
    if (mIsAttachedToWindow) {
      // Add the listener
      if (mOnPreDrawListener == null) {
        mOnPreDrawListener = new OnPreDrawListener();
      }
      final ViewTreeObserver vto = getViewTreeObserver();
      vto.addOnPreDrawListener(mOnPreDrawListener);
    }

    // Record that we need the listener regardless of whether or not we're attached.
    // We'll add the real listener when we become attached.
    mNeedsPreDrawListener = true;
  }

  /**
   * Remove the pre-draw listener if we're attached to a window and mark that we currently do not
   * need it when attached.
   */
  void removePreDrawListener() {
    if (mIsAttachedToWindow) {
      if (mOnPreDrawListener != null) {
        final ViewTreeObserver vto = getViewTreeObserver();
        vto.removeOnPreDrawListener(mOnPreDrawListener);
      }
    }
    mNeedsPreDrawListener = false;
  }

  /**
   * Adjust the child left, top, right, bottom rect to the correct anchor view position, respecting
   * gravity and anchor gravity.
   *
   * <p>Note that child translation properties are ignored in this process, allowing children to be
   * animated away from their anchor. However, if the anchor view is animated, the child will be
   * offset to match the anchor's translated position.
   */
  void offsetChildToAnchor(View child, int layoutDirection) {
    final LayoutParams lp = (LayoutParams) child.getLayoutParams();
    if (lp.mAnchorView != null) {
      final Rect anchorRect = acquireTempRect();
      final Rect childRect = acquireTempRect();
      final Rect desiredChildRect = acquireTempRect();

      getDescendantRect(lp.mAnchorView, anchorRect);
      getChildRect(child, false, childRect);

      int childWidth = child.getMeasuredWidth();
      int childHeight = child.getMeasuredHeight();
      getDesiredAnchoredChildRectWithoutConstraints(
          child, layoutDirection, anchorRect, desiredChildRect, lp, childWidth, childHeight);
      boolean changed =
          desiredChildRect.left != childRect.left || desiredChildRect.top != childRect.top;
      constrainChildRect(lp, desiredChildRect, childWidth, childHeight);

      final int dx = desiredChildRect.left - childRect.left;
      final int dy = desiredChildRect.top - childRect.top;

      if (dx != 0) {
        ViewCompat.offsetLeftAndRight(child, dx);
      }
      if (dy != 0) {
        ViewCompat.offsetTopAndBottom(child, dy);
      }

      if (changed) {
        // If we have needed to move, make sure to notify the child's Behavior
        final Behavior b = lp.getBehavior();
        if (b != null) {
          b.onDependentViewChanged(this, child, lp.mAnchorView);
        }
      }

      releaseTempRect(anchorRect);
      releaseTempRect(childRect);
      releaseTempRect(desiredChildRect);
    }
  }

  /**
   * Check if a given point in the CoordinatorLayout's coordinates are within the view bounds of the
   * given direct child view.
   *
   * @param child child view to test
   * @param x X coordinate to test, in the CoordinatorLayout's coordinate system
   * @param y Y coordinate to test, in the CoordinatorLayout's coordinate system
   * @return true if the point is within the child view's bounds, false otherwise
   */
  public boolean isPointInChildBounds(View child, int x, int y) {
    final Rect r = acquireTempRect();
    getDescendantRect(child, r);
    try {
      return r.contains(x, y);
    } finally {
      releaseTempRect(r);
    }
  }

  /**
   * Check whether two views overlap each other. The views need to be descendants of this {@link
   * CoordinatorLayout} in the view hierarchy.
   *
   * @param first first child view to test
   * @param second second child view to test
   * @return true if both views are visible and overlap each other
   */
  public boolean doViewsOverlap(View first, View second) {
    if (first.getVisibility() == VISIBLE && second.getVisibility() == VISIBLE) {
      final Rect firstRect = acquireTempRect();
      getChildRect(first, first.getParent() != this, firstRect);
      final Rect secondRect = acquireTempRect();
      getChildRect(second, second.getParent() != this, secondRect);
      try {
        return !(firstRect.left > secondRect.right
            || firstRect.top > secondRect.bottom
            || firstRect.right < secondRect.left
            || firstRect.bottom < secondRect.top);
      } finally {
        releaseTempRect(firstRect);
        releaseTempRect(secondRect);
      }
    }
    return false;
  }

  @Override
  public LayoutParams generateLayoutParams(AttributeSet attrs) {
    return new LayoutParams(getContext(), attrs);
  }

  @Override
  protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
    if (p instanceof LayoutParams) {
      return new LayoutParams((LayoutParams) p);
    } else if (p instanceof MarginLayoutParams) {
      return new LayoutParams((MarginLayoutParams) p);
    }
    return new LayoutParams(p);
  }

  @Override
  protected LayoutParams generateDefaultLayoutParams() {
    return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
  }

  @Override
  protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
    return p instanceof LayoutParams && super.checkLayoutParams(p);
  }

  @Override
  public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
    boolean handled = false;

    final int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      final View view = getChildAt(i);
      if (view.getVisibility() == View.GONE) {
        // If it's GONE, don't dispatch
        continue;
      }
      final LayoutParams lp = (LayoutParams) view.getLayoutParams();
      final Behavior viewBehavior = lp.getBehavior();
      if (viewBehavior != null) {
        final boolean accepted =
            viewBehavior.onStartNestedScroll(this, view, child, target, nestedScrollAxes);
        handled |= accepted;

        lp.acceptNestedScroll(accepted);
      } else {
        lp.acceptNestedScroll(false);
      }
    }
    return handled;
  }

  @Override
  public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
    mNestedScrollingParentHelper.onNestedScrollAccepted(child, target, nestedScrollAxes);
    mNestedScrollingDirectChild = child;
    mNestedScrollingTarget = target;

    final int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      final View view = getChildAt(i);
      final LayoutParams lp = (LayoutParams) view.getLayoutParams();
      if (!lp.isNestedScrollAccepted()) {
        continue;
      }

      final Behavior viewBehavior = lp.getBehavior();
      if (viewBehavior != null) {
        viewBehavior.onNestedScrollAccepted(this, view, child, target, nestedScrollAxes);
      }
    }
  }

  @Override
  public void onStopNestedScroll(View target) {
    mNestedScrollingParentHelper.onStopNestedScroll(target);

    final int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      final View view = getChildAt(i);
      final LayoutParams lp = (LayoutParams) view.getLayoutParams();
      if (!lp.isNestedScrollAccepted()) {
        continue;
      }

      final Behavior viewBehavior = lp.getBehavior();
      if (viewBehavior != null) {
        viewBehavior.onStopNestedScroll(this, view, target);
      }
      lp.resetNestedScroll();
      lp.resetChangedAfterNestedScroll();
    }

    mNestedScrollingDirectChild = null;
    mNestedScrollingTarget = null;
  }

  @Override
  public void onNestedScroll(
      View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed) {
    final int childCount = getChildCount();
    boolean accepted = false;

    for (int i = 0; i < childCount; i++) {
      final View view = getChildAt(i);
      if (view.getVisibility() == GONE) {
        // If the child is GONE, skip...
        continue;
      }

      final LayoutParams lp = (LayoutParams) view.getLayoutParams();
      if (!lp.isNestedScrollAccepted()) {
        continue;
      }

      final Behavior viewBehavior = lp.getBehavior();
      if (viewBehavior != null) {
        viewBehavior.onNestedScroll(
            this, view, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
        accepted = true;
      }
    }

    if (accepted) {
      onChildViewsChanged(EVENT_NESTED_SCROLL);
    }
  }

  @Override
  public void onNestedPreScroll(View target, int dx, int dy, int[] consumed) {
    int xConsumed = 0;
    int yConsumed = 0;
    boolean accepted = false;

    final int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      final View view = getChildAt(i);
      if (view.getVisibility() == GONE) {
        // If the child is GONE, skip...
        continue;
      }

      final LayoutParams lp = (LayoutParams) view.getLayoutParams();
      if (!lp.isNestedScrollAccepted()) {
        continue;
      }

      final Behavior viewBehavior = lp.getBehavior();
      if (viewBehavior != null) {
        mTempIntPair[0] = mTempIntPair[1] = 0;
        viewBehavior.onNestedPreScroll(this, view, target, dx, dy, mTempIntPair);

        xConsumed =
            dx > 0 ? Math.max(xConsumed, mTempIntPair[0]) : Math.min(xConsumed, mTempIntPair[0]);
        yConsumed =
            dy > 0 ? Math.max(yConsumed, mTempIntPair[1]) : Math.min(yConsumed, mTempIntPair[1]);

        accepted = true;
      }
    }

    consumed[0] = xConsumed;
    consumed[1] = yConsumed;

    if (accepted) {
      onChildViewsChanged(EVENT_NESTED_SCROLL);
    }
  }

  @Override
  public boolean onNestedFling(View target, float velocityX, float velocityY, boolean consumed) {
    boolean handled = false;

    final int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      final View view = getChildAt(i);
      if (view.getVisibility() == GONE) {
        // If the child is GONE, skip...
        continue;
      }

      final LayoutParams lp = (LayoutParams) view.getLayoutParams();
      if (!lp.isNestedScrollAccepted()) {
        continue;
      }

      final Behavior viewBehavior = lp.getBehavior();
      if (viewBehavior != null) {
        handled |= viewBehavior.onNestedFling(this, view, target, velocityX, velocityY, consumed);
      }
    }
    if (handled) {
      onChildViewsChanged(EVENT_NESTED_SCROLL);
    }
    return handled;
  }

  @Override
  public boolean onNestedPreFling(View target, float velocityX, float velocityY) {
    boolean handled = false;

    final int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      final View view = getChildAt(i);
      if (view.getVisibility() == GONE) {
        // If the child is GONE, skip...
        continue;
      }

      final LayoutParams lp = (LayoutParams) view.getLayoutParams();
      if (!lp.isNestedScrollAccepted()) {
        continue;
      }

      final Behavior viewBehavior = lp.getBehavior();
      if (viewBehavior != null) {
        handled |= viewBehavior.onNestedPreFling(this, view, target, velocityX, velocityY);
      }
    }
    return handled;
  }

  @Override
  public int getNestedScrollAxes() {
    return mNestedScrollingParentHelper.getNestedScrollAxes();
  }

  class OnPreDrawListener implements ViewTreeObserver.OnPreDrawListener {
    @Override
    public boolean onPreDraw() {
      onChildViewsChanged(EVENT_PRE_DRAW);
      return true;
    }
  }

  /** Sorts child views with higher Z values to the beginning of a collection. */
  static class ViewElevationComparator implements Comparator<View> {
    @Override
    public int compare(View lhs, View rhs) {
      final float lz = ViewCompat.getZ(lhs);
      final float rz = ViewCompat.getZ(rhs);
      if (lz > rz) {
        return -1;
      } else if (lz < rz) {
        return 1;
      }
      return 0;
    }
  }

  /**
   * Defines the default {@link Behavior} of a {@link View} class.
   *
   * <p>When writing a custom view, use this annotation to define the default behavior when used as
   * a direct child of an {@link CoordinatorLayout}. The default behavior can be overridden using
   * {@link LayoutParams#setBehavior}.
   *
   * <p>Example: <code>@DefaultBehavior(MyBehavior.class)</code>
   */
  @Retention(RetentionPolicy.RUNTIME)
  public @interface DefaultBehavior {
    Class<? extends Behavior> value();
  }

  /**
   * Interaction behavior plugin for child views of {@link CoordinatorLayout}.
   *
   * <p>A Behavior implements one or more interactions that a user can take on a child view. These
   * interactions may include drags, swipes, flings, or any other gestures.
   *
   * @param <V> The View type that this Behavior operates on
   */
  public abstract static class Behavior<V extends View> {

    /** Default constructor for instantiating Behaviors. */
    public Behavior() {}

    /**
     * Default constructor for inflating Behaviors from layout. The Behavior will have the
     * opportunity to parse specially defined layout parameters. These parameters will appear on the
     * child view tag.
     *
     * @param context
     * @param attrs
     */
    public Behavior(Context context, AttributeSet attrs) {}

    /**
     * Called when the Behavior has been attached to a LayoutParams instance.
     *
     * <p>This will be called after the LayoutParams has been instantiated and can be modified.
     *
     * @param params the LayoutParams instance that this Behavior has been attached to
     */
    public void onAttachedToLayoutParams(@NonNull CoordinatorLayout.LayoutParams params) {}

    /**
     * Called when the Behavior has been detached from its holding LayoutParams instance.
     *
     * <p>This will only be called if the Behavior has been explicitly removed from the LayoutParams
     * instance via {@link LayoutParams#setBehavior(Behavior)}. It will not be called if the
     * associated view is removed from the CoordinatorLayout or similar.
     */
    public void onDetachedFromLayoutParams() {}

    /**
     * Respond to CoordinatorLayout touch events before they are dispatched to child views.
     *
     * <p>Behaviors can use this to monitor inbound touch events until one decides to intercept the
     * rest of the event stream to take an action on its associated child view. This method will
     * return false until it detects the proper intercept conditions, then return true once those
     * conditions have occurred.
     *
     * <p>Once a Behavior intercepts touch events, the rest of the event stream will be sent to the
     * {@link #onTouchEvent} method.
     *
     * <p>This method will be called regardless of the visibility of the associated child of the
     * behavior. If you only wish to handle touch events when the child is visible, you should add a
     * check to {@link View#isShown()} on the given child.
     *
     * <p>The default implementation of this method always returns false.
     *
     * @param parent the parent view currently receiving this touch event
     * @param child the child view associated with this Behavior
     * @param ev the MotionEvent describing the touch event being processed
     * @return true if this Behavior would like to intercept and take over the event stream. The
     *     default always returns false.
     */
    public boolean onInterceptTouchEvent(CoordinatorLayout parent, V child, MotionEvent ev) {
      return false;
    }

    /**
     * Respond to CoordinatorLayout touch events after this Behavior has started {@link
     * #onInterceptTouchEvent intercepting} them.
     *
     * <p>Behaviors may intercept touch events in order to help the CoordinatorLayout manipulate its
     * child views. For example, a Behavior may allow a user to drag a UI pane open or closed. This
     * method should perform actual mutations of view layout state.
     *
     * <p>This method will be called regardless of the visibility of the associated child of the
     * behavior. If you only wish to handle touch events when the child is visible, you should add a
     * check to {@link View#isShown()} on the given child.
     *
     * @param parent the parent view currently receiving this touch event
     * @param child the child view associated with this Behavior
     * @param ev the MotionEvent describing the touch event being processed
     * @return true if this Behavior handled this touch event and would like to continue receiving
     *     events in this stream. The default always returns false.
     */
    public boolean onTouchEvent(CoordinatorLayout parent, V child, MotionEvent ev) {
      return false;
    }

    /**
     * Supply a scrim color that will be painted behind the associated child view.
     *
     * <p>A scrim may be used to indicate that the other elements beneath it are not currently
     * interactive or actionable, drawing user focus and attention to the views above the scrim.
     *
     * <p>The default implementation returns {@link Color#BLACK}.
     *
     * @param parent the parent view of the given child
     * @param child the child view above the scrim
     * @return the desired scrim color in 0xAARRGGBB format. The default return value is {@link
     *     Color#BLACK}.
     * @see #getScrimOpacity(CoordinatorLayout, android.view.View)
     */
    @ColorInt
    public int getScrimColor(CoordinatorLayout parent, V child) {
      return Color.BLACK;
    }

    /**
     * Determine the current opacity of the scrim behind a given child view
     *
     * <p>A scrim may be used to indicate that the other elements beneath it are not currently
     * interactive or actionable, drawing user focus and attention to the views above the scrim.
     *
     * <p>The default implementation returns 0.0f.
     *
     * @param parent the parent view of the given child
     * @param child the child view above the scrim
     * @return the desired scrim opacity from 0.0f to 1.0f. The default return value is 0.0f.
     */
    @FloatRange(from = 0, to = 1)
    public float getScrimOpacity(CoordinatorLayout parent, V child) {
      return 0.f;
    }

    /**
     * Determine whether interaction with views behind the given child in the child order should be
     * blocked.
     *
     * <p>The default implementation returns true if {@link #getScrimOpacity(CoordinatorLayout,
     * android.view.View)} would return > 0.0f.
     *
     * @param parent the parent view of the given child
     * @param child the child view to test
     * @return true if {@link #getScrimOpacity(CoordinatorLayout, android.view.View)} would return >
     *     0.0f.
     */
    public boolean blocksInteractionBelow(CoordinatorLayout parent, V child) {
      return getScrimOpacity(parent, child) > 0.f;
    }

    /**
     * Determine whether the supplied child view has another specific sibling view as a layout
     * dependency.
     *
     * <p>This method will be called at least once in response to a layout request. If it returns
     * true for a given child and dependency view pair, the parent CoordinatorLayout will:
     *
     * <ol>
     *   <li>Always lay out this child after the dependent child is laid out, regardless of child
     *       order.
     *   <li>Call {@link #onDependentViewChanged} when the dependency view's layout or position
     *       changes.
     * </ol>
     *
     * @param parent the parent view of the given child
     * @param child the child view to test
     * @param dependency the proposed dependency of child
     * @return true if child's layout depends on the proposed dependency's layout, false otherwise
     * @see #onDependentViewChanged(CoordinatorLayout, android.view.View, android.view.View)
     */
    public boolean layoutDependsOn(CoordinatorLayout parent, V child, View dependency) {
      return false;
    }

    /**
     * Respond to a change in a child's dependent view
     *
     * <p>This method is called whenever a dependent view changes in size or position outside of the
     * standard layout flow. A Behavior may use this method to appropriately update the child view
     * in response.
     *
     * <p>A view's dependency is determined by {@link #layoutDependsOn(CoordinatorLayout,
     * android.view.View, android.view.View)} or if {@code child} has set another view as it's
     * anchor.
     *
     * <p>Note that if a Behavior changes the layout of a child via this method, it should also be
     * able to reconstruct the correct position in {@link #onLayoutChild(CoordinatorLayout,
     * android.view.View, int) onLayoutChild}. <code>onDependentViewChanged</code> will not be
     * called during normal layout since the layout of each child view will always happen in
     * dependency order.
     *
     * <p>If the Behavior changes the child view's size or position, it should return true. The
     * default implementation returns false.
     *
     * @param parent the parent view of the given child
     * @param child the child view to manipulate
     * @param dependency the dependent view that changed
     * @return true if the Behavior changed the child view's size or position, false otherwise
     */
    public boolean onDependentViewChanged(CoordinatorLayout parent, V child, View dependency) {
      return false;
    }

    /**
     * Respond to a child's dependent view being removed.
     *
     * <p>This method is called after a dependent view has been removed from the parent. A Behavior
     * may use this method to appropriately update the child view in response.
     *
     * <p>A view's dependency is determined by {@link #layoutDependsOn(CoordinatorLayout,
     * android.view.View, android.view.View)} or if {@code child} has set another view as it's
     * anchor.
     *
     * @param parent the parent view of the given child
     * @param child the child view to manipulate
     * @param dependency the dependent view that has been removed
     */
    public void onDependentViewRemoved(CoordinatorLayout parent, V child, View dependency) {}

    /**
     * Called when the parent CoordinatorLayout is about to measure the given child view.
     *
     * <p>This method can be used to perform custom or modified measurement of a child view in place
     * of the default child measurement behavior. The Behavior's implementation can delegate to the
     * standard CoordinatorLayout measurement behavior by calling {@link
     * CoordinatorLayout#onMeasureChild(android.view.View, int, int, int, int)
     * parent.onMeasureChild}.
     *
     * @param parent the parent CoordinatorLayout
     * @param child the child to measure
     * @param parentWidthMeasureSpec the width requirements for this view
     * @param widthUsed extra space that has been used up by the parent horizontally (possibly by
     *     other children of the parent)
     * @param parentHeightMeasureSpec the height requirements for this view
     * @param heightUsed extra space that has been used up by the parent vertically (possibly by
     *     other children of the parent)
     * @return true if the Behavior measured the child view, false if the CoordinatorLayout should
     *     perform its default measurement
     */
    public boolean onMeasureChild(
        CoordinatorLayout parent,
        V child,
        int parentWidthMeasureSpec,
        int widthUsed,
        int parentHeightMeasureSpec,
        int heightUsed) {
      return false;
    }

    /**
     * Called when the parent CoordinatorLayout is about the lay out the given child view.
     *
     * <p>This method can be used to perform custom or modified layout of a child view in place of
     * the default child layout behavior. The Behavior's implementation can delegate to the standard
     * CoordinatorLayout measurement behavior by calling {@link
     * CoordinatorLayout#onLayoutChild(android.view.View, int) parent.onLayoutChild}.
     *
     * <p>If a Behavior implements {@link #onDependentViewChanged(CoordinatorLayout,
     * android.view.View, android.view.View)} to change the position of a view in response to a
     * dependent view changing, it should also implement <code>onLayoutChild</code> in such a way
     * that respects those dependent views. <code>onLayoutChild</code> will always be called for a
     * dependent view <em>after</em> its dependency has been laid out.
     *
     * @param parent the parent CoordinatorLayout
     * @param child child view to lay out
     * @param layoutDirection the resolved layout direction for the CoordinatorLayout, such as
     *     {@link ViewCompat#LAYOUT_DIRECTION_LTR} or {@link ViewCompat#LAYOUT_DIRECTION_RTL}.
     * @return true if the Behavior performed layout of the child view, false to request default
     *     layout behavior
     */
    public boolean onLayoutChild(CoordinatorLayout parent, V child, int layoutDirection) {
      return false;
    }

    // Utility methods for accessing child-specific, behavior-modifiable properties.

    /**
     * Associate a Behavior-specific tag object with the given child view. This object will be
     * stored with the child view's LayoutParams.
     *
     * @param child child view to set tag with
     * @param tag tag object to set
     */
    public static void setTag(View child, Object tag) {
      final LayoutParams lp = (LayoutParams) child.getLayoutParams();
      lp.mBehaviorTag = tag;
    }

    /**
     * Get the behavior-specific tag object with the given child view. This object is stored with
     * the child view's LayoutParams.
     *
     * @param child child view to get tag with
     * @return the previously stored tag object
     */
    public static Object getTag(View child) {
      final LayoutParams lp = (LayoutParams) child.getLayoutParams();
      return lp.mBehaviorTag;
    }

    /**
     * Called when a descendant of the CoordinatorLayout attempts to initiate a nested scroll.
     *
     * <p>Any Behavior associated with any direct child of the CoordinatorLayout may respond to this
     * event and return true to indicate that the CoordinatorLayout should act as a nested scrolling
     * parent for this scroll. Only Behaviors that return true from this method will receive
     * subsequent nested scroll events.
     *
     * @param coordinatorLayout the CoordinatorLayout parent of the view this Behavior is associated
     *     with
     * @param child the child view of the CoordinatorLayout this Behavior is associated with
     * @param directTargetChild the child view of the CoordinatorLayout that either is or contains
     *     the target of the nested scroll operation
     * @param target the descendant view of the CoordinatorLayout initiating the nested scroll
     * @param nestedScrollAxes the axes that this nested scroll applies to. See {@link
     *     ViewCompat#SCROLL_AXIS_HORIZONTAL}, {@link ViewCompat#SCROLL_AXIS_VERTICAL}
     * @return true if the Behavior wishes to accept this nested scroll
     * @see NestedScrollingParent#onStartNestedScroll(View, View, int)
     */
    public boolean onStartNestedScroll(
        CoordinatorLayout coordinatorLayout,
        V child,
        View directTargetChild,
        View target,
        int nestedScrollAxes) {
      return false;
    }

    /**
     * Called when a nested scroll has been accepted by the CoordinatorLayout.
     *
     * <p>Any Behavior associated with any direct child of the CoordinatorLayout may elect to accept
     * the nested scroll as part of {@link #onStartNestedScroll}. Each Behavior that returned true
     * will receive subsequent nested scroll events for that nested scroll.
     *
     * @param coordinatorLayout the CoordinatorLayout parent of the view this Behavior is associated
     *     with
     * @param child the child view of the CoordinatorLayout this Behavior is associated with
     * @param directTargetChild the child view of the CoordinatorLayout that either is or contains
     *     the target of the nested scroll operation
     * @param target the descendant view of the CoordinatorLayout initiating the nested scroll
     * @param nestedScrollAxes the axes that this nested scroll applies to. See {@link
     *     ViewCompat#SCROLL_AXIS_HORIZONTAL}, {@link ViewCompat#SCROLL_AXIS_VERTICAL}
     * @see NestedScrollingParent#onNestedScrollAccepted(View, View, int)
     */
    public void onNestedScrollAccepted(
        CoordinatorLayout coordinatorLayout,
        V child,
        View directTargetChild,
        View target,
        int nestedScrollAxes) {
      // Do nothing
    }

    /**
     * Called when a nested scroll has ended.
     *
     * <p>Any Behavior associated with any direct child of the CoordinatorLayout may elect to accept
     * the nested scroll as part of {@link #onStartNestedScroll}. Each Behavior that returned true
     * will receive subsequent nested scroll events for that nested scroll.
     *
     * <p><code>onStopNestedScroll</code> marks the end of a single nested scroll event sequence.
     * This is a good place to clean up any state related to the nested scroll.
     *
     * @param coordinatorLayout the CoordinatorLayout parent of the view this Behavior is associated
     *     with
     * @param child the child view of the CoordinatorLayout this Behavior is associated with
     * @param target the descendant view of the CoordinatorLayout that initiated the nested scroll
     * @see NestedScrollingParent#onStopNestedScroll(View)
     */
    public void onStopNestedScroll(CoordinatorLayout coordinatorLayout, V child, View target) {
      // Do nothing
    }

    /**
     * Called when a nested scroll in progress has updated and the target has scrolled or attempted
     * to scroll.
     *
     * <p>Any Behavior associated with the direct child of the CoordinatorLayout may elect to accept
     * the nested scroll as part of {@link #onStartNestedScroll}. Each Behavior that returned true
     * will receive subsequent nested scroll events for that nested scroll.
     *
     * <p><code>onNestedScroll</code> is called each time the nested scroll is updated by the nested
     * scrolling child, with both consumed and unconsumed components of the scroll supplied in
     * pixels. <em>Each Behavior responding to the nested scroll will receive the same values.</em>
     *
     * @param coordinatorLayout the CoordinatorLayout parent of the view this Behavior is associated
     *     with
     * @param child the child view of the CoordinatorLayout this Behavior is associated with
     * @param target the descendant view of the CoordinatorLayout performing the nested scroll
     * @param dxConsumed horizontal pixels consumed by the target's own scrolling operation
     * @param dyConsumed vertical pixels consumed by the target's own scrolling operation
     * @param dxUnconsumed horizontal pixels not consumed by the target's own scrolling operation,
     *     but requested by the user
     * @param dyUnconsumed vertical pixels not consumed by the target's own scrolling operation, but
     *     requested by the user
     * @see NestedScrollingParent#onNestedScroll(View, int, int, int, int)
     */
    public void onNestedScroll(
        CoordinatorLayout coordinatorLayout,
        V child,
        View target,
        int dxConsumed,
        int dyConsumed,
        int dxUnconsumed,
        int dyUnconsumed) {
      // Do nothing
    }

    /**
     * Called when a nested scroll in progress is about to update, before the target has consumed
     * any of the scrolled distance.
     *
     * <p>Any Behavior associated with the direct child of the CoordinatorLayout may elect to accept
     * the nested scroll as part of {@link #onStartNestedScroll}. Each Behavior that returned true
     * will receive subsequent nested scroll events for that nested scroll.
     *
     * <p><code>onNestedPreScroll</code> is called each time the nested scroll is updated by the
     * nested scrolling child, before the nested scrolling child has consumed the scroll distance
     * itself. <em>Each Behavior responding to the nested scroll will receive the same values.</em>
     * The CoordinatorLayout will report as consumed the maximum number of pixels in either
     * direction that any Behavior responding to the nested scroll reported as consumed.
     *
     * @param coordinatorLayout the CoordinatorLayout parent of the view this Behavior is associated
     *     with
     * @param child the child view of the CoordinatorLayout this Behavior is associated with
     * @param target the descendant view of the CoordinatorLayout performing the nested scroll
     * @param dx the raw horizontal number of pixels that the user attempted to scroll
     * @param dy the raw vertical number of pixels that the user attempted to scroll
     * @param consumed out parameter. consumed[0] should be set to the distance of dx that was
     *     consumed, consumed[1] should be set to the distance of dy that was consumed
     * @see NestedScrollingParent#onNestedPreScroll(View, int, int, int[])
     */
    public void onNestedPreScroll(
        CoordinatorLayout coordinatorLayout, V child, View target, int dx, int dy, int[] consumed) {
      // Do nothing
    }

    /**
     * Called when a nested scrolling child is starting a fling or an action that would be a fling.
     *
     * <p>Any Behavior associated with the direct child of the CoordinatorLayout may elect to accept
     * the nested scroll as part of {@link #onStartNestedScroll}. Each Behavior that returned true
     * will receive subsequent nested scroll events for that nested scroll.
     *
     * <p><code>onNestedFling</code> is called when the current nested scrolling child view detects
     * the proper conditions for a fling. It reports if the child itself consumed the fling. If it
     * did not, the child is expected to show some sort of overscroll indication. This method should
     * return true if it consumes the fling, so that a child that did not itself take an action in
     * response can choose not to show an overfling indication.
     *
     * @param coordinatorLayout the CoordinatorLayout parent of the view this Behavior is associated
     *     with
     * @param child the child view of the CoordinatorLayout this Behavior is associated with
     * @param target the descendant view of the CoordinatorLayout performing the nested scroll
     * @param velocityX horizontal velocity of the attempted fling
     * @param velocityY vertical velocity of the attempted fling
     * @param consumed true if the nested child view consumed the fling
     * @return true if the Behavior consumed the fling
     * @see NestedScrollingParent#onNestedFling(View, float, float, boolean)
     */
    public boolean onNestedFling(
        CoordinatorLayout coordinatorLayout,
        V child,
        View target,
        float velocityX,
        float velocityY,
        boolean consumed) {
      return false;
    }

    /**
     * Called when a nested scrolling child is about to start a fling.
     *
     * <p>Any Behavior associated with the direct child of the CoordinatorLayout may elect to accept
     * the nested scroll as part of {@link #onStartNestedScroll}. Each Behavior that returned true
     * will receive subsequent nested scroll events for that nested scroll.
     *
     * <p><code>onNestedPreFling</code> is called when the current nested scrolling child view
     * detects the proper conditions for a fling, but it has not acted on it yet. A Behavior can
     * return true to indicate that it consumed the fling. If at least one Behavior returns true,
     * the fling should not be acted upon by the child.
     *
     * @param coordinatorLayout the CoordinatorLayout parent of the view this Behavior is associated
     *     with
     * @param child the child view of the CoordinatorLayout this Behavior is associated with
     * @param target the descendant view of the CoordinatorLayout performing the nested scroll
     * @param velocityX horizontal velocity of the attempted fling
     * @param velocityY vertical velocity of the attempted fling
     * @return true if the Behavior consumed the fling
     * @see NestedScrollingParent#onNestedPreFling(View, float, float)
     */
    public boolean onNestedPreFling(
        CoordinatorLayout coordinatorLayout,
        V child,
        View target,
        float velocityX,
        float velocityY) {
      return false;
    }

    /**
     * Called when the window insets have changed.
     *
     * <p>Any Behavior associated with the direct child of the CoordinatorLayout may elect to handle
     * the window inset change on behalf of it's associated view.
     *
     * @param coordinatorLayout the CoordinatorLayout parent of the view this Behavior is associated
     *     with
     * @param child the child view of the CoordinatorLayout this Behavior is associated with
     * @param insets the new window insets.
     * @return The insets supplied, minus any insets that were consumed
     */
    @NonNull
    public WindowInsetsCompat onApplyWindowInsets(
        CoordinatorLayout coordinatorLayout, V child, WindowInsetsCompat insets) {
      return insets;
    }

    /**
     * Called when a child of the view associated with this behavior wants a particular rectangle to
     * be positioned onto the screen.
     *
     * <p>The contract for this method is the same as {@link
     * ViewParent#requestChildRectangleOnScreen(View, Rect, boolean)}.
     *
     * @param coordinatorLayout the CoordinatorLayout parent of the view this Behavior is associated
     *     with
     * @param child the child view of the CoordinatorLayout this Behavior is associated with
     * @param rectangle The rectangle which the child wishes to be on the screen in the child's
     *     coordinates
     * @param immediate true to forbid animated or delayed scrolling, false otherwise
     * @return true if the Behavior handled the request
     * @see ViewParent#requestChildRectangleOnScreen(View, Rect, boolean)
     */
    public boolean onRequestChildRectangleOnScreen(
        CoordinatorLayout coordinatorLayout, V child, Rect rectangle, boolean immediate) {
      return false;
    }

    /**
     * Hook allowing a behavior to re-apply a representation of its internal state that had
     * previously been generated by {@link #onSaveInstanceState}. This function will never be called
     * with a null state.
     *
     * @param parent the parent CoordinatorLayout
     * @param child child view to restore from
     * @param state The frozen state that had previously been returned by {@link
     *     #onSaveInstanceState}.
     * @see #onSaveInstanceState()
     */
    public void onRestoreInstanceState(CoordinatorLayout parent, V child, Parcelable state) {
      // no-op
    }

    /**
     * Hook allowing a behavior to generate a representation of its internal state that can later be
     * used to create a new instance with that same state. This state should only contain
     * information that is not persistent or can not be reconstructed later.
     *
     * <p>Behavior state is only saved when both the parent {@link CoordinatorLayout} and a view
     * using this behavior have valid IDs set.
     *
     * @param parent the parent CoordinatorLayout
     * @param child child view to restore from
     * @return Returns a Parcelable object containing the behavior's current dynamic state.
     * @see #onRestoreInstanceState(android.os.Parcelable)
     * @see View#onSaveInstanceState()
     */
    public Parcelable onSaveInstanceState(CoordinatorLayout parent, V child) {
      return BaseSavedState.EMPTY_STATE;
    }

    /**
     * Called when a view is set to dodge view insets.
     *
     * <p>This method allows a behavior to update the rectangle that should be dodged. The rectangle
     * should be in the parent's coordinate system and within the child's bounds. If not, a {@link
     * IllegalArgumentException} is thrown.
     *
     * @param parent the CoordinatorLayout parent of the view this Behavior is associated with
     * @param child the child view of the CoordinatorLayout this Behavior is associated with
     * @param rect the rect to update with the dodge rectangle
     * @return true the rect was updated, false if we should use the child's bounds
     */
    public boolean getInsetDodgeRect(
        @NonNull CoordinatorLayout parent, @NonNull V child, @NonNull Rect rect) {
      return false;
    }
  }

  /** Parameters describing the desired layout for a child of a {@link CoordinatorLayout}. */
  public static class LayoutParams extends ViewGroup.MarginLayoutParams {
    /** A {@link Behavior} that the child view should obey. */
    Behavior mBehavior;

    boolean mBehaviorResolved = false;

    /**
     * A {@link Gravity} value describing how this child view should lay out. If either or both of
     * the axes are not specified, they are treated by CoordinatorLayout as {@link Gravity#TOP} or
     * {@link GravityCompat#START}. If an {@link #setAnchorId(int) anchor} is also specified, the
     * gravity describes how this child view should be positioned relative to its anchored position.
     */
    public int gravity = Gravity.NO_GRAVITY;

    /**
     * A {@link Gravity} value describing which edge of a child view's {@link #getAnchorId() anchor}
     * view the child should position itself relative to.
     */
    public int anchorGravity = Gravity.NO_GRAVITY;

    /**
     * The index of the horizontal keyline specified to the parent CoordinatorLayout that this child
     * should align relative to. If an {@link #setAnchorId(int) anchor} is present the keyline will
     * be ignored.
     */
    public int keyline = -1;

    /**
     * A {@link View#getId() view id} of a descendant view of the CoordinatorLayout that this child
     * should position relative to.
     */
    int mAnchorId = View.NO_ID;

    /**
     * A {@link Gravity} value describing how this child view insets the CoordinatorLayout. Other
     * child views which are set to dodge the same inset edges will be moved appropriately so that
     * the views do not overlap.
     */
    public int insetEdge = Gravity.NO_GRAVITY;

    /**
     * A {@link Gravity} value describing how this child view dodges any inset child views in the
     * CoordinatorLayout. Any views which are inset on the same edge as this view is set to dodge
     * will result in this view being moved so that the views do not overlap.
     */
    public int dodgeInsetEdges = Gravity.NO_GRAVITY;

    int mInsetOffsetX;
    int mInsetOffsetY;

    View mAnchorView;
    View mAnchorDirectChild;

    private boolean mDidBlockInteraction;
    private boolean mDidAcceptNestedScroll;
    private boolean mDidChangeAfterNestedScroll;

    final Rect mLastChildRect = new Rect();

    Object mBehaviorTag;

    public LayoutParams(int width, int height) {
      super(width, height);
    }

    LayoutParams(Context context, AttributeSet attrs) {
      super(context, attrs);

      final TypedArray a =
          context.obtainStyledAttributes(attrs, R.styleable.CoordinatorLayout_Layout);

      this.gravity =
          a.getInteger(
              R.styleable.CoordinatorLayout_Layout_android_layout_gravity, Gravity.NO_GRAVITY);
      mAnchorId = a.getResourceId(R.styleable.CoordinatorLayout_Layout_layout_anchor, View.NO_ID);
      this.anchorGravity =
          a.getInteger(
              R.styleable.CoordinatorLayout_Layout_layout_anchorGravity, Gravity.NO_GRAVITY);

      this.keyline = a.getInteger(R.styleable.CoordinatorLayout_Layout_layout_keyline, -1);

      insetEdge = a.getInt(R.styleable.CoordinatorLayout_Layout_layout_insetEdge, 0);
      dodgeInsetEdges = a.getInt(R.styleable.CoordinatorLayout_Layout_layout_dodgeInsetEdges, 0);
      mBehaviorResolved = a.hasValue(R.styleable.CoordinatorLayout_Layout_layout_behavior);
      if (mBehaviorResolved) {
        mBehavior =
            parseBehavior(
                context, attrs, a.getString(R.styleable.CoordinatorLayout_Layout_layout_behavior));
      }
      a.recycle();

      if (mBehavior != null) {
        // If we have a Behavior, dispatch that it has been attached
        mBehavior.onAttachedToLayoutParams(this);
      }
    }

    public LayoutParams(LayoutParams p) {
      super(p);
    }

    public LayoutParams(MarginLayoutParams p) {
      super(p);
    }

    public LayoutParams(ViewGroup.LayoutParams p) {
      super(p);
    }

    /**
     * Get the id of this view's anchor.
     *
     * @return A {@link View#getId() view id} or {@link View#NO_ID} if there is no anchor
     */
    @IdRes
    public int getAnchorId() {
      return mAnchorId;
    }

    /**
     * Set the id of this view's anchor.
     *
     * <p>The view with this id must be a descendant of the CoordinatorLayout containing the child
     * view this LayoutParams belongs to. It may not be the child view with this LayoutParams or a
     * descendant of it.
     *
     * @param id The {@link View#getId() view id} of the anchor or {@link View#NO_ID} if there is no
     *     anchor
     */
    public void setAnchorId(@IdRes int id) {
      invalidateAnchor();
      mAnchorId = id;
    }

    /**
     * Get the behavior governing the layout and interaction of the child view within a parent
     * CoordinatorLayout.
     *
     * @return The current behavior or null if no behavior is specified
     */
    @Nullable
    public Behavior getBehavior() {
      return mBehavior;
    }

    /**
     * Set the behavior governing the layout and interaction of the child view within a parent
     * CoordinatorLayout.
     *
     * <p>Setting a new behavior will remove any currently associated {@link
     * Behavior#setTag(android.view.View, Object) Behavior tag}.
     *
     * @param behavior The behavior to set or null for no special behavior
     */
    public void setBehavior(@Nullable Behavior behavior) {
      if (mBehavior != behavior) {
        if (mBehavior != null) {
          // First detach any old behavior
          mBehavior.onDetachedFromLayoutParams();
        }

        mBehavior = behavior;
        mBehaviorTag = null;
        mBehaviorResolved = true;

        if (behavior != null) {
          // Now dispatch that the Behavior has been attached
          behavior.onAttachedToLayoutParams(this);
        }
      }
    }

    /**
     * Set the last known position rect for this child view
     *
     * @param r the rect to set
     */
    void setLastChildRect(Rect r) {
      mLastChildRect.set(r);
    }

    /**
     * Get the last known position rect for this child view. Note: do not mutate the result of this
     * call.
     */
    Rect getLastChildRect() {
      return mLastChildRect;
    }

    /**
     * Returns true if the anchor id changed to another valid view id since the anchor view was
     * resolved.
     */
    boolean checkAnchorChanged() {
      return mAnchorView == null && mAnchorId != View.NO_ID;
    }

    /**
     * Returns true if the associated Behavior previously blocked interaction with other views below
     * the associated child since the touch behavior tracking was last {@link
     * #resetTouchBehaviorTracking() reset}.
     *
     * @see #isBlockingInteractionBelow(CoordinatorLayout, android.view.View)
     */
    boolean didBlockInteraction() {
      if (mBehavior == null) {
        mDidBlockInteraction = false;
      }
      return mDidBlockInteraction;
    }

    /**
     * Check if the associated Behavior wants to block interaction below the given child view. The
     * given child view should be the child this LayoutParams is associated with.
     *
     * <p>Once interaction is blocked, it will remain blocked until touch interaction tracking is
     * {@link #resetTouchBehaviorTracking() reset}.
     *
     * @param parent the parent CoordinatorLayout
     * @param child the child view this LayoutParams is associated with
     * @return true to block interaction below the given child
     */
    boolean isBlockingInteractionBelow(CoordinatorLayout parent, View child) {
      if (mDidBlockInteraction) {
        return true;
      }

      return mDidBlockInteraction |=
          mBehavior != null ? mBehavior.blocksInteractionBelow(parent, child) : false;
    }

    /**
     * Reset tracking of Behavior-specific touch interactions. This includes interaction blocking.
     *
     * @see #isBlockingInteractionBelow(CoordinatorLayout, android.view.View)
     * @see #didBlockInteraction()
     */
    void resetTouchBehaviorTracking() {
      mDidBlockInteraction = false;
    }

    void resetNestedScroll() {
      mDidAcceptNestedScroll = false;
    }

    void acceptNestedScroll(boolean accept) {
      mDidAcceptNestedScroll = accept;
    }

    boolean isNestedScrollAccepted() {
      return mDidAcceptNestedScroll;
    }

    boolean getChangedAfterNestedScroll() {
      return mDidChangeAfterNestedScroll;
    }

    void setChangedAfterNestedScroll(boolean changed) {
      mDidChangeAfterNestedScroll = changed;
    }

    void resetChangedAfterNestedScroll() {
      mDidChangeAfterNestedScroll = false;
    }

    /**
     * Check if an associated child view depends on another child view of the CoordinatorLayout.
     *
     * @param parent the parent CoordinatorLayout
     * @param child the child to check
     * @param dependency the proposed dependency to check
     * @return true if child depends on dependency
     */
    boolean dependsOn(CoordinatorLayout parent, View child, View dependency) {
      return dependency == mAnchorDirectChild
          || shouldDodge(dependency, ViewCompat.getLayoutDirection(parent))
          || (mBehavior != null && mBehavior.layoutDependsOn(parent, child, dependency));
    }

    /**
     * Invalidate the cached anchor view and direct child ancestor of that anchor. The anchor will
     * need to be {@link #findAnchorView(CoordinatorLayout, android.view.View) found} before being
     * used again.
     */
    void invalidateAnchor() {
      mAnchorView = mAnchorDirectChild = null;
    }

    /**
     * Locate the appropriate anchor view by the current {@link #setAnchorId(int) anchor id} or
     * return the cached anchor view if already known.
     *
     * @param parent the parent CoordinatorLayout
     * @param forChild the child this LayoutParams is associated with
     * @return the located descendant anchor view, or null if the anchor id is {@link View#NO_ID}.
     */
    View findAnchorView(CoordinatorLayout parent, View forChild) {
      if (mAnchorId == View.NO_ID) {
        mAnchorView = mAnchorDirectChild = null;
        return null;
      }

      if (mAnchorView == null || !verifyAnchorView(forChild, parent)) {
        resolveAnchorView(forChild, parent);
      }
      return mAnchorView;
    }

    /**
     * Determine the anchor view for the child view this LayoutParams is assigned to. Assumes
     * mAnchorId is valid.
     */
    private void resolveAnchorView(final View forChild, final CoordinatorLayout parent) {
      mAnchorView = parent.findViewById(mAnchorId);
      if (mAnchorView != null) {
        if (mAnchorView == parent) {
          if (parent.isInEditMode()) {
            mAnchorView = mAnchorDirectChild = null;
            return;
          }
          throw new IllegalStateException(
              "View can not be anchored to the the parent CoordinatorLayout");
        }

        View directChild = mAnchorView;
        for (ViewParent p = mAnchorView.getParent(); p != parent && p != null; p = p.getParent()) {
          if (p == forChild) {
            if (parent.isInEditMode()) {
              mAnchorView = mAnchorDirectChild = null;
              return;
            }
            throw new IllegalStateException("Anchor must not be a descendant of the anchored view");
          }
          if (p instanceof View) {
            directChild = (View) p;
          }
        }
        mAnchorDirectChild = directChild;
      } else {
        if (parent.isInEditMode()) {
          mAnchorView = mAnchorDirectChild = null;
          return;
        }
        throw new IllegalStateException(
            "Could not find CoordinatorLayout descendant view"
                + " with id "
                + parent.getResources().getResourceName(mAnchorId)
                + " to anchor view "
                + forChild);
      }
    }

    /**
     * Verify that the previously resolved anchor view is still valid - that it is still a
     * descendant of the expected parent view, it is not the child this LayoutParams is assigned to
     * or a descendant of it, and it has the expected id.
     */
    private boolean verifyAnchorView(View forChild, CoordinatorLayout parent) {
      if (mAnchorView.getId() != mAnchorId) {
        return false;
      }

      View directChild = mAnchorView;
      for (ViewParent p = mAnchorView.getParent(); p != parent; p = p.getParent()) {
        if (p == null || p == forChild) {
          mAnchorView = mAnchorDirectChild = null;
          return false;
        }
        if (p instanceof View) {
          directChild = (View) p;
        }
      }
      mAnchorDirectChild = directChild;
      return true;
    }

    /** Checks whether the view with this LayoutParams should dodge the specified view. */
    private boolean shouldDodge(View other, int layoutDirection) {
      LayoutParams lp = (LayoutParams) other.getLayoutParams();
      final int absInset = GravityCompat.getAbsoluteGravity(lp.insetEdge, layoutDirection);
      return absInset != Gravity.NO_GRAVITY
          && (absInset & GravityCompat.getAbsoluteGravity(dodgeInsetEdges, layoutDirection))
              == absInset;
    }
  }

  private class HierarchyChangeListener implements OnHierarchyChangeListener {
    HierarchyChangeListener() {}

    @Override
    public void onChildViewAdded(View parent, View child) {
      if (mOnHierarchyChangeListener != null) {
        mOnHierarchyChangeListener.onChildViewAdded(parent, child);
      }
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
      onChildViewsChanged(EVENT_VIEW_REMOVED);

      if (mOnHierarchyChangeListener != null) {
        mOnHierarchyChangeListener.onChildViewRemoved(parent, child);
      }
    }
  }

  @Override
  protected void onRestoreInstanceState(Parcelable state) {
    if (!(state instanceof SavedState)) {
      super.onRestoreInstanceState(state);
      return;
    }

    final SavedState ss = (SavedState) state;
    super.onRestoreInstanceState(ss.getSuperState());

    final SparseArray<Parcelable> behaviorStates = ss.behaviorStates;

    for (int i = 0, count = getChildCount(); i < count; i++) {
      final View child = getChildAt(i);
      final int childId = child.getId();
      final LayoutParams lp = getResolvedLayoutParams(child);
      final Behavior b = lp.getBehavior();

      if (childId != NO_ID && b != null) {
        Parcelable savedState = behaviorStates.get(childId);
        if (savedState != null) {
          b.onRestoreInstanceState(this, child, savedState);
        }
      }
    }
  }

  @Override
  protected Parcelable onSaveInstanceState() {
    final SavedState ss = new SavedState(super.onSaveInstanceState());

    final SparseArray<Parcelable> behaviorStates = new SparseArray<>();
    for (int i = 0, count = getChildCount(); i < count; i++) {
      final View child = getChildAt(i);
      final int childId = child.getId();
      final LayoutParams lp = (LayoutParams) child.getLayoutParams();
      final Behavior b = lp.getBehavior();

      if (childId != NO_ID && b != null) {
        // If the child has an ID and a Behavior, let it save some state...
        Parcelable state = b.onSaveInstanceState(this, child);
        if (state != null) {
          behaviorStates.append(childId, state);
        }
      }
    }
    ss.behaviorStates = behaviorStates;
    return ss;
  }

  @Override
  public boolean requestChildRectangleOnScreen(View child, Rect rectangle, boolean immediate) {
    final CoordinatorLayout.LayoutParams lp = (LayoutParams) child.getLayoutParams();
    final Behavior behavior = lp.getBehavior();

    if (behavior != null
        && behavior.onRequestChildRectangleOnScreen(this, child, rectangle, immediate)) {
      return true;
    }

    return super.requestChildRectangleOnScreen(child, rectangle, immediate);
  }

  private void setupForInsets() {
    if (Build.VERSION.SDK_INT < 21) {
      return;
    }

    if (ViewCompat.getFitsSystemWindows(this)) {
      if (mApplyWindowInsetsListener == null) {
        mApplyWindowInsetsListener =
            new android.support.v4.view.OnApplyWindowInsetsListener() {
              @Override
              public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
                return setWindowInsets(insets);
              }
            };
      }
      // First apply the insets listener
      ViewCompat.setOnApplyWindowInsetsListener(this, mApplyWindowInsetsListener);

      // Now set the sys ui flags to enable us to lay out in the window insets
      setSystemUiVisibility(
          View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    } else {
      ViewCompat.setOnApplyWindowInsetsListener(this, null);
    }
  }

  protected static class SavedState extends AbsSavedState {
    SparseArray<Parcelable> behaviorStates;

    public SavedState(Parcel source, ClassLoader loader) {
      super(source, loader);

      final int size = source.readInt();

      final int[] ids = new int[size];
      source.readIntArray(ids);

      final Parcelable[] states = source.readParcelableArray(loader);

      behaviorStates = new SparseArray<>(size);
      for (int i = 0; i < size; i++) {
        behaviorStates.append(ids[i], states[i]);
      }
    }

    public SavedState(Parcelable superState) {
      super(superState);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      super.writeToParcel(dest, flags);

      final int size = behaviorStates != null ? behaviorStates.size() : 0;
      dest.writeInt(size);

      final int[] ids = new int[size];
      final Parcelable[] states = new Parcelable[size];

      for (int i = 0; i < size; i++) {
        ids[i] = behaviorStates.keyAt(i);
        states[i] = behaviorStates.valueAt(i);
      }
      dest.writeIntArray(ids);
      dest.writeParcelableArray(states, flags);
    }

    public static final Parcelable.Creator<SavedState> CREATOR =
        new ClassLoaderCreator<SavedState>() {
          @Override
          public SavedState createFromParcel(Parcel in, ClassLoader loader) {
            return new SavedState(in, loader);
          }

          @Override
          public SavedState createFromParcel(Parcel in) {
            return new SavedState(in, null);
          }

          @Override
          public SavedState[] newArray(int size) {
            return new SavedState[size];
          }
        };
  }
}
