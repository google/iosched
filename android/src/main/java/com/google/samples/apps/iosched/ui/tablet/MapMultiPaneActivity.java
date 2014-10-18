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

import android.app.FragmentBreadCrumbs;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.BaseMapActivity;
import com.google.samples.apps.iosched.ui.MapFragment;
import com.google.samples.apps.iosched.ui.NearbyFragment;
import com.google.samples.apps.iosched.ui.PartnersFragment;
import com.google.samples.apps.iosched.ui.SessionDetailActivity;
import com.google.samples.apps.iosched.ui.SessionsFragment;
import com.google.samples.apps.iosched.util.AnalyticsManager;
import com.google.samples.apps.iosched.util.UIUtils;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;

/**
 * A multi-pane activity, where the primary navigation pane is a
 * {@link MapFragment}, that shows {@link NearbyFragment},
 * {@link PartnersFragment} as popups.
 */
public class MapMultiPaneActivity extends BaseMapActivity implements
        FragmentManager.OnBackStackChangedListener,
        MapFragment.Callbacks,
        SessionsFragment.Callbacks {

    private static final String SCREEN_LABEL = "MapMultipane";

    private boolean mPauseBackStackWatcher = false;

    private FragmentBreadCrumbs mFragmentBreadCrumbs;
    private String mSelectedRoomName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        FragmentManager fm = getFragmentManager();
        fm.addOnBackStackChangedListener(this);

        mFragmentBreadCrumbs = (FragmentBreadCrumbs) findViewById(R.id.breadcrumbs);
        mFragmentBreadCrumbs.setActivity(this);

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
        if (mMapFragment != null) {
            mMapFragment.setCenterPadding(
                    landscape ? (detailShown ? 0.25f : 0f) : 0,
                    landscape ? 0 : (detailShown ? 0.25f : 0));
        }
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
        getLUtils().startActivityWithTransition(
                new Intent(Intent.ACTION_VIEW,
                        ScheduleContract.Sessions.buildSessionUri(sessionId)),
                clickedView,
                SessionDetailActivity.TRANSITION_NAME_PHOTO
        );
    }

    @Override
    public void onTagMetadataLoaded(TagMetadata metadata) {}

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
}
