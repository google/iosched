/*
 * Copyright 2012 Google Inc.
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

package com.google.android.apps.iosched.ui.tablet;

import android.annotation.TargetApi;
import android.app.FragmentBreadCrumbs;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.*;
import com.google.android.apps.iosched.ui.SandboxDetailFragment;

/**
 * A multi-pane activity, where the primary navigation pane is a
 * {@link MapFragment}, that shows {@link SessionsFragment},
 * {@link SessionDetailFragment}, {@link com.google.android.apps.iosched.ui.SandboxFragment}, and
 * {@link com.google.android.apps.iosched.ui.SandboxDetailFragment} as popups. This activity requires API level 11
 * or greater because of its use of {@link FragmentBreadCrumbs}.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MapMultiPaneActivity extends BaseActivity implements
        FragmentManager.OnBackStackChangedListener,
        MapFragment.Callbacks,
        SessionsFragment.Callbacks,
        SandboxFragment.Callbacks,
        SandboxDetailFragment.Callbacks{

    private boolean mPauseBackStackWatcher = false;

    private FragmentBreadCrumbs mFragmentBreadCrumbs;
    private String mSelectedRoomName;

    private MapFragment mMapFragment;

    private boolean isSessionShown = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(this);

        mFragmentBreadCrumbs = (FragmentBreadCrumbs) findViewById(R.id.breadcrumbs);
        mFragmentBreadCrumbs.setActivity(this);

        mMapFragment = (MapFragment) fm.findFragmentByTag("map");
        if (mMapFragment == null) {
            mMapFragment = new MapFragment();
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
        popupView.setLayoutParams(popupLayoutParams);

        popupView.requestLayout();

        updateMapPadding();
    }

    private void clearBackStack(boolean pauseWatcher) {
        if (pauseWatcher) {
            mPauseBackStackWatcher = true;
        }

        FragmentManager fm = getSupportFragmentManager();
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

        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
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
         String detailTitle;
        if(isSessionShown){
            detailTitle = getString(R.string.title_session_detail);
        }else{
            detailTitle = getString(R.string.title_sandbox_detail);
        }

        if (getSupportFragmentManager().getBackStackEntryCount() >= 2) {
            mFragmentBreadCrumbs.setParentTitle(mSelectedRoomName, mSelectedRoomName,
                    mFragmentBreadCrumbsClickListener);
            mFragmentBreadCrumbs.setTitle(detailTitle, detailTitle);
        } else {
            mFragmentBreadCrumbs.setParentTitle(null, null, null);
            mFragmentBreadCrumbs.setTitle(mSelectedRoomName, mSelectedRoomName);
        }
    }

    private View.OnClickListener mFragmentBreadCrumbsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            getSupportFragmentManager().popBackStack();
        }
    };

    @Override
    public void onSessionRoomSelected(String roomId, String roomTitle) {
        // Load room details
        mSelectedRoomName = roomTitle;
        isSessionShown = true;

        SessionsFragment fragment = new SessionsFragment();
        Uri uri = ScheduleContract.Rooms.buildSessionsDirUri(roomId);

        showList(fragment,uri);
    }

    @Override
    public void onSandboxRoomSelected(String trackId, String roomTitle) {
        // Load room details
        mSelectedRoomName = roomTitle;
        isSessionShown = false;

        Fragment fragment = new SandboxFragment();
        Uri uri = ScheduleContract.Tracks.buildSandboxUri(trackId);

        showList(fragment,uri);
    }

    @Override
    public boolean onCompanySelected(String companyId) {
        isSessionShown = false;

        final Uri uri = ScheduleContract.Sandbox.buildCompanyUri(companyId);
        SandboxDetailFragment fragment = new SandboxDetailFragment();

        showDetails(fragment,uri);

        return false;
    }

    @Override
    public boolean onSessionSelected(String sessionId) {
        isSessionShown = true;

        final Uri uri = ScheduleContract.Sessions.buildSessionUri(sessionId);
        SessionDetailFragment fragment = new SessionDetailFragment();

        showDetails(fragment,uri);

        return false;
    }

    private void showList(Fragment fragment, Uri uri){
        // Show the sessions in the room
        clearBackStack(true);
        showDetailPane(true);
        fragment.setArguments(BaseActivity.intentToFragmentArguments(
                new Intent(Intent.ACTION_VIEW,
                        uri
                )));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_detail, fragment)
                .addToBackStack(null)
                .commit();
        updateBreadCrumbs();
    }

    private void showDetails(Fragment fragment, Uri uri){
        // Show the session details
        showDetailPane(true);
        Intent intent = new Intent(Intent.ACTION_VIEW,uri);
        intent.putExtra(SessionDetailFragment.EXTRA_VARIABLE_HEIGHT_HEADER, true);
        fragment.setArguments(BaseActivity.intentToFragmentArguments(intent));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_detail, fragment)
                .addToBackStack(null)
                .commit();
        updateBreadCrumbs();
    }

    @Override
    public void onTrackIdAvailable(String trackId) {
    }
}
