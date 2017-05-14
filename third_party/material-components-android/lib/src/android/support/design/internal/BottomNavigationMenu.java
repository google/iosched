/*
 * Copyright (C) 2016 The Android Open Source Project
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

package android.support.design.internal;

import static android.support.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.content.Context;
import android.support.annotation.RestrictTo;
import android.support.v7.view.menu.MenuBuilder;
import android.support.v7.view.menu.MenuItemImpl;
import android.view.MenuItem;
import android.view.SubMenu;

/** @hide */
@RestrictTo(LIBRARY_GROUP)
public final class BottomNavigationMenu extends MenuBuilder {
  public static final int MAX_ITEM_COUNT = 5;

  public BottomNavigationMenu(Context context) {
    super(context);
  }

  @Override
  public SubMenu addSubMenu(int group, int id, int categoryOrder, CharSequence title) {
    throw new UnsupportedOperationException("BottomNavigationView does not support submenus");
  }

  @Override
  protected MenuItem addInternal(int group, int id, int categoryOrder, CharSequence title) {
    if (size() + 1 > MAX_ITEM_COUNT) {
      throw new IllegalArgumentException(
          "Maximum number of items supported by BottomNavigationView is "
              + MAX_ITEM_COUNT
              + ". Limit can be checked with BottomNavigationView#getMaxItemCount()");
    }
    stopDispatchingItemsChanged();
    final MenuItem item = super.addInternal(group, id, categoryOrder, title);
    if (item instanceof MenuItemImpl) {
      ((MenuItemImpl) item).setExclusiveCheckable(true);
    }
    startDispatchingItemsChanged();
    return item;
  }
}
