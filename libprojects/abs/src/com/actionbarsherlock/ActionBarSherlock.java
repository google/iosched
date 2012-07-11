package com.actionbarsherlock;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.internal.ActionBarSherlockCompat;
import com.actionbarsherlock.internal.ActionBarSherlockNative;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

/**
 * <p>Helper for implementing the action bar design pattern across all versions
 * of Android.</p>
 *
 * <p>This class will manage interaction with a custom action bar based on the
 * Android 4.0 source code. The exposed API mirrors that of its native
 * counterpart and you should refer to its documentation for instruction.</p>
 *
 * @author Jake Wharton <jakewharton@gmail.com>
 */
public abstract class ActionBarSherlock {
    protected static final String TAG = "ActionBarSherlock";
    protected static final boolean DEBUG = false;

    private static final Class<?>[] CONSTRUCTOR_ARGS = new Class[] { Activity.class, int.class };
    private static final HashMap<Implementation, Class<? extends ActionBarSherlock>> IMPLEMENTATIONS =
            new HashMap<Implementation, Class<? extends ActionBarSherlock>>();

    static {
        //Register our two built-in implementations
        registerImplementation(ActionBarSherlockCompat.class);
        registerImplementation(ActionBarSherlockNative.class);
    }


    /**
     * <p>Denotes an implementation of ActionBarSherlock which provides an
     * action bar-enhanced experience.</p>
     */
    @Target(ElementType.TYPE)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Implementation {
        static final int DEFAULT_API = -1;
        static final int DEFAULT_DPI = -1;

        int api() default DEFAULT_API;
        int dpi() default DEFAULT_DPI;
    }


    /** Activity interface for menu creation callback. */
    public interface OnCreatePanelMenuListener {
        public boolean onCreatePanelMenu(int featureId, Menu menu);
    }
    /** Activity interface for menu creation callback. */
    public interface OnCreateOptionsMenuListener {
        public boolean onCreateOptionsMenu(Menu menu);
    }
    /** Activity interface for menu item selection callback. */
    public interface OnMenuItemSelectedListener {
        public boolean onMenuItemSelected(int featureId, MenuItem item);
    }
    /** Activity interface for menu item selection callback. */
    public interface OnOptionsItemSelectedListener {
        public boolean onOptionsItemSelected(MenuItem item);
    }
    /** Activity interface for menu preparation callback. */
    public interface OnPreparePanelListener {
        public boolean onPreparePanel(int featureId, View view, Menu menu);
    }
    /** Activity interface for menu preparation callback. */
    public interface OnPrepareOptionsMenuListener {
        public boolean onPrepareOptionsMenu(Menu menu);
    }
    /** Activity interface for action mode finished callback. */
    public interface OnActionModeFinishedListener {
        public void onActionModeFinished(ActionMode mode);
    }
    /** Activity interface for action mode started callback. */
    public interface OnActionModeStartedListener {
        public void onActionModeStarted(ActionMode mode);
    }


    /**
     * If set, the logic in these classes will assume that an {@link Activity}
     * is dispatching all of the required events to the class. This flag should
     * only be used internally or if you are creating your own base activity
     * modeled after one of the included types (e.g., {@code SherlockActivity}).
     */
    public static final int FLAG_DELEGATE = 1;


    /**
     * Register an ActionBarSherlock implementation.
     *
     * @param implementationClass Target implementation class which extends
     * {@link ActionBarSherlock}. This class must also be annotated with
     * {@link Implementation}.
     */
    public static void registerImplementation(Class<? extends ActionBarSherlock> implementationClass) {
        if (!implementationClass.isAnnotationPresent(Implementation.class)) {
            throw new IllegalArgumentException("Class " + implementationClass.getSimpleName() + " is not annotated with @Implementation");
        } else if (IMPLEMENTATIONS.containsValue(implementationClass)) {
            if (DEBUG) Log.w(TAG, "Class " + implementationClass.getSimpleName() + " already registered");
            return;
        }

        Implementation impl = implementationClass.getAnnotation(Implementation.class);
        if (DEBUG) Log.i(TAG, "Registering " + implementationClass.getSimpleName() + " with qualifier " + impl);
        IMPLEMENTATIONS.put(impl, implementationClass);
    }

    /**
     * Unregister an ActionBarSherlock implementation. <strong>This should be
     * considered very volatile and you should only use it if you know what
     * you are doing.</strong> You have been warned.
     *
     * @param implementationClass Target implementation class.
     * @return Boolean indicating whether the class was removed.
     */
    public static boolean unregisterImplementation(Class<? extends ActionBarSherlock> implementationClass) {
        return IMPLEMENTATIONS.values().remove(implementationClass);
    }

    /**
     * Wrap an activity with an action bar abstraction which will enable the
     * use of a custom implementation on platforms where a native version does
     * not exist.
     *
     * @param activity Activity to wrap.
     * @return Instance to interact with the action bar.
     */
    public static ActionBarSherlock wrap(Activity activity) {
        return wrap(activity, 0);
    }

    /**
     * Wrap an activity with an action bar abstraction which will enable the
     * use of a custom implementation on platforms where a native version does
     * not exist.
     *
     * @param activity Owning activity.
     * @param flags Option flags to control behavior.
     * @return Instance to interact with the action bar.
     */
    public static ActionBarSherlock wrap(Activity activity, int flags) {
        //Create a local implementation map we can modify
        HashMap<Implementation, Class<? extends ActionBarSherlock>> impls =
                new HashMap<Implementation, Class<? extends ActionBarSherlock>>(IMPLEMENTATIONS);
        boolean hasQualfier;

        /* DPI FILTERING */
        hasQualfier = false;
        for (Implementation key : impls.keySet()) {
            //Only honor TVDPI as a specific qualifier
            if (key.dpi() == DisplayMetrics.DENSITY_TV) {
                hasQualfier = true;
                break;
            }
        }
        if (hasQualfier) {
            final boolean isTvDpi = activity.getResources().getDisplayMetrics().densityDpi == DisplayMetrics.DENSITY_TV;
            for (Iterator<Implementation> keys = impls.keySet().iterator(); keys.hasNext(); ) {
                int keyDpi = keys.next().dpi();
                if ((isTvDpi && keyDpi != DisplayMetrics.DENSITY_TV)
                        || (!isTvDpi && keyDpi == DisplayMetrics.DENSITY_TV)) {
                    keys.remove();
                }
            }
        }

        /* API FILTERING */
        hasQualfier = false;
        for (Implementation key : impls.keySet()) {
            if (key.api() != Implementation.DEFAULT_API) {
                hasQualfier = true;
                break;
            }
        }
        if (hasQualfier) {
            final int runtimeApi = Build.VERSION.SDK_INT;
            int bestApi = 0;
            for (Iterator<Implementation> keys = impls.keySet().iterator(); keys.hasNext(); ) {
                int keyApi = keys.next().api();
                if (keyApi > runtimeApi) {
                    keys.remove();
                } else if (keyApi > bestApi) {
                    bestApi = keyApi;
                }
            }
            for (Iterator<Implementation> keys = impls.keySet().iterator(); keys.hasNext(); ) {
                if (keys.next().api() != bestApi) {
                    keys.remove();
                }
            }
        }

        if (impls.size() > 1) {
            throw new IllegalStateException("More than one implementation matches configuration.");
        }
        if (impls.isEmpty()) {
            throw new IllegalStateException("No implementations match configuration.");
        }
        Class<? extends ActionBarSherlock> impl = impls.values().iterator().next();
        if (DEBUG) Log.i(TAG, "Using implementation: " + impl.getSimpleName());

        try {
            Constructor<? extends ActionBarSherlock> ctor = impl.getConstructor(CONSTRUCTOR_ARGS);
            return ctor.newInstance(activity, flags);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }


    /** Activity which is displaying the action bar. Also used for context. */
    protected final Activity mActivity;
    /** Whether delegating actions for the activity or managing ourselves. */
    protected final boolean mIsDelegate;

    /** Reference to our custom menu inflater which supports action items. */
    protected MenuInflater mMenuInflater;



    protected ActionBarSherlock(Activity activity, int flags) {
        if (DEBUG) Log.d(TAG, "[<ctor>] activity: " + activity + ", flags: " + flags);

        mActivity = activity;
        mIsDelegate = (flags & FLAG_DELEGATE) != 0;
    }


    /**
     * Get the current action bar instance.
     *
     * @return Action bar instance.
     */
    public abstract ActionBar getActionBar();


    ///////////////////////////////////////////////////////////////////////////
    // Lifecycle and interaction callbacks when delegating
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Notify action bar of a configuration change event. Should be dispatched
     * after the call to the superclass implementation.
     *
     * <blockquote><pre>
     * @Override
     * public void onConfigurationChanged(Configuration newConfig) {
     *     super.onConfigurationChanged(newConfig);
     *     mSherlock.dispatchConfigurationChanged(newConfig);
     * }
     * </pre></blockquote>
     *
     * @param newConfig The new device configuration.
     */
    public void dispatchConfigurationChanged(Configuration newConfig) {}

    /**
     * Notify the action bar that the activity has finished its resuming. This
     * should be dispatched after the call to the superclass implementation.
     *
     * <blockquote><pre>
     * @Override
     * protected void onPostResume() {
     *     super.onPostResume();
     *     mSherlock.dispatchPostResume();
     * }
     * </pre></blockquote>
     */
    public void dispatchPostResume() {}

    /**
     * Notify the action bar that the activity is pausing. This should be
     * dispatched before the call to the superclass implementation.
     *
     * <blockquote><pre>
     * @Override
     * protected void onPause() {
     *     mSherlock.dispatchPause();
     *     super.onPause();
     * }
     * </pre></blockquote>
     */
    public void dispatchPause() {}

    /**
     * Notify the action bar that the activity is stopping. This should be
     * called before the superclass implementation.
     *
     * <blockquote><p>
     * @Override
     * protected void onStop() {
     *     mSherlock.dispatchStop();
     *     super.onStop();
     * }
     * </p></blockquote>
     */
    public void dispatchStop() {}

    /**
     * Indicate that the menu should be recreated by calling
     * {@link OnCreateOptionsMenuListener#onCreateOptionsMenu(com.actionbarsherlock.view.Menu)}.
     */
    public abstract void dispatchInvalidateOptionsMenu();

    /**
     * Notify the action bar that it should display its overflow menu if it is
     * appropriate for the device. The implementation should conditionally
     * call the superclass method only if this method returns {@code false}.
     *
     * <blockquote><p>
     * @Override
     * public void openOptionsMenu() {
     *     if (!mSherlock.dispatchOpenOptionsMenu()) {
     *         super.openOptionsMenu();
     *     }
     * }
     * </p></blockquote>
     *
     * @return {@code true} if the opening of the menu was handled internally.
     */
    public boolean dispatchOpenOptionsMenu() {
        return false;
    }

    /**
     * Notify the action bar that it should close its overflow menu if it is
     * appropriate for the device. This implementation should conditionally
     * call the superclass method only if this method returns {@code false}.
     *
     * <blockquote><pre>
     * @Override
     * public void closeOptionsMenu() {
     *     if (!mSherlock.dispatchCloseOptionsMenu()) {
     *         super.closeOptionsMenu();
     *     }
     * }
     * </pre></blockquote>
     *
     * @return {@code true} if the closing of the menu was handled internally.
     */
    public boolean dispatchCloseOptionsMenu() {
        return false;
    }

    /**
     * Notify the class that the activity has finished its creation. This
     * should be called after the superclass implementation.
     *
     * <blockquote><pre>
     * @Override
     * protected void onPostCreate(Bundle savedInstanceState) {
     *     mSherlock.dispatchPostCreate(savedInstanceState);
     *     super.onPostCreate(savedInstanceState);
     * }
     * </pre></blockquote>
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle
     *                           contains the data it most recently supplied in
     *                           {@link Activity#}onSaveInstanceState(Bundle)}.
     *                           <strong>Note: Otherwise it is null.</strong>
     */
    public void dispatchPostCreate(Bundle savedInstanceState) {}

    /**
     * Notify the action bar that the title has changed and the action bar
     * should be updated to reflect the change. This should be called before
     * the superclass implementation.
     *
     * <blockquote><pre>
     *  @Override
     *  protected void onTitleChanged(CharSequence title, int color) {
     *      mSherlock.dispatchTitleChanged(title, color);
     *      super.onTitleChanged(title, color);
     *  }
     * </pre></blockquote>
     *
     * @param title New activity title.
     * @param color New activity color.
     */
    public void dispatchTitleChanged(CharSequence title, int color) {}

    /**
     * Notify the action bar the user has created a key event. This is used to
     * toggle the display of the overflow action item with the menu key and to
     * close the action mode or expanded action item with the back key.
     *
     * <blockquote><pre>
     * @Override
     * public boolean dispatchKeyEvent(KeyEvent event) {
     *     if (mSherlock.dispatchKeyEvent(event)) {
     *         return true;
     *     }
     *     return super.dispatchKeyEvent(event);
     * }
     * </pre></blockquote>
     *
     * @param event Description of the key event.
     * @return {@code true} if the event was handled.
     */
    public boolean dispatchKeyEvent(KeyEvent event) {
        return false;
    }

    /**
     * Notify the action bar that the Activity has triggered a menu creation
     * which should happen on the conclusion of {@link Activity#onCreate}. This
     * will be used to gain a reference to the native menu for native and
     * overflow binding as well as to indicate when compatibility create should
     * occur for the first time.
     *
     * @param menu Activity native menu.
     * @return {@code true} since we always want to say that we have a native
     */
    public abstract boolean dispatchCreateOptionsMenu(android.view.Menu menu);

    /**
     * Notify the action bar that the Activity has triggered a menu preparation
     * which usually means that the user has requested the overflow menu via a
     * hardware menu key. You should return the result of this method call and
     * not call the superclass implementation.
     *
     * <blockquote><p>
     * @Override
     * public final boolean onPrepareOptionsMenu(android.view.Menu menu) {
     *     return mSherlock.dispatchPrepareOptionsMenu(menu);
     * }
     * </p></blockquote>
     *
     * @param menu Activity native menu.
     * @return {@code true} if menu display should proceed.
     */
    public abstract boolean dispatchPrepareOptionsMenu(android.view.Menu menu);

    /**
     * Notify the action bar that a native options menu item has been selected.
     * The implementation should return the result of this method call.
     *
     * <blockquote><p>
     * @Override
     * public final boolean onOptionsItemSelected(android.view.MenuItem item) {
     *     return mSherlock.dispatchOptionsItemSelected(item);
     * }
     * </p></blockquote>
     *
     * @param item Options menu item.
     * @return @{code true} if the selection was handled.
     */
    public abstract boolean dispatchOptionsItemSelected(android.view.MenuItem item);

    /**
     * Notify the action bar that the overflow menu has been opened. The
     * implementation should conditionally return {@code true} if this method
     * returns {@code true}, otherwise return the result of the superclass
     * method.
     *
     * <blockquote><p>
     * @Override
     * public final boolean onMenuOpened(int featureId, android.view.Menu menu) {
     *     if (mSherlock.dispatchMenuOpened(featureId, menu)) {
     *         return true;
     *     }
     *     return super.onMenuOpened(featureId, menu);
     * }
     * </p></blockquote>
     *
     * @param featureId Window feature which triggered the event.
     * @param menu Activity native menu.
     * @return {@code true} if the event was handled by this method.
     */
    public boolean dispatchMenuOpened(int featureId, android.view.Menu menu) {
        return false;
    }

    /**
     * Notify the action bar that the overflow menu has been closed. This
     * method should be called before the superclass implementation.
     *
     * <blockquote><p>
     * @Override
     * public void onPanelClosed(int featureId, android.view.Menu menu) {
     *     mSherlock.dispatchPanelClosed(featureId, menu);
     *     super.onPanelClosed(featureId, menu);
     * }
     * </p></blockquote>
     *
     * @param featureId
     * @param menu
     */
    public void dispatchPanelClosed(int featureId, android.view.Menu menu) {}

    /**
     * Notify the action bar that the activity has been destroyed. This method
     * should be called before the superclass implementation.
     *
     * <blockquote><p>
     * @Override
     * public void onDestroy() {
     *     mSherlock.dispatchDestroy();
     *     super.onDestroy();
     * }
     * </p></blockquote>
     */
    public void dispatchDestroy() {}


    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Internal method to trigger the menu creation process.
     *
     * @return {@code true} if menu creation should proceed.
     */
    protected final boolean callbackCreateOptionsMenu(Menu menu) {
        if (DEBUG) Log.d(TAG, "[callbackCreateOptionsMenu] menu: " + menu);

        boolean result = true;
        if (mActivity instanceof OnCreatePanelMenuListener) {
            OnCreatePanelMenuListener listener = (OnCreatePanelMenuListener)mActivity;
            result = listener.onCreatePanelMenu(Window.FEATURE_OPTIONS_PANEL, menu);
        } else if (mActivity instanceof OnCreateOptionsMenuListener) {
            OnCreateOptionsMenuListener listener = (OnCreateOptionsMenuListener)mActivity;
            result = listener.onCreateOptionsMenu(menu);
        }

        if (DEBUG) Log.d(TAG, "[callbackCreateOptionsMenu] returning " + result);
        return result;
    }

    /**
     * Internal method to trigger the menu preparation process.
     *
     * @return {@code true} if menu preparation should proceed.
     */
    protected final boolean callbackPrepareOptionsMenu(Menu menu) {
        if (DEBUG) Log.d(TAG, "[callbackPrepareOptionsMenu] menu: " + menu);

        boolean result = true;
        if (mActivity instanceof OnPreparePanelListener) {
            OnPreparePanelListener listener = (OnPreparePanelListener)mActivity;
            result = listener.onPreparePanel(Window.FEATURE_OPTIONS_PANEL, null, menu);
        } else if (mActivity instanceof OnPrepareOptionsMenuListener) {
            OnPrepareOptionsMenuListener listener = (OnPrepareOptionsMenuListener)mActivity;
            result = listener.onPrepareOptionsMenu(menu);
        }

        if (DEBUG) Log.d(TAG, "[callbackPrepareOptionsMenu] returning " + result);
        return result;
    }

    /**
     * Internal method for dispatching options menu selection to the owning
     * activity callback.
     *
     * @param item Selected options menu item.
     * @return {@code true} if the item selection was handled in the callback.
     */
    protected final boolean callbackOptionsItemSelected(MenuItem item) {
        if (DEBUG) Log.d(TAG, "[callbackOptionsItemSelected] item: " + item.getTitleCondensed());

        boolean result = false;
        if (mActivity instanceof OnMenuItemSelectedListener) {
            OnMenuItemSelectedListener listener = (OnMenuItemSelectedListener)mActivity;
            result = listener.onMenuItemSelected(Window.FEATURE_OPTIONS_PANEL, item);
        } else if (mActivity instanceof OnOptionsItemSelectedListener) {
            OnOptionsItemSelectedListener listener = (OnOptionsItemSelectedListener)mActivity;
            result = listener.onOptionsItemSelected(item);
        }

        if (DEBUG) Log.d(TAG, "[callbackOptionsItemSelected] returning " + result);
        return result;
    }


    ///////////////////////////////////////////////////////////////////////////
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Query for the availability of a certain feature.
     *
     * @param featureId The feature ID to check.
     * @return {@code true} if feature is enabled, {@code false} otherwise.
     */
    public abstract boolean hasFeature(int featureId);

    /**
     * Enable extended screen features. This must be called before
     * {@code setContentView()}. May be called as many times as desired as long
     * as it is before {@code setContentView()}. If not called, no extended
     * features will be available. You can not turn off a feature once it is
     * requested.
     *
     * @param featureId The desired features, defined as constants by Window.
     * @return Returns true if the requested feature is supported and now
     * enabled.
     */
    public abstract boolean requestFeature(int featureId);

    /**
     * Set extra options that will influence the UI for this window.
     *
     * @param uiOptions Flags specifying extra options for this window.
     */
    public abstract void setUiOptions(int uiOptions);

    /**
     * Set extra options that will influence the UI for this window. Only the
     * bits filtered by mask will be modified.
     *
     * @param uiOptions Flags specifying extra options for this window.
     * @param mask Flags specifying which options should be modified. Others
     *             will remain unchanged.
     */
    public abstract void setUiOptions(int uiOptions, int mask);

    /**
     * Set the content of the activity inside the action bar.
     *
     * @param layoutResId Layout resource ID.
     */
    public abstract void setContentView(int layoutResId);

    /**
     * Set the content of the activity inside the action bar.
     *
     * @param view The desired content to display.
     */
    public void setContentView(View view) {
        if (DEBUG) Log.d(TAG, "[setContentView] view: " + view);

        setContentView(view, new ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT));
    }

    /**
     * Set the content of the activity inside the action bar.
     *
     * @param view The desired content to display.
     * @param params Layout parameters to apply to the view.
     */
    public abstract void setContentView(View view, ViewGroup.LayoutParams params);

    /**
     * Variation on {@link #setContentView(android.view.View, android.view.ViewGroup.LayoutParams)}
     * to add an additional content view to the screen. Added after any
     * existing ones on the screen -- existing views are NOT removed.
     *
     * @param view The desired content to display.
     * @param params Layout parameters for the view.
     */
    public abstract void addContentView(View view, ViewGroup.LayoutParams params);

    /**
     * Change the title associated with this activity.
     */
    public abstract void setTitle(CharSequence title);

    /**
     * Change the title associated with this activity.
     */
    public void setTitle(int resId) {
        if (DEBUG) Log.d(TAG, "[setTitle] resId: " + resId);

        setTitle(mActivity.getString(resId));
    }

    /**
     * Sets the visibility of the progress bar in the title.
     * <p>
     * In order for the progress bar to be shown, the feature must be requested
     * via {@link #requestWindowFeature(int)}.
     *
     * @param visible Whether to show the progress bars in the title.
     */
    public abstract void setProgressBarVisibility(boolean visible);

    /**
     * Sets the visibility of the indeterminate progress bar in the title.
     * <p>
     * In order for the progress bar to be shown, the feature must be requested
     * via {@link #requestWindowFeature(int)}.
     *
     * @param visible Whether to show the progress bars in the title.
     */
    public abstract void setProgressBarIndeterminateVisibility(boolean visible);

    /**
     * Sets whether the horizontal progress bar in the title should be indeterminate (the circular
     * is always indeterminate).
     * <p>
     * In order for the progress bar to be shown, the feature must be requested
     * via {@link #requestWindowFeature(int)}.
     *
     * @param indeterminate Whether the horizontal progress bar should be indeterminate.
     */
    public abstract void setProgressBarIndeterminate(boolean indeterminate);

    /**
     * Sets the progress for the progress bars in the title.
     * <p>
     * In order for the progress bar to be shown, the feature must be requested
     * via {@link #requestWindowFeature(int)}.
     *
     * @param progress The progress for the progress bar. Valid ranges are from
     *            0 to 10000 (both inclusive). If 10000 is given, the progress
     *            bar will be completely filled and will fade out.
     */
    public abstract void setProgress(int progress);

    /**
     * Sets the secondary progress for the progress bar in the title. This
     * progress is drawn between the primary progress (set via
     * {@link #setProgress(int)} and the background. It can be ideal for media
     * scenarios such as showing the buffering progress while the default
     * progress shows the play progress.
     * <p>
     * In order for the progress bar to be shown, the feature must be requested
     * via {@link #requestWindowFeature(int)}.
     *
     * @param secondaryProgress The secondary progress for the progress bar. Valid ranges are from
     *            0 to 10000 (both inclusive).
     */
    public abstract void setSecondaryProgress(int secondaryProgress);

    /**
     * Get a menu inflater instance which supports the newer menu attributes.
     *
     * @return Menu inflater instance.
     */
    public MenuInflater getMenuInflater() {
        if (DEBUG) Log.d(TAG, "[getMenuInflater]");

        // Make sure that action views can get an appropriate theme.
        if (mMenuInflater == null) {
            if (getActionBar() != null) {
                mMenuInflater = new MenuInflater(getThemedContext());
            } else {
                mMenuInflater = new MenuInflater(mActivity);
            }
        }
        return mMenuInflater;
    }

    protected abstract Context getThemedContext();

    /**
     * Start an action mode.
     *
     * @param callback Callback that will manage lifecycle events for this
     *                 context mode.
     * @return The ContextMode that was started, or null if it was canceled.
     * @see ActionMode
     */
    public abstract ActionMode startActionMode(ActionMode.Callback callback);
}
