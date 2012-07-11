/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (C) 2011 Jake Wharton
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

package com.actionbarsherlock.view;

import android.content.Context;

/**
 * <p>Abstract base class for a top-level window look and behavior policy. An
 * instance of this class should be used as the top-level view added to the
 * window manager. It provides standard UI policies such as a background, title
 * area, default key processing, etc.</p>
 *
 * <p>The only existing implementation of this abstract class is
 * android.policy.PhoneWindow, which you should instantiate when needing a
 * Window. Eventually that class will be refactored and a factory method added
 * for creating Window instances without knowing about a particular
 * implementation.</p>
 */
public abstract class Window extends android.view.Window {
    public static final long FEATURE_ACTION_BAR = android.view.Window.FEATURE_ACTION_BAR;
    public static final long FEATURE_ACTION_BAR_OVERLAY = android.view.Window.FEATURE_ACTION_BAR_OVERLAY;
    public static final long FEATURE_ACTION_MODE_OVERLAY = android.view.Window.FEATURE_ACTION_MODE_OVERLAY;
    public static final long FEATURE_NO_TITLE = android.view.Window.FEATURE_NO_TITLE;
    public static final long FEATURE_PROGRESS = android.view.Window.FEATURE_PROGRESS;
    public static final long FEATURE_INDETERMINATE_PROGRESS = android.view.Window.FEATURE_INDETERMINATE_PROGRESS;

    /**
     * Create a new instance for a context.
     *
     * @param context Context.
     */
    private Window(Context context) {
        super(context);
    }


    public interface Callback {
        /**
         * Called when a panel's menu item has been selected by the user.
         *
         * @param featureId The panel that the menu is in.
         * @param item The menu item that was selected.
         *
         * @return boolean Return true to finish processing of selection, or
         *         false to perform the normal menu handling (calling its
         *         Runnable or sending a Message to its target Handler).
         */
        public boolean onMenuItemSelected(int featureId, MenuItem item);
    }
}
