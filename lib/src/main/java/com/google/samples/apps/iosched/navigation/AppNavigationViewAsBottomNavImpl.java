/*
 * Copyright (c) 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.samples.apps.iosched.navigation;

import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.view.Menu;
import android.view.MenuItem;

import com.google.samples.apps.iosched.navigation.NavigationModel.NavigationItemEnum;

import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class AppNavigationViewAsBottomNavImpl extends AppNavigationViewAbstractImpl
        implements BottomNavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = makeLogTag(AppNavigationViewAsBottomNavImpl.class);

    private final BottomNavigationView mNavigationView;

    public AppNavigationViewAsBottomNavImpl(final BottomNavigationView navigationView) {
        mNavigationView = navigationView;
    }

    @Override
    public void displayNavigationItems(final NavigationItemEnum[] items) {
        final Menu menu = mNavigationView.getMenu();
        for (NavigationItemEnum item : items) {
            final MenuItem menuItem = menu.findItem(item.getId());
            if (menuItem != null) {
                menuItem.setVisible(true);
                menuItem.setIcon(item.getIconResource());
                menuItem.setTitle(item.getTitleResource());
                if (item == mSelfItem) {
                    menuItem.setChecked(true);
                }
            } else {
                LOGE(TAG, "Menu Item for navigation item with title " +
                        (item.getTitleResource() != 0
                                ? mActivity.getResources().getString(item.getTitleResource())
                                : "") + "not found");
            }
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
        final NavigationItemEnum navItem = NavigationItemEnum.getById(item.getItemId());
        if (navItem != null && navItem != mSelfItem) {
            itemSelected(navItem);
            // Return true so that BottomNavigationView updates itself to show the new item
            return true;
        }
        return false;
    }

    @Override
    public void setUpView() {
        mNavigationView.setOnNavigationItemSelectedListener(this);
    }

    @Override
    public void showNavigation() {
        // Don't need this I think
    }
}
