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

package com.google.samples.apps.iosched.map;

import android.Manifest;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.navigation.NavigationModel;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.util.AnalyticsHelper;
import com.google.samples.apps.iosched.util.PermissionsUtils;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * Activity that displays a {@link com.google.samples.apps.iosched.map.MapFragment} and a {@link
 * com.google.samples.apps.iosched.map.MapInfoFragment}. Supports 'detached' mode, where the toolbar
 * contains an up navigation option that finishes this Activity. (see {@link #EXTRA_DETACHED_MODE}
 * Optionally a room can be specified via {@link #EXTRA_ROOM} that pans the map to its indicated
 * marker.
 *
 * @see com.google.samples.apps.iosched.map.MapInfoFragment#newInstace(android.content.Context)
 */
public class MapActivity extends BaseActivity
        implements SlideableInfoFragment.Callback, MapFragment.Callbacks,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = makeLogTag(MapActivity.class);

    private static final int REQUEST_LOCATION_PERMISSION = 1;

    public static final String[] PERMISSIONS =
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION};

    private static final String SCREEN_LABEL = "Map";

    /**
     * When specified, will automatically point the map to the requested room.
     */
    public static final String EXTRA_ROOM = "com.google.android.iosched.extra.ROOM";

    public static final String EXTRA_DETACHED_MODE
            = "com.google.samples.apps.iosched.EXTRA_DETACHED_MODE";

    public static final String BUNDLE_STATE_MAPVIEW = "mapview";

    private boolean mDetachedMode;

    protected MapFragment mMapFragment;

    private MapInfoFragment mInfoFragment;

    private View mInfoContainer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FragmentManager fm = getFragmentManager();
        mMapFragment = (MapFragment) fm.findFragmentByTag("map");

        mDetachedMode = getIntent().getBooleanExtra(EXTRA_DETACHED_MODE, false);

        if (isFinishing()) {
            return;
        }

        setContentView(R.layout.map_act);
        mInfoContainer = findViewById(R.id.map_detail_popup);

        // ANALYTICS SCREEN: View the Map screen on a phone
        // Contains: Nothing (Page name is a constant)
        AnalyticsHelper.sendScreenView(SCREEN_LABEL);

        overridePendingTransition(0, 0);
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Save the mapview state in a separate bundle parameter
        final Bundle mapviewState = new Bundle();
        mMapFragment.onSaveInstanceState(mapviewState);
        outState.putBundle(BUNDLE_STATE_MAPVIEW, mapviewState);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        if (mDetachedMode) {
            final Toolbar toolbar = getToolbar();
            toolbar.setNavigationIcon(R.drawable.ic_up);
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });
        }

        if (mMapFragment == null) {
            // Either restore the state of the map or read it from the Intent extras.
            if (savedInstanceState != null) {
                // Restore state from existing bundle
                Bundle previousState = savedInstanceState.getBundle(BUNDLE_STATE_MAPVIEW);
                mMapFragment = MapFragment.newInstance(previousState);
            } else {
                // Get highlight room id (if specified in intent extras)
                final String highlightRoomId = getIntent().hasExtra(EXTRA_ROOM) ? getIntent()
                        .getExtras().getString(EXTRA_ROOM) : null;
                mMapFragment = MapFragment.newInstance(highlightRoomId);
            }
            getFragmentManager().beginTransaction()
                                .add(R.id.fragment_container_map, mMapFragment, "map")
                                .commit();
        }

        if (mInfoFragment == null) {
            mInfoFragment = MapInfoFragment.newInstace(this);
            getFragmentManager().beginTransaction()
                                .add(R.id.fragment_container_map_info, mInfoFragment, "mapsheet")
                                .commit();
        }

        mDetachedMode = getIntent().getBooleanExtra(EXTRA_DETACHED_MODE, false);

        attemptEnableMyLocation();
    }

    @Override
    public void onBackPressed() {
        if (mInfoFragment != null && mInfoFragment.isExpanded()) {
            mInfoFragment.minimize();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected NavigationModel.NavigationItemEnum getSelfNavDrawerItem() {
        return mDetachedMode ? NavigationModel.NavigationItemEnum.INVALID :
                NavigationModel.NavigationItemEnum.MAP;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (getIntent().getBooleanExtra(EXTRA_DETACHED_MODE, false)
                && item.getItemId() == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Adjust the padding in the map fragment when the info fragment has been resized. The size is
     * self reported from the fragment and has to be adjusted before it can be applied to the map.
     * <p/>
     * For {@link com.google.samples.apps.iosched.map.InlineInfoFragment} (that is only displayed on
     * the left of the screen), the full extend of its container layout (including padding) is
     * passed to the map fragment. For {@link com.google.samples.apps.iosched.map
     * .SlideableInfoFragment} (that is only displayed at the bottom of the screen), its actual
     * height is passed through to the map.
     */
    @Override
    public void onInfoSizeChanged(int left, int top, int right, int bottom) {
        if (mMapFragment != null) {
            if (mInfoFragment instanceof InlineInfoFragment) {
                // InlineInfoFragment is shown on the left on tablet layouts.
                // Use the right edge of its containers for padding of the map.
                //TODO: RTL support - compare left and right positioning
                if (right > 0) {
                    mMapFragment.setMapInsets(mInfoContainer.getRight(), 0, 0, 0);
                } else {
                    mMapFragment.setMapInsets(0, 0, 0, 0);
                }
            } else if (mInfoFragment instanceof SlideableInfoFragment) {
                // SlideableInfoFragment is only shown on phone layouts at the bottom of the screen,
                // but only up to 50% of the total height of the screen
                if ((float) top / (float) bottom > SlideableInfoFragment.MAX_PANEL_PADDING_FACTOR) {

                    mMapFragment.setMapInsets(0, 0, 0, bottom - top);
                }
                final int bottomPadding = Math.min(bottom - top,
                        Math.round(bottom * SlideableInfoFragment.MAX_PANEL_PADDING_FACTOR));
                mMapFragment.setMapInsets(0, 0, 0, bottomPadding);
            }
            // F
        }
    }

    @Override
    public void onSessionClicked(String sessionId) {
        // ANALYTICS EVENT: Click on a session in the Maps screen.
        // Contains: The session ID.
        AnalyticsHelper.sendEvent(SCREEN_LABEL, "selectsession", sessionId);
        startActivity(new Intent(Intent.ACTION_VIEW,
                ScheduleContract.Sessions.buildSessionUri(sessionId)));
    }

    @Override
    public void onInfoShowVenue() {
        if (mInfoFragment != null) {
            mInfoFragment.showVenue();
        }
        setTabletInfoVisibility(View.VISIBLE);

    }

    @Override
    public void onInfoShowTitle(String label, int icon) {
        if (mInfoFragment != null) {
            mInfoFragment.showTitleOnly(icon, label);
        }
        setTabletInfoVisibility(View.VISIBLE);

    }

    @Override
    public void onInfoShowSessionlist(String roomId, String roomTitle, int roomType) {
        if (mInfoFragment != null) {
            mInfoFragment.showSessionList(roomId, roomTitle, roomType);
        }
        setTabletInfoVisibility(View.VISIBLE);

    }

    @Override
    public void onInfoShowFirstSessionTitle(String roomId, String roomTitle, int roomType) {
        if (mInfoFragment != null) {
            mInfoFragment.showFirstSessionTitle(roomId, roomTitle, roomType);
        }
        setTabletInfoVisibility(View.VISIBLE);
    }

    @Override
    public void onInfoHide() {
        if (mInfoFragment != null) {
            mInfoFragment.hide();
        }
        setTabletInfoVisibility(View.GONE);
    }

    private void setTabletInfoVisibility(int visibility) {
        final View view = findViewById(R.id.map_detail_popup);
        if (view != null) {
            view.setVisibility(visibility);

        }
    }

    /**
     * Enables the 'My Location' feature on the map fragment if the location permission has been
     * granted. If the permission is not available yet, it is requested.
     */
    public void attemptEnableMyLocation() {
        // Check if the permission has already been granted.
        if (PermissionsUtils.permissionsAlreadyGranted(this, PERMISSIONS)) {
            // Permission has been granted.
            if (mMapFragment != null) {
                mMapFragment.setMyLocationEnabled(true);
                return;
            }
        }

        // The permissions have not been granted yet. Request them.
        ActivityCompat.requestPermissions(this, PERMISSIONS, REQUEST_LOCATION_PERMISSION);
    }

    @Override
    public void onRequestPermissionsResult(final int requestCode,
            @NonNull final String[] permissions,
            @NonNull final int[] grantResults) {

        if (requestCode != REQUEST_LOCATION_PERMISSION) {
            return;
        }

        if (permissions.length == PERMISSIONS.length &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission has been granted.
            if (mMapFragment != null) {
                mMapFragment.setMyLocationEnabled(true);
            }
        } else {
            // Permission was denied. Display error message that disappears after a short while.
             PermissionsUtils.displayConditionalPermissionDenialSnackbar(this,
                    R.string.map_permission_denied, PERMISSIONS, REQUEST_LOCATION_PERMISSION, false);

        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
