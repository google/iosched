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

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.BaseActivity;
import com.google.android.apps.iosched.ui.SessionDetailFragment;
import com.google.android.apps.iosched.ui.SessionsFragment;
import com.google.android.apps.iosched.ui.TrackInfoHelperFragment;
import com.google.android.apps.iosched.ui.VendorDetailFragment;
import com.google.android.apps.iosched.ui.VendorsFragment;
import com.google.android.apps.iosched.ui.widget.ShowHideMasterLayout;
import com.google.android.apps.iosched.util.BeamUtils;
import com.google.android.apps.iosched.util.UIUtils;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.SearchView;

import static com.google.android.apps.iosched.util.LogUtils.LOGD;

/**
 * A multi-pane activity, consisting of a {@link TracksDropdownFragment} (top-left), a
 * {@link SessionsFragment} or {@link VendorsFragment} (bottom-left), and
 * a {@link SessionDetailFragment} or {@link VendorDetailFragment} (right pane).
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SessionsVendorsMultiPaneActivity extends BaseActivity implements
        ActionBar.TabListener,
        SessionsFragment.Callbacks,
        VendorsFragment.Callbacks,
        VendorDetailFragment.Callbacks,
        TracksDropdownFragment.Callbacks,
        TrackInfoHelperFragment.Callbacks {

    public static final String EXTRA_MASTER_URI =
            "com.google.android.apps.iosched.extra.MASTER_URI";

    private static final String STATE_VIEW_TYPE = "view_type";

    private TracksDropdownFragment mTracksDropdownFragment;
    private Fragment mDetailFragment;
    private boolean mFullUI = false;

    private ShowHideMasterLayout mShowHideMasterLayout;

    private String mViewType;

    private boolean mInitialTabSelect = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (BeamUtils.wasLaunchedThroughBeamFirstTime(this, getIntent())) {
            BeamUtils.setBeamUnlocked(this);
            showFirstBeamDialog();
        }

        BeamUtils.tryUpdateIntentFromBeam(this);

        super.onCreate(savedInstanceState);

        trySetBeamCallback();

        setContentView(R.layout.activity_sessions_vendors);

        final FragmentManager fm = getSupportFragmentManager();
        mTracksDropdownFragment = (TracksDropdownFragment) fm.findFragmentById(
                R.id.fragment_tracks_dropdown);

        mShowHideMasterLayout = (ShowHideMasterLayout) findViewById(R.id.show_hide_master_layout);
        if (mShowHideMasterLayout != null) {
            mShowHideMasterLayout.setFlingToExposeMasterEnabled(true);
        }

        routeIntent(getIntent(), savedInstanceState != null);

        if (savedInstanceState != null) {
            if (mFullUI) {
                getSupportActionBar().setSelectedNavigationItem(
                        TracksDropdownFragment.VIEW_TYPE_SESSIONS.equals(
                                savedInstanceState.getString(STATE_VIEW_TYPE)) ? 0 : 1);
            }

            mDetailFragment = fm.findFragmentById(R.id.fragment_container_detail);
            updateDetailBackground();
        }

        // This flag prevents onTabSelected from triggering extra master pane reloads
        // unless it's actually being triggered by the user (and not automatically by
        // the system)
        mInitialTabSelect = false;

        EasyTracker.getTracker().setContext(this);
    }

    private void routeIntent(Intent intent, boolean updateSurfaceOnly) {
        Uri uri = intent.getData();
        if (uri == null) {
            return;
        }

        if (intent.hasExtra(Intent.EXTRA_TITLE)) {
            setTitle(intent.getStringExtra(Intent.EXTRA_TITLE));
        }

        String mimeType = getContentResolver().getType(uri);

        if (ScheduleContract.Tracks.CONTENT_ITEM_TYPE.equals(mimeType)) {
            // Load track details
            showFullUI(true);
            if (!updateSurfaceOnly) {
                // TODO: don't assume the URI will contain the track ID
                String selectedTrackId = ScheduleContract.Tracks.getTrackId(uri);
                loadTrackList(TracksDropdownFragment.VIEW_TYPE_SESSIONS, selectedTrackId);
                onTrackSelected(selectedTrackId);
                if (mShowHideMasterLayout != null) {
                    mShowHideMasterLayout.showMaster(true, ShowHideMasterLayout.FLAG_IMMEDIATE);
                }
            }

        } else if (ScheduleContract.Sessions.CONTENT_TYPE.equals(mimeType)) {
            // Load a session list, hiding the tracks dropdown and the tabs
            mViewType = TracksDropdownFragment.VIEW_TYPE_SESSIONS;
            showFullUI(false);
            if (!updateSurfaceOnly) {
                loadSessionList(uri, null);
                if (mShowHideMasterLayout != null) {
                    mShowHideMasterLayout.showMaster(true, ShowHideMasterLayout.FLAG_IMMEDIATE);
                }
            }

        } else if (ScheduleContract.Sessions.CONTENT_ITEM_TYPE.equals(mimeType)) {
            // Load session details
            if (intent.hasExtra(EXTRA_MASTER_URI)) {
                mViewType = TracksDropdownFragment.VIEW_TYPE_SESSIONS;
                showFullUI(false);
                if (!updateSurfaceOnly) {
                    loadSessionList((Uri) intent.getParcelableExtra(EXTRA_MASTER_URI),
                            ScheduleContract.Sessions.getSessionId(uri));
                    loadSessionDetail(uri);
                }
            } else {
                mViewType = TracksDropdownFragment.VIEW_TYPE_SESSIONS; // prepare for onTrackInfo...
                showFullUI(true);
                if (!updateSurfaceOnly) {
                    loadSessionDetail(uri);
                    loadTrackInfoFromSessionUri(uri);
                }
            }

        } else if (ScheduleContract.Vendors.CONTENT_TYPE.equals(mimeType)) {
            // Load a vendor list
            mViewType = TracksDropdownFragment.VIEW_TYPE_VENDORS;
            showFullUI(false);
            if (!updateSurfaceOnly) {
                loadVendorList(uri, null);
                if (mShowHideMasterLayout != null) {
                    mShowHideMasterLayout.showMaster(true, ShowHideMasterLayout.FLAG_IMMEDIATE);
                }
            }

        } else if (ScheduleContract.Vendors.CONTENT_ITEM_TYPE.equals(mimeType)) {
            // Load vendor details
            mViewType = TracksDropdownFragment.VIEW_TYPE_VENDORS;
            showFullUI(false);
            if (!updateSurfaceOnly) {
                Uri masterUri = (Uri) intent.getParcelableExtra(EXTRA_MASTER_URI);
                if (masterUri == null) {
                    masterUri = ScheduleContract.Vendors.CONTENT_URI;
                }
                loadVendorList(masterUri, ScheduleContract.Vendors.getVendorId(uri));
                loadVendorDetail(uri);
            }
        }

        updateDetailBackground();
    }

    private void showFullUI(boolean fullUI) {
        mFullUI = fullUI;
        final ActionBar actionBar = getSupportActionBar();
        final FragmentManager fm = getSupportFragmentManager();

        if (fullUI) {
            actionBar.removeAllTabs();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.addTab(actionBar.newTab()
                    .setText(R.string.title_sessions)
                    .setTag(TracksDropdownFragment.VIEW_TYPE_SESSIONS)
                    .setTabListener(this));
            actionBar.addTab(actionBar.newTab()
                    .setText(R.string.title_vendors)
                    .setTag(TracksDropdownFragment.VIEW_TYPE_VENDORS)
                    .setTabListener(this));

            fm.beginTransaction()
                    .show(fm.findFragmentById(R.id.fragment_tracks_dropdown))
                    .commit();
        } else {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
            actionBar.setDisplayShowTitleEnabled(true);

            fm.beginTransaction()
                    .hide(fm.findFragmentById(R.id.fragment_tracks_dropdown))
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getSupportMenuInflater().inflate(R.menu.search, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        if (searchItem != null && UIUtils.hasHoneycomb()) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
                searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mShowHideMasterLayout != null && !mShowHideMasterLayout.isMasterVisible()) {
                    // If showing the detail view, pressing Up should show the master pane.
                    mShowHideMasterLayout.showMaster(true, 0);
                    return true;
                }
                break;

            case R.id.menu_search:
                if (!UIUtils.hasHoneycomb()) {
                    startSearch(null, false, Bundle.EMPTY, false);
                    return true;
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_VIEW_TYPE, mViewType);
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        loadTrackList((String) tab.getTag());
        if (!mInitialTabSelect) {
            onTrackSelected(mTracksDropdownFragment.getSelectedTrackId());
            if (mShowHideMasterLayout != null) {
                mShowHideMasterLayout.showMaster(true, 0);
            }
        }
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    private void loadTrackList(String viewType) {
        loadTrackList(viewType, null);
    }

    private void loadTrackList(String viewType, String selectTrackId) {
        if (mDetailFragment != null && !mViewType.equals(viewType)) {
            getSupportFragmentManager().beginTransaction()
                    .remove(mDetailFragment)
                    .commit();
            mDetailFragment = null;
        }

        mViewType = viewType;
        if (selectTrackId != null) {
            mTracksDropdownFragment.loadTrackList(viewType, selectTrackId);
        } else {
            mTracksDropdownFragment.loadTrackList(viewType);
        }

        updateDetailBackground();
    }

    private void updateDetailBackground() {
        if (mDetailFragment == null) {
            if (TracksDropdownFragment.VIEW_TYPE_SESSIONS.equals(mViewType)) {
                findViewById(R.id.fragment_container_detail).setBackgroundResource(
                        R.drawable.grey_frame_on_white_empty_sessions);
            } else {
                findViewById(R.id.fragment_container_detail).setBackgroundResource(
                        R.drawable.grey_frame_on_white_empty_sandbox);
            }
        } else {
            findViewById(R.id.fragment_container_detail).setBackgroundResource(
                    R.drawable.grey_frame_on_white);
        }
    }

    private void loadSessionList(Uri sessionsUri, String selectSessionId) {
        SessionsFragment fragment = new SessionsFragment();
        fragment.setSelectedSessionId(selectSessionId);
        fragment.setArguments(BaseActivity.intentToFragmentArguments(
                new Intent(Intent.ACTION_VIEW, sessionsUri)));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_master, fragment)
                .commit();
    }

    private void loadSessionDetail(Uri sessionUri) {
        BeamUtils.setBeamSessionUri(this, sessionUri);
        SessionDetailFragment fragment = new SessionDetailFragment();
        fragment.setArguments(BaseActivity.intentToFragmentArguments(
                new Intent(Intent.ACTION_VIEW, sessionUri)));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_detail, fragment)
                .commit();
        mDetailFragment = fragment;
        updateDetailBackground();

        // If loading session details in portrait, hide the master pane
        if (mShowHideMasterLayout != null) {
            mShowHideMasterLayout.showMaster(false, 0);
        }
    }

    private void loadVendorList(Uri vendorsUri, String selectVendorId) {
        VendorsFragment fragment = new VendorsFragment();
        fragment.setSelectedVendorId(selectVendorId);
        fragment.setArguments(BaseActivity.intentToFragmentArguments(
                new Intent(Intent.ACTION_VIEW, vendorsUri)));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_master, fragment)
                .commit();
    }

    private void loadVendorDetail(Uri vendorUri) {
        VendorDetailFragment fragment = new VendorDetailFragment();
        fragment.setArguments(BaseActivity.intentToFragmentArguments(
                new Intent(Intent.ACTION_VIEW, vendorUri)));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_detail, fragment)
                .commit();
        mDetailFragment = fragment;
        updateDetailBackground();

        // If loading session details in portrait, hide the master pane
        if (mShowHideMasterLayout != null) {
            mShowHideMasterLayout.showMaster(false, 0);
        }
    }

    @Override
    public void onTrackNameAvailable(String trackId, String trackName) {
        String trackType;

        if (TracksDropdownFragment.VIEW_TYPE_SESSIONS.equals(mViewType)) {
            trackType = getString(R.string.title_sessions);
        } else {
            trackType = getString(R.string.title_vendors);
        }

        EasyTracker.getTracker().trackView(trackType + ": " + getTitle());
        LOGD("Tracker", trackType + ": " + mTracksDropdownFragment.getTrackName());
    }

    @Override
    public void onTrackSelected(String trackId) {
        boolean allTracks = (ScheduleContract.Tracks.ALL_TRACK_ID.equals(trackId));

        if (TracksDropdownFragment.VIEW_TYPE_SESSIONS.equals(mViewType)) {
            loadSessionList(allTracks
                    ? ScheduleContract.Sessions.CONTENT_URI
                    : ScheduleContract.Tracks.buildSessionsUri(trackId), null);

        } else {
            loadVendorList(allTracks
                    ? ScheduleContract.Vendors.CONTENT_URI
                    : ScheduleContract.Tracks.buildVendorsUri(trackId), null);

        }
    }

    @Override
    public boolean onSessionSelected(String sessionId) {
        loadSessionDetail(ScheduleContract.Sessions.buildSessionUri(sessionId));
        return true;
    }

    @Override
    public boolean onVendorSelected(String vendorId) {
        loadVendorDetail(ScheduleContract.Vendors.buildVendorUri(vendorId));
        return true;
    }

    private TrackInfoHelperFragment mTrackInfoHelperFragment;
    private String mTrackInfoLoadCookie;

    private void loadTrackInfoFromSessionUri(Uri sessionUri) {
        mTrackInfoLoadCookie = ScheduleContract.Sessions.getSessionId(sessionUri);
        Uri trackDirUri = ScheduleContract.Sessions.buildTracksDirUri(
                ScheduleContract.Sessions.getSessionId(sessionUri));
        android.support.v4.app.FragmentTransaction ft =
                getSupportFragmentManager().beginTransaction();
        if (mTrackInfoHelperFragment != null) {
            ft.remove(mTrackInfoHelperFragment);
        }
        mTrackInfoHelperFragment = TrackInfoHelperFragment.newFromTrackUri(trackDirUri);
        ft.add(mTrackInfoHelperFragment, "track_info").commit();
    }

    @Override
    public void onTrackInfoAvailable(String trackId, String trackName, int trackColor) {
        loadTrackList(mViewType, trackId);
        boolean allTracks = (ScheduleContract.Tracks.ALL_TRACK_ID.equals(trackId));

        if (TracksDropdownFragment.VIEW_TYPE_SESSIONS.equals(mViewType)) {
            loadSessionList(allTracks
                    ? ScheduleContract.Sessions.CONTENT_URI
                    : ScheduleContract.Tracks.buildSessionsUri(trackId),
                    mTrackInfoLoadCookie);
        } else {
            loadVendorList(allTracks
                    ? ScheduleContract.Vendors.CONTENT_URI
                    : ScheduleContract.Tracks.buildVendorsUri(trackId),
                    mTrackInfoLoadCookie);
        }
    }

    @Override
    public void onTrackIdAvailable(String trackId) {
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    private void trySetBeamCallback() {
        if (UIUtils.hasICS()) {
            BeamUtils.setBeamCompleteCallback(this, new NfcAdapter.OnNdefPushCompleteCallback() {
                @Override
                public void onNdefPushComplete(NfcEvent event) {
                    // Beam has been sent
                    if (!BeamUtils.isBeamUnlocked(SessionsVendorsMultiPaneActivity.this)) {
                        BeamUtils.setBeamUnlocked(SessionsVendorsMultiPaneActivity.this);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showFirstBeamDialog();
                            }
                        });
                    }
                }
            });
        }
    }

    private void showFirstBeamDialog() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.just_beamed)
                .setMessage(R.string.beam_unlocked_session)
                .setNegativeButton(R.string.close, null)
                .setPositiveButton(R.string.view_beam_session,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface di, int i) {
                                BeamUtils.launchBeamSession(SessionsVendorsMultiPaneActivity.this);
                                di.dismiss();
                            }
                        })
                .create()
                .show();
    }
}
