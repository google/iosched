/*
 * Copyright (C) 2007 The Android Open Source Project
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

import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * Subclass of {@link Menu} for sub menus.
 * <p>
 * Sub menus do not support item icons, or nested sub menus.
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For information about creating menus, read the
 * <a href="{@docRoot}guide/topics/ui/menus.html">Menus</a> developer guide.</p>
 * </div>
 */

public interface SubMenu extends Menu {
    /**
     * Sets the submenu header's title to the title given in <var>titleRes</var>
     * resource identifier.
     *
     * @param titleRes The string resource identifier used for the title.
     * @return This SubMenu so additional setters can be called.
     */
    public SubMenu setHeaderTitle(int titleRes);

    /**
     * Sets the submenu header's title to the title given in <var>title</var>.
     *
     * @param title The character sequence used for the title.
     * @return This SubMenu so additional setters can be called.
     */
    public SubMenu setHeaderTitle(CharSequence title);

    /**
     * Sets the submenu header's icon to the icon given in <var>iconRes</var>
     * resource id.
     *
     * @param iconRes The resource identifier used for the icon.
     * @return This SubMenu so additional setters can be called.
     */
    public SubMenu setHeaderIcon(int iconRes);

    /**
     * Sets the submenu header's icon to the icon given in <var>icon</var>
     * {@link Drawable}.
     *
     * @param icon The {@link Drawable} used for the icon.
     * @return This SubMenu so additional setters can be called.
     */
    public SubMenu setHeaderIcon(Drawable icon);

    /**
     * Sets the header of the submenu to the {@link View} given in
     * <var>view</var>. This replaces the header title and icon (and those
     * replace this).
     *
     * @param view The {@link View} used for the header.
     * @return This SubMenu so additional setters can be called.
     */
    public SubMenu setHeaderView(View view);

    /**
     * Clears the header of the submenu.
     */
    public void clearHeader();

    /**
     * Change the icon associated with this submenu's item in its parent menu.
     *
     * @see MenuItem#setIcon(int)
     * @param iconRes The new icon (as a resource ID) to be displayed.
     * @return This SubMenu so additional setters can be called.
     */
    public SubMenu setIcon(int iconRes);

    /**
     * Change the icon associated with this submenu's item in its parent menu.
     *
     * @see MenuItem#setIcon(Drawable)
     * @param icon The new icon (as a Drawable) to be displayed.
     * @return This SubMenu so additional setters can be called.
     */
    public SubMenu setIcon(Drawable icon);

    /**
     * Gets the {@link MenuItem} that represents this submenu in the parent
     * menu.  Use this for setting additional item attributes.
     *
     * @return The {@link MenuItem} that launches the submenu when invoked.
     */
    public MenuItem getItem();
}
