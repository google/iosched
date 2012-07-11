package com.actionbarsherlock.internal;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.actionbarsherlock.internal.ResourcesCompat.getResources_getBoolean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;
import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.util.AndroidRuntimeException;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.Window;
import android.view.accessibility.AccessibilityEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.actionbarsherlock.ActionBarSherlock;
import com.actionbarsherlock.R;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.internal.app.ActionBarImpl;
import com.actionbarsherlock.internal.view.StandaloneActionMode;
import com.actionbarsherlock.internal.view.menu.ActionMenuPresenter;
import com.actionbarsherlock.internal.view.menu.MenuBuilder;
import com.actionbarsherlock.internal.view.menu.MenuItemImpl;
import com.actionbarsherlock.internal.view.menu.MenuPresenter;
import com.actionbarsherlock.internal.widget.ActionBarContainer;
import com.actionbarsherlock.internal.widget.ActionBarContextView;
import com.actionbarsherlock.internal.widget.ActionBarView;
import com.actionbarsherlock.internal.widget.IcsProgressBar;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

@ActionBarSherlock.Implementation(api = 7)
public class ActionBarSherlockCompat extends ActionBarSherlock implements MenuBuilder.Callback, com.actionbarsherlock.view.Window.Callback, MenuPresenter.Callback, android.view.MenuItem.OnMenuItemClickListener {
    /** Window features which are enabled by default. */
    protected static final int DEFAULT_FEATURES = 0;


    public ActionBarSherlockCompat(Activity activity, int flags) {
        super(activity, flags);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Properties
    ///////////////////////////////////////////////////////////////////////////

    /** Whether or not the device has a dedicated menu key button. */
    private boolean mReserveOverflow;
    /** Lazy-load indicator for {@link #mReserveOverflow}. */
    private boolean mReserveOverflowSet = false;

    /** Current menu instance for managing action items. */
    private MenuBuilder mMenu;
    /** Map between native options items and sherlock items. */
    protected HashMap<android.view.MenuItem, MenuItemImpl> mNativeItemMap;
    /** Indication of a long-press on the hardware menu key. */
    private boolean mMenuKeyIsLongPress = false;

    /** Parent view of the window decoration (action bar, mode, etc.). */
    private ViewGroup mDecor;
    /** Parent view of the activity content. */
    private ViewGroup mContentParent;

    /** Whether or not the title is stable and can be displayed. */
    private boolean mIsTitleReady = false;
    /** Whether or not the parent activity has been destroyed. */
    private boolean mIsDestroyed = false;

    /* Emulate PanelFeatureState */
    private boolean mClosingActionMenu;
    private boolean mMenuIsPrepared;
    private boolean mMenuRefreshContent;
    private Bundle mMenuFrozenActionViewState;

    /** Implementation which backs the action bar interface API. */
    private ActionBarImpl aActionBar;
    /** Main action bar view which displays the core content. */
    private ActionBarView wActionBar;
    /** Relevant window and action bar features flags. */
    private int mFeatures = DEFAULT_FEATURES;
    /** Relevant user interface option flags. */
    private int mUiOptions = 0;

    /** Decor indeterminate progress indicator. */
    private IcsProgressBar mCircularProgressBar;
    /** Decor progress indicator. */
    private IcsProgressBar mHorizontalProgressBar;

    /** Current displayed context action bar, if any. */
    private ActionMode mActionMode;
    /** Parent view in which the context action bar is displayed. */
    private ActionBarContextView mActionModeView;

    /** Title view used with dialogs. */
    private TextView mTitleView;
    /** Current activity title. */
    private CharSequence mTitle = null;
    /** Whether or not this "activity" is floating (i.e., a dialog) */
    private boolean mIsFloating;



    ///////////////////////////////////////////////////////////////////////////
    // Instance methods
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public ActionBar getActionBar() {
        if (DEBUG) Log.d(TAG, "[getActionBar]");

        initActionBar();
        return aActionBar;
    }

    private void initActionBar() {
        if (DEBUG) Log.d(TAG, "[initActionBar]");

        // Initializing the window decor can change window feature flags.
        // Make sure that we have the correct set before performing the test below.
        if (mDecor == null) {
            installDecor();
        }

        if ((aActionBar != null) || !hasFeature(Window.FEATURE_ACTION_BAR) || hasFeature(Window.FEATURE_NO_TITLE) || mActivity.isChild()) {
            return;
        }

        aActionBar = new ActionBarImpl(mActivity, mFeatures);

        if (!mIsDelegate) {
            //We may never get another chance to set the title
            wActionBar.setWindowTitle(mActivity.getTitle());
        }
    }

    @Override
    protected Context getThemedContext() {
        return aActionBar.getThemedContext();
    }

    @Override
    public void setTitle(CharSequence title) {
        if (DEBUG) Log.d(TAG, "[setTitle] title: " + title);

        dispatchTitleChanged(title, 0);
    }

    @Override
    public ActionMode startActionMode(ActionMode.Callback callback) {
        if (DEBUG) Log.d(TAG, "[startActionMode] callback: " + callback);

        if (mActionMode != null) {
            mActionMode.finish();
        }

        final ActionMode.Callback wrappedCallback = new ActionModeCallbackWrapper(callback);
        ActionMode mode = null;

        //Emulate Activity's onWindowStartingActionMode:
        initActionBar();
        if (aActionBar != null) {
            mode = aActionBar.startActionMode(wrappedCallback);
        }

        if (mode != null) {
            mActionMode = mode;
        } else {
            if (mActionModeView == null) {
                ViewStub stub = (ViewStub)mDecor.findViewById(R.id.abs__action_mode_bar_stub);
                if (stub != null) {
                    mActionModeView = (ActionBarContextView)stub.inflate();
                }
            }
            if (mActionModeView != null) {
                mActionModeView.killMode();
                mode = new StandaloneActionMode(mActivity, mActionModeView, wrappedCallback, true);
                if (callback.onCreateActionMode(mode, mode.getMenu())) {
                    mode.invalidate();
                    mActionModeView.initForMode(mode);
                    mActionModeView.setVisibility(View.VISIBLE);
                    mActionMode = mode;
                    mActionModeView.sendAccessibilityEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED);
                } else {
                    mActionMode = null;
                }
            }
        }
        if (mActionMode != null && mActivity instanceof OnActionModeStartedListener) {
            ((OnActionModeStartedListener)mActivity).onActionModeStarted(mActionMode);
        }
        return mActionMode;
    }


    ///////////////////////////////////////////////////////////////////////////
    // Lifecycle and interaction callbacks for delegation
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void dispatchConfigurationChanged(Configuration newConfig) {
        if (DEBUG) Log.d(TAG, "[dispatchConfigurationChanged] newConfig: " + newConfig);

        if (aActionBar != null) {
            aActionBar.onConfigurationChanged(newConfig);
        }
    }

    @Override
    public void dispatchPostResume() {
        if (DEBUG) Log.d(TAG, "[dispatchPostResume]");

        if (aActionBar != null) {
            aActionBar.setShowHideAnimationEnabled(true);
        }
    }

    @Override
    public void dispatchPause() {
        if (DEBUG) Log.d(TAG, "[dispatchPause]");

        if (wActionBar != null && wActionBar.isOverflowMenuShowing()) {
            wActionBar.hideOverflowMenu();
        }
    }

    @Override
    public void dispatchStop() {
        if (DEBUG) Log.d(TAG, "[dispatchStop]");

        if (aActionBar != null) {
            aActionBar.setShowHideAnimationEnabled(false);
        }
    }

    @Override
    public void dispatchInvalidateOptionsMenu() {
        if (DEBUG) Log.d(TAG, "[dispatchInvalidateOptionsMenu]");

        Bundle savedActionViewStates = null;
        if (mMenu != null) {
            savedActionViewStates = new Bundle();
            mMenu.saveActionViewStates(savedActionViewStates);
            if (savedActionViewStates.size() > 0) {
                mMenuFrozenActionViewState = savedActionViewStates;
            }
            // This will be started again when the panel is prepared.
            mMenu.stopDispatchingItemsChanged();
            mMenu.clear();
        }
        mMenuRefreshContent = true;

        // Prepare the options panel if we have an action bar
        if (wActionBar != null) {
            mMenuIsPrepared = false;
            preparePanel();
        }
    }

    @Override
    public boolean dispatchOpenOptionsMenu() {
        if (DEBUG) Log.d(TAG, "[dispatchOpenOptionsMenu]");

        if (!isReservingOverflow()) {
            return false;
        }

        return wActionBar.showOverflowMenu();
    }

    @Override
    public boolean dispatchCloseOptionsMenu() {
        if (DEBUG) Log.d(TAG, "[dispatchCloseOptionsMenu]");

        if (!isReservingOverflow()) {
            return false;
        }

        return wActionBar.hideOverflowMenu();
    }

    @Override
    public void dispatchPostCreate(Bundle savedInstanceState) {
        if (DEBUG) Log.d(TAG, "[dispatchOnPostCreate]");

        if (mIsDelegate) {
            mIsTitleReady = true;
        }

        if (mDecor == null) {
            initActionBar();
        }
    }

    @Override
    public boolean dispatchCreateOptionsMenu(android.view.Menu menu) {
        if (DEBUG) {
            Log.d(TAG, "[dispatchCreateOptionsMenu] android.view.Menu: " + menu);
            Log.d(TAG, "[dispatchCreateOptionsMenu] returning true");
        }
        return true;
    }

    @Override
    public boolean dispatchPrepareOptionsMenu(android.view.Menu menu) {
        if (DEBUG) Log.d(TAG, "[dispatchPrepareOptionsMenu] android.view.Menu: " + menu);

        if (mActionMode != null) {
            return false;
        }

        mMenuIsPrepared = false;
        if (!preparePanel()) {
            return false;
        }

        if (isReservingOverflow()) {
            return false;
        }

        if (mNativeItemMap == null) {
            mNativeItemMap = new HashMap<android.view.MenuItem, MenuItemImpl>();
        } else {
            mNativeItemMap.clear();
        }

        if (mMenu == null) {
            return false;
        }

        boolean result = mMenu.bindNativeOverflow(menu, this, mNativeItemMap);
        if (DEBUG) Log.d(TAG, "[dispatchPrepareOptionsMenu] returning " + result);
        return result;
    }

    @Override
    public boolean dispatchOptionsItemSelected(android.view.MenuItem item) {
        throw new IllegalStateException("Native callback invoked. Create a test case and report!");
    }

    @Override
    public boolean dispatchMenuOpened(int featureId, android.view.Menu menu) {
        if (DEBUG) Log.d(TAG, "[dispatchMenuOpened] featureId: " + featureId + ", menu: " + menu);

        if (featureId == Window.FEATURE_ACTION_BAR || featureId == Window.FEATURE_OPTIONS_PANEL) {
            if (aActionBar != null) {
                aActionBar.dispatchMenuVisibilityChanged(true);
            }
            return true;
        }

        return false;
    }

    @Override
    public void dispatchPanelClosed(int featureId, android.view.Menu menu){
        if (DEBUG) Log.d(TAG, "[dispatchPanelClosed] featureId: " + featureId + ", menu: " + menu);

        if (featureId == Window.FEATURE_ACTION_BAR || featureId == Window.FEATURE_OPTIONS_PANEL) {
            if (aActionBar != null) {
                aActionBar.dispatchMenuVisibilityChanged(false);
            }
        }
    }

    @Override
    public void dispatchTitleChanged(CharSequence title, int color) {
        if (DEBUG) Log.d(TAG, "[dispatchTitleChanged] title: " + title + ", color: " + color);

        if (!mIsDelegate || mIsTitleReady) {
            if (mTitleView != null) {
                mTitleView.setText(title);
            } else if (wActionBar != null) {
                wActionBar.setWindowTitle(title);
            }
        }

        mTitle = title;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (DEBUG) Log.d(TAG, "[dispatchKeyEvent] event: " + event);

        final int keyCode = event.getKeyCode();

        // Not handled by the view hierarchy, does the action bar want it
        // to cancel out of something special?
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            final int action = event.getAction();
            // Back cancels action modes first.
            if (mActionMode != null) {
                if (action == KeyEvent.ACTION_UP) {
                    mActionMode.finish();
                }
                if (DEBUG) Log.d(TAG, "[dispatchKeyEvent] returning true");
                return true;
            }

            // Next collapse any expanded action views.
            if (wActionBar != null && wActionBar.hasExpandedActionView()) {
                if (action == KeyEvent.ACTION_UP) {
                    wActionBar.collapseActionView();
                }
                if (DEBUG) Log.d(TAG, "[dispatchKeyEvent] returning true");
                return true;
            }
        }

        boolean result = false;
        if (keyCode == KeyEvent.KEYCODE_MENU && isReservingOverflow()) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.isLongPress()) {
                mMenuKeyIsLongPress = true;
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                if (!mMenuKeyIsLongPress) {
                    if (mActionMode == null && wActionBar != null) {
                        if (wActionBar.isOverflowMenuShowing()) {
                            wActionBar.hideOverflowMenu();
                        } else {
                            wActionBar.showOverflowMenu();
                        }
                    }
                    result = true;
                }
                mMenuKeyIsLongPress = false;
            }
        }

        if (DEBUG) Log.d(TAG, "[dispatchKeyEvent] returning " + result);
        return result;
    }

    @Override
    public void dispatchDestroy() {
        mIsDestroyed = true;
    }


    ///////////////////////////////////////////////////////////////////////////
    // Menu callback lifecycle and creation
    ///////////////////////////////////////////////////////////////////////////

    private boolean preparePanel() {
        // Already prepared (isPrepared will be reset to false later)
        if (mMenuIsPrepared) {
            return true;
        }

        // Init the panel state's menu--return false if init failed
        if (mMenu == null || mMenuRefreshContent) {
            if (mMenu == null) {
                if (!initializePanelMenu() || (mMenu == null)) {
                    return false;
                }
            }

            if (wActionBar != null) {
                wActionBar.setMenu(mMenu, this);
            }

            // Call callback, and return if it doesn't want to display menu.

            // Creating the panel menu will involve a lot of manipulation;
            // don't dispatch change events to presenters until we're done.
            mMenu.stopDispatchingItemsChanged();
            if (!callbackCreateOptionsMenu(mMenu)) {
                // Ditch the menu created above
                mMenu = null;

                if (wActionBar != null) {
                    // Don't show it in the action bar either
                    wActionBar.setMenu(null, this);
                }

                return false;
            }

            mMenuRefreshContent = false;
        }

        // Callback and return if the callback does not want to show the menu

        // Preparing the panel menu can involve a lot of manipulation;
        // don't dispatch change events to presenters until we're done.
        mMenu.stopDispatchingItemsChanged();

        // Restore action view state before we prepare. This gives apps
        // an opportunity to override frozen/restored state in onPrepare.
        if (mMenuFrozenActionViewState != null) {
            mMenu.restoreActionViewStates(mMenuFrozenActionViewState);
            mMenuFrozenActionViewState = null;
        }

        if (!callbackPrepareOptionsMenu(mMenu)) {
            if (wActionBar != null) {
                // The app didn't want to show the menu for now but it still exists.
                // Clear it out of the action bar.
                wActionBar.setMenu(null, this);
            }
            mMenu.startDispatchingItemsChanged();
            return false;
        }

        // Set the proper keymap
        KeyCharacterMap kmap = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD);
        mMenu.setQwertyMode(kmap.getKeyboardType() != KeyCharacterMap.NUMERIC);
        mMenu.startDispatchingItemsChanged();

        // Set other state
        mMenuIsPrepared = true;

        return true;
    }

    public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
        return callbackOptionsItemSelected(item);
    }

    public void onMenuModeChange(MenuBuilder menu) {
        reopenMenu(true);
    }

    private void reopenMenu(boolean toggleMenuMode) {
        if (wActionBar != null && wActionBar.isOverflowReserved()) {
            if (!wActionBar.isOverflowMenuShowing() || !toggleMenuMode) {
                if (wActionBar.getVisibility() == View.VISIBLE) {
                    if (callbackPrepareOptionsMenu(mMenu)) {
                        wActionBar.showOverflowMenu();
                    }
                }
            } else {
                wActionBar.hideOverflowMenu();
            }
            return;
        }
    }

    private boolean initializePanelMenu() {
        Context context = mActivity;//getContext();

        // If we have an action bar, initialize the menu with a context themed for it.
        if (wActionBar != null) {
            TypedValue outValue = new TypedValue();
            Resources.Theme currentTheme = context.getTheme();
            currentTheme.resolveAttribute(R.attr.actionBarWidgetTheme,
                    outValue, true);
            final int targetThemeRes = outValue.resourceId;

            if (targetThemeRes != 0 /*&& context.getThemeResId() != targetThemeRes*/) {
                context = new ContextThemeWrapper(context, targetThemeRes);
            }
        }

        mMenu = new MenuBuilder(context);
        mMenu.setCallback(this);

        return true;
    }

    void checkCloseActionMenu(Menu menu) {
        if (mClosingActionMenu) {
            return;
        }

        mClosingActionMenu = true;
        wActionBar.dismissPopupMenus();
        //Callback cb = getCallback();
        //if (cb != null && !isDestroyed()) {
        //    cb.onPanelClosed(FEATURE_ACTION_BAR, menu);
        //}
        mClosingActionMenu = false;
    }

    @Override
    public boolean onOpenSubMenu(MenuBuilder subMenu) {
        return true;
    }

    @Override
    public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        checkCloseActionMenu(menu);
    }

    @Override
    public boolean onMenuItemClick(android.view.MenuItem item) {
        if (DEBUG) Log.d(TAG, "[mNativeItemListener.onMenuItemClick] item: " + item);

        final MenuItemImpl sherlockItem = mNativeItemMap.get(item);
        if (sherlockItem != null) {
            sherlockItem.invoke();
        } else {
            Log.e(TAG, "Options item \"" + item + "\" not found in mapping");
        }

        return true; //Do not allow continuation of native handling
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        return callbackOptionsItemSelected(item);
    }


    ///////////////////////////////////////////////////////////////////////////
    // Progress bar interaction and internal handling
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void setProgressBarVisibility(boolean visible) {
        if (DEBUG) Log.d(TAG, "[setProgressBarVisibility] visible: " + visible);

        setFeatureInt(Window.FEATURE_PROGRESS, visible ? Window.PROGRESS_VISIBILITY_ON :
            Window.PROGRESS_VISIBILITY_OFF);
    }

    @Override
    public void setProgressBarIndeterminateVisibility(boolean visible) {
        if (DEBUG) Log.d(TAG, "[setProgressBarIndeterminateVisibility] visible: " + visible);

        setFeatureInt(Window.FEATURE_INDETERMINATE_PROGRESS,
                visible ? Window.PROGRESS_VISIBILITY_ON : Window.PROGRESS_VISIBILITY_OFF);
    }

    @Override
    public void setProgressBarIndeterminate(boolean indeterminate) {
        if (DEBUG) Log.d(TAG, "[setProgressBarIndeterminate] indeterminate: " + indeterminate);

        setFeatureInt(Window.FEATURE_PROGRESS,
                indeterminate ? Window.PROGRESS_INDETERMINATE_ON : Window.PROGRESS_INDETERMINATE_OFF);
    }

    @Override
    public void setProgress(int progress) {
        if (DEBUG) Log.d(TAG, "[setProgress] progress: " + progress);

        setFeatureInt(Window.FEATURE_PROGRESS, progress + Window.PROGRESS_START);
    }

    @Override
    public void setSecondaryProgress(int secondaryProgress) {
        if (DEBUG) Log.d(TAG, "[setSecondaryProgress] secondaryProgress: " + secondaryProgress);

        setFeatureInt(Window.FEATURE_PROGRESS,
                secondaryProgress + Window.PROGRESS_SECONDARY_START);
    }

    private void setFeatureInt(int featureId, int value) {
        updateInt(featureId, value, false);
    }

    private void updateInt(int featureId, int value, boolean fromResume) {
        // Do nothing if the decor is not yet installed... an update will
        // need to be forced when we eventually become active.
        if (mContentParent == null) {
            return;
        }

        final int featureMask = 1 << featureId;

        if ((getFeatures() & featureMask) == 0 && !fromResume) {
            return;
        }

        onIntChanged(featureId, value);
    }

    private void onIntChanged(int featureId, int value) {
        if (featureId == Window.FEATURE_PROGRESS || featureId == Window.FEATURE_INDETERMINATE_PROGRESS) {
            updateProgressBars(value);
        }
    }

    private void updateProgressBars(int value) {
        IcsProgressBar circularProgressBar = getCircularProgressBar(true);
        IcsProgressBar horizontalProgressBar = getHorizontalProgressBar(true);

        final int features = mFeatures;//getLocalFeatures();
        if (value == Window.PROGRESS_VISIBILITY_ON) {
            if ((features & (1 << Window.FEATURE_PROGRESS)) != 0) {
                int level = horizontalProgressBar.getProgress();
                int visibility = (horizontalProgressBar.isIndeterminate() || level < 10000) ?
                        View.VISIBLE : View.INVISIBLE;
                horizontalProgressBar.setVisibility(visibility);
            }
            if ((features & (1 << Window.FEATURE_INDETERMINATE_PROGRESS)) != 0) {
                circularProgressBar.setVisibility(View.VISIBLE);
            }
        } else if (value == Window.PROGRESS_VISIBILITY_OFF) {
            if ((features & (1 << Window.FEATURE_PROGRESS)) != 0) {
                horizontalProgressBar.setVisibility(View.GONE);
            }
            if ((features & (1 << Window.FEATURE_INDETERMINATE_PROGRESS)) != 0) {
                circularProgressBar.setVisibility(View.GONE);
            }
        } else if (value == Window.PROGRESS_INDETERMINATE_ON) {
            horizontalProgressBar.setIndeterminate(true);
        } else if (value == Window.PROGRESS_INDETERMINATE_OFF) {
            horizontalProgressBar.setIndeterminate(false);
        } else if (Window.PROGRESS_START <= value && value <= Window.PROGRESS_END) {
            // We want to set the progress value before testing for visibility
            // so that when the progress bar becomes visible again, it has the
            // correct level.
            horizontalProgressBar.setProgress(value - Window.PROGRESS_START);

            if (value < Window.PROGRESS_END) {
                showProgressBars(horizontalProgressBar, circularProgressBar);
            } else {
                hideProgressBars(horizontalProgressBar, circularProgressBar);
            }
        } else if (Window.PROGRESS_SECONDARY_START <= value && value <= Window.PROGRESS_SECONDARY_END) {
            horizontalProgressBar.setSecondaryProgress(value - Window.PROGRESS_SECONDARY_START);

            showProgressBars(horizontalProgressBar, circularProgressBar);
        }
    }

    private void showProgressBars(IcsProgressBar horizontalProgressBar, IcsProgressBar spinnyProgressBar) {
        final int features = mFeatures;//getLocalFeatures();
        if ((features & (1 << Window.FEATURE_INDETERMINATE_PROGRESS)) != 0 &&
                spinnyProgressBar.getVisibility() == View.INVISIBLE) {
            spinnyProgressBar.setVisibility(View.VISIBLE);
        }
        // Only show the progress bars if the primary progress is not complete
        if ((features & (1 << Window.FEATURE_PROGRESS)) != 0 &&
                horizontalProgressBar.getProgress() < 10000) {
            horizontalProgressBar.setVisibility(View.VISIBLE);
        }
    }

    private void hideProgressBars(IcsProgressBar horizontalProgressBar, IcsProgressBar spinnyProgressBar) {
        final int features = mFeatures;//getLocalFeatures();
        Animation anim = AnimationUtils.loadAnimation(mActivity, android.R.anim.fade_out);
        anim.setDuration(1000);
        if ((features & (1 << Window.FEATURE_INDETERMINATE_PROGRESS)) != 0 &&
                spinnyProgressBar.getVisibility() == View.VISIBLE) {
            spinnyProgressBar.startAnimation(anim);
            spinnyProgressBar.setVisibility(View.INVISIBLE);
        }
        if ((features & (1 << Window.FEATURE_PROGRESS)) != 0 &&
                horizontalProgressBar.getVisibility() == View.VISIBLE) {
            horizontalProgressBar.startAnimation(anim);
            horizontalProgressBar.setVisibility(View.INVISIBLE);
        }
    }

    private IcsProgressBar getCircularProgressBar(boolean shouldInstallDecor) {
        if (mCircularProgressBar != null) {
            return mCircularProgressBar;
        }
        if (mContentParent == null && shouldInstallDecor) {
            installDecor();
        }
        mCircularProgressBar = (IcsProgressBar)mDecor.findViewById(R.id.abs__progress_circular);
        if (mCircularProgressBar != null) {
            mCircularProgressBar.setVisibility(View.INVISIBLE);
        }
        return mCircularProgressBar;
    }

    private IcsProgressBar getHorizontalProgressBar(boolean shouldInstallDecor) {
        if (mHorizontalProgressBar != null) {
            return mHorizontalProgressBar;
        }
        if (mContentParent == null && shouldInstallDecor) {
            installDecor();
        }
        mHorizontalProgressBar = (IcsProgressBar)mDecor.findViewById(R.id.abs__progress_horizontal);
        if (mHorizontalProgressBar != null) {
            mHorizontalProgressBar.setVisibility(View.INVISIBLE);
        }
        return mHorizontalProgressBar;
    }


    ///////////////////////////////////////////////////////////////////////////
    // Feature management and content interaction and creation
    ///////////////////////////////////////////////////////////////////////////

    private int getFeatures() {
        if (DEBUG) Log.d(TAG, "[getFeatures] returning " + mFeatures);

        return mFeatures;
    }

    @Override
    public boolean hasFeature(int featureId) {
        if (DEBUG) Log.d(TAG, "[hasFeature] featureId: " + featureId);

        boolean result = (mFeatures & (1 << featureId)) != 0;
        if (DEBUG) Log.d(TAG, "[hasFeature] returning " + result);
        return result;
    }

    @Override
    public boolean requestFeature(int featureId) {
        if (DEBUG) Log.d(TAG, "[requestFeature] featureId: " + featureId);

        if (mContentParent != null) {
            throw new AndroidRuntimeException("requestFeature() must be called before adding content");
        }

        switch (featureId) {
            case Window.FEATURE_ACTION_BAR:
            case Window.FEATURE_ACTION_BAR_OVERLAY:
            case Window.FEATURE_ACTION_MODE_OVERLAY:
            case Window.FEATURE_INDETERMINATE_PROGRESS:
            case Window.FEATURE_NO_TITLE:
            case Window.FEATURE_PROGRESS:
                mFeatures |= (1 << featureId);
                return true;

            default:
                return false;
        }
    }

    @Override
    public void setUiOptions(int uiOptions) {
        if (DEBUG) Log.d(TAG, "[setUiOptions] uiOptions: " + uiOptions);

        mUiOptions = uiOptions;
    }

    @Override
    public void setUiOptions(int uiOptions, int mask) {
        if (DEBUG) Log.d(TAG, "[setUiOptions] uiOptions: " + uiOptions + ", mask: " + mask);

        mUiOptions = (mUiOptions & ~mask) | (uiOptions & mask);
    }

    @Override
    public void setContentView(int layoutResId) {
        if (DEBUG) Log.d(TAG, "[setContentView] layoutResId: " + layoutResId);

        if (mContentParent == null) {
            installDecor();
        } else {
            mContentParent.removeAllViews();
        }
        mActivity.getLayoutInflater().inflate(layoutResId, mContentParent);

        android.view.Window.Callback callback = mActivity.getWindow().getCallback();
        if (callback != null) {
            callback.onContentChanged();
        }

        initActionBar();
    }

    @Override
    public void setContentView(View view, ViewGroup.LayoutParams params) {
        if (DEBUG) Log.d(TAG, "[setContentView] view: " + view + ", params: " + params);

        if (mContentParent == null) {
            installDecor();
        } else {
            mContentParent.removeAllViews();
        }
        mContentParent.addView(view, params);

        android.view.Window.Callback callback = mActivity.getWindow().getCallback();
        if (callback != null) {
            callback.onContentChanged();
        }

        initActionBar();
    }

    @Override
    public void addContentView(View view, ViewGroup.LayoutParams params) {
        if (DEBUG) Log.d(TAG, "[addContentView] view: " + view + ", params: " + params);

        if (mContentParent == null) {
            installDecor();
        }
        mContentParent.addView(view, params);

        initActionBar();
    }

    private void installDecor() {
        if (DEBUG) Log.d(TAG, "[installDecor]");

        if (mDecor == null) {
            mDecor = (ViewGroup)mActivity.getWindow().getDecorView().findViewById(android.R.id.content);
        }
        if (mContentParent == null) {
            //Since we are not operating at the window level we need to take
            //into account the fact that the true decor may have already been
            //initialized and had content attached to it. If that is the case,
            //copy over its children to our new content container.
            List<View> views = null;
            if (mDecor.getChildCount() > 0) {
                views = new ArrayList<View>(1); //Usually there's only one child
                for (int i = 0, children = mDecor.getChildCount(); i < children; i++) {
                    View child = mDecor.getChildAt(0);
                    mDecor.removeView(child);
                    views.add(child);
                }
            }

            mContentParent = generateLayout();

            //Copy over the old children. See above for explanation.
            if (views != null) {
                for (View child : views) {
                    mContentParent.addView(child);
                }
            }

            mTitleView = (TextView)mDecor.findViewById(android.R.id.title);
            if (mTitleView != null) {
                if (hasFeature(Window.FEATURE_NO_TITLE)) {
                    mTitleView.setVisibility(View.GONE);
                    if (mContentParent instanceof FrameLayout) {
                        ((FrameLayout)mContentParent).setForeground(null);
                    }
                } else {
                    mTitleView.setText(mTitle);
                }
            } else {
                wActionBar = (ActionBarView)mDecor.findViewById(R.id.abs__action_bar);
                if (wActionBar != null) {
                    wActionBar.setWindowCallback(this);
                    if (wActionBar.getTitle() == null) {
                        wActionBar.setWindowTitle(mActivity.getTitle());
                    }
                    if (hasFeature(Window.FEATURE_PROGRESS)) {
                        wActionBar.initProgress();
                    }
                    if (hasFeature(Window.FEATURE_INDETERMINATE_PROGRESS)) {
                        wActionBar.initIndeterminateProgress();
                    }

                    //Since we don't require onCreate dispatching, parse for uiOptions here
                    int uiOptions = loadUiOptionsFromManifest(mActivity);
                    if (uiOptions != 0) {
                        mUiOptions = uiOptions;
                    }

                    boolean splitActionBar = false;
                    final boolean splitWhenNarrow = (mUiOptions & ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW) != 0;
                    if (splitWhenNarrow) {
                        splitActionBar = getResources_getBoolean(mActivity, R.bool.abs__split_action_bar_is_narrow);
                    } else {
                        splitActionBar = mActivity.getTheme()
                                .obtainStyledAttributes(R.styleable.SherlockTheme)
                                .getBoolean(R.styleable.SherlockTheme_windowSplitActionBar, false);
                    }
                    final ActionBarContainer splitView = (ActionBarContainer)mDecor.findViewById(R.id.abs__split_action_bar);
                    if (splitView != null) {
                        wActionBar.setSplitView(splitView);
                        wActionBar.setSplitActionBar(splitActionBar);
                        wActionBar.setSplitWhenNarrow(splitWhenNarrow);

                        mActionModeView = (ActionBarContextView)mDecor.findViewById(R.id.abs__action_context_bar);
                        mActionModeView.setSplitView(splitView);
                        mActionModeView.setSplitActionBar(splitActionBar);
                        mActionModeView.setSplitWhenNarrow(splitWhenNarrow);
                    } else if (splitActionBar) {
                        Log.e(TAG, "Requested split action bar with incompatible window decor! Ignoring request.");
                    }

                    // Post the panel invalidate for later; avoid application onCreateOptionsMenu
                    // being called in the middle of onCreate or similar.
                    mDecor.post(new Runnable() {
                        @Override
                        public void run() {
                            //Invalidate if the panel menu hasn't been created before this.
                            if (!mIsDestroyed && !mActivity.isFinishing() && mMenu == null) {
                                dispatchInvalidateOptionsMenu();
                            }
                        }
                    });
                }
            }
        }
    }

    private ViewGroup generateLayout() {
        if (DEBUG) Log.d(TAG, "[generateLayout]");

        // Apply data from current theme.

        TypedArray a = mActivity.getTheme().obtainStyledAttributes(R.styleable.SherlockTheme);

        mIsFloating = a.getBoolean(R.styleable.SherlockTheme_android_windowIsFloating, false);

        if (!a.hasValue(R.styleable.SherlockTheme_windowActionBar)) {
            throw new IllegalStateException("You must use Theme.Sherlock, Theme.Sherlock.Light, Theme.Sherlock.Light.DarkActionBar, or a derivative.");
        }

        if (a.getBoolean(R.styleable.SherlockTheme_windowNoTitle, false)) {
            requestFeature(Window.FEATURE_NO_TITLE);
        } else if (a.getBoolean(R.styleable.SherlockTheme_windowActionBar, false)) {
            // Don't allow an action bar if there is no title.
            requestFeature(Window.FEATURE_ACTION_BAR);
        }

        if (a.getBoolean(R.styleable.SherlockTheme_windowActionBarOverlay, false)) {
            requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        }

        if (a.getBoolean(R.styleable.SherlockTheme_windowActionModeOverlay, false)) {
            requestFeature(Window.FEATURE_ACTION_MODE_OVERLAY);
        }

        a.recycle();

        int layoutResource;
        if (!hasFeature(Window.FEATURE_NO_TITLE)) {
            if (mIsFloating) {
                //Trash original dialog LinearLayout
                mDecor = (ViewGroup)mDecor.getParent();
                mDecor.removeAllViews();

                layoutResource = R.layout.abs__dialog_title_holo;
            } else {
                if (hasFeature(Window.FEATURE_ACTION_BAR_OVERLAY)) {
                    layoutResource = R.layout.abs__screen_action_bar_overlay;
                } else {
                    layoutResource = R.layout.abs__screen_action_bar;
                }
            }
        } else if (hasFeature(Window.FEATURE_ACTION_MODE_OVERLAY) && !hasFeature(Window.FEATURE_NO_TITLE)) {
            layoutResource = R.layout.abs__screen_simple_overlay_action_mode;
        } else {
            layoutResource = R.layout.abs__screen_simple;
        }

        if (DEBUG) Log.d(TAG, "[generateLayout] using screen XML " + mActivity.getResources().getString(layoutResource));
        View in = mActivity.getLayoutInflater().inflate(layoutResource, null);
        mDecor.addView(in, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));

        ViewGroup contentParent = (ViewGroup)mDecor.findViewById(R.id.abs__content);
        if (contentParent == null) {
            throw new RuntimeException("Couldn't find content container view");
        }

        //Make our new child the true content view (for fragments). VERY VOLATILE!
        mDecor.setId(View.NO_ID);
        contentParent.setId(android.R.id.content);

        if (hasFeature(Window.FEATURE_INDETERMINATE_PROGRESS)) {
            IcsProgressBar progress = getCircularProgressBar(false);
            if (progress != null) {
                progress.setIndeterminate(true);
            }
        }

        return contentParent;
    }


    ///////////////////////////////////////////////////////////////////////////
    // Miscellaneous
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Determine whether or not the device has a dedicated menu key.
     *
     * @return {@code true} if native menu key is present.
     */
    private boolean isReservingOverflow() {
        if (!mReserveOverflowSet) {
            mReserveOverflow = ActionMenuPresenter.reserveOverflow(mActivity);
            mReserveOverflowSet = true;
        }
        return mReserveOverflow;
    }

    private static int loadUiOptionsFromManifest(Activity activity) {
        int uiOptions = 0;
        try {
            final String thisPackage = activity.getClass().getName();
            if (DEBUG) Log.i(TAG, "Parsing AndroidManifest.xml for " + thisPackage);

            final String packageName = activity.getApplicationInfo().packageName;
            final AssetManager am = activity.createPackageContext(packageName, 0).getAssets();
            final XmlResourceParser xml = am.openXmlResourceParser("AndroidManifest.xml");

            int eventType = xml.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String name = xml.getName();

                    if ("application".equals(name)) {
                        //Check if the <application> has the attribute
                        if (DEBUG) Log.d(TAG, "Got <application>");

                        for (int i = xml.getAttributeCount() - 1; i >= 0; i--) {
                            if (DEBUG) Log.d(TAG, xml.getAttributeName(i) + ": " + xml.getAttributeValue(i));

                            if ("uiOptions".equals(xml.getAttributeName(i))) {
                                uiOptions = xml.getAttributeIntValue(i, 0);
                                break; //out of for loop
                            }
                        }
                    } else if ("activity".equals(name)) {
                        //Check if the <activity> is us and has the attribute
                        if (DEBUG) Log.d(TAG, "Got <activity>");
                        Integer activityUiOptions = null;
                        String activityPackage = null;
                        boolean isOurActivity = false;

                        for (int i = xml.getAttributeCount() - 1; i >= 0; i--) {
                            if (DEBUG) Log.d(TAG, xml.getAttributeName(i) + ": " + xml.getAttributeValue(i));

                            //We need both uiOptions and name attributes
                            String attrName = xml.getAttributeName(i);
                            if ("uiOptions".equals(attrName)) {
                                activityUiOptions = xml.getAttributeIntValue(i, 0);
                            } else if ("name".equals(attrName)) {
                                activityPackage = cleanActivityName(packageName, xml.getAttributeValue(i));
                                if (!thisPackage.equals(activityPackage)) {
                                    break; //out of for loop
                                }
                                isOurActivity = true;
                            }

                            //Make sure we have both attributes before processing
                            if ((activityUiOptions != null) && (activityPackage != null)) {
                                //Our activity, uiOptions specified, override with our value
                                uiOptions = activityUiOptions.intValue();
                            }
                        }
                        if (isOurActivity) {
                            //If we matched our activity but it had no logo don't
                            //do any more processing of the manifest
                            break;
                        }
                    }
                }
                eventType = xml.nextToken();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (DEBUG) Log.i(TAG, "Returning " + Integer.toHexString(uiOptions));
        return uiOptions;
    }

    public static String cleanActivityName(String manifestPackage, String activityName) {
        if (activityName.charAt(0) == '.') {
            //Relative activity name (e.g., android:name=".ui.SomeClass")
            return manifestPackage + activityName;
        }
        if (activityName.indexOf('.', 1) == -1) {
            //Unqualified activity name (e.g., android:name="SomeClass")
            return manifestPackage + "." + activityName;
        }
        //Fully-qualified activity name (e.g., "com.my.package.SomeClass")
        return activityName;
    }

    /**
     * Clears out internal reference when the action mode is destroyed.
     */
    private class ActionModeCallbackWrapper implements ActionMode.Callback {
        private final ActionMode.Callback mWrapped;

        public ActionModeCallbackWrapper(ActionMode.Callback wrapped) {
            mWrapped = wrapped;
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            return mWrapped.onCreateActionMode(mode, menu);
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return mWrapped.onPrepareActionMode(mode, menu);
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            return mWrapped.onActionItemClicked(mode, item);
        }

        public void onDestroyActionMode(ActionMode mode) {
            mWrapped.onDestroyActionMode(mode);
            if (mActionModeView != null) {
                mActionModeView.setVisibility(View.GONE);
                mActionModeView.removeAllViews();
            }
            if (mActivity instanceof OnActionModeFinishedListener) {
                ((OnActionModeFinishedListener)mActivity).onActionModeFinished(mActionMode);
            }
            mActionMode = null;
        }
    }
}
