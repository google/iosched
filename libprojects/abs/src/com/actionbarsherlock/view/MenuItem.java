/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;

/**
 * Interface for direct access to a previously created menu item.
 * <p>
 * An Item is returned by calling one of the {@link android.view.Menu#add}
 * methods.
 * <p>
 * For a feature set of specific menu types, see {@link Menu}.
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For information about creating menus, read the
 * <a href="{@docRoot}guide/topics/ui/menus.html">Menus</a> developer guide.</p>
 * </div>
 */
public interface MenuItem {
    /*
     * These should be kept in sync with attrs.xml enum constants for showAsAction
     */
    /** Never show this item as a button in an Action Bar. */
    public static final int SHOW_AS_ACTION_NEVER = android.view.MenuItem.SHOW_AS_ACTION_NEVER;
    /** Show this item as a button in an Action Bar if the system decides there is room for it. */
    public static final int SHOW_AS_ACTION_IF_ROOM = android.view.MenuItem.SHOW_AS_ACTION_IF_ROOM;
    /**
     * Always show this item as a button in an Action Bar.
     * Use sparingly! If too many items are set to always show in the Action Bar it can
     * crowd the Action Bar and degrade the user experience on devices with smaller screens.
     * A good rule of thumb is to have no more than 2 items set to always show at a time.
     */
    public static final int SHOW_AS_ACTION_ALWAYS = android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;

    /**
     * When this item is in the action bar, always show it with a text label even if
     * it also has an icon specified.
     */
    public static final int SHOW_AS_ACTION_WITH_TEXT = android.view.MenuItem.SHOW_AS_ACTION_WITH_TEXT;

    /**
     * This item's action view collapses to a normal menu item.
     * When expanded, the action view temporarily takes over
     * a larger segment of its container.
     */
    public static final int SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW = android.view.MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW;

    /**
     * Interface definition for a callback to be invoked when a menu item is
     * clicked.
     *
     * @see Activity#onContextItemSelected(MenuItem)
     * @see Activity#onOptionsItemSelected(MenuItem)
     */
    public interface OnMenuItemClickListener {
        /**
         * Called when a menu item has been invoked.  This is the first code
         * that is executed; if it returns true, no other callbacks will be
         * executed.
         *
         * @param item The menu item that was invoked.
         *
         * @return Return true to consume this click and prevent others from
         *         executing.
         */
        public boolean onMenuItemClick(MenuItem item);
    }

    /**
     * Interface definition for a callback to be invoked when a menu item
     * marked with {@link MenuItem#SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW} is
     * expanded or collapsed.
     *
     * @see MenuItem#expandActionView()
     * @see MenuItem#collapseActionView()
     * @see MenuItem#setShowAsActionFlags(int)
     */
    public interface OnActionExpandListener {
        /**
         * Called when a menu item with {@link MenuItem#SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW}
         * is expanded.
         * @param item Item that was expanded
         * @return true if the item should expand, false if expansion should be suppressed.
         */
        public boolean onMenuItemActionExpand(MenuItem item);

        /**
         * Called when a menu item with {@link MenuItem#SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW}
         * is collapsed.
         * @param item Item that was collapsed
         * @return true if the item should collapse, false if collapsing should be suppressed.
         */
        public boolean onMenuItemActionCollapse(MenuItem item);
    }

    /**
     * Return the identifier for this menu item.  The identifier can not
     * be changed after the menu is created.
     *
     * @return The menu item's identifier.
     */
    public int getItemId();

    /**
     * Return the group identifier that this menu item is part of. The group
     * identifier can not be changed after the menu is created.
     *
     * @return The menu item's group identifier.
     */
    public int getGroupId();

    /**
     * Return the category and order within the category of this item. This
     * item will be shown before all items (within its category) that have
     * order greater than this value.
     * <p>
     * An order integer contains the item's category (the upper bits of the
     * integer; set by or/add the category with the order within the
     * category) and the ordering of the item within that category (the
     * lower bits). Example categories are {@link Menu#CATEGORY_SYSTEM},
     * {@link Menu#CATEGORY_SECONDARY}, {@link Menu#CATEGORY_ALTERNATIVE},
     * {@link Menu#CATEGORY_CONTAINER}. See {@link Menu} for a full list.
     *
     * @return The order of this item.
     */
    public int getOrder();

    /**
     * Change the title associated with this item.
     *
     * @param title The new text to be displayed.
     * @return This Item so additional setters can be called.
     */
    public MenuItem setTitle(CharSequence title);

    /**
     * Change the title associated with this item.
     * <p>
     * Some menu types do not sufficient space to show the full title, and
     * instead a condensed title is preferred. See {@link Menu} for more
     * information.
     *
     * @param title The resource id of the new text to be displayed.
     * @return This Item so additional setters can be called.
     * @see #setTitleCondensed(CharSequence)
     */

    public MenuItem setTitle(int title);

    /**
     * Retrieve the current title of the item.
     *
     * @return The title.
     */
    public CharSequence getTitle();

    /**
     * Change the condensed title associated with this item. The condensed
     * title is used in situations where the normal title may be too long to
     * be displayed.
     *
     * @param title The new text to be displayed as the condensed title.
     * @return This Item so additional setters can be called.
     */
    public MenuItem setTitleCondensed(CharSequence title);

    /**
     * Retrieve the current condensed title of the item. If a condensed
     * title was never set, it will return the normal title.
     *
     * @return The condensed title, if it exists.
     *         Otherwise the normal title.
     */
    public CharSequence getTitleCondensed();

    /**
     * Change the icon associated with this item. This icon will not always be
     * shown, so the title should be sufficient in describing this item. See
     * {@link Menu} for the menu types that support icons.
     *
     * @param icon The new icon (as a Drawable) to be displayed.
     * @return This Item so additional setters can be called.
     */
    public MenuItem setIcon(Drawable icon);

    /**
     * Change the icon associated with this item. This icon will not always be
     * shown, so the title should be sufficient in describing this item. See
     * {@link Menu} for the menu types that support icons.
     * <p>
     * This method will set the resource ID of the icon which will be used to
     * lazily get the Drawable when this item is being shown.
     *
     * @param iconRes The new icon (as a resource ID) to be displayed.
     * @return This Item so additional setters can be called.
     */
    public MenuItem setIcon(int iconRes);

    /**
     * Returns the icon for this item as a Drawable (getting it from resources if it hasn't been
     * loaded before).
     *
     * @return The icon as a Drawable.
     */
    public Drawable getIcon();

    /**
     * Change the Intent associated with this item.  By default there is no
     * Intent associated with a menu item.  If you set one, and nothing
     * else handles the item, then the default behavior will be to call
     * {@link android.content.Context#startActivity} with the given Intent.
     *
     * <p>Note that setIntent() can not be used with the versions of
     * {@link Menu#add} that take a Runnable, because {@link Runnable#run}
     * does not return a value so there is no way to tell if it handled the
     * item.  In this case it is assumed that the Runnable always handles
     * the item, and the intent will never be started.
     *
     * @see #getIntent
     * @param intent The Intent to associated with the item.  This Intent
     *               object is <em>not</em> copied, so be careful not to
     *               modify it later.
     * @return This Item so additional setters can be called.
     */
    public MenuItem setIntent(Intent intent);

    /**
     * Return the Intent associated with this item.  This returns a
     * reference to the Intent which you can change as desired to modify
     * what the Item is holding.
     *
     * @see #setIntent
     * @return Returns the last value supplied to {@link #setIntent}, or
     *         null.
     */
    public Intent getIntent();

    /**
     * Change both the numeric and alphabetic shortcut associated with this
     * item. Note that the shortcut will be triggered when the key that
     * generates the given character is pressed alone or along with with the alt
     * key. Also note that case is not significant and that alphabetic shortcut
     * characters will be displayed in lower case.
     * <p>
     * See {@link Menu} for the menu types that support shortcuts.
     *
     * @param numericChar The numeric shortcut key. This is the shortcut when
     *        using a numeric (e.g., 12-key) keyboard.
     * @param alphaChar The alphabetic shortcut key. This is the shortcut when
     *        using a keyboard with alphabetic keys.
     * @return This Item so additional setters can be called.
     */
    public MenuItem setShortcut(char numericChar, char alphaChar);

    /**
     * Change the numeric shortcut associated with this item.
     * <p>
     * See {@link Menu} for the menu types that support shortcuts.
     *
     * @param numericChar The numeric shortcut key.  This is the shortcut when
     *                 using a 12-key (numeric) keyboard.
     * @return This Item so additional setters can be called.
     */
    public MenuItem setNumericShortcut(char numericChar);

    /**
     * Return the char for this menu item's numeric (12-key) shortcut.
     *
     * @return Numeric character to use as a shortcut.
     */
    public char getNumericShortcut();

    /**
     * Change the alphabetic shortcut associated with this item. The shortcut
     * will be triggered when the key that generates the given character is
     * pressed alone or along with with the alt key. Case is not significant and
     * shortcut characters will be displayed in lower case. Note that menu items
     * with the characters '\b' or '\n' as shortcuts will get triggered by the
     * Delete key or Carriage Return key, respectively.
     * <p>
     * See {@link Menu} for the menu types that support shortcuts.
     *
     * @param alphaChar The alphabetic shortcut key. This is the shortcut when
     *        using a keyboard with alphabetic keys.
     * @return This Item so additional setters can be called.
     */
    public MenuItem setAlphabeticShortcut(char alphaChar);

    /**
     * Return the char for this menu item's alphabetic shortcut.
     *
     * @return Alphabetic character to use as a shortcut.
     */
    public char getAlphabeticShortcut();

    /**
     * Control whether this item can display a check mark. Setting this does
     * not actually display a check mark (see {@link #setChecked} for that);
     * rather, it ensures there is room in the item in which to display a
     * check mark.
     * <p>
     * See {@link Menu} for the menu types that support check marks.
     *
     * @param checkable Set to true to allow a check mark, false to
     *            disallow. The default is false.
     * @see #setChecked
     * @see #isCheckable
     * @see Menu#setGroupCheckable
     * @return This Item so additional setters can be called.
     */
    public MenuItem setCheckable(boolean checkable);

    /**
     * Return whether the item can currently display a check mark.
     *
     * @return If a check mark can be displayed, returns true.
     *
     * @see #setCheckable
     */
    public boolean isCheckable();

    /**
     * Control whether this item is shown with a check mark.  Note that you
     * must first have enabled checking with {@link #setCheckable} or else
     * the check mark will not appear.  If this item is a member of a group that contains
     * mutually-exclusive items (set via {@link Menu#setGroupCheckable(int, boolean, boolean)},
     * the other items in the group will be unchecked.
     * <p>
     * See {@link Menu} for the menu types that support check marks.
     *
     * @see #setCheckable
     * @see #isChecked
     * @see Menu#setGroupCheckable
     * @param checked Set to true to display a check mark, false to hide
     *                it.  The default value is false.
     * @return This Item so additional setters can be called.
     */
    public MenuItem setChecked(boolean checked);

    /**
     * Return whether the item is currently displaying a check mark.
     *
     * @return If a check mark is displayed, returns true.
     *
     * @see #setChecked
     */
    public boolean isChecked();

    /**
     * Sets the visibility of the menu item. Even if a menu item is not visible,
     * it may still be invoked via its shortcut (to completely disable an item,
     * set it to invisible and {@link #setEnabled(boolean) disabled}).
     *
     * @param visible If true then the item will be visible; if false it is
     *        hidden.
     * @return This Item so additional setters can be called.
     */
    public MenuItem setVisible(boolean visible);

    /**
     * Return the visibility of the menu item.
     *
     * @return If true the item is visible; else it is hidden.
     */
    public boolean isVisible();

    /**
     * Sets whether the menu item is enabled. Disabling a menu item will not
     * allow it to be invoked via its shortcut. The menu item will still be
     * visible.
     *
     * @param enabled If true then the item will be invokable; if false it is
     *        won't be invokable.
     * @return This Item so additional setters can be called.
     */
    public MenuItem setEnabled(boolean enabled);

    /**
     * Return the enabled state of the menu item.
     *
     * @return If true the item is enabled and hence invokable; else it is not.
     */
    public boolean isEnabled();

    /**
     * Check whether this item has an associated sub-menu.  I.e. it is a
     * sub-menu of another menu.
     *
     * @return If true this item has a menu; else it is a
     *         normal item.
     */
    public boolean hasSubMenu();

    /**
     * Get the sub-menu to be invoked when this item is selected, if it has
     * one. See {@link #hasSubMenu()}.
     *
     * @return The associated menu if there is one, else null
     */
    public SubMenu getSubMenu();

    /**
     * Set a custom listener for invocation of this menu item. In most
     * situations, it is more efficient and easier to use
     * {@link Activity#onOptionsItemSelected(MenuItem)} or
     * {@link Activity#onContextItemSelected(MenuItem)}.
     *
     * @param menuItemClickListener The object to receive invokations.
     * @return This Item so additional setters can be called.
     * @see Activity#onOptionsItemSelected(MenuItem)
     * @see Activity#onContextItemSelected(MenuItem)
     */
    public MenuItem setOnMenuItemClickListener(MenuItem.OnMenuItemClickListener menuItemClickListener);

    /**
     * Gets the extra information linked to this menu item.  This extra
     * information is set by the View that added this menu item to the
     * menu.
     *
     * @see OnCreateContextMenuListener
     * @return The extra information linked to the View that added this
     *         menu item to the menu. This can be null.
     */
    public ContextMenuInfo getMenuInfo();

    /**
     * Sets how this item should display in the presence of an Action Bar.
     * The parameter actionEnum is a flag set. One of {@link #SHOW_AS_ACTION_ALWAYS},
     * {@link #SHOW_AS_ACTION_IF_ROOM}, or {@link #SHOW_AS_ACTION_NEVER} should
     * be used, and you may optionally OR the value with {@link #SHOW_AS_ACTION_WITH_TEXT}.
     * SHOW_AS_ACTION_WITH_TEXT requests that when the item is shown as an action,
     * it should be shown with a text label.
     *
     * @param actionEnum How the item should display. One of
     * {@link #SHOW_AS_ACTION_ALWAYS}, {@link #SHOW_AS_ACTION_IF_ROOM}, or
     * {@link #SHOW_AS_ACTION_NEVER}. SHOW_AS_ACTION_NEVER is the default.
     *
     * @see android.app.ActionBar
     * @see #setActionView(View)
     */
    public void setShowAsAction(int actionEnum);

    /**
     * Sets how this item should display in the presence of an Action Bar.
     * The parameter actionEnum is a flag set. One of {@link #SHOW_AS_ACTION_ALWAYS},
     * {@link #SHOW_AS_ACTION_IF_ROOM}, or {@link #SHOW_AS_ACTION_NEVER} should
     * be used, and you may optionally OR the value with {@link #SHOW_AS_ACTION_WITH_TEXT}.
     * SHOW_AS_ACTION_WITH_TEXT requests that when the item is shown as an action,
     * it should be shown with a text label.
     *
     * <p>Note: This method differs from {@link #setShowAsAction(int)} only in that it
     * returns the current MenuItem instance for call chaining.
     *
     * @param actionEnum How the item should display. One of
     * {@link #SHOW_AS_ACTION_ALWAYS}, {@link #SHOW_AS_ACTION_IF_ROOM}, or
     * {@link #SHOW_AS_ACTION_NEVER}. SHOW_AS_ACTION_NEVER is the default.
     *
     * @see android.app.ActionBar
     * @see #setActionView(View)
     * @return This MenuItem instance for call chaining.
     */
    public MenuItem setShowAsActionFlags(int actionEnum);

    /**
     * Set an action view for this menu item. An action view will be displayed in place
     * of an automatically generated menu item element in the UI when this item is shown
     * as an action within a parent.
     * <p>
     *   <strong>Note:</strong> Setting an action view overrides the action provider
     *           set via {@link #setActionProvider(ActionProvider)}.
     * </p>
     *
     * @param view View to use for presenting this item to the user.
     * @return This Item so additional setters can be called.
     *
     * @see #setShowAsAction(int)
     */
    public MenuItem setActionView(View view);

    /**
     * Set an action view for this menu item. An action view will be displayed in place
     * of an automatically generated menu item element in the UI when this item is shown
     * as an action within a parent.
     * <p>
     *   <strong>Note:</strong> Setting an action view overrides the action provider
     *           set via {@link #setActionProvider(ActionProvider)}.
     * </p>
     *
     * @param resId Layout resource to use for presenting this item to the user.
     * @return This Item so additional setters can be called.
     *
     * @see #setShowAsAction(int)
     */
    public MenuItem setActionView(int resId);

    /**
     * Returns the currently set action view for this menu item.
     *
     * @return This item's action view
     *
     * @see #setActionView(View)
     * @see #setShowAsAction(int)
     */
    public View getActionView();

    /**
     * Sets the {@link ActionProvider} responsible for creating an action view if
     * the item is placed on the action bar. The provider also provides a default
     * action invoked if the item is placed in the overflow menu.
     * <p>
     *   <strong>Note:</strong> Setting an action provider overrides the action view
     *           set via {@link #setActionView(int)} or {@link #setActionView(View)}.
     * </p>
     *
     * @param actionProvider The action provider.
     * @return This Item so additional setters can be called.
     *
     * @see ActionProvider
     */
    public MenuItem setActionProvider(ActionProvider actionProvider);

    /**
     * Gets the {@link ActionProvider}.
     *
     * @return The action provider.
     *
     * @see ActionProvider
     * @see #setActionProvider(ActionProvider)
     */
    public ActionProvider getActionProvider();

    /**
     * Expand the action view associated with this menu item.
     * The menu item must have an action view set, as well as
     * the showAsAction flag {@link #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW}.
     * If a listener has been set using {@link #setOnActionExpandListener(OnActionExpandListener)}
     * it will have its {@link OnActionExpandListener#onMenuItemActionExpand(MenuItem)}
     * method invoked. The listener may return false from this method to prevent expanding
     * the action view.
     *
     * @return true if the action view was expanded, false otherwise.
     */
    public boolean expandActionView();

    /**
     * Collapse the action view associated with this menu item.
     * The menu item must have an action view set, as well as the showAsAction flag
     * {@link #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW}. If a listener has been set using
     * {@link #setOnActionExpandListener(OnActionExpandListener)} it will have its
     * {@link OnActionExpandListener#onMenuItemActionCollapse(MenuItem)} method invoked.
     * The listener may return false from this method to prevent collapsing the action view.
     *
     * @return true if the action view was collapsed, false otherwise.
     */
    public boolean collapseActionView();

    /**
     * Returns true if this menu item's action view has been expanded.
     *
     * @return true if the item's action view is expanded, false otherwise.
     *
     * @see #expandActionView()
     * @see #collapseActionView()
     * @see #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW
     * @see OnActionExpandListener
     */
    public boolean isActionViewExpanded();

    /**
     * Set an {@link OnActionExpandListener} on this menu item to be notified when
     * the associated action view is expanded or collapsed. The menu item must
     * be configured to expand or collapse its action view using the flag
     * {@link #SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW}.
     *
     * @param listener Listener that will respond to expand/collapse events
     * @return This menu item instance for call chaining
     */
    public MenuItem setOnActionExpandListener(OnActionExpandListener listener);
}