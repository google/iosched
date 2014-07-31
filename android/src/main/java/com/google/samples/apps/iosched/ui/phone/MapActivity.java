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

package com.google.samples.apps.iosched.ui.phone;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.MenuItem;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.ui.MapFragment;
import com.google.samples.apps.iosched.ui.NearbyActivity;
import com.google.samples.apps.iosched.ui.NearbyFragment;
import com.google.samples.apps.iosched.ui.PartnersFragment;
import com.google.samples.apps.iosched.util.AnalyticsManager;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class MapActivity extends NearbyActivity implements MapFragment.Callbacks {

    private static final String TAG = makeLogTag(MapActivity.class);
    public static final String EXTRA_DETACHED_MODE
            = "com.google.samples.apps.iosched.EXTRA_DETACHED_MODE";

    private static final String SCREEN_LABEL = "Map";
    private static final String PARTNERS_FRAGMENT_TAG = "partners";

    private int mActionBarOnColor;
    private int mActionBarOffColor;
    private ColorDrawable mActionBarBgDrawable;
    private boolean mPopupVisible = false; // Nearby or Partners

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.activity_map);
        getLPreviewUtils().trySetActionBar();

        if (null == savedInstanceState) {
            // Pass arguments to MapFragment
            MapFragment fragment = MapFragment.newInstance();
            fragment.setArguments(intentToFragmentArguments(getIntent()));
            getFragmentManager().beginTransaction()
                    .replace(R.id.main_content, fragment)
                    .commit();
        }

        /* [ANALYTICS:SCREEN]
         * TRIGGER:   View the Map screen on a phone.
         * LABEL:     "Map"
         * [/ANALYTICS]
         */
        AnalyticsManager.sendScreenView(SCREEN_LABEL);
        LOGD("Tracker", SCREEN_LABEL);

        overridePendingTransition(0, 0);

        final Resources res = getResources();
        mActionBarOffColor = res.getColor(R.color.translucent_actionbar_background);
        mActionBarOnColor = res.getColor(R.color.theme_primary);

        // Initialise and set background drawable here explicitly to ensure the background
        // is drawn  when the background color is changed on JellyBean
        mActionBarBgDrawable = new ColorDrawable(mActionBarOffColor);
        getActionBar().setBackgroundDrawable(mActionBarBgDrawable);

        getFragmentManager().addOnBackStackChangedListener(
                new FragmentManager.OnBackStackChangedListener() {
                    @Override
                    public void onBackStackChanged() {
                        mPopupVisible = (getFragmentManager().getBackStackEntryCount() == 1);
                        updateActionBarNavigation();
                    }
                }
        );

        updateActionBarNavigation();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        Fragment nearbyFragment = getFragmentManager().findFragmentByTag(NEARBY_FRAGMENT_TAG);
        Fragment partnersFragment = getFragmentManager().findFragmentByTag(PARTNERS_FRAGMENT_TAG);
        mPopupVisible = nearbyFragment != null || partnersFragment != null;
        updateActionBarNavigation();
    }

    private void updateActionBarNavigation() {
        boolean show = !isNavDrawerOpen();
        ActionBar ab = getActionBar();
        if (getLPreviewUtils().shouldChangeActionBarForDrawer()) {
            ab.setDisplayShowTitleEnabled(show);
            ab.setDisplayUseLogoEnabled(!show);
        }

        if (mPopupVisible) {
            ab.hide();
        } else {
            ab.show();
        }
    }

    @Override
    protected void onNavDrawerStateChanged(boolean isOpen, boolean isAnimating) {
        super.onNavDrawerStateChanged(isOpen, isAnimating);
        updateActionBarNavigation();
    }

    @Override
    protected void onNavDrawerSlide(float offset) {
        super.onNavDrawerSlide(offset);
        // Make Action Bar gradually fade into the theme color
        mActionBarBgDrawable.setColor(Color.argb(
                Color.alpha(mActionBarOffColor) + (int) (offset * (Color.alpha(mActionBarOnColor) - Color.alpha(mActionBarOffColor))),
                Color.red(mActionBarOffColor) + (int) (offset * (Color.red(mActionBarOnColor) - Color.red(mActionBarOffColor))),
                Color.green(mActionBarOffColor) + (int) (offset * (Color.green(mActionBarOnColor) - Color.green(mActionBarOffColor))),
                Color.blue(mActionBarOffColor) + (int) (offset * (Color.blue(mActionBarOnColor) - Color.blue(mActionBarOffColor)))
        ));
        getActionBar().setBackgroundDrawable(mActionBarBgDrawable);
    }

    @Override
    public void onSessionRoomSelected(String roomId, String roomTitle) {
        // we no longer have a screen that shows sessions on a given room
    }

    @Override
    public void onShowPartners() {
        getFragmentManager().beginTransaction()
                .replace(R.id.main_content, PartnersFragment.newInstance(true),
                        PARTNERS_FRAGMENT_TAG)
                .addToBackStack(null)
                .commit();
    }

    @Override
    protected int getSelfNavDrawerItem() {
        if (getIntent().getBooleanExtra(EXTRA_DETACHED_MODE, false)) {
            // in detached mode, we don't have a nav drawer
            return NAVDRAWER_ITEM_INVALID;
        } else {
            return NAVDRAWER_ITEM_MAP;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (getIntent().getBooleanExtra(EXTRA_DETACHED_MODE, false)
                && item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    // Show whichever Fragment has been provided by NearbyActivity.
    @Override
    protected void showNearbyFragment(String tag) {
        getFragmentManager().beginTransaction()
                .replace(R.id.main_content, NearbyFragment.newInstance(true), tag)
                .addToBackStack(null)
                .commit();
    }
}
