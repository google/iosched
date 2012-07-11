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

package com.actionbarsherlock.view;

import android.content.Context;
import android.view.View;

/**
 * This class is a mediator for accomplishing a given task, for example sharing a file.
 * It is responsible for creating a view that performs an action that accomplishes the task.
 * This class also implements other functions such a performing a default action.
 * <p>
 * An ActionProvider can be optionally specified for a {@link MenuItem} and in such a
 * case it will be responsible for creating the action view that appears in the
 * {@link android.app.ActionBar} as a substitute for the menu item when the item is
 * displayed as an action item. Also the provider is responsible for performing a
 * default action if a menu item placed on the overflow menu of the ActionBar is
 * selected and none of the menu item callbacks has handled the selection. For this
 * case the provider can also optionally provide a sub-menu for accomplishing the
 * task at hand.
 * </p>
 * <p>
 * There are two ways for using an action provider for creating and handling of action views:
 * <ul>
 * <li>
 * Setting the action provider on a {@link MenuItem} directly by calling
 * {@link MenuItem#setActionProvider(ActionProvider)}.
 * </li>
 * <li>
 * Declaring the action provider in the menu XML resource. For example:
 * <pre>
 * <code>
 *   &lt;item android:id="@+id/my_menu_item"
 *     android:title="Title"
 *     android:icon="@drawable/my_menu_item_icon"
 *     android:showAsAction="ifRoom"
 *     android:actionProviderClass="foo.bar.SomeActionProvider" /&gt;
 * </code>
 * </pre>
 * </li>
 * </ul>
 * </p>
 *
 * @see MenuItem#setActionProvider(ActionProvider)
 * @see MenuItem#getActionProvider()
 */
public abstract class ActionProvider {
    private SubUiVisibilityListener mSubUiVisibilityListener;

    /**
     * Creates a new instance.
     *
     * @param context Context for accessing resources.
     */
    public ActionProvider(Context context) {
    }

    /**
     * Factory method for creating new action views.
     *
     * @return A new action view.
     */
    public abstract View onCreateActionView();

    /**
     * Performs an optional default action.
     * <p>
     * For the case of an action provider placed in a menu item not shown as an action this
     * method is invoked if previous callbacks for processing menu selection has handled
     * the event.
     * </p>
     * <p>
     * A menu item selection is processed in the following order:
     * <ul>
     * <li>
     * Receiving a call to {@link MenuItem.OnMenuItemClickListener#onMenuItemClick
     *  MenuItem.OnMenuItemClickListener.onMenuItemClick}.
     * </li>
     * <li>
     * Receiving a call to {@link android.app.Activity#onOptionsItemSelected(MenuItem)
     *  Activity.onOptionsItemSelected(MenuItem)}
     * </li>
     * <li>
     * Receiving a call to {@link android.app.Fragment#onOptionsItemSelected(MenuItem)
     *  Fragment.onOptionsItemSelected(MenuItem)}
     * </li>
     * <li>
     * Launching the {@link android.content.Intent} set via
     * {@link MenuItem#setIntent(android.content.Intent) MenuItem.setIntent(android.content.Intent)}
     * </li>
     * <li>
     * Invoking this method.
     * </li>
     * </ul>
     * </p>
     * <p>
     * The default implementation does not perform any action and returns false.
     * </p>
     */
    public boolean onPerformDefaultAction() {
        return false;
    }

    /**
     * Determines if this ActionProvider has a submenu associated with it.
     *
     * <p>Associated submenus will be shown when an action view is not. This
     * provider instance will receive a call to {@link #onPrepareSubMenu(SubMenu)}
     * after the call to {@link #onPerformDefaultAction()} and before a submenu is
     * displayed to the user.
     *
     * @return true if the item backed by this provider should have an associated submenu
     */
    public boolean hasSubMenu() {
        return false;
    }

    /**
     * Called to prepare an associated submenu for the menu item backed by this ActionProvider.
     *
     * <p>if {@link #hasSubMenu()} returns true, this method will be called when the
     * menu item is selected to prepare the submenu for presentation to the user. Apps
     * may use this to create or alter submenu content right before display.
     *
     * @param subMenu Submenu that will be displayed
     */
    public void onPrepareSubMenu(SubMenu subMenu) {
    }

    /**
     * Notify the system that the visibility of an action view's sub-UI such as
     * an anchored popup has changed. This will affect how other system
     * visibility notifications occur.
     *
     * @hide Pending future API approval
     */
    public void subUiVisibilityChanged(boolean isVisible) {
        if (mSubUiVisibilityListener != null) {
            mSubUiVisibilityListener.onSubUiVisibilityChanged(isVisible);
        }
    }

    /**
     * @hide Internal use only
     */
    public void setSubUiVisibilityListener(SubUiVisibilityListener listener) {
        mSubUiVisibilityListener = listener;
    }

    /**
     * @hide Internal use only
     */
    public interface SubUiVisibilityListener {
        public void onSubUiVisibilityChanged(boolean isVisible);
    }
}
