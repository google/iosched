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

package com.google.samples.apps.iosched.ui.tablet;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentBreadCrumbs;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.*;
import com.google.samples.apps.iosched.ui.phone.MapActivity;
import com.google.samples.apps.iosched.util.AnalyticsManager;
import com.google.samples.apps.iosched.util.UIUtils;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;

/**
 * A multi-pane activity, where the primary navigation pane is a
 * {@link MapFragment}, that shows {@link SessionsFragment},
 * {@link SessionDetailFragment} as popups. This activity requires API level 11
 * or greater because of its use of {@link FragmentBreadCrumbs}.
 */
public class MapMultiPaneActivity extends NearbyActivity implements
        FragmentManager.OnBackStackChangedListener,
        MapFragment.Callbacks,
        SessionsFragment.Callbacks {

    private static final String SCREEN_LABEL = "MapMultipane";

    private boolean mPauseBackStackWatcher = false;

    private FragmentBreadCrumbs mFragmentBreadCrumbs;
    private String mSelectedRoomName;

    private MapFragment mMapFragment;

    private int mActionBarOnColor;
    private int mActionBarOffColor;
    private ColorDrawable mActionBarBgDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        getLPreviewUtils().trySetActionBar();

        FragmentManager fm = getFragmentManager();
        fm.addOnBackStackChangedListener(this);

        mFragmentBreadCrumbs = (FragmentBreadCrumbs) findViewById(R.id.breadcrumbs);
        mFragmentBreadCrumbs.setActivity(this);

        mMapFragment = (MapFragment) fm.findFragmentByTag("map");
        if (mMapFragment == null) {
            mMapFragment = MapFragment.newInstance();
            mMapFragment.setArguments(intentToFragmentArguments(getIntent()));

            fm.beginTransaction()
                    .add(R.id.fragment_container_map, mMapFragment, "map")
                    .commit();
        }

        findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                clearBackStack(false);
            }
        });

        updateBreadCrumbs();
        onConfigurationChanged(getResources().getConfiguration());

        /* [ANALYTICS:SCREEN]
         * TRIGGER:   View the map screen on a tablet.
         * LABEL:    'MapMultipane'
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
    }

    private void updateActionBarNavigation() {
        boolean show = !isNavDrawerOpen();
        if (getLPreviewUtils().shouldChangeActionBarForDrawer()) {
            ActionBar ab = getActionBar();
            ab.setDisplayShowTitleEnabled(show);
            ab.setDisplayUseLogoEnabled(!show);
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
    protected int getSelfNavDrawerItem() {
        if (getIntent().getBooleanExtra(MapActivity.EXTRA_DETACHED_MODE, false)) {
            // in detached mode, we don't have a nav drawer
            return NAVDRAWER_ITEM_INVALID;
        } else {
            return NAVDRAWER_ITEM_MAP;
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        boolean landscape = (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE);

        LinearLayout spacerView = (LinearLayout) findViewById(R.id.map_detail_spacer);
        spacerView.setOrientation(landscape ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        spacerView.setGravity(landscape ? Gravity.END : Gravity.BOTTOM);

        View popupView = findViewById(R.id.map_detail_popup);
        LinearLayout.LayoutParams popupLayoutParams = (LinearLayout.LayoutParams)
                popupView.getLayoutParams();

        popupLayoutParams.width = landscape ? 0 : ViewGroup.LayoutParams.MATCH_PARENT;
        popupLayoutParams.height = landscape ? ViewGroup.LayoutParams.MATCH_PARENT : 0;
        popupLayoutParams.topMargin =
                getResources().getDimensionPixelSize(R.dimen.multipane_half_padding) +
                        (landscape ? UIUtils.calculateActionBarSize(this) : 0);
        popupView.setLayoutParams(popupLayoutParams);

        popupView.requestLayout();

        updateMapPadding();
    }

    private void clearBackStack(boolean pauseWatcher) {
        if (pauseWatcher) {
            mPauseBackStackWatcher = true;
        }

        FragmentManager fm = getFragmentManager();
        while (fm.getBackStackEntryCount() > 0) {
            fm.popBackStackImmediate();
        }

        if (pauseWatcher) {
            mPauseBackStackWatcher = false;
        }
    }

    public void onBackStackChanged() {
        if (mPauseBackStackWatcher) {
            return;
        }

        if (getFragmentManager().getBackStackEntryCount() == 0) {
            showDetailPane(false);
        }

        updateBreadCrumbs();
    }

    private void showDetailPane(boolean show) {
        View detailPopup = findViewById(R.id.map_detail_spacer);
        if (show != (detailPopup.getVisibility() == View.VISIBLE)) {
            detailPopup.setVisibility(show ? View.VISIBLE : View.GONE);

            updateMapPadding();
        }
    }

    private void updateMapPadding() {
        // Pan the map left or up depending on the orientation.
        boolean landscape = getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
        boolean detailShown = findViewById(R.id.map_detail_spacer).getVisibility() == View.VISIBLE;

        mMapFragment.setCenterPadding(
                landscape ? (detailShown ? 0.25f : 0f) : 0,
                landscape ? 0 : (detailShown ? 0.25f : 0));
    }

    void updateBreadCrumbs() {
        mFragmentBreadCrumbs.setParentTitle(null, null, null);
        mFragmentBreadCrumbs.setTitle(mSelectedRoomName, mSelectedRoomName);
    }

    @Override
    public void onSessionRoomSelected(String roomId, String roomTitle) {
        // We no longer have a way to display sessions that happen in a given room
    }

    @Override
    public void onShowPartners() {
        showDetailPane(true);
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_detail, PartnersFragment.newInstance(false))
                .setBreadCrumbTitle(R.string.partners)
                .setBreadCrumbShortTitle(R.string.partners)
                .addToBackStack(null)
                .commit();
        updateBreadCrumbs();
    }

    @Override
    public void onSessionSelected(String sessionId, View clickedView) {
        /* [ANALYTICS:EVENT]
         * TRIGGER:   Click on a session in the Maps screen.
         * CATEGORY:  'Maps'
         * ACTION:    'selectsession'
         * LABEL:     session ID (for example "3284-fac320-2492048-bf391')
         * [/ANALYTICS]
         */
        AnalyticsManager.sendEvent(SCREEN_LABEL, "selectsession", sessionId);
        getLPreviewUtils().startActivityWithTransition(
                new Intent(Intent.ACTION_VIEW,
                        ScheduleContract.Sessions.buildSessionUri(sessionId)),
                clickedView,
                SessionDetailFragment.VIEW_NAME_PHOTO
        );
    }

    @Override
    public void onTagMetadataLoaded(TagMetadata metadata) {}

    private void showList(Fragment fragment, Uri uri){
        // Show the sessions in the room
        clearBackStack(true);
        showDetailPane(true);
        fragment.setArguments(BaseActivity.intentToFragmentArguments(
                new Intent(Intent.ACTION_VIEW,
                        uri
                )));
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_detail, fragment)
                .addToBackStack(null)
                .commit();
        updateBreadCrumbs();
    }

    private void showDetails(Fragment fragment, Uri uri){
        // Show the session details
        showDetailPane(true);
        Intent intent = new Intent(Intent.ACTION_VIEW,uri);
        //intent.putExtra(SessionDetailFragment.EXTRA_VARIABLE_HEIGHT_HEADER, true);
        fragment.setArguments(BaseActivity.intentToFragmentArguments(intent));
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_detail, fragment)
                .addToBackStack(null)
                .commit();
        updateBreadCrumbs();
    }


    // TODO: This should also update the breadcrumbs, which will likely involve a major
    // refactoring of the way breadcrumbs are handled. Perhaps we can store breadcrumb titles in
    // every back stack entry...
    @Override
    protected void showNearbyFragment(String tag) {
        showDetailPane(true);
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_detail, NearbyFragment.newInstance(false), tag)
                .setBreadCrumbTitle(R.string.map_nearby_button)
                .setBreadCrumbShortTitle(R.string.map_nearby_button)
                .addToBackStack(null)
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (getIntent().getBooleanExtra(MapActivity.EXTRA_DETACHED_MODE, false)
                && item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }
}
