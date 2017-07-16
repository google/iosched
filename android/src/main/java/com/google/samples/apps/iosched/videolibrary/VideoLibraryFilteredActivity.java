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

package com.google.samples.apps.iosched.videolibrary;

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.util.AnalyticsHelper;
import com.google.samples.apps.iosched.util.ImageLoader;
import com.google.samples.apps.iosched.util.UIUtils;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * This Activity displays all the videos of past Google I/O sessions. You can also filter them per
 * year and/or topics.
 *
 * You can set the initial filter when launching this activity by adding The Topic and/or year to
 * the extras. For this use the {@code KEY_FILTER_TOPIC} and {@code KEY_FILTER_YEAR} keys.
 */
public class VideoLibraryFilteredActivity extends BaseActivity implements
        Toolbar.OnMenuItemClickListener,
        VideoLibraryFilteredFragment.VideoLibraryFilteredContainer {

    private static final String TAG = makeLogTag(VideoLibraryFilteredActivity.class);

    private static final String SCREEN_LABEL = "Filtered Video Library";

    protected static final String KEY_FILTER_TOPIC =
            "com.google.samples.apps.iosched.KEY_FILTER_TOPIC";

    protected static final String KEY_FILTER_YEAR =
            "com.google.samples.apps.iosched.KEY_FILTER_YEAR";

    private DrawerLayout mDrawerLayout;

    private CollapsingToolbarLayout mCollapsingToolbar;

    private ImageView mHeaderImage;

    private ImageLoader mImageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.video_library_filtered_act);

        mImageLoader = new ImageLoader(this);
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        mCollapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar);
        mHeaderImage = (ImageView) findViewById(R.id.header_image);
        setTitle(R.string.title_video_library);

        // ANALYTICS SCREEN: View the Filtered Video Library screen
        // Contains: Nothing (Page name is a constant)
        AnalyticsHelper.sendScreenView(SCREEN_LABEL);
        LOGD("Tracker", SCREEN_LABEL);

        // Add the back button to the toolbar.
        setToolbarAsUp(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateUpOrBack(VideoLibraryFilteredActivity.this, null);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Add the filter button to the toolbar.
        Toolbar toolbar = getToolbar();
        toolbar.inflateMenu(R.menu.video_library_filtered);
        toolbar.setOnMenuItemClickListener(this);
        return true;
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.menu_filter:
                LOGD(TAG, "Clicking Filter menu button on FilteredVideoLib.");
                mDrawerLayout.openDrawer(GravityCompat.END);
                return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.END)) {
            mDrawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void filtersUpdated(@NonNull final String title, @Nullable final String selectionImage,
            @ColorInt int trackColor) {
        setTitle(title);
        if (selectionImage != null) {
            mHeaderImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
            mImageLoader.loadImage(selectionImage, mHeaderImage);
        } else {
            mHeaderImage.setScaleType(ImageView.ScaleType.FIT_CENTER);
            mHeaderImage.setImageResource(R.drawable.ic_hash_io_16_monochrome);
        }
        final int statusBarColor =
                trackColor != Color.TRANSPARENT ? UIUtils.adjustColorForStatusBar(trackColor) :
                        UIUtils.getThemeColor(this, R.attr.colorPrimaryDark,
                                R.color.theme_primary_dark);
        final @ColorInt int toolbarScrim = trackColor != Color.TRANSPARENT ? trackColor :
                ContextCompat.getColor(this, R.color.io16_light_grey);
        mCollapsingToolbar.setContentScrimColor(toolbarScrim);
        mDrawerLayout.setStatusBarBackgroundColor(statusBarColor);
    }
}
