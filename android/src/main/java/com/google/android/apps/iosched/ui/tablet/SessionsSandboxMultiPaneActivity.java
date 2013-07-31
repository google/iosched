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
import android.app.SearchManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SlidingPaneLayout;
import android.support.v7.app.ActionBar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SearchView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.*;
import com.google.android.apps.iosched.util.BeamUtils;
import com.google.android.apps.iosched.util.ImageLoader;
import com.google.android.apps.iosched.util.UIUtils;

import static com.google.android.apps.iosched.util.LogUtils.LOGD;

/**
 * A multi-pane activity, consisting of a {@link TracksDropdownFragment}, a
 * {@link SessionsFragment} or {@link com.google.android.apps.iosched.ui.SandboxFragment}, and {@link SessionDetailFragment} or
 * {@link com.google.android.apps.iosched.ui.SandboxDetailFragment}.
 *
 * This activity requires API level 11 or greater.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class SessionsSandboxMultiPaneActivity extends BaseActivity implements
        ActionBar.OnNavigationListener,
        SessionsFragment.Callbacks,
        SandboxFragment.Callbacks,
        SandboxDetailFragment.Callbacks,
        TracksDropdownFragment.Callbacks,
        TrackInfoHelperFragment.Callbacks,
        ImageLoader.ImageLoaderProvider {

    public static final String EXTRA_MASTER_URI =
            "com.google.android.apps.iosched.extra.MASTER_URI";

    public static final String EXTRA_DEFAULT_VIEW_TYPE =
            "com.google.android.apps.iosched.extra.DEFAULT_VIEW_TYPE";

    private static final String STATE_VIEW_TYPE = "view_type";

    private TracksDropdownFragment mTracksDropdownFragment;
    private Fragment mDetailFragment;
    private boolean mFullUI = false;

    private SlidingPaneLayout mSlidingPaneLayout;

    private int mViewType;

    private boolean mInitialTabSelect = true;

    private ImageLoader mImageLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UIUtils.tryTranslateHttpIntent(this);
        BeamUtils.tryUpdateIntentFromBeam(this);

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sessions_sandbox);

        final FragmentManager fm = getSupportFragmentManager();
        mTracksDropdownFragment = (TracksDropdownFragment) fm.findFragmentById(
                R.id.fragment_tracks_dropdown);

        mSlidingPaneLayout = (SlidingPaneLayout) findViewById(R.id.sliding_pane_layout);

        // Offset the left pane by its full width and left margin when collapsed
        // (ViewPager-like presentation)
        mSlidingPaneLayout.setParallaxDistance(
                getResources().getDimensionPixelSize(R.dimen.sliding_pane_width) +
                        getResources().getDimensionPixelSize(R.dimen.multipane_padding));
        mSlidingPaneLayout.setSliderFadeColor(getResources().getColor(
                R.color.sliding_pane_content_fade));

        routeIntent(getIntent(), savedInstanceState != null);

        if (savedInstanceState != null) {
            if (mFullUI) {
                int viewType = savedInstanceState.getInt(STATE_VIEW_TYPE);
                getSupportActionBar().setSelectedNavigationItem(viewType);
            }

            mDetailFragment = fm.findFragmentById(R.id.fragment_container_detail);
            updateDetailBackground();
        }

        // This flag prevents onTabSelected from triggering extra master pane reloads
        // unless it's actually being triggered by the user (and not automatically by
        // the system)
        mInitialTabSelect = false;

        mImageLoader = new ImageLoader(this, R.drawable.person_image_empty)
                .setMaxImageSize(getResources().getDimensionPixelSize(R.dimen.speaker_image_size))
                .setFadeInImage(UIUtils.hasHoneycombMR1());

        EasyTracker.getInstance().setContext(this);
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
                int defaultViewType = intent.getIntExtra(EXTRA_DEFAULT_VIEW_TYPE,
                        TracksDropdownFragment.VIEW_TYPE_SESSIONS);
                String selectedTrackId = ScheduleContract.Tracks.getTrackId(uri);
                loadTrackList(defaultViewType, selectedTrackId);
                getSupportActionBar().setSelectedNavigationItem(defaultViewType);
                onTrackSelected(selectedTrackId);
                mSlidingPaneLayout.openPane();
            }

        } else if (ScheduleContract.Sessions.CONTENT_TYPE.equals(mimeType)) {
            // Load a session list, hiding the tracks dropdown and the tabs
            mViewType = TracksDropdownFragment.VIEW_TYPE_SESSIONS;
            showFullUI(false);
            if (!updateSurfaceOnly) {
                loadSessionList(uri, null);
                mSlidingPaneLayout.openPane();
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

        } else if (ScheduleContract.Sandbox.CONTENT_TYPE.equals(mimeType)) {
            // Load a sandbox company list
            mViewType = TracksDropdownFragment.VIEW_TYPE_SANDBOX;
            showFullUI(false);
            if (!updateSurfaceOnly) {
                loadSandboxList(uri, null);
                mSlidingPaneLayout.openPane();
            }

        } else if (ScheduleContract.Sandbox.CONTENT_ITEM_TYPE.equals(mimeType)) {
            // Load company details
            mViewType = TracksDropdownFragment.VIEW_TYPE_SANDBOX;
            showFullUI(false);
            if (!updateSurfaceOnly) {
                Uri masterUri = intent.getParcelableExtra(EXTRA_MASTER_URI);
                if (masterUri == null) {
                    masterUri = ScheduleContract.Sandbox.CONTENT_URI;
                }
                loadSandboxList(masterUri, ScheduleContract.Sandbox.getCompanyId(uri));
                loadSandboxDetail(uri);
            }
        }

        updateDetailBackground();
    }

    private void showFullUI(boolean fullUI) {
        mFullUI = fullUI;
        final ActionBar actionBar = getSupportActionBar();
        final FragmentManager fm = getSupportFragmentManager();

        if (fullUI) {
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setListNavigationCallbacks(mActionBarSpinnerAdapter, this);
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

    private SpinnerAdapter mActionBarSpinnerAdapter = new BaseAdapter() {
        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public Object getItem(int position) {
            return position;
        }

        @Override
        public long getItemId(int position) {
            return position + 1;
        }

        private int getLabelResId(int position) {
            switch (position) {
                case TracksDropdownFragment.VIEW_TYPE_SESSIONS:
                    return R.string.title_sessions;
                case TracksDropdownFragment.VIEW_TYPE_OFFICE_HOURS:
                    return R.string.title_office_hours;
                case TracksDropdownFragment.VIEW_TYPE_SANDBOX:
                    return R.string.title_sandbox;
            }
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup container) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(
                        android.R.layout.simple_spinner_item,
                        container, false);
            }
            ((TextView) convertView.findViewById(android.R.id.text1)).setText(
                    getLabelResId(position));
            return convertView;
        }

        @Override
        public View getDropDownView(int position, View convertView,
                ViewGroup container) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(
                        android.R.layout.simple_spinner_dropdown_item,
                        container, false);
            }
            ((TextView) convertView.findViewById(android.R.id.text1)).setText(
                    getLabelResId(position));
            return convertView;
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.search, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_search);
        if (searchItem != null && UIUtils.hasHoneycomb()) {
            SearchView searchView = (SearchView) searchItem.getActionView();
            if (searchView != null) {
                SearchManager searchManager = (SearchManager) getSystemService(SEARCH_SERVICE);
                searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
                searchView.setQueryRefinementEnabled(true);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                if (mSlidingPaneLayout.isSlideable() && !mSlidingPaneLayout.isOpen()) {
                    // If showing the detail view, pressing Up should show the master pane.
                    mSlidingPaneLayout.openPane();
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
        outState.putInt(STATE_VIEW_TYPE, mViewType);
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        loadTrackList(itemPosition); // itemPosition == view type
        if (!mInitialTabSelect) {
            onTrackSelected(mTracksDropdownFragment.getSelectedTrackId());
            mSlidingPaneLayout.openPane();
        }
        return true;
    }

    private void loadTrackList(int viewType) {
        loadTrackList(viewType, null);
    }

    private void loadTrackList(int viewType, String selectTrackId) {
        if (mDetailFragment != null && mViewType != viewType) {
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
            if (TracksDropdownFragment.VIEW_TYPE_SESSIONS == mViewType
                    || TracksDropdownFragment.VIEW_TYPE_OFFICE_HOURS == mViewType) {
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
        mSlidingPaneLayout.closePane();
    }

    private void loadSandboxList(Uri sandboxUri, String selectCompanyId) {
        SandboxFragment fragment = new SandboxFragment();
        fragment.setSelectedCompanyId(selectCompanyId);
        fragment.setArguments(BaseActivity.intentToFragmentArguments(
                new Intent(Intent.ACTION_VIEW, sandboxUri)));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_master, fragment)
                .commit();
    }

    private void loadSandboxDetail(Uri companyUri) {
        SandboxDetailFragment fragment = new SandboxDetailFragment();
        fragment.setArguments(BaseActivity.intentToFragmentArguments(
                new Intent(Intent.ACTION_VIEW, companyUri)));
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container_detail, fragment)
                .commit();
        mDetailFragment = fragment;
        updateDetailBackground();

        // If loading session details in portrait, hide the master pane
        mSlidingPaneLayout.closePane();
    }

    @Override
    public void onTrackNameAvailable(String trackId, String trackName) {
        String trackType = null;

        switch (mViewType) {
            case TracksDropdownFragment.VIEW_TYPE_SESSIONS:
                trackType = getString(R.string.title_sessions);
                break;
            case TracksDropdownFragment.VIEW_TYPE_OFFICE_HOURS:
                trackType = getString(R.string.title_office_hours);
                break;
            case TracksDropdownFragment.VIEW_TYPE_SANDBOX:
                trackType = getString(R.string.title_sandbox);
                break;
        }

        EasyTracker.getTracker().sendView(trackType + ": " + getTitle());
        LOGD("Tracker", trackType + ": " + mTracksDropdownFragment.getTrackName());
    }

    @Override
    public void onTrackSelected(String trackId) {
        boolean allTracks = (ScheduleContract.Tracks.ALL_TRACK_ID.equals(trackId));

        switch (mViewType) {
            case TracksDropdownFragment.VIEW_TYPE_SESSIONS:
                loadSessionList((allTracks
                        ? ScheduleContract.Sessions.CONTENT_URI
                        : ScheduleContract.Tracks.buildSessionsUri(trackId))
                        .buildUpon()
                        .appendQueryParameter(ScheduleContract.Sessions.QUERY_PARAMETER_FILTER,
                                ScheduleContract.Sessions.QUERY_VALUE_FILTER_SESSIONS_CODELABS_ONLY)
                        .build(), null);
                break;

            case TracksDropdownFragment.VIEW_TYPE_OFFICE_HOURS:
                loadSessionList((allTracks
                        ? ScheduleContract.Sessions.CONTENT_URI
                        : ScheduleContract.Tracks.buildSessionsUri(trackId))
                        .buildUpon()
                        .appendQueryParameter(
                                ScheduleContract.Sessions.QUERY_PARAMETER_FILTER,
                                ScheduleContract.Sessions.QUERY_VALUE_FILTER_OFFICE_HOURS_ONLY)
                        .build(), null);
                break;

            case TracksDropdownFragment.VIEW_TYPE_SANDBOX:
                loadSandboxList(allTracks
                        ? ScheduleContract.Sandbox.CONTENT_URI
                        : ScheduleContract.Tracks.buildSandboxUri(trackId), null);
                break;
        }
    }

    @Override
    public boolean onSessionSelected(String sessionId) {
        loadSessionDetail(ScheduleContract.Sessions.buildSessionUri(sessionId));
        return true;
    }

    @Override
    public boolean onCompanySelected(String companyId) {
        loadSandboxDetail(ScheduleContract.Sandbox.buildCompanyUri(companyId));
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
    public void onTrackInfoAvailable(String trackId, TrackInfo track) {
        loadTrackList(mViewType, trackId);
        boolean allTracks = (ScheduleContract.Tracks.ALL_TRACK_ID.equals(trackId));

        switch (mViewType) {
            case TracksDropdownFragment.VIEW_TYPE_SESSIONS:
                loadSessionList((allTracks
                        ? ScheduleContract.Sessions.CONTENT_URI
                        : ScheduleContract.Tracks.buildSessionsUri(trackId))
                        .buildUpon()
                        .appendQueryParameter(ScheduleContract.Sessions.QUERY_PARAMETER_FILTER,
                                ScheduleContract.Sessions.QUERY_VALUE_FILTER_SESSIONS_CODELABS_ONLY)
                        .build(), mTrackInfoLoadCookie);
                break;

            case TracksDropdownFragment.VIEW_TYPE_OFFICE_HOURS:
                loadSessionList((allTracks
                        ? ScheduleContract.Sessions.CONTENT_URI
                        : ScheduleContract.Tracks.buildSessionsUri(trackId))
                        .buildUpon()
                        .appendQueryParameter(
                                ScheduleContract.Sessions.QUERY_PARAMETER_FILTER,
                                ScheduleContract.Sessions.QUERY_VALUE_FILTER_OFFICE_HOURS_ONLY)
                        .build(), mTrackInfoLoadCookie);
                break;

            case TracksDropdownFragment.VIEW_TYPE_SANDBOX:
                loadSandboxList(allTracks
                        ? ScheduleContract.Sandbox.CONTENT_URI
                        : ScheduleContract.Tracks.buildSandboxUri(trackId),
                        mTrackInfoLoadCookie);
                break;
        }
    }

    @Override
    public void onTrackIdAvailable(String trackId) {
    }

    @Override
    public ImageLoader getImageLoaderInstance() {
        return mImageLoader;
    }
}
