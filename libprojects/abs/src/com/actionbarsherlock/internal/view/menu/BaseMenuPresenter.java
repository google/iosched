/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.actionbarsherlock.internal.view.menu;

import java.util.ArrayList;
import android.content.Context;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Base class for MenuPresenters that have a consistent container view and item
 * views. Behaves similarly to an AdapterView in that existing item views will
 * be reused if possible when items change.
 */
public abstract class BaseMenuPresenter implements MenuPresenter {
    private static final boolean IS_HONEYCOMB = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;

    protected Context mSystemContext;
    protected Context mContext;
    protected MenuBuilder mMenu;
    protected LayoutInflater mSystemInflater;
    protected LayoutInflater mInflater;
    private Callback mCallback;

    private int mMenuLayoutRes;
    private int mItemLayoutRes;

    protected MenuView mMenuView;

    private int mId;

    /**
     * Construct a new BaseMenuPresenter.
     *
     * @param context Context for generating system-supplied views
     * @param menuLayoutRes Layout resource ID for the menu container view
     * @param itemLayoutRes Layout resource ID for a single item view
     */
    public BaseMenuPresenter(Context context, int menuLayoutRes, int itemLayoutRes) {
        mSystemContext = context;
        mSystemInflater = LayoutInflater.from(context);
        mMenuLayoutRes = menuLayoutRes;
        mItemLayoutRes = itemLayoutRes;
    }

    @Override
    public void initForMenu(Context context, MenuBuilder menu) {
        mContext = context;
        mInflater = LayoutInflater.from(mContext);
        mMenu = menu;
    }

    @Override
    public MenuView getMenuView(ViewGroup root) {
        if (mMenuView == null) {
            mMenuView = (MenuView) mSystemInflater.inflate(mMenuLayoutRes, root, false);
            mMenuView.initialize(mMenu);
            updateMenuView(true);
        }

        return mMenuView;
    }

    /**
     * Reuses item views when it can
     */
    public void updateMenuView(boolean cleared) {
        final ViewGroup parent = (ViewGroup) mMenuView;
        if (parent == null) return;

        int childIndex = 0;
        if (mMenu != null) {
            mMenu.flagActionItems();
            ArrayList<MenuItemImpl> visibleItems = mMenu.getVisibleItems();
            final int itemCount = visibleItems.size();
            for (int i = 0; i < itemCount; i++) {
                MenuItemImpl item = visibleItems.get(i);
                if (shouldIncludeItem(childIndex, item)) {
                    final View convertView = parent.getChildAt(childIndex);
                    final MenuItemImpl oldItem = convertView instanceof MenuView.ItemView ?
                            ((MenuView.ItemView) convertView).getItemData() : null;
                    final View itemView = getItemView(item, convertView, parent);
                    if (item != oldItem) {
                        // Don't let old states linger with new data.
                        itemView.setPressed(false);
                        if (IS_HONEYCOMB) itemView.jumpDrawablesToCurrentState();
                    }
                    if (itemView != convertView) {
                        addItemView(itemView, childIndex);
                    }
                    childIndex++;
                }
            }
        }

        // Remove leftover views.
        while (childIndex < parent.getChildCount()) {
            if (!filterLeftoverView(parent, childIndex)) {
                childIndex++;
            }
        }
    }

    /**
     * Add an item view at the given index.
     *
     * @param itemView View to add
     * @param childIndex Index within the parent to insert at
     */
    protected void addItemView(View itemView, int childIndex) {
        final ViewGroup currentParent = (ViewGroup) itemView.getParent();
        if (currentParent != null) {
            currentParent.removeView(itemView);
        }
        ((ViewGroup) mMenuView).addView(itemView, childIndex);
    }

    /**
     * Filter the child view at index and remove it if appropriate.
     * @param parent Parent to filter from
     * @param childIndex Index to filter
     * @return true if the child view at index was removed
     */
    protected boolean filterLeftoverView(ViewGroup parent, int childIndex) {
        parent.removeViewAt(childIndex);
        return true;
    }

    public void setCallback(Callback cb) {
        mCallback = cb;
    }

    /**
     * Create a new item view that can be re-bound to other item data later.
     *
     * @return The new item view
     */
    public MenuView.ItemView createItemView(ViewGroup parent) {
        return (MenuView.ItemView) mSystemInflater.inflate(mItemLayoutRes, parent, false);
    }

    /**
     * Prepare an item view for use. See AdapterView for the basic idea at work here.
     * This may require creating a new item view, but well-behaved implementations will
     * re-use the view passed as convertView if present. The returned view will be populated
     * with data from the item parameter.
     *
     * @param item Item to present
     * @param convertView Existing view to reuse
     * @param parent Intended parent view - use for inflation.
     * @return View that presents the requested menu item
     */
    public View getItemView(MenuItemImpl item, View convertView, ViewGroup parent) {
        MenuView.ItemView itemView;
        if (convertView instanceof MenuView.ItemView) {
            itemView = (MenuView.ItemView) convertView;
        } else {
            itemView = createItemView(parent);
        }
        bindItemView(item, itemView);
        return (View) itemView;
    }

    /**
     * Bind item data to an existing item view.
     *
     * @param item Item to bind
     * @param itemView View to populate with item data
     */
    public abstract void bindItemView(MenuItemImpl item, MenuView.ItemView itemView);

    /**
     * Filter item by child index and item data.
     *
     * @param childIndex Indended presentation index of this item
     * @param item Item to present
     * @return true if this item should be included in this menu presentation; false otherwise
     */
    public boolean shouldIncludeItem(int childIndex, MenuItemImpl item) {
        return true;
    }

    public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        if (mCallback != null) {
            mCallback.onCloseMenu(menu, allMenusAreClosing);
        }
    }

    public boolean onSubMenuSelected(SubMenuBuilder menu) {
        if (mCallback != null) {
            return mCallback.onOpenSubMenu(menu);
        }
        return false;
    }

    public boolean flagActionItems() {
        return false;
    }

    public boolean expandItemActionView(MenuBuilder menu, MenuItemImpl item) {
        return false;
    }

    public boolean collapseItemActionView(MenuBuilder menu, MenuItemImpl item) {
        return false;
    }

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }
}
