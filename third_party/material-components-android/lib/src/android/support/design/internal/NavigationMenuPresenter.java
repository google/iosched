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

package android.support.design.internal;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.StyleRes;
import android.support.design.R;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuItemImpl;
import android.support.v7.view.menu.MenuPresenter;
import android.support.v7.view.menu.MenuView;
import android.support.v7.view.menu.SubMenuBuilder;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;

/** @hide */
@RestrictTo(LIBRARY_GROUP)
public class NavigationMenuPresenter implements MenuPresenter {

  private static final String STATE_HIERARCHY = "android:menu:list";
  private static final String STATE_ADAPTER = "android:menu:adapter";
  private static final String STATE_HEADER = "android:menu:header";

  private NavigationMenuView mMenuView;
  LinearLayout mHeaderLayout;

  private Callback mCallback;
  MenuBuilder mMenu;
  private int mId;

  NavigationMenuAdapter mAdapter;
  LayoutInflater mLayoutInflater;

  int mTextAppearance;
  boolean mTextAppearanceSet;
  ColorStateList mTextColor;
  ColorStateList mIconTintList;
  Drawable mItemBackground;

  /**
   * Padding to be inserted at the top of the list to avoid the first menu item from being placed
   * underneath the status bar.
   */
  private int mPaddingTopDefault;

  /** Padding for separators between items */
  int mPaddingSeparator;

  @Override
  public void initForMenu(Context context, MenuBuilder menu) {
    mLayoutInflater = LayoutInflater.from(context);
    mMenu = menu;
    Resources res = context.getResources();
    mPaddingSeparator =
        res.getDimensionPixelOffset(R.dimen.design_navigation_separator_vertical_padding);
  }

  @Override
  public MenuView getMenuView(ViewGroup root) {
    if (mMenuView == null) {
      mMenuView =
          (NavigationMenuView)
              mLayoutInflater.inflate(R.layout.design_navigation_menu, root, false);
      if (mAdapter == null) {
        mAdapter = new NavigationMenuAdapter();
      }
      mHeaderLayout =
          (LinearLayout)
              mLayoutInflater.inflate(R.layout.design_navigation_item_header, mMenuView, false);
      mMenuView.setAdapter(mAdapter);
    }
    return mMenuView;
  }

  @Override
  public void updateMenuView(boolean cleared) {
    if (mAdapter != null) {
      mAdapter.update();
    }
  }

  @Override
  public void setCallback(Callback cb) {
    mCallback = cb;
  }

  @Override
  public boolean onSubMenuSelected(SubMenuBuilder subMenu) {
    return false;
  }

  @Override
  public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
    if (mCallback != null) {
      mCallback.onCloseMenu(menu, allMenusAreClosing);
    }
  }

  @Override
  public boolean flagActionItems() {
    return false;
  }

  @Override
  public boolean expandItemActionView(MenuBuilder menu, MenuItemImpl item) {
    return false;
  }

  @Override
  public boolean collapseItemActionView(MenuBuilder menu, MenuItemImpl item) {
    return false;
  }

  @Override
  public int getId() {
    return mId;
  }

  public void setId(int id) {
    mId = id;
  }

  @Override
  public Parcelable onSaveInstanceState() {
    if (Build.VERSION.SDK_INT >= 11) {
      // API 9-10 does not support ClassLoaderCreator, therefore things can crash if they're
      // loaded via different loaders. Rather than crash we just won't save state on those
      // platforms
      final Bundle state = new Bundle();
      if (mMenuView != null) {
        SparseArray<Parcelable> hierarchy = new SparseArray<>();
        mMenuView.saveHierarchyState(hierarchy);
        state.putSparseParcelableArray(STATE_HIERARCHY, hierarchy);
      }
      if (mAdapter != null) {
        state.putBundle(STATE_ADAPTER, mAdapter.createInstanceState());
      }
      if (mHeaderLayout != null) {
        SparseArray<Parcelable> header = new SparseArray<>();
        mHeaderLayout.saveHierarchyState(header);
        state.putSparseParcelableArray(STATE_HEADER, header);
      }
      return state;
    }
    return null;
  }

  @Override
  public void onRestoreInstanceState(final Parcelable parcelable) {
    if (parcelable instanceof Bundle) {
      Bundle state = (Bundle) parcelable;
      SparseArray<Parcelable> hierarchy = state.getSparseParcelableArray(STATE_HIERARCHY);
      if (hierarchy != null) {
        mMenuView.restoreHierarchyState(hierarchy);
      }
      Bundle adapterState = state.getBundle(STATE_ADAPTER);
      if (adapterState != null) {
        mAdapter.restoreInstanceState(adapterState);
      }
      SparseArray<Parcelable> header = state.getSparseParcelableArray(STATE_HEADER);
      if (header != null) {
        mHeaderLayout.restoreHierarchyState(header);
      }
    }
  }

  public void setCheckedItem(MenuItemImpl item) {
    mAdapter.setCheckedItem(item);
  }

  public View inflateHeaderView(@LayoutRes int res) {
    View view = mLayoutInflater.inflate(res, mHeaderLayout, false);
    addHeaderView(view);
    return view;
  }

  public void addHeaderView(@NonNull View view) {
    mHeaderLayout.addView(view);
    // The padding on top should be cleared.
    mMenuView.setPadding(0, 0, 0, mMenuView.getPaddingBottom());
  }

  public void removeHeaderView(@NonNull View view) {
    mHeaderLayout.removeView(view);
    if (mHeaderLayout.getChildCount() == 0) {
      mMenuView.setPadding(0, mPaddingTopDefault, 0, mMenuView.getPaddingBottom());
    }
  }

  public int getHeaderCount() {
    return mHeaderLayout.getChildCount();
  }

  public View getHeaderView(int index) {
    return mHeaderLayout.getChildAt(index);
  }

  @Nullable
  public ColorStateList getItemTintList() {
    return mIconTintList;
  }

  public void setItemIconTintList(@Nullable ColorStateList tint) {
    mIconTintList = tint;
    updateMenuView(false);
  }

  @Nullable
  public ColorStateList getItemTextColor() {
    return mTextColor;
  }

  public void setItemTextColor(@Nullable ColorStateList textColor) {
    mTextColor = textColor;
    updateMenuView(false);
  }

  public void setItemTextAppearance(@StyleRes int resId) {
    mTextAppearance = resId;
    mTextAppearanceSet = true;
    updateMenuView(false);
  }

  @Nullable
  public Drawable getItemBackground() {
    return mItemBackground;
  }

  public void setItemBackground(@Nullable Drawable itemBackground) {
    mItemBackground = itemBackground;
    updateMenuView(false);
  }

  public void setUpdateSuspended(boolean updateSuspended) {
    if (mAdapter != null) {
      mAdapter.setUpdateSuspended(updateSuspended);
    }
  }

  public void dispatchApplyWindowInsets(WindowInsetsCompat insets) {
    int top = insets.getSystemWindowInsetTop();
    if (mPaddingTopDefault != top) {
      mPaddingTopDefault = top;
      if (mHeaderLayout.getChildCount() == 0) {
        mMenuView.setPadding(0, mPaddingTopDefault, 0, mMenuView.getPaddingBottom());
      }
    }
    ViewCompat.dispatchApplyWindowInsets(mHeaderLayout, insets);
  }

  private abstract static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View itemView) {
      super(itemView);
    }
  }

  private static class NormalViewHolder extends ViewHolder {

    public NormalViewHolder(
        LayoutInflater inflater, ViewGroup parent, View.OnClickListener listener) {
      super(inflater.inflate(R.layout.design_navigation_item, parent, false));
      itemView.setOnClickListener(listener);
    }
  }

  private static class SubheaderViewHolder extends ViewHolder {

    public SubheaderViewHolder(LayoutInflater inflater, ViewGroup parent) {
      super(inflater.inflate(R.layout.design_navigation_item_subheader, parent, false));
    }
  }

  private static class SeparatorViewHolder extends ViewHolder {

    public SeparatorViewHolder(LayoutInflater inflater, ViewGroup parent) {
      super(inflater.inflate(R.layout.design_navigation_item_separator, parent, false));
    }
  }

  private static class HeaderViewHolder extends ViewHolder {

    public HeaderViewHolder(View itemView) {
      super(itemView);
    }
  }

  /**
   * Handles click events for the menu items. The items has to be {@link NavigationMenuItemView}.
   */
  final View.OnClickListener mOnClickListener =
      new View.OnClickListener() {

        @Override
        public void onClick(View v) {
          NavigationMenuItemView itemView = (NavigationMenuItemView) v;
          setUpdateSuspended(true);
          MenuItemImpl item = itemView.getItemData();
          boolean result = mMenu.performItemAction(item, NavigationMenuPresenter.this, 0);
          if (item != null && item.isCheckable() && result) {
            mAdapter.setCheckedItem(item);
          }
          setUpdateSuspended(false);
          updateMenuView(false);
        }
      };

  private class NavigationMenuAdapter extends RecyclerView.Adapter<ViewHolder> {

    private static final String STATE_CHECKED_ITEM = "android:menu:checked";

    private static final String STATE_ACTION_VIEWS = "android:menu:action_views";
    private static final int VIEW_TYPE_NORMAL = 0;
    private static final int VIEW_TYPE_SUBHEADER = 1;
    private static final int VIEW_TYPE_SEPARATOR = 2;
    private static final int VIEW_TYPE_HEADER = 3;

    private final ArrayList<NavigationMenuItem> mItems = new ArrayList<>();
    private MenuItemImpl mCheckedItem;
    private boolean mUpdateSuspended;

    NavigationMenuAdapter() {
      prepareMenuItems();
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public int getItemCount() {
      return mItems.size();
    }

    @Override
    public int getItemViewType(int position) {
      NavigationMenuItem item = mItems.get(position);
      if (item instanceof NavigationMenuSeparatorItem) {
        return VIEW_TYPE_SEPARATOR;
      } else if (item instanceof NavigationMenuHeaderItem) {
        return VIEW_TYPE_HEADER;
      } else if (item instanceof NavigationMenuTextItem) {
        NavigationMenuTextItem textItem = (NavigationMenuTextItem) item;
        if (textItem.getMenuItem().hasSubMenu()) {
          return VIEW_TYPE_SUBHEADER;
        } else {
          return VIEW_TYPE_NORMAL;
        }
      }
      throw new RuntimeException("Unknown item type.");
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
      switch (viewType) {
        case VIEW_TYPE_NORMAL:
          return new NormalViewHolder(mLayoutInflater, parent, mOnClickListener);
        case VIEW_TYPE_SUBHEADER:
          return new SubheaderViewHolder(mLayoutInflater, parent);
        case VIEW_TYPE_SEPARATOR:
          return new SeparatorViewHolder(mLayoutInflater, parent);
        case VIEW_TYPE_HEADER:
          return new HeaderViewHolder(mHeaderLayout);
      }
      return null;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
      switch (getItemViewType(position)) {
        case VIEW_TYPE_NORMAL:
          {
            NavigationMenuItemView itemView = (NavigationMenuItemView) holder.itemView;
            itemView.setIconTintList(mIconTintList);
            if (mTextAppearanceSet) {
              itemView.setTextAppearance(mTextAppearance);
            }
            if (mTextColor != null) {
              itemView.setTextColor(mTextColor);
            }
            ViewCompat.setBackground(
                itemView,
                mItemBackground != null ? mItemBackground.getConstantState().newDrawable() : null);
            NavigationMenuTextItem item = (NavigationMenuTextItem) mItems.get(position);
            itemView.setNeedsEmptyIcon(item.needsEmptyIcon);
            itemView.initialize(item.getMenuItem(), 0);
            break;
          }
        case VIEW_TYPE_SUBHEADER:
          {
            TextView subHeader = (TextView) holder.itemView;
            NavigationMenuTextItem item = (NavigationMenuTextItem) mItems.get(position);
            subHeader.setText(item.getMenuItem().getTitle());
            break;
          }
        case VIEW_TYPE_SEPARATOR:
          {
            NavigationMenuSeparatorItem item = (NavigationMenuSeparatorItem) mItems.get(position);
            holder.itemView.setPadding(0, item.getPaddingTop(), 0, item.getPaddingBottom());
            break;
          }
        case VIEW_TYPE_HEADER:
          {
            break;
          }
      }
    }

    @Override
    public void onViewRecycled(ViewHolder holder) {
      if (holder instanceof NormalViewHolder) {
        ((NavigationMenuItemView) holder.itemView).recycle();
      }
    }

    public void update() {
      prepareMenuItems();
      notifyDataSetChanged();
    }

    /**
     * Flattens the visible menu items of {@link #mMenu} into {@link #mItems}, while inserting
     * separators between items when necessary.
     */
    private void prepareMenuItems() {
      if (mUpdateSuspended) {
        return;
      }
      mUpdateSuspended = true;
      mItems.clear();
      mItems.add(new NavigationMenuHeaderItem());

      int currentGroupId = -1;
      int currentGroupStart = 0;
      boolean currentGroupHasIcon = false;
      for (int i = 0, totalSize = mMenu.getVisibleItems().size(); i < totalSize; i++) {
        MenuItemImpl item = mMenu.getVisibleItems().get(i);
        if (item.isChecked()) {
          setCheckedItem(item);
        }
        if (item.isCheckable()) {
          item.setExclusiveCheckable(false);
        }
        if (item.hasSubMenu()) {
          SubMenu subMenu = item.getSubMenu();
          if (subMenu.hasVisibleItems()) {
            if (i != 0) {
              mItems.add(new NavigationMenuSeparatorItem(mPaddingSeparator, 0));
            }
            mItems.add(new NavigationMenuTextItem(item));
            boolean subMenuHasIcon = false;
            int subMenuStart = mItems.size();
            for (int j = 0, size = subMenu.size(); j < size; j++) {
              MenuItemImpl subMenuItem = (MenuItemImpl) subMenu.getItem(j);
              if (subMenuItem.isVisible()) {
                if (!subMenuHasIcon && subMenuItem.getIcon() != null) {
                  subMenuHasIcon = true;
                }
                if (subMenuItem.isCheckable()) {
                  subMenuItem.setExclusiveCheckable(false);
                }
                if (item.isChecked()) {
                  setCheckedItem(item);
                }
                mItems.add(new NavigationMenuTextItem(subMenuItem));
              }
            }
            if (subMenuHasIcon) {
              appendTransparentIconIfMissing(subMenuStart, mItems.size());
            }
          }
        } else {
          int groupId = item.getGroupId();
          if (groupId != currentGroupId) { // first item in group
            currentGroupStart = mItems.size();
            currentGroupHasIcon = item.getIcon() != null;
            if (i != 0) {
              currentGroupStart++;
              mItems.add(new NavigationMenuSeparatorItem(mPaddingSeparator, mPaddingSeparator));
            }
          } else if (!currentGroupHasIcon && item.getIcon() != null) {
            currentGroupHasIcon = true;
            appendTransparentIconIfMissing(currentGroupStart, mItems.size());
          }
          NavigationMenuTextItem textItem = new NavigationMenuTextItem(item);
          textItem.needsEmptyIcon = currentGroupHasIcon;
          mItems.add(textItem);
          currentGroupId = groupId;
        }
      }
      mUpdateSuspended = false;
    }

    private void appendTransparentIconIfMissing(int startIndex, int endIndex) {
      for (int i = startIndex; i < endIndex; i++) {
        NavigationMenuTextItem textItem = (NavigationMenuTextItem) mItems.get(i);
        textItem.needsEmptyIcon = true;
      }
    }

    public void setCheckedItem(MenuItemImpl checkedItem) {
      if (mCheckedItem == checkedItem || !checkedItem.isCheckable()) {
        return;
      }
      if (mCheckedItem != null) {
        mCheckedItem.setChecked(false);
      }
      mCheckedItem = checkedItem;
      checkedItem.setChecked(true);
    }

    public Bundle createInstanceState() {
      Bundle state = new Bundle();
      if (mCheckedItem != null) {
        state.putInt(STATE_CHECKED_ITEM, mCheckedItem.getItemId());
      }
      // Store the states of the action views.
      SparseArray<ParcelableSparseArray> actionViewStates = new SparseArray<>();
      for (int i = 0, size = mItems.size(); i < size; i++) {
        NavigationMenuItem navigationMenuItem = mItems.get(i);
        if (navigationMenuItem instanceof NavigationMenuTextItem) {
          MenuItemImpl item = ((NavigationMenuTextItem) navigationMenuItem).getMenuItem();
          View actionView = item != null ? item.getActionView() : null;
          if (actionView != null) {
            ParcelableSparseArray container = new ParcelableSparseArray();
            actionView.saveHierarchyState(container);
            actionViewStates.put(item.getItemId(), container);
          }
        }
      }
      state.putSparseParcelableArray(STATE_ACTION_VIEWS, actionViewStates);
      return state;
    }

    public void restoreInstanceState(Bundle state) {
      int checkedItem = state.getInt(STATE_CHECKED_ITEM, 0);
      if (checkedItem != 0) {
        mUpdateSuspended = true;
        for (int i = 0, size = mItems.size(); i < size; i++) {
          NavigationMenuItem item = mItems.get(i);
          if (item instanceof NavigationMenuTextItem) {
            MenuItemImpl menuItem = ((NavigationMenuTextItem) item).getMenuItem();
            if (menuItem != null && menuItem.getItemId() == checkedItem) {
              setCheckedItem(menuItem);
              break;
            }
          }
        }
        mUpdateSuspended = false;
        prepareMenuItems();
      }
      // Restore the states of the action views.
      SparseArray<ParcelableSparseArray> actionViewStates =
          state.getSparseParcelableArray(STATE_ACTION_VIEWS);
      if (actionViewStates != null) {
        for (int i = 0, size = mItems.size(); i < size; i++) {
          NavigationMenuItem navigationMenuItem = mItems.get(i);
          if (!(navigationMenuItem instanceof NavigationMenuTextItem)) {
            continue;
          }
          MenuItemImpl item = ((NavigationMenuTextItem) navigationMenuItem).getMenuItem();
          if (item == null) {
            continue;
          }
          View actionView = item.getActionView();
          if (actionView == null) {
            continue;
          }
          ParcelableSparseArray container = actionViewStates.get(item.getItemId());
          if (container == null) {
            continue;
          }
          actionView.restoreHierarchyState(container);
        }
      }
    }

    public void setUpdateSuspended(boolean updateSuspended) {
      mUpdateSuspended = updateSuspended;
    }
  }

  /** Unified data model for all sorts of navigation menu items. */
  private interface NavigationMenuItem {}

  /** Normal or subheader items. */
  private static class NavigationMenuTextItem implements NavigationMenuItem {

    private final MenuItemImpl mMenuItem;

    boolean needsEmptyIcon;

    NavigationMenuTextItem(MenuItemImpl item) {
      mMenuItem = item;
    }

    public MenuItemImpl getMenuItem() {
      return mMenuItem;
    }
  }

  /** Separator items. */
  private static class NavigationMenuSeparatorItem implements NavigationMenuItem {

    private final int mPaddingTop;

    private final int mPaddingBottom;

    public NavigationMenuSeparatorItem(int paddingTop, int paddingBottom) {
      mPaddingTop = paddingTop;
      mPaddingBottom = paddingBottom;
    }

    public int getPaddingTop() {
      return mPaddingTop;
    }

    public int getPaddingBottom() {
      return mPaddingBottom;
    }
  }

  /** Header (not subheader) items. */
  private static class NavigationMenuHeaderItem implements NavigationMenuItem {
    NavigationMenuHeaderItem() {}
    // The actual content is hold by NavigationMenuPresenter#mHeaderLayout.
  }
}
