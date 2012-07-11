package com.actionbarsherlock.internal;

import com.actionbarsherlock.ActionBarSherlock;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.internal.app.ActionBarWrapper;
import com.actionbarsherlock.internal.view.menu.MenuWrapper;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.MenuInflater;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;

@ActionBarSherlock.Implementation(api = 14)
public class ActionBarSherlockNative extends ActionBarSherlock {
    private ActionBarWrapper mActionBar;
    private ActionModeWrapper mActionMode;
    private MenuWrapper mMenu;

    public ActionBarSherlockNative(Activity activity, int flags) {
        super(activity, flags);
    }


    @Override
    public ActionBar getActionBar() {
        if (DEBUG) Log.d(TAG, "[getActionBar]");

        initActionBar();
        return mActionBar;
    }

    private void initActionBar() {
        if (mActionBar != null || mActivity.getActionBar() == null) {
            return;
        }

        mActionBar = new ActionBarWrapper(mActivity);
    }

    @Override
    public void dispatchInvalidateOptionsMenu() {
        if (DEBUG) Log.d(TAG, "[dispatchInvalidateOptionsMenu]");

        mActivity.getWindow().invalidatePanelMenu(Window.FEATURE_OPTIONS_PANEL);
    }

    @Override
    public boolean dispatchCreateOptionsMenu(android.view.Menu menu) {
        if (DEBUG) Log.d(TAG, "[dispatchCreateOptionsMenu] menu: " + menu);

        if (mMenu == null || menu != mMenu.unwrap()) {
            mMenu = new MenuWrapper(menu);
        }

        final boolean result = callbackCreateOptionsMenu(mMenu);
        if (DEBUG) Log.d(TAG, "[dispatchCreateOptionsMenu] returning " + result);
        return result;
    }

    @Override
    public boolean dispatchPrepareOptionsMenu(android.view.Menu menu) {
        if (DEBUG) Log.d(TAG, "[dispatchPrepareOptionsMenu] menu: " + menu);

        final boolean result = callbackPrepareOptionsMenu(mMenu);
        if (DEBUG) Log.d(TAG, "[dispatchPrepareOptionsMenu] returning " + result);
        return result;
    }

    @Override
    public boolean dispatchOptionsItemSelected(android.view.MenuItem item) {
        if (DEBUG) Log.d(TAG, "[dispatchOptionsItemSelected] item: " + item.getTitleCondensed());

        final boolean result = callbackOptionsItemSelected(mMenu.findItem(item));
        if (DEBUG) Log.d(TAG, "[dispatchOptionsItemSelected] returning " + result);
        return result;
    }

    @Override
    public boolean hasFeature(int feature) {
        if (DEBUG) Log.d(TAG, "[hasFeature] feature: " + feature);

        final boolean result = mActivity.getWindow().hasFeature(feature);
        if (DEBUG) Log.d(TAG, "[hasFeature] returning " + result);
        return result;
    }

    @Override
    public boolean requestFeature(int featureId) {
        if (DEBUG) Log.d(TAG, "[requestFeature] featureId: " + featureId);

        final boolean result = mActivity.getWindow().requestFeature(featureId);
        if (DEBUG) Log.d(TAG, "[requestFeature] returning " + result);
        return result;
    }

    @Override
    public void setUiOptions(int uiOptions) {
        if (DEBUG) Log.d(TAG, "[setUiOptions] uiOptions: " + uiOptions);

        mActivity.getWindow().setUiOptions(uiOptions);
    }

    @Override
    public void setUiOptions(int uiOptions, int mask) {
        if (DEBUG) Log.d(TAG, "[setUiOptions] uiOptions: " + uiOptions + ", mask: " + mask);

        mActivity.getWindow().setUiOptions(uiOptions, mask);
    }

    @Override
    public void setContentView(int layoutResId) {
        if (DEBUG) Log.d(TAG, "[setContentView] layoutResId: " + layoutResId);

        mActivity.getWindow().setContentView(layoutResId);
        initActionBar();
    }

    @Override
    public void setContentView(View view, LayoutParams params) {
        if (DEBUG) Log.d(TAG, "[setContentView] view: " + view + ", params: " + params);

        mActivity.getWindow().setContentView(view, params);
        initActionBar();
    }

    @Override
    public void addContentView(View view, LayoutParams params) {
        if (DEBUG) Log.d(TAG, "[addContentView] view: " + view + ", params: " + params);

        mActivity.getWindow().addContentView(view, params);
        initActionBar();
    }

    @Override
    public void setTitle(CharSequence title) {
        if (DEBUG) Log.d(TAG, "[setTitle] title: " + title);

        mActivity.getWindow().setTitle(title);
    }

    @Override
    public void setProgressBarVisibility(boolean visible) {
        if (DEBUG) Log.d(TAG, "[setProgressBarVisibility] visible: " + visible);

        mActivity.setProgressBarVisibility(visible);
    }

    @Override
    public void setProgressBarIndeterminateVisibility(boolean visible) {
        if (DEBUG) Log.d(TAG, "[setProgressBarIndeterminateVisibility] visible: " + visible);

        mActivity.setProgressBarIndeterminateVisibility(visible);
    }

    @Override
    public void setProgressBarIndeterminate(boolean indeterminate) {
        if (DEBUG) Log.d(TAG, "[setProgressBarIndeterminate] indeterminate: " + indeterminate);

        mActivity.setProgressBarIndeterminate(indeterminate);
    }

    @Override
    public void setProgress(int progress) {
        if (DEBUG) Log.d(TAG, "[setProgress] progress: " + progress);

        mActivity.setProgress(progress);
    }

    @Override
    public void setSecondaryProgress(int secondaryProgress) {
        if (DEBUG) Log.d(TAG, "[setSecondaryProgress] secondaryProgress: " + secondaryProgress);

        mActivity.setSecondaryProgress(secondaryProgress);
    }

    @Override
    protected Context getThemedContext() {
        Context context = mActivity;
        TypedValue outValue = new TypedValue();
        mActivity.getTheme().resolveAttribute(android.R.attr.actionBarWidgetTheme, outValue, true);
        if (outValue.resourceId != 0) {
            //We are unable to test if this is the same as our current theme
            //so we just wrap it and hope that if the attribute was specified
            //then the user is intentionally specifying an alternate theme.
            context = new ContextThemeWrapper(context, outValue.resourceId);
        }
        return context;
    }

    @Override
    public ActionMode startActionMode(com.actionbarsherlock.view.ActionMode.Callback callback) {
        if (DEBUG) Log.d(TAG, "[startActionMode] callback: " + callback);

        if (mActionMode != null) {
            mActionMode.finish();
        }
        ActionModeCallbackWrapper wrapped = null;
        if (callback != null) {
            wrapped = new ActionModeCallbackWrapper(callback);
        }

        //Calling this will trigger the callback wrapper's onCreate which
        //is where we will set the new instance to mActionMode since we need
        //to pass it through to the sherlock callbacks and the call below
        //will not have returned yet to store its value.
        mActivity.startActionMode(wrapped);

        return mActionMode;
    }

    private class ActionModeCallbackWrapper implements android.view.ActionMode.Callback {
        private final ActionMode.Callback mCallback;

        public ActionModeCallbackWrapper(ActionMode.Callback callback) {
            mCallback = callback;
        }

        @Override
        public boolean onCreateActionMode(android.view.ActionMode mode, android.view.Menu menu) {
            //See ActionBarSherlockNative#startActionMode
            mActionMode = new ActionModeWrapper(mode);

            return mCallback.onCreateActionMode(mActionMode, mActionMode.getMenu());
        }

        @Override
        public boolean onPrepareActionMode(android.view.ActionMode mode, android.view.Menu menu) {
            return mCallback.onPrepareActionMode(mActionMode, mActionMode.getMenu());
        }

        @Override
        public boolean onActionItemClicked(android.view.ActionMode mode, android.view.MenuItem item) {
            return mCallback.onActionItemClicked(mActionMode, mActionMode.getMenu().findItem(item));
        }

        @Override
        public void onDestroyActionMode(android.view.ActionMode mode) {
            mCallback.onDestroyActionMode(mActionMode);
        }
    }

    private class ActionModeWrapper extends ActionMode {
        private final android.view.ActionMode mActionMode;
        private MenuWrapper mMenu = null;

        ActionModeWrapper(android.view.ActionMode actionMode) {
            mActionMode = actionMode;
        }

        @Override
        public void setTitle(CharSequence title) {
            mActionMode.setTitle(title);
        }

        @Override
        public void setTitle(int resId) {
            mActionMode.setTitle(resId);
        }

        @Override
        public void setSubtitle(CharSequence subtitle) {
            mActionMode.setSubtitle(subtitle);
        }

        @Override
        public void setSubtitle(int resId) {
            mActionMode.setSubtitle(resId);
        }

        @Override
        public void setCustomView(View view) {
            mActionMode.setCustomView(view);
        }

        @Override
        public void invalidate() {
            mActionMode.invalidate();
        }

        @Override
        public void finish() {
            mActionMode.finish();
        }

        @Override
        public MenuWrapper getMenu() {
            if (mMenu == null) {
                mMenu = new MenuWrapper(mActionMode.getMenu());
            }
            return mMenu;
        }

        @Override
        public CharSequence getTitle() {
            return mActionMode.getTitle();
        }

        @Override
        public CharSequence getSubtitle() {
            return mActionMode.getSubtitle();
        }

        @Override
        public View getCustomView() {
            return mActionMode.getCustomView();
        }

        @Override
        public MenuInflater getMenuInflater() {
            return ActionBarSherlockNative.this.getMenuInflater();
        }

        @Override
        public void setTag(Object tag) {
            mActionMode.setTag(tag);
        }

        @Override
        public Object getTag() {
            return mActionMode.getTag();
        }
    }
}
