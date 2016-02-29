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

import com.google.samples.apps.iosched.navigation.NavigationModel.NavigationItemEnum;

/**
 * Configuration file for items to show in the {@link AppNavigationView}. This is used by the {@link
 * NavigationModel}.
 */
public class NavigationConfig {

    private final static NavigationItemEnum[] COMMON_ITEMS_AFTER_CUSTOM = new NavigationItemEnum[]{
            NavigationItemEnum.SEPARATOR, NavigationItemEnum.SOCIAL,
            NavigationItemEnum.VIDEO_LIBRARY, NavigationItemEnum.SEPARATOR_SPECIAL,
            NavigationItemEnum.SETTINGS, NavigationItemEnum.ABOUT
    };

    public final static NavigationItemEnum[] NAVIGATION_ITEMS_LOGGEDIN_ATTENDING =
            concatenateItems(new NavigationItemEnum[]{NavigationItemEnum.MY_SCHEDULE,
                    NavigationItemEnum.EXPLORE, NavigationItemEnum.MAP}, COMMON_ITEMS_AFTER_CUSTOM);

    public final static NavigationItemEnum[] NAVIGATION_ITEMS_LOGGEDIN_REMOTE =
            concatenateItems(new NavigationItemEnum[]{NavigationItemEnum.MY_SCHEDULE,
                    NavigationItemEnum.EXPLORE}, COMMON_ITEMS_AFTER_CUSTOM);


    public final static NavigationItemEnum[] NAVIGATION_ITEMS_LOGGEDOUT_ATTENDING =
            concatenateItems(new NavigationItemEnum[]{NavigationItemEnum.SIGN_IN,
                    NavigationItemEnum.EXPLORE, NavigationItemEnum.MAP}, COMMON_ITEMS_AFTER_CUSTOM);


    public final static NavigationItemEnum[] NAVIGATION_ITEMS_LOGGEDOUT_REMOTE =
            concatenateItems(new NavigationItemEnum[]{NavigationItemEnum.SIGN_IN,
                    NavigationItemEnum.EXPLORE}, COMMON_ITEMS_AFTER_CUSTOM);

    private static NavigationItemEnum[] concatenateItems(NavigationItemEnum[] first,
            NavigationItemEnum[] second) {
        NavigationItemEnum[] items = new NavigationItemEnum[first.length + second.length];
        for (int i = 0; i < first.length; i++) {
            items[i] = first[i];
        }
        for (int i = 0; i < second.length; i++) {
            items[first.length + i] = second[i];
        }
        return items;
    }

    public static NavigationItemEnum[] appendItem(NavigationItemEnum[] first,
            NavigationItemEnum second) {
        return concatenateItems(first, new NavigationItemEnum[]{second});
    }

}
