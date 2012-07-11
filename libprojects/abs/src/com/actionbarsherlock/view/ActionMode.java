/*
 * Copyright (C) 2010 The Android Open Source Project
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

import android.view.View;


/**
 * Represents a contextual mode of the user interface. Action modes can be used for
 * modal interactions with content and replace parts of the normal UI until finished.
 * Examples of good action modes include selection modes, search, content editing, etc.
 */
public abstract class ActionMode {
    private Object mTag;

    /**
     * Set a tag object associated with this ActionMode.
     *
     * <p>Like the tag available to views, this allows applications to associate arbitrary
     * data with an ActionMode for later reference.
     *
     * @param tag Tag to associate with this ActionMode
     *
     * @see #getTag()
     */
    public void setTag(Object tag) {
        mTag = tag;
    }

    /**
     * Retrieve the tag object associated with this ActionMode.
     *
     * <p>Like the tag available to views, this allows applications to associate arbitrary
     * data with an ActionMode for later reference.
     *
     * @return Tag associated with this ActionMode
     *
     * @see #setTag(Object)
     */
    public Object getTag() {
        return mTag;
    }

    /**
     * Set the title of the action mode. This method will have no visible effect if
     * a custom view has been set.
     *
     * @param title Title string to set
     *
     * @see #setTitle(int)
     * @see #setCustomView(View)
     */
    public abstract void setTitle(CharSequence title);

    /**
     * Set the title of the action mode. This method will have no visible effect if
     * a custom view has been set.
     *
     * @param resId Resource ID of a string to set as the title
     *
     * @see #setTitle(CharSequence)
     * @see #setCustomView(View)
     */
    public abstract void setTitle(int resId);

    /**
     * Set the subtitle of the action mode. This method will have no visible effect if
     * a custom view has been set.
     *
     * @param subtitle Subtitle string to set
     *
     * @see #setSubtitle(int)
     * @see #setCustomView(View)
     */
    public abstract void setSubtitle(CharSequence subtitle);

    /**
     * Set the subtitle of the action mode. This method will have no visible effect if
     * a custom view has been set.
     *
     * @param resId Resource ID of a string to set as the subtitle
     *
     * @see #setSubtitle(CharSequence)
     * @see #setCustomView(View)
     */
    public abstract void setSubtitle(int resId);

    /**
     * Set a custom view for this action mode. The custom view will take the place of
     * the title and subtitle. Useful for things like search boxes.
     *
     * @param view Custom view to use in place of the title/subtitle.
     *
     * @see #setTitle(CharSequence)
     * @see #setSubtitle(CharSequence)
     */
    public abstract void setCustomView(View view);

    /**
     * Invalidate the action mode and refresh menu content. The mode's
     * {@link ActionMode.Callback} will have its
     * {@link Callback#onPrepareActionMode(ActionMode, Menu)} method called.
     * If it returns true the menu will be scanned for updated content and any relevant changes
     * will be reflected to the user.
     */
    public abstract void invalidate();

    /**
     * Finish and close this action mode. The action mode's {@link ActionMode.Callback} will
     * have its {@link Callback#onDestroyActionMode(ActionMode)} method called.
     */
    public abstract void finish();

    /**
     * Returns the menu of actions that this action mode presents.
     * @return The action mode's menu.
     */
    public abstract Menu getMenu();

    /**
     * Returns the current title of this action mode.
     * @return Title text
     */
    public abstract CharSequence getTitle();

    /**
     * Returns the current subtitle of this action mode.
     * @return Subtitle text
     */
    public abstract CharSequence getSubtitle();

    /**
     * Returns the current custom view for this action mode.
     * @return The current custom view
     */
    public abstract View getCustomView();

    /**
     * Returns a {@link MenuInflater} with the ActionMode's context.
     */
    public abstract MenuInflater getMenuInflater();

    /**
     * Returns whether the UI presenting this action mode can take focus or not.
     * This is used by internal components within the framework that would otherwise
     * present an action mode UI that requires focus, such as an EditText as a custom view.
     *
     * @return true if the UI used to show this action mode can take focus
     * @hide Internal use only
     */
    public boolean isUiFocusable() {
        return true;
    }

    /**
     * Callback interface for action modes. Supplied to
     * {@link View#startActionMode(Callback)}, a Callback
     * configures and handles events raised by a user's interaction with an action mode.
     *
     * <p>An action mode's lifecycle is as follows:
     * <ul>
     * <li>{@link Callback#onCreateActionMode(ActionMode, Menu)} once on initial
     * creation</li>
     * <li>{@link Callback#onPrepareActionMode(ActionMode, Menu)} after creation
     * and any time the {@link ActionMode} is invalidated</li>
     * <li>{@link Callback#onActionItemClicked(ActionMode, MenuItem)} any time a
     * contextual action button is clicked</li>
     * <li>{@link Callback#onDestroyActionMode(ActionMode)} when the action mode
     * is closed</li>
     * </ul>
     */
    public interface Callback {
        /**
         * Called when action mode is first created. The menu supplied will be used to
         * generate action buttons for the action mode.
         *
         * @param mode ActionMode being created
         * @param menu Menu used to populate action buttons
         * @return true if the action mode should be created, false if entering this
         *              mode should be aborted.
         */
        public boolean onCreateActionMode(ActionMode mode, Menu menu);

        /**
         * Called to refresh an action mode's action menu whenever it is invalidated.
         *
         * @param mode ActionMode being prepared
         * @param menu Menu used to populate action buttons
         * @return true if the menu or action mode was updated, false otherwise.
         */
        public boolean onPrepareActionMode(ActionMode mode, Menu menu);

        /**
         * Called to report a user click on an action button.
         *
         * @param mode The current ActionMode
         * @param item The item that was clicked
         * @return true if this callback handled the event, false if the standard MenuItem
         *          invocation should continue.
         */
        public boolean onActionItemClicked(ActionMode mode, MenuItem item);

        /**
         * Called when an action mode is about to be exited and destroyed.
         *
         * @param mode The current ActionMode being destroyed
         */
        public void onDestroyActionMode(ActionMode mode);
    }
}