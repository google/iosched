/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityOptions;
import android.app.SharedElementListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.graphics.drawable.AnimatedStateListDrawable;
import android.os.Build;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toolbar;

import com.google.samples.apps.iosched.R;

import java.lang.Override;
import java.util.List;
import java.util.Map;

@TargetApi(Build.VERSION_CODES.L)
public class LPreviewUtilsImpl extends LPreviewUtilsBase {
    private static final int[] STATE_CHECKED = new int[]{android.R.attr.state_checked};
    private static final int[] STATE_UNCHECKED = new int[]{};

    private static Typeface sMediumTypeface;

    private ActionBarDrawerToggleWrapper mDrawerToggleWrapper;
    private DrawerLayout mDrawerLayout;
    private Toolbar mActionBarToolbar;

    LPreviewUtilsImpl(Activity activity) {
        super(activity);
    }

    @Override
    public ActionBarDrawerToggleWrapper setupDrawerToggle(DrawerLayout drawerLayout, DrawerLayout.DrawerListener drawerListener) {
        // On L, use a different drawer indicator
        if (mActionBarToolbar != null) {
            mActionBarToolbar.setNavigationIcon(R.drawable.ic_drawer);
            mActionBarToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mDrawerLayout.isDrawerOpen(Gravity.START)) {
                        mDrawerLayout.closeDrawer(Gravity.START);
                    } else {
                        mDrawerLayout.openDrawer(Gravity.START);
                    }
                }
            });
        } else {
            mActivity.getActionBar().setHomeAsUpIndicator(R.drawable.ic_drawer);
        }
        // On L, stub out the ActionBarDrawerToggle
        mDrawerLayout = drawerLayout;
        mDrawerLayout.setDrawerListener(drawerListener);
        mDrawerToggleWrapper = new ActionBarDrawerToggleWrapper();
        return mDrawerToggleWrapper;
    }

    public class ActionBarDrawerToggleWrapper extends LPreviewUtilsBase.ActionBarDrawerToggleWrapper {
        public void syncState() {
        }

        public void onConfigurationChanged(Configuration newConfig) {
        }

        public boolean onOptionsItemSelected(MenuItem item) {
            // Toggle drawer
            if (item.getItemId() == android.R.id.home) {
                if (mDrawerLayout.isDrawerOpen(Gravity.START)) {
                    mDrawerLayout.closeDrawer(Gravity.START);
                } else {
                    mDrawerLayout.openDrawer(Gravity.START);
                }
                return true;
            }
            return false;
        }
    }

    @Override
    public void setViewElevation(View v, float elevation) {
        v.setElevation(elevation);
    }

    @Override
    public void trySetActionBar() {
        mActionBarToolbar = (Toolbar) mActivity.findViewById(R.id.toolbar_actionbar);
        if (mActionBarToolbar != null) {
            mActivity.setActionBar(mActionBarToolbar);
        }
    }

    @Override
    public boolean hasLPreviewAPIs() {
        return true;
    }

    public void startActivityWithTransition(Intent intent, final View clickedView,
            final String sharedElementName) {
        ActivityOptions options = null;
        if (clickedView != null && !TextUtils.isEmpty(sharedElementName)) {
            options = ActivityOptions.makeSceneTransitionAnimation(
                    mActivity, clickedView, sharedElementName);
        }

        mActivity.setExitSharedElementListener(new SharedElementListener() {
            @Override
            public void remapSharedElements(List<String> names, Map<String, View> sharedElements) {
                super.remapSharedElements(names, sharedElements);
                sharedElements.put(sharedElementName, clickedView);
            }
        });

        mActivity.startActivity(intent, (options != null) ? options.toBundle() : null);
    }

    @Override
    public void setViewName(View v, String viewName) {
        v.setViewName(viewName);
    }

    @Override
    public void postponeEnterTransition() {
        mActivity.postponeEnterTransition();
    }

    @Override
    public void startPostponedEnterTransition() {
        mActivity.startPostponedEnterTransition();
    }

    @Override
    public void showHideActionBarIfPartOfDecor(boolean show) {
        if (mActionBarToolbar != null) {
            // Action bar is part of the layout
            return;
        }

        // Action bar is part of window decor
        super.showHideActionBarIfPartOfDecor(show);
    }

    public boolean shouldChangeActionBarForDrawer() {
        return false;
    }

    @Override
    public void setMediumTypeface(TextView textView) {
        if (sMediumTypeface == null) {
            sMediumTypeface = Typeface.create("sans-serif-medium", Typeface.NORMAL);
        }

        textView.setTypeface(sMediumTypeface);
    }

    @Override
    public int getStatusBarColor() {
        return mActivity.getWindow().getStatusBarColor();
    }

    @Override
    public void setStatusBarColor(int color) {
        mActivity.getWindow().setStatusBarColor(color);
    }

    @Override
    public void setOrAnimatePlusCheckIcon(final ImageView imageView, boolean isCheck,
            boolean allowAnimate) {
        AnimatedStateListDrawable drawable = (AnimatedStateListDrawable)
                mActivity.getResources().getDrawable(R.drawable.add_schedule_fab_icon_anim);
        imageView.setImageDrawable(drawable);
        if (allowAnimate) {
            // TODO: figure out if there's a way to always animate from current state
            imageView.setImageState(isCheck ? STATE_UNCHECKED : STATE_CHECKED, false);
            drawable.jumpToCurrentState();
            imageView.setImageState(isCheck ? STATE_CHECKED : STATE_UNCHECKED, false);
        } else {
            imageView.setImageState(isCheck ? STATE_CHECKED : STATE_UNCHECKED, false);
            drawable.jumpToCurrentState();
        }
    }
}
