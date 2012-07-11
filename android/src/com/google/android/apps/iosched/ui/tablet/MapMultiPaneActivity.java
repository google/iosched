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

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.BaseActivity;
import com.google.android.apps.iosched.ui.MapFragment;
import com.google.android.apps.iosched.ui.SessionDetailFragment;
import com.google.android.apps.iosched.ui.SessionsFragment;
import com.google.android.apps.iosched.ui.VendorDetailFragment;
import com.google.android.apps.iosched.ui.VendorsFragment;

import android.annotation.TargetApi;
import android.app.FragmentBreadCrumbs;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * A multi-pane activity, where the full-screen content is a {@link MapFragment} and popup content
 * may be visible at any given time, containing either a {@link SessionsFragment} (representing
 * sessions for a given room) or a {@link SessionDetailFragment}.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class MapMultiPaneActivity extends BaseActivity implements
        FragmentManager.OnBackStackChangedListener,
        MapFragment.Callbacks,
        SessionsFragment.Callbacks,
        LoaderManager.LoaderCallbacks<Cursor> {

    private boolean mPauseBackStackWatcher = false;

    private FragmentBreadCrumbs mFragmentBreadCrumbs;
    private String mSelectedRoomName;

    private MapFragment mMapFragment;

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
        spacerView.setGravity(landscape ? Gravity.RIGHT : Gravity.BOTTOM);

        View popupView = findViewById(R.id.map_detail_popup);
        LinearLayout.LayoutParams popupLayoutParams = (LinearLayout.LayoutParams)
                popupView.getLayoutParams();
        popupLayoutParams.width = landscape ? 0 : ViewGroup.LayoutParams.MATCH_PARENT;
        popupLayoutParams.height = landscape ? ViewGroup.LayoutParams.MATCH_PARENT : 0;
        popupView.setLayoutParams(popupLayoutParams);

        popupView.requestLayout();
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

            // Pan the map left or up depending on the orientation.
            boolean landscape = getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_LANDSCAPE;
            mMapFragment.panBy(
                    landscape ? (show ? 0.25f : -0.25f) : 0,
                    landscape ? 0 : (show ? 0.25f : -0.25f));
        }
    }

    public void updateBreadCrumbs() {
        final String title = (mSelectedRoomName != null)
                ? mSelectedRoomName
                : getString(R.string.title_sessions);
        final String detailTitle = getString(R.string.title_session_detail);

        if (getSupportFragmentManager().getBackStackEntryCount() >= 2) {
            mFragmentBreadCrumbs.setParentTitle(title, title, mFragmentBreadCrumbsClickListener);
            mFragmentBreadCrumbs.setTitle(detailTitle, detailTitle);
        } else {
            mFragmentBreadCrumbs.setParentTitle(null, null, null);
            mFragmentBreadCrumbs.setTitle(title, title);
        }
    }

    private View.OnClickListener mFragmentBreadCrumbsClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            getSupportFragmentManager().popBackStack();
        }
    };

    @Override
    public void onRoomSelected(String roomId) {
        // Load room details
        mSelectedRoomName = null;
        Bundle loadRoomDataArgs = new Bundle();
        loadRoomDataArgs.putString("room_id", roomId);
        getSupportLoaderManager().restartLoader(0, loadRoomDataArgs, this); // force load

        // Show the sessions in the room
        clearBackStack(true);
        showDetailPane(true);
        SessionsFragment fragment = new SessionsFragment();
        fragment.setArguments(BaseActivity.intentToFragmentArguments(
                new Intent(Intent.ACTION_VIEW,
                        ScheduleContract.Rooms.buildSessionsDirUri(roomId))));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_detail, fragment)
                .addToBackStack(null)
                .commit();
        updateBreadCrumbs();
    }

    @Override
    public boolean onSessionSelected(String sessionId) {
        // Show the session details
        showDetailPane(true);
        SessionDetailFragment fragment = new SessionDetailFragment();
        Intent intent = new Intent(Intent.ACTION_VIEW,
                ScheduleContract.Sessions.buildSessionUri(sessionId));
        intent.putExtra(SessionDetailFragment.EXTRA_VARIABLE_HEIGHT_HEADER, true);
        fragment.setArguments(BaseActivity.intentToFragmentArguments(intent));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_detail, fragment)
                .addToBackStack(null)
                .commit();
        updateBreadCrumbs();
        return false;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        return new CursorLoader(this,
                ScheduleContract.Rooms.buildRoomUri(data.getString("room_id")),
                RoomsQuery.PROJECTION,
                null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        try {
            if (!cursor.moveToFirst()) {
                return;
            }

            mSelectedRoomName = cursor.getString(RoomsQuery.ROOM_NAME);
            updateBreadCrumbs();

        } finally {
            cursor.close();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private interface RoomsQuery {
        String[] PROJECTION = {
                ScheduleContract.Rooms.ROOM_ID,
                ScheduleContract.Rooms.ROOM_NAME,
                ScheduleContract.Rooms.ROOM_FLOOR,
        };

        int ROOM_ID = 0;
        int ROOM_NAME = 1;
        int ROOM_FLOOR = 2;
    }
}
