/*
 * Copyright 2011 Google Inc.
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

package com.google.android.apps.iosched.util;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;

/**
 * A <em>really</em> dumb implementation of the {@link MenuItem} interface, that's only useful for
 * our old-actionbar purposes. See <code>com.android.internal.view.menu.MenuItemImpl</code> in
 * AOSP for a more complete implementation.
 */
public class SimpleMenuItem implements MenuItem {

    private SimpleMenu mMenu;

    private final int mId;
    private final int mOrder;
    private CharSequence mTitle;
    private CharSequence mTitleCondensed;
    private Drawable mIconDrawable;
    private int mIconResId = 0;
    private boolean mEnabled = true;

    public SimpleMenuItem(SimpleMenu menu, int id, int order, CharSequence title) {
        mMenu = menu;
        mId = id;
        mOrder = order;
        mTitle = title;
    }

    public int getItemId() {
        return mId;
    }

    public int getOrder() {
        return mOrder;
    }

    public MenuItem setTitle(CharSequence title) {
        mTitle = title;
        return this;
    }

    public MenuItem setTitle(int titleRes) {
        return setTitle(mMenu.getContext().getString(titleRes));
    }

    public CharSequence getTitle() {
        return mTitle;
    }

    public MenuItem setTitleCondensed(CharSequence title) {
        mTitleCondensed = title;
        return this;
    }

    public CharSequence getTitleCondensed() {
        return mTitleCondensed != null ? mTitleCondensed : mTitle;
    }

   public MenuItem setIcon(Drawable icon) {
        mIconResId = 0;
        mIconDrawable = icon;
        return this;
    }

    public MenuItem setIcon(int iconResId) {
        mIconDrawable = null;
        mIconResId = iconResId;
        return this;
    }

    public Drawable getIcon() {
        if (mIconDrawable != null) {
            return mIconDrawable;
        }

        if (mIconResId != 0) {
            return mMenu.getResources().getDrawable(mIconResId);
        }

        return null;
    }

    public MenuItem setEnabled(boolean enabled) {
        mEnabled = enabled;
        return this;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    // No-op operations. We use no-ops to allow inflation from menu XML.

    public int getGroupId() {
        return 0;
    }

    public View getActionView() {
        return null;
    }

    public MenuItem setIntent(Intent intent) {
        // Noop
        return this;
    }

    public Intent getIntent() {
        return null;
    }

    public MenuItem setShortcut(char c, char c1) {
        // Noop
        return this;
    }

    public MenuItem setNumericShortcut(char c) {
        // Noop
        return this;
    }

    public char getNumericShortcut() {
        return 0;
    }

    public MenuItem setAlphabeticShortcut(char c) {
        // Noop
        return this;
    }

    public char getAlphabeticShortcut() {
        return 0;
    }

    public MenuItem setCheckable(boolean b) {
        // Noop
        return this;
    }

    public boolean isCheckable() {
        return false;
    }

    public MenuItem setChecked(boolean b) {
        // Noop
        return this;
    }

    public boolean isChecked() {
        return false;
    }

    public MenuItem setVisible(boolean b) {
        // Noop
        return this;
    }

    public boolean isVisible() {
        return true;
    }

    public boolean hasSubMenu() {
        return false;
    }

    public SubMenu getSubMenu() {
        return null;
    }

    public MenuItem setOnMenuItemClickListener(
            OnMenuItemClickListener onMenuItemClickListener) {
        // Noop
        return this;
    }

    public ContextMenu.ContextMenuInfo getMenuInfo() {
        return null;
    }

    public void setShowAsAction(int i) {
        // Noop
    }

    public MenuItem setActionView(View view) {
        // Noop
        return this;
    }

    public MenuItem setActionView(int i) {
        // Noop
        return this;
    }

}
