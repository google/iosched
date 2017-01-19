/*
 * Copyright (c) 2016 Google Inc.
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

import android.app.Activity;

import com.google.samples.apps.iosched.login.LoginStateListener;
import com.google.samples.apps.iosched.navigation.NavigationModel.NavigationItemEnum;

/**
 * This represents a general navigation view, independent of UI implementation details.
 * <p/>
 * It is assumed that there is one NavigationView per {@link Activity}. It may or may not be
 * visible.
 */
public interface AppNavigationView {

    /**
     * Call this when the {@link Activity} is ready to process the NavigationView. Implements
     * general set up of the view.
     *
     * @param activity           The activity showing the NavigationView
     * @param loginStateListener The navigation contains state related to login, so a login listener
     *                           should be attached to it.
     * @param self               The {@link NavigationItemEnum} of the activity showing the
     *                           NavigationView. Pass in {@link NavigationItemEnum#INVALID} if the
     *                           activity should not display the NavigationView.
     */
    void activityReady(Activity activity, LoginStateListener loginStateListener,
            NavigationItemEnum self);

    /**
     * Implements UI specific logic to perform initial set up for the NavigationView. This is
     * expected to be called only once.
     */
    void setUpView();

    /**
     * Call this when some action in the {@link Activity} requires the navigation items to be
     * refreshed (eg user logging in). Implements updating navigation items.
     */
    void updateNavigationItems();

    /**
     * Implements UI specific logic to display the {@code items}. This is expected to be called each
     * time the navigation items change.
     */
    void displayNavigationItems(NavigationItemEnum[] items);

    /**
     * Implements launching the {@link Activity} linked to the {@code item}.
     */
    void itemSelected(NavigationItemEnum item);

    /**
     * Implements UI specific logic to display the NavigationView. Note that if the NavigationView
     * should always be visible, this method is empty.
     */
    void showNavigation();

}
