package com.actionbarsherlock.internal.app;

import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.app.FragmentTransaction;
import android.view.View;
import android.widget.SpinnerAdapter;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class ActionBarWrapper extends ActionBar implements android.app.ActionBar.OnNavigationListener, android.app.ActionBar.OnMenuVisibilityListener {
    private final Activity mActivity;
    private final android.app.ActionBar mActionBar;
    private ActionBar.OnNavigationListener mNavigationListener;
    private Set<OnMenuVisibilityListener> mMenuVisibilityListeners = new HashSet<OnMenuVisibilityListener>(1);
    private FragmentTransaction mFragmentTransaction;


    public ActionBarWrapper(Activity activity) {
        mActivity = activity;
        mActionBar = activity.getActionBar();
        if (mActionBar != null) {
            mActionBar.addOnMenuVisibilityListener(this);
        }
    }


    @Override
    public void setHomeButtonEnabled(boolean enabled) {
        mActionBar.setHomeButtonEnabled(enabled);
    }

    @Override
    public Context getThemedContext() {
        return mActionBar.getThemedContext();
    }

    @Override
    public void setCustomView(View view) {
        mActionBar.setCustomView(view);
    }

    @Override
    public void setCustomView(View view, LayoutParams layoutParams) {
        android.app.ActionBar.LayoutParams lp = new android.app.ActionBar.LayoutParams(layoutParams);
        lp.gravity = layoutParams.gravity;
        lp.bottomMargin = layoutParams.bottomMargin;
        lp.topMargin = layoutParams.topMargin;
        lp.leftMargin = layoutParams.leftMargin;
        lp.rightMargin = layoutParams.rightMargin;
        mActionBar.setCustomView(view, lp);
    }

    @Override
    public void setCustomView(int resId) {
        mActionBar.setCustomView(resId);
    }

    @Override
    public void setIcon(int resId) {
        mActionBar.setIcon(resId);
    }

    @Override
    public void setIcon(Drawable icon) {
        mActionBar.setIcon(icon);
    }

    @Override
    public void setLogo(int resId) {
        mActionBar.setLogo(resId);
    }

    @Override
    public void setLogo(Drawable logo) {
        mActionBar.setLogo(logo);
    }

    @Override
    public void setListNavigationCallbacks(SpinnerAdapter adapter, OnNavigationListener callback) {
        mNavigationListener = callback;
        mActionBar.setListNavigationCallbacks(adapter, (callback != null) ? this : null);
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        //This should never be a NullPointerException since we only set
        //ourselves as the listener when the callback is not null.
        return mNavigationListener.onNavigationItemSelected(itemPosition, itemId);
    }

    @Override
    public void setSelectedNavigationItem(int position) {
        mActionBar.setSelectedNavigationItem(position);
    }

    @Override
    public int getSelectedNavigationIndex() {
        return mActionBar.getSelectedNavigationIndex();
    }

    @Override
    public int getNavigationItemCount() {
        return mActionBar.getNavigationItemCount();
    }

    @Override
    public void setTitle(CharSequence title) {
        mActionBar.setTitle(title);
    }

    @Override
    public void setTitle(int resId) {
        mActionBar.setTitle(resId);
    }

    @Override
    public void setSubtitle(CharSequence subtitle) {
        mActionBar.setSubtitle(subtitle);
    }

    @Override
    public void setSubtitle(int resId) {
        mActionBar.setSubtitle(resId);
    }

    @Override
    public void setDisplayOptions(int options) {
        mActionBar.setDisplayOptions(options);
    }

    @Override
    public void setDisplayOptions(int options, int mask) {
        mActionBar.setDisplayOptions(options, mask);
    }

    @Override
    public void setDisplayUseLogoEnabled(boolean useLogo) {
        mActionBar.setDisplayUseLogoEnabled(useLogo);
    }

    @Override
    public void setDisplayShowHomeEnabled(boolean showHome) {
        mActionBar.setDisplayShowHomeEnabled(showHome);
    }

    @Override
    public void setDisplayHomeAsUpEnabled(boolean showHomeAsUp) {
        mActionBar.setDisplayHomeAsUpEnabled(showHomeAsUp);
    }

    @Override
    public void setDisplayShowTitleEnabled(boolean showTitle) {
        mActionBar.setDisplayShowTitleEnabled(showTitle);
    }

    @Override
    public void setDisplayShowCustomEnabled(boolean showCustom) {
        mActionBar.setDisplayShowCustomEnabled(showCustom);
    }

    @Override
    public void setBackgroundDrawable(Drawable d) {
        mActionBar.setBackgroundDrawable(d);
    }

    @Override
    public void setStackedBackgroundDrawable(Drawable d) {
        mActionBar.setStackedBackgroundDrawable(d);
    }

    @Override
    public void setSplitBackgroundDrawable(Drawable d) {
        mActionBar.setSplitBackgroundDrawable(d);
    }

    @Override
    public View getCustomView() {
        return mActionBar.getCustomView();
    }

    @Override
    public CharSequence getTitle() {
        return mActionBar.getTitle();
    }

    @Override
    public CharSequence getSubtitle() {
        return mActionBar.getSubtitle();
    }

    @Override
    public int getNavigationMode() {
        return mActionBar.getNavigationMode();
    }

    @Override
    public void setNavigationMode(int mode) {
        mActionBar.setNavigationMode(mode);
    }

    @Override
    public int getDisplayOptions() {
        return mActionBar.getDisplayOptions();
    }

    public class TabWrapper extends ActionBar.Tab implements android.app.ActionBar.TabListener {
        final android.app.ActionBar.Tab mNativeTab;
        private Object mTag;
        private TabListener mListener;

        public TabWrapper(android.app.ActionBar.Tab nativeTab) {
            mNativeTab = nativeTab;
            mNativeTab.setTag(this);
        }

        @Override
        public int getPosition() {
            return mNativeTab.getPosition();
        }

        @Override
        public Drawable getIcon() {
            return mNativeTab.getIcon();
        }

        @Override
        public CharSequence getText() {
            return mNativeTab.getText();
        }

        @Override
        public Tab setIcon(Drawable icon) {
            mNativeTab.setIcon(icon);
            return this;
        }

        @Override
        public Tab setIcon(int resId) {
            mNativeTab.setIcon(resId);
            return this;
        }

        @Override
        public Tab setText(CharSequence text) {
            mNativeTab.setText(text);
            return this;
        }

        @Override
        public Tab setText(int resId) {
            mNativeTab.setText(resId);
            return this;
        }

        @Override
        public Tab setCustomView(View view) {
            mNativeTab.setCustomView(view);
            return this;
        }

        @Override
        public Tab setCustomView(int layoutResId) {
            mNativeTab.setCustomView(layoutResId);
            return this;
        }

        @Override
        public View getCustomView() {
            return mNativeTab.getCustomView();
        }

        @Override
        public Tab setTag(Object obj) {
            mTag = obj;
            return this;
        }

        @Override
        public Object getTag() {
            return mTag;
        }

        @Override
        public Tab setTabListener(TabListener listener) {
            mNativeTab.setTabListener(listener != null ? this : null);
            mListener = listener;
            return this;
        }

        @Override
        public void select() {
            mNativeTab.select();
        }

        @Override
        public Tab setContentDescription(int resId) {
            mNativeTab.setContentDescription(resId);
            return this;
        }

        @Override
        public Tab setContentDescription(CharSequence contentDesc) {
            mNativeTab.setContentDescription(contentDesc);
            return this;
        }

        @Override
        public CharSequence getContentDescription() {
            return mNativeTab.getContentDescription();
        }

        @Override
        public void onTabReselected(android.app.ActionBar.Tab tab, android.app.FragmentTransaction ft) {
            if (mListener != null) {
                FragmentTransaction trans = null;
                if (mActivity instanceof SherlockFragmentActivity) {
                    trans = ((SherlockFragmentActivity)mActivity).getSupportFragmentManager().beginTransaction()
                            .disallowAddToBackStack();
                }

                mListener.onTabReselected(this, trans);

                if (trans != null && !trans.isEmpty()) {
                    trans.commit();
                }
            }
        }

        @Override
        public void onTabSelected(android.app.ActionBar.Tab tab, android.app.FragmentTransaction ft) {
            if (mListener != null) {

                if (mFragmentTransaction == null && mActivity instanceof SherlockFragmentActivity) {
                    mFragmentTransaction = ((SherlockFragmentActivity)mActivity).getSupportFragmentManager().beginTransaction()
                            .disallowAddToBackStack();
                }

                mListener.onTabSelected(this, mFragmentTransaction);

                if (mFragmentTransaction != null) {
                    if (!mFragmentTransaction.isEmpty()) {
                        mFragmentTransaction.commit();
                    }
                    mFragmentTransaction = null;
                }
            }
        }

        @Override
        public void onTabUnselected(android.app.ActionBar.Tab tab, android.app.FragmentTransaction ft) {
            if (mListener != null) {
                FragmentTransaction trans = null;
                if (mActivity instanceof SherlockFragmentActivity) {
                    trans = ((SherlockFragmentActivity)mActivity).getSupportFragmentManager().beginTransaction()
                            .disallowAddToBackStack();
                    mFragmentTransaction = trans;
                }

                mListener.onTabUnselected(this, trans);
            }
        }
    }

    @Override
    public Tab newTab() {
        return new TabWrapper(mActionBar.newTab());
    }

    @Override
    public void addTab(Tab tab) {
        mActionBar.addTab(((TabWrapper)tab).mNativeTab);
    }

    @Override
    public void addTab(Tab tab, boolean setSelected) {
        mActionBar.addTab(((TabWrapper)tab).mNativeTab, setSelected);
    }

    @Override
    public void addTab(Tab tab, int position) {
        mActionBar.addTab(((TabWrapper)tab).mNativeTab, position);
    }

    @Override
    public void addTab(Tab tab, int position, boolean setSelected) {
        mActionBar.addTab(((TabWrapper)tab).mNativeTab, position, setSelected);
    }

    @Override
    public void removeTab(Tab tab) {
        mActionBar.removeTab(((TabWrapper)tab).mNativeTab);
    }

    @Override
    public void removeTabAt(int position) {
        mActionBar.removeTabAt(position);
    }

    @Override
    public void removeAllTabs() {
        mActionBar.removeAllTabs();
    }

    @Override
    public void selectTab(Tab tab) {
        mActionBar.selectTab(((TabWrapper)tab).mNativeTab);
    }

    @Override
    public Tab getSelectedTab() {
        android.app.ActionBar.Tab selected = mActionBar.getSelectedTab();
        return (selected != null) ? (Tab)selected.getTag() : null;
    }

    @Override
    public Tab getTabAt(int index) {
        android.app.ActionBar.Tab selected = mActionBar.getTabAt(index);
        return (selected != null) ? (Tab)selected.getTag() : null;
    }

    @Override
    public int getTabCount() {
        return mActionBar.getTabCount();
    }

    @Override
    public int getHeight() {
        return mActionBar.getHeight();
    }

    @Override
    public void show() {
        mActionBar.show();
    }

    @Override
    public void hide() {
        mActionBar.hide();
    }

    @Override
    public boolean isShowing() {
        return mActionBar.isShowing();
    }

    @Override
    public void addOnMenuVisibilityListener(OnMenuVisibilityListener listener) {
        mMenuVisibilityListeners.add(listener);
    }

    @Override
    public void removeOnMenuVisibilityListener(OnMenuVisibilityListener listener) {
        mMenuVisibilityListeners.remove(listener);
    }

    @Override
    public void onMenuVisibilityChanged(boolean isVisible) {
        for (OnMenuVisibilityListener listener : mMenuVisibilityListeners) {
            listener.onMenuVisibilityChanged(isVisible);
        }
    }
}
