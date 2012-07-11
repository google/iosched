package com.actionbarsherlock.internal.view.menu;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import com.actionbarsherlock.internal.view.ActionProviderWrapper;
import com.actionbarsherlock.view.ActionProvider;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;

public class MenuItemWrapper implements MenuItem, android.view.MenuItem.OnMenuItemClickListener {
    private final android.view.MenuItem mNativeItem;
    private SubMenu mSubMenu = null;
    private OnMenuItemClickListener mMenuItemClickListener = null;
    private OnActionExpandListener mActionExpandListener = null;
    private android.view.MenuItem.OnActionExpandListener mNativeActionExpandListener = null;


    public MenuItemWrapper(android.view.MenuItem nativeItem) {
        if (nativeItem == null) {
            throw new IllegalStateException("Wrapped menu item cannot be null.");
        }
        mNativeItem = nativeItem;
    }


    @Override
    public int getItemId() {
        return mNativeItem.getItemId();
    }

    @Override
    public int getGroupId() {
        return mNativeItem.getGroupId();
    }

    @Override
    public int getOrder() {
        return mNativeItem.getOrder();
    }

    @Override
    public MenuItem setTitle(CharSequence title) {
        mNativeItem.setTitle(title);
        return this;
    }

    @Override
    public MenuItem setTitle(int title) {
        mNativeItem.setTitle(title);
        return this;
    }

    @Override
    public CharSequence getTitle() {
        return mNativeItem.getTitle();
    }

    @Override
    public MenuItem setTitleCondensed(CharSequence title) {
        mNativeItem.setTitleCondensed(title);
        return this;
    }

    @Override
    public CharSequence getTitleCondensed() {
        return mNativeItem.getTitleCondensed();
    }

    @Override
    public MenuItem setIcon(Drawable icon) {
        mNativeItem.setIcon(icon);
        return this;
    }

    @Override
    public MenuItem setIcon(int iconRes) {
        mNativeItem.setIcon(iconRes);
        return this;
    }

    @Override
    public Drawable getIcon() {
        return mNativeItem.getIcon();
    }

    @Override
    public MenuItem setIntent(Intent intent) {
        mNativeItem.setIntent(intent);
        return this;
    }

    @Override
    public Intent getIntent() {
        return mNativeItem.getIntent();
    }

    @Override
    public MenuItem setShortcut(char numericChar, char alphaChar) {
        mNativeItem.setShortcut(numericChar, alphaChar);
        return this;
    }

    @Override
    public MenuItem setNumericShortcut(char numericChar) {
        mNativeItem.setNumericShortcut(numericChar);
        return this;
    }

    @Override
    public char getNumericShortcut() {
        return mNativeItem.getNumericShortcut();
    }

    @Override
    public MenuItem setAlphabeticShortcut(char alphaChar) {
        mNativeItem.setAlphabeticShortcut(alphaChar);
        return this;
    }

    @Override
    public char getAlphabeticShortcut() {
        return mNativeItem.getAlphabeticShortcut();
    }

    @Override
    public MenuItem setCheckable(boolean checkable) {
        mNativeItem.setCheckable(checkable);
        return this;
    }

    @Override
    public boolean isCheckable() {
        return mNativeItem.isCheckable();
    }

    @Override
    public MenuItem setChecked(boolean checked) {
        mNativeItem.setChecked(checked);
        return this;
    }

    @Override
    public boolean isChecked() {
        return mNativeItem.isChecked();
    }

    @Override
    public MenuItem setVisible(boolean visible) {
        mNativeItem.setVisible(visible);
        return this;
    }

    @Override
    public boolean isVisible() {
        return mNativeItem.isVisible();
    }

    @Override
    public MenuItem setEnabled(boolean enabled) {
        mNativeItem.setEnabled(enabled);
        return this;
    }

    @Override
    public boolean isEnabled() {
        return mNativeItem.isEnabled();
    }

    @Override
    public boolean hasSubMenu() {
        return mNativeItem.hasSubMenu();
    }

    @Override
    public SubMenu getSubMenu() {
        if (hasSubMenu() && (mSubMenu == null)) {
            mSubMenu = new SubMenuWrapper(mNativeItem.getSubMenu());
        }
        return mSubMenu;
    }

    @Override
    public MenuItem setOnMenuItemClickListener(OnMenuItemClickListener menuItemClickListener) {
        mMenuItemClickListener = menuItemClickListener;
        //Register ourselves as the listener to proxy
        mNativeItem.setOnMenuItemClickListener(this);
        return this;
    }

    @Override
    public boolean onMenuItemClick(android.view.MenuItem item) {
        if (mMenuItemClickListener != null) {
            return mMenuItemClickListener.onMenuItemClick(this);
        }
        return false;
    }

    @Override
    public ContextMenuInfo getMenuInfo() {
        return mNativeItem.getMenuInfo();
    }

    @Override
    public void setShowAsAction(int actionEnum) {
        mNativeItem.setShowAsAction(actionEnum);
    }

    @Override
    public MenuItem setShowAsActionFlags(int actionEnum) {
        mNativeItem.setShowAsActionFlags(actionEnum);
        return this;
    }

    @Override
    public MenuItem setActionView(View view) {
        mNativeItem.setActionView(view);
        return this;
    }

    @Override
    public MenuItem setActionView(int resId) {
        mNativeItem.setActionView(resId);
        return this;
    }

    @Override
    public View getActionView() {
        return mNativeItem.getActionView();
    }

    @Override
    public MenuItem setActionProvider(ActionProvider actionProvider) {
        mNativeItem.setActionProvider(new ActionProviderWrapper(actionProvider));
        return this;
    }

    @Override
    public ActionProvider getActionProvider() {
        android.view.ActionProvider nativeProvider = mNativeItem.getActionProvider();
        if (nativeProvider != null && nativeProvider instanceof ActionProviderWrapper) {
            return ((ActionProviderWrapper)nativeProvider).unwrap();
        }
        return null;
    }

    @Override
    public boolean expandActionView() {
        return mNativeItem.expandActionView();
    }

    @Override
    public boolean collapseActionView() {
        return mNativeItem.collapseActionView();
    }

    @Override
    public boolean isActionViewExpanded() {
        return mNativeItem.isActionViewExpanded();
    }

    @Override
    public MenuItem setOnActionExpandListener(OnActionExpandListener listener) {
        mActionExpandListener = listener;

        if (mNativeActionExpandListener == null) {
            mNativeActionExpandListener = new android.view.MenuItem.OnActionExpandListener() {
                @Override
                public boolean onMenuItemActionExpand(android.view.MenuItem menuItem) {
                    if (mActionExpandListener != null) {
                        return mActionExpandListener.onMenuItemActionExpand(MenuItemWrapper.this);
                    }
                    return false;
                }

                @Override
                public boolean onMenuItemActionCollapse(android.view.MenuItem menuItem) {
                    if (mActionExpandListener != null) {
                        return mActionExpandListener.onMenuItemActionCollapse(MenuItemWrapper.this);
                    }
                    return false;
                }
            };

            //Register our inner-class as the listener to proxy method calls
            mNativeItem.setOnActionExpandListener(mNativeActionExpandListener);
        }

        return this;
    }
}
