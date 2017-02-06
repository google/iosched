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

import android.os.Bundle;
import android.support.annotation.Nullable;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.about.AboutActivity;
import com.google.samples.apps.iosched.archframework.Model;
import com.google.samples.apps.iosched.archframework.QueryEnum;
import com.google.samples.apps.iosched.archframework.UserActionEnum;
import com.google.samples.apps.iosched.debug.DebugActivity;
import com.google.samples.apps.iosched.explore.ExploreIOActivity;
import com.google.samples.apps.iosched.map.MapActivity;
import com.google.samples.apps.iosched.myschedule.MyScheduleActivity;
import com.google.samples.apps.iosched.navigation.NavigationModel.NavigationQueryEnum;
import com.google.samples.apps.iosched.navigation.NavigationModel.NavigationUserActionEnum;
import com.google.samples.apps.iosched.settings.SettingsActivity;
import com.google.samples.apps.iosched.videolibrary.VideoLibraryActivity;

/**
 * Determines which items to show in the {@link AppNavigationView}.
 */
public class NavigationModel implements Model<NavigationQueryEnum, NavigationUserActionEnum> {

    private NavigationItemEnum[] mItems;

    public NavigationItemEnum[] getItems() {
        return mItems;
    }

    @Override
    public NavigationQueryEnum[] getQueries() {
        return NavigationQueryEnum.values();
    }

    @Override
    public NavigationUserActionEnum[] getUserActions() {
        return NavigationUserActionEnum.values();
    }

    @Override
    public void deliverUserAction(final NavigationUserActionEnum action,
            @Nullable final Bundle args,
            final UserActionCallback<NavigationUserActionEnum> callback) {
        switch (action) {
            case RELOAD_ITEMS:
                mItems = null;
                populateNavigationItems();
                callback.onModelUpdated(this, action);
                break;
        }
    }

    @Override
    public void requestData(final NavigationQueryEnum query,
            final DataQueryCallback<NavigationQueryEnum> callback) {
        switch (query) {
            case LOAD_ITEMS:
                if (mItems != null) {
                    callback.onModelUpdated(this, query);
                } else {
                    populateNavigationItems();
                    callback.onModelUpdated(this, query);
                }
                break;
        }
    }

    private void populateNavigationItems() {
        NavigationItemEnum[] items = NavigationConfig.ITEMS;
        mItems = NavigationConfig.filterOutItemsDisabledInBuildConfig(items);
    }

    @Override
    public void cleanUp() {
        // no-op
    }

    /**
     * List of all possible navigation items.
     */
    public enum NavigationItemEnum {
        MY_SCHEDULE(R.id.myschedule_nav_item, R.string.navdrawer_item_my_schedule,
                R.drawable.ic_navview_schedule, MyScheduleActivity.class, true),
        EXPLORE(R.id.explore_nav_item, R.string.navdrawer_item_explore,
                R.drawable.ic_navview_explore, ExploreIOActivity.class, true),
        MAP(R.id.map_nav_item, R.string.navdrawer_item_map, R.drawable.ic_navview_map,
                MapActivity.class, true),
        ABOUT(R.id.about_nav_item, R.string.description_about, R.drawable.ic_about,
                AboutActivity.class, true),
        INVALID(12, 0, 0, null),

        // Deprecated?
        SIGN_IN(R.id.signin_nav_item, R.string.navdrawer_item_sign_in, 0, null),
        VIDEO_LIBRARY(R.id.videos_nav_item, R.string.navdrawer_item_video_library,
                R.drawable.ic_navview_video_library, VideoLibraryActivity.class),
        SETTINGS(R.id.settings_nav_item, R.string.navdrawer_item_settings, R.drawable.ic_navview_settings,
                SettingsActivity.class),
        DEBUG(R.id.debug_nav_item, R.string.navdrawer_item_debug, R.drawable.ic_navview_settings,
                DebugActivity.class);

        private int id;

        private int titleResource;

        private int iconResource;

        private Class classToLaunch;

        private boolean finishCurrentActivity;

        NavigationItemEnum(int id, int titleResource, int iconResource, Class classToLaunch) {
            this(id, titleResource, iconResource, classToLaunch, false);
        }

        NavigationItemEnum(int id, int titleResource, int iconResource, Class classToLaunch,
                boolean finishCurrentActivity) {
            this.id = id;
            this.titleResource = titleResource;
            this.iconResource = iconResource;
            this.classToLaunch = classToLaunch;
            this.finishCurrentActivity = finishCurrentActivity;
        }

        public int getId() {
            return id;
        }

        public int getTitleResource() {
            return titleResource;
        }

        public int getIconResource() {
            return iconResource;
        }

        public Class getClassToLaunch() {
            return classToLaunch;
        }

        public boolean finishCurrentActivity() {
            return finishCurrentActivity;
        }

        public static NavigationItemEnum getById(int id) {
            for (NavigationItemEnum value : NavigationItemEnum.values()) {
                if (value.getId() == id) {
                    return value;
                }
            }
            return INVALID;
        }

    }

    public enum NavigationQueryEnum implements QueryEnum {
        LOAD_ITEMS(0);

        private int id;

        NavigationQueryEnum(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String[] getProjection() {
            return new String[0];
        }
    }

    public enum NavigationUserActionEnum implements UserActionEnum {
        RELOAD_ITEMS(0);

        private int id;

        NavigationUserActionEnum(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }
    }
}
