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

import com.google.samples.apps.iosched.lib.BuildConfig;
import com.google.samples.apps.iosched.navigation.NavigationModel.NavigationItemEnum;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration file for items to show in the {@link AppNavigationView}. This is used by the {@link
 * NavigationModel}.
 */
public class NavigationConfig {

    public final static NavigationItemEnum[] ITEMS = new NavigationItemEnum[]{
            NavigationItemEnum.MY_SCHEDULE,
            NavigationItemEnum.MY_IO,
            NavigationItemEnum.FEED,
            NavigationItemEnum.MAP,
            NavigationItemEnum.DEBUG,
    };

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

    public static NavigationItemEnum[] filterOutItemsDisabledInBuildConfig(
            NavigationItemEnum[] items) {
        List<NavigationItemEnum> enabledItems = new ArrayList<>();
        for (int i = 0; i < items.length; i++) {
            boolean includeItem = true;
            switch (items[i]) {
                case MY_SCHEDULE:
                    includeItem = BuildConfig.ENABLE_MYSCHEDULE_IN_NAVIGATION;
                    break;
                case FEED:
                    includeItem = BuildConfig.ENABLE_FEED_IN_NAVIGATION;
                    break;
                case MAP:
                    includeItem = BuildConfig.ENABLE_MAP_IN_NAVIGATION;
                    break;
                case SIGN_IN:
                    includeItem = BuildConfig.ENABLE_SIGNIN_IN_NAVIGATION;
                    break;
                case SETTINGS:
                    includeItem = BuildConfig.ENABLE_SETTINGS_IN_NAVIGATION;
                    break;
                case ABOUT:
                    includeItem = BuildConfig.ENABLE_ABOUT_IN_NAVIGATION;
                    break;
                case DEBUG:
                    includeItem = BuildConfig.DEBUG;
                    break;
            }

            if (includeItem) {
                enabledItems.add(items[i]);
            }
        }
        return enabledItems.toArray(new NavigationItemEnum[enabledItems.size()]);
    }

}
