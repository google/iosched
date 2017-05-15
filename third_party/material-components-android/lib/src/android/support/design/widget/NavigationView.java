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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.StyleRes;
import android.support.design.R;
import android.support.design.internal.NavigationMenu;
import android.support.design.internal.NavigationMenuPresenter;
import android.support.design.internal.ScrimInsetsFrameLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.AbsSavedState;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v7.content.res.AppCompatResources;
import android.support.v7.view.SupportMenuInflater;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuItemImpl;
import android.support.v7.widget.TintTypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;

/**
 * Represents a standard navigation menu for application. The menu contents can be populated by a
 * menu resource file.
 *
 * <p>NavigationView is typically placed inside a {@link android.support.v4.widget.DrawerLayout}.
 *
 * <pre>
 * &lt;android.support.v4.widget.DrawerLayout xmlns:android="http://schemas.android.com/apk/res/android"
 *     xmlns:app="http://schemas.android.com/apk/res-auto"
 *     android:id="@+id/drawer_layout"
 *     android:layout_width="match_parent"
 *     android:layout_height="match_parent"
 *     android:fitsSystemWindows="true"&gt;
 *
 *     &lt;!-- Your contents --&gt;
 *
 *     &lt;android.support.design.widget.NavigationView
 *         android:id="@+id/navigation"
 *         android:layout_width="wrap_content"
 *         android:layout_height="match_parent"
 *         android:layout_gravity="start"
 *         app:menu="@menu/my_navigation_items" /&gt;
 * &lt;/android.support.v4.widget.DrawerLayout&gt;
 * </pre>
 */
public class NavigationView extends ScrimInsetsFrameLayout {

  private static final int[] CHECKED_STATE_SET = {android.R.attr.state_checked};
  private static final int[] DISABLED_STATE_SET = {-android.R.attr.state_enabled};

  private static final int PRESENTER_NAVIGATION_VIEW_ID = 1;

  private final NavigationMenu mMenu;
  private final NavigationMenuPresenter mPresenter = new NavigationMenuPresenter();

  OnNavigationItemSelectedListener mListener;
  private int mMaxWidth;

  private MenuInflater mMenuInflater;

  public NavigationView(Context context) {
    this(context, null);
  }

  public NavigationView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public NavigationView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    ThemeUtils.checkAppCompatTheme(context);

    // Create the menu
    mMenu = new NavigationMenu(context);

    // Custom attributes
    TintTypedArray a =
        TintTypedArray.obtainStyledAttributes(
            context,
            attrs,
            R.styleable.NavigationView,
            defStyleAttr,
            R.style.Widget_Design_NavigationView);

    ViewCompat.setBackground(this, a.getDrawable(R.styleable.NavigationView_android_background));
    if (a.hasValue(R.styleable.NavigationView_elevation)) {
      ViewCompat.setElevation(
          this, a.getDimensionPixelSize(R.styleable.NavigationView_elevation, 0));
    }
    ViewCompat.setFitsSystemWindows(
        this, a.getBoolean(R.styleable.NavigationView_android_fitsSystemWindows, false));

    mMaxWidth = a.getDimensionPixelSize(R.styleable.NavigationView_android_maxWidth, 0);

    final ColorStateList itemIconTint;
    if (a.hasValue(R.styleable.NavigationView_itemIconTint)) {
      itemIconTint = a.getColorStateList(R.styleable.NavigationView_itemIconTint);
    } else {
      itemIconTint = createDefaultColorStateList(android.R.attr.textColorSecondary);
    }

    boolean textAppearanceSet = false;
    int textAppearance = 0;
    if (a.hasValue(R.styleable.NavigationView_itemTextAppearance)) {
      textAppearance = a.getResourceId(R.styleable.NavigationView_itemTextAppearance, 0);
      textAppearanceSet = true;
    }

    ColorStateList itemTextColor = null;
    if (a.hasValue(R.styleable.NavigationView_itemTextColor)) {
      itemTextColor = a.getColorStateList(R.styleable.NavigationView_itemTextColor);
    }

    if (!textAppearanceSet && itemTextColor == null) {
      // If there isn't a text appearance set, we'll use a default text color
      itemTextColor = createDefaultColorStateList(android.R.attr.textColorPrimary);
    }

    final Drawable itemBackground = a.getDrawable(R.styleable.NavigationView_itemBackground);

    mMenu.setCallback(
        new MenuBuilder.Callback() {
          @Override
          public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
            return mListener != null && mListener.onNavigationItemSelected(item);
          }

          @Override
          public void onMenuModeChange(MenuBuilder menu) {}
        });
    mPresenter.setId(PRESENTER_NAVIGATION_VIEW_ID);
    mPresenter.initForMenu(context, mMenu);
    mPresenter.setItemIconTintList(itemIconTint);
    if (textAppearanceSet) {
      mPresenter.setItemTextAppearance(textAppearance);
    }
    mPresenter.setItemTextColor(itemTextColor);
    mPresenter.setItemBackground(itemBackground);
    mMenu.addMenuPresenter(mPresenter);
    addView((View) mPresenter.getMenuView(this));

    if (a.hasValue(R.styleable.NavigationView_menu)) {
      inflateMenu(a.getResourceId(R.styleable.NavigationView_menu, 0));
    }

    if (a.hasValue(R.styleable.NavigationView_headerLayout)) {
      inflateHeaderView(a.getResourceId(R.styleable.NavigationView_headerLayout, 0));
    }

    a.recycle();
  }

  @Override
  protected Parcelable onSaveInstanceState() {
    Parcelable superState = super.onSaveInstanceState();
    SavedState state = new SavedState(superState);
    state.menuState = new Bundle();
    mMenu.savePresenterStates(state.menuState);
    return state;
  }

  @Override
  protected void onRestoreInstanceState(Parcelable savedState) {
    if (!(savedState instanceof SavedState)) {
      super.onRestoreInstanceState(savedState);
      return;
    }
    SavedState state = (SavedState) savedState;
    super.onRestoreInstanceState(state.getSuperState());
    mMenu.restorePresenterStates(state.menuState);
  }

  /**
   * Set a listener that will be notified when a menu item is selected.
   *
   * @param listener The listener to notify
   */
  public void setNavigationItemSelectedListener(
      @Nullable OnNavigationItemSelectedListener listener) {
    mListener = listener;
  }

  @Override
  protected void onMeasure(int widthSpec, int heightSpec) {
    switch (MeasureSpec.getMode(widthSpec)) {
      case MeasureSpec.EXACTLY:
        // Nothing to do
        break;
      case MeasureSpec.AT_MOST:
        widthSpec =
            MeasureSpec.makeMeasureSpec(
                Math.min(MeasureSpec.getSize(widthSpec), mMaxWidth), MeasureSpec.EXACTLY);
        break;
      case MeasureSpec.UNSPECIFIED:
        widthSpec = MeasureSpec.makeMeasureSpec(mMaxWidth, MeasureSpec.EXACTLY);
        break;
    }
    // Let super sort out the height
    super.onMeasure(widthSpec, heightSpec);
  }

  /** @hide */
  @RestrictTo(LIBRARY_GROUP)
  @Override
  protected void onInsetsChanged(WindowInsetsCompat insets) {
    mPresenter.dispatchApplyWindowInsets(insets);
  }

  /**
   * Inflate a menu resource into this navigation view.
   *
   * <p>Existing items in the menu will not be modified or removed.
   *
   * @param resId ID of a menu resource to inflate
   */
  public void inflateMenu(int resId) {
    mPresenter.setUpdateSuspended(true);
    getMenuInflater().inflate(resId, mMenu);
    mPresenter.setUpdateSuspended(false);
    mPresenter.updateMenuView(false);
  }

  /** Returns the {@link Menu} instance associated with this navigation view. */
  public Menu getMenu() {
    return mMenu;
  }

  /**
   * Inflates a View and add it as a header of the navigation menu.
   *
   * @param res The layout resource ID.
   * @return a newly inflated View.
   */
  public View inflateHeaderView(@LayoutRes int res) {
    return mPresenter.inflateHeaderView(res);
  }

  /**
   * Adds a View as a header of the navigation menu.
   *
   * @param view The view to be added as a header of the navigation menu.
   */
  public void addHeaderView(@NonNull View view) {
    mPresenter.addHeaderView(view);
  }

  /**
   * Removes a previously-added header view.
   *
   * @param view The view to remove
   */
  public void removeHeaderView(@NonNull View view) {
    mPresenter.removeHeaderView(view);
  }

  /**
   * Gets the number of headers in this NavigationView.
   *
   * @return A positive integer representing the number of headers.
   */
  public int getHeaderCount() {
    return mPresenter.getHeaderCount();
  }

  /**
   * Gets the header view at the specified position.
   *
   * @param index The position at which to get the view from.
   * @return The header view the specified position or null if the position does not exist in this
   *     NavigationView.
   */
  public View getHeaderView(int index) {
    return mPresenter.getHeaderView(index);
  }

  /**
   * Returns the tint which is applied to our menu items' icons.
   *
   * @see #setItemIconTintList(ColorStateList)
   * @attr ref R.styleable#NavigationView_itemIconTint
   */
  @Nullable
  public ColorStateList getItemIconTintList() {
    return mPresenter.getItemTintList();
  }

  /**
   * Set the tint which is applied to our menu items' icons.
   *
   * @param tint the tint to apply.
   * @attr ref R.styleable#NavigationView_itemIconTint
   */
  public void setItemIconTintList(@Nullable ColorStateList tint) {
    mPresenter.setItemIconTintList(tint);
  }

  /**
   * Returns the tint which is applied to our menu items' icons.
   *
   * @see #setItemTextColor(ColorStateList)
   * @attr ref R.styleable#NavigationView_itemTextColor
   */
  @Nullable
  public ColorStateList getItemTextColor() {
    return mPresenter.getItemTextColor();
  }

  /**
   * Set the text color to be used on our menu items.
   *
   * @see #getItemTextColor()
   * @attr ref R.styleable#NavigationView_itemTextColor
   */
  public void setItemTextColor(@Nullable ColorStateList textColor) {
    mPresenter.setItemTextColor(textColor);
  }

  /**
   * Returns the background drawable for our menu items.
   *
   * @see #setItemBackgroundResource(int)
   * @attr ref R.styleable#NavigationView_itemBackground
   */
  @Nullable
  public Drawable getItemBackground() {
    return mPresenter.getItemBackground();
  }

  /**
   * Set the background of our menu items to the given resource.
   *
   * @param resId The identifier of the resource.
   * @attr ref R.styleable#NavigationView_itemBackground
   */
  public void setItemBackgroundResource(@DrawableRes int resId) {
    setItemBackground(ContextCompat.getDrawable(getContext(), resId));
  }

  /**
   * Set the background of our menu items to a given resource. The resource should refer to a
   * Drawable object or null to use the default background set on this navigation menu.
   *
   * @attr ref R.styleable#NavigationView_itemBackground
   */
  public void setItemBackground(@Nullable Drawable itemBackground) {
    mPresenter.setItemBackground(itemBackground);
  }

  /**
   * Sets the currently checked item in this navigation menu.
   *
   * @param id The item ID of the currently checked item.
   */
  public void setCheckedItem(@IdRes int id) {
    MenuItem item = mMenu.findItem(id);
    if (item != null) {
      mPresenter.setCheckedItem((MenuItemImpl) item);
    }
  }

  /**
   * Set the text appearance of the menu items to a given resource.
   *
   * @attr ref R.styleable#NavigationView_itemTextAppearance
   */
  public void setItemTextAppearance(@StyleRes int resId) {
    mPresenter.setItemTextAppearance(resId);
  }

  private MenuInflater getMenuInflater() {
    if (mMenuInflater == null) {
      mMenuInflater = new SupportMenuInflater(getContext());
    }
    return mMenuInflater;
  }

  private ColorStateList createDefaultColorStateList(int baseColorThemeAttr) {
    final TypedValue value = new TypedValue();
    if (!getContext().getTheme().resolveAttribute(baseColorThemeAttr, value, true)) {
      return null;
    }
    ColorStateList baseColor = AppCompatResources.getColorStateList(getContext(), value.resourceId);
    if (!getContext()
        .getTheme()
        .resolveAttribute(android.support.v7.appcompat.R.attr.colorPrimary, value, true)) {
      return null;
    }
    int colorPrimary = value.data;
    int defaultColor = baseColor.getDefaultColor();
    return new ColorStateList(
        new int[][] {DISABLED_STATE_SET, CHECKED_STATE_SET, EMPTY_STATE_SET},
        new int[] {
          baseColor.getColorForState(DISABLED_STATE_SET, defaultColor), colorPrimary, defaultColor
        });
  }

  /** Listener for handling events on navigation items. */
  public interface OnNavigationItemSelectedListener {

    /**
     * Called when an item in the navigation menu is selected.
     *
     * @param item The selected item
     * @return true to display the item as the selected item
     */
    public boolean onNavigationItemSelected(@NonNull MenuItem item);
  }

  /**
   * User interface state that is stored by NavigationView for implementing onSaveInstanceState().
   */
  public static class SavedState extends AbsSavedState {
    public Bundle menuState;

    public SavedState(Parcel in, ClassLoader loader) {
      super(in, loader);
      menuState = in.readBundle(loader);
    }

    public SavedState(Parcelable superState) {
      super(superState);
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
      super.writeToParcel(dest, flags);
      dest.writeBundle(menuState);
    }

    public static final Creator<SavedState> CREATOR =
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
