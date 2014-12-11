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

package com.google.samples.apps.iosched.ui;

import android.annotation.SuppressLint;
import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.view.*;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import android.widget.LinearLayout.LayoutParams;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.samples.apps.iosched.ui.widget.SlidingTabLayout;
import com.google.samples.apps.iosched.util.AnalyticsManager;
import com.google.samples.apps.iosched.util.SessionsHelper;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.ArrayList;
import java.util.HashMap;

import static com.google.samples.apps.iosched.util.LogUtils.*;

/**
 * An activity that displays the session live stream video which is pulled in from YouTube. The
 * UI adapts for both phone and tablet. As we want to prevent the YouTube player from restarting
 * and buffering again on orientation change, we handle configuration changes manually.
 */
public class SessionLivestreamActivity extends BaseActivity implements
        LoaderCallbacks<Cursor>,
        YouTubePlayer.OnInitializedListener,
        YouTubePlayer.PlayerStateChangeListener,
        YouTubePlayer.OnFullscreenListener,
        ActionBar.OnNavigationListener {

    private static final String TAG = makeLogTag(SessionLivestreamActivity.class);
    private static final String EXTRA_PREFIX = "com.google.android.iosched.extra.";
    private static final int YOUTUBE_RECOVERY_RESULT = 1;
    private static final String LOADER_SESSIONS_ARG = "futureSessions";

    public static final String EXTRA_YOUTUBE_URL = EXTRA_PREFIX + "youtube_url";
    public static final String EXTRA_TITLE = EXTRA_PREFIX + "title";
    public static final String EXTRA_ABSTRACT = EXTRA_PREFIX + "abstract";
    public static final String EXTRA_CAPTIONS = EXTRA_PREFIX + "captions";

    private static final String TAG_CAPTIONS = "captions";
    private static final int TABNUM_SESSION_SUMMARY = 0;
    private static final int TABNUM_LIVE_CAPTIONS = 1;

    private boolean mIsTablet;
    private boolean mIsFullscreen = false;
    private boolean mLoadFromExtras = false;
    private boolean mTrackPlay = true;

    private TabsAdapter mTabsAdapter;
    private LinearLayout mTabsContentLayout;
    private YouTubePlayer mYouTubePlayer;
    private YouTubePlayerFragment mYouTubeFragment;
    private LinearLayout mPlayerContainer;
    private String mYouTubeVideoId;
    private LinearLayout mMainLayout;
    private LinearLayout mVideoLayout;
    private FrameLayout mFullscreenCaptions;
    private MenuItem mCaptionsMenuItem;
    private MenuItem mShareMenuItem;
    private Runnable mShareMenuDeferredSetup;
    private Runnable mSessionSummaryDeferredSetup;
    private SessionShareData mSessionShareData;
    private boolean mSessionsFound;
    private int mYouTubeFullscreenFlags;

    private LivestreamAdapter mLivestreamAdapter;

    private Uri mSessionUri;
    private String mSessionId;
    private String mCaptionsUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_livestream);
        mIsTablet = UIUtils.isTablet(this);

        // Set up YouTube player
        mYouTubeFragment = (YouTubePlayerFragment)
                getFragmentManager().findFragmentById(R.id.livestream_player);
        mYouTubeFragment.initialize(Config.YOUTUBE_API_KEY, this);

        // Views that are common over all layouts
        mMainLayout = (LinearLayout) findViewById(R.id.livestream_mainlayout);
        adjustMainLayoutForActionBar();
        mPlayerContainer = (LinearLayout) findViewById(R.id.livestream_player_container);
        mFullscreenCaptions = (FrameLayout) findViewById(R.id.fullscreen_captions);
        final LayoutParams params = (LayoutParams) mFullscreenCaptions.getLayoutParams();
        params.setMargins(0, getActionBarHeightPx(), 0, getActionBarHeightPx());
        mFullscreenCaptions.setLayoutParams(params);
        mTabsContentLayout = (LinearLayout) findViewById(R.id.livestream_tabs_layout);

        // Set up ViewPager and adapter
        ViewPager viewPager = (ViewPager) findViewById(R.id.livestream_pager);
        viewPager.setOffscreenPageLimit(2);
        viewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.page_margin_width));
        mTabsAdapter = new TabsAdapter(getFragmentManager());
        viewPager.setAdapter(mTabsAdapter);
        viewPager.setOnPageChangeListener(mTabsAdapter);

        if (mIsTablet) {
            // Tablet UI specific views
            mVideoLayout = (LinearLayout) findViewById(R.id.livestream_video_layout);
        }

        mTabsAdapter.addTab(getString(R.string.session_livestream_info),
                new SessionSummaryFragment(), TABNUM_SESSION_SUMMARY);

        mTabsAdapter.addTab(getString(R.string.session_livestream_captions),
                new SessionCaptionsFragment(), TABNUM_LIVE_CAPTIONS);

        // Set up sliding tabs w/ViewPager
        SlidingTabLayout slidingTabLayout =
                (SlidingTabLayout) findViewById(R.id.livestream_sliding_tabs);
        slidingTabLayout.setCustomTabView(R.layout.tab_indicator, android.R.id.text1);

        Resources res = getResources();
        slidingTabLayout.setSelectedIndicatorColors(
                res.getColor(R.color.tab_selected_strip));
        slidingTabLayout.setDistributeEvenly(true);
        slidingTabLayout.setViewPager(viewPager);

        // Reload all other data in this activity
        reloadFromIntent(getIntent());

        // Update layout based on current configuration
        updateLayout(getResources().getConfiguration());

        // Set up action bar
        if (!mLoadFromExtras) {
            // Start sessions query to populate action bar navigation spinner
            getLoaderManager().initLoader(SessionsQuery._TOKEN, null, this);
            mLivestreamAdapter = new LivestreamAdapter(getActionBar().getThemedContext());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mSessionSummaryDeferredSetup != null) {
            mSessionSummaryDeferredSetup.run();
            mSessionSummaryDeferredSetup = null;
        }
    }

    /**
     * Reloads all data in the activity and fragments from a given intent
     * @param intent The intent to load from
     */
    private void reloadFromIntent(Intent intent) {
        final String youtubeUrl = intent.getStringExtra(EXTRA_YOUTUBE_URL);
        // Check if youtube url is set as an extra first
        if (youtubeUrl != null) {
            mLoadFromExtras = true;
            String actionBarTitle = getString(R.string.session_livestream_title);
            getActionBar().setTitle(actionBarTitle);
            updateSessionViews(youtubeUrl,
                    intent.getStringExtra(EXTRA_TITLE),
                    intent.getStringExtra(EXTRA_ABSTRACT),
                    Config.CONFERENCE_HASHTAG,
                    intent.getStringExtra(EXTRA_CAPTIONS));
        } else {
            // Otherwise load from session uri
            reloadFromUri(intent.getData());
        }
    }

    /**
     * Reloads all data in the activity and fragments from a given uri
     * @param newUri The session uri to load from
     */
    private void reloadFromUri(Uri newUri) {
        mSessionUri = newUri;
        if (mSessionUri != null && mSessionUri.getPathSegments().size() >= 2) {
            mSessionId = Sessions.getSessionId(mSessionUri);
            getLoaderManager().restartLoader(SessionSummaryQuery._TOKEN, null, this);
        } else {
            // No session uri, get out
            mSessionUri = null;
            finish();
        }
    }

    /**
     * Helper method to start this activity using only extras (rather than session uri).
     * @param context The package context
     * @param youtubeUrl The youtube url or video id to load
     * @param title The title to show in the session info fragment, can be null
     * @param sessionAbstract The session abstract to show in the session info fragment, can be null
     */
    public static void startFromExtras(Context context, String youtubeUrl,
            String title, String sessionAbstract, String captionsUrl) {
        if (youtubeUrl == null) {
            return;
        }
        final Intent i = new Intent();
        i.setClass(context, SessionLivestreamActivity.class);
        i.putExtra(EXTRA_YOUTUBE_URL, youtubeUrl);
        i.putExtra(EXTRA_TITLE, title);
        i.putExtra(EXTRA_ABSTRACT, sessionAbstract);
        i.putExtra(EXTRA_CAPTIONS, captionsUrl);
        context.startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.session_livestream, menu);
        mCaptionsMenuItem = menu.findItem(R.id.menu_captions);
        mShareMenuItem = menu.findItem(R.id.menu_share);
        if (mShareMenuDeferredSetup != null) {
            mShareMenuDeferredSetup.run();
        }
        if (!mIsTablet && Configuration.ORIENTATION_LANDSCAPE ==
                getResources().getConfiguration().orientation) {
            mCaptionsMenuItem.setVisible(true);
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_captions:
                if (mIsFullscreen) {
                    if (mFullscreenCaptions.getVisibility() == View.GONE) {
                        mFullscreenCaptions.setVisibility(View.VISIBLE);
                        SessionCaptionsFragment captionsFragment;
                        captionsFragment = (SessionCaptionsFragment)
                                getFragmentManager().findFragmentByTag(TAG_CAPTIONS);
                        if (captionsFragment == null) {
                            captionsFragment = new SessionCaptionsFragment();
                            captionsFragment.setDarkTheme(true);
                            FragmentTransaction ft = getFragmentManager().beginTransaction();
                            ft.add(R.id.fullscreen_captions, captionsFragment, TAG_CAPTIONS);
                            ft.commit();
                        }
                        captionsFragment.updateViews(mCaptionsUrl);
                        return true;
                    }
                }
                mFullscreenCaptions.setVisibility(View.GONE);
                break;
            case R.id.menu_share:
                if (mSessionShareData != null) {
                    new SessionsHelper(this).shareSession(this,
                            R.string.share_livestream_template,
                            mSessionShareData.title,
                            mSessionShareData.hashtag,
                            mSessionShareData.sessionUrl);
                    return true;
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // Need to handle configuration changes so as not to interrupt YT player and require
        // buffering again
        updateLayout(newConfig);
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        final Cursor cursor = (Cursor) mLivestreamAdapter.getItem(itemPosition);
        final String sessionId = cursor.getString(SessionsQuery.SESSION_ID);
        if (sessionId != null) {
            reloadFromUri(Sessions.buildSessionUri(sessionId));
            return true;
        }
        return false;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case SessionSummaryQuery._TOKEN:
                return new CursorLoader(this, Sessions.buildSessionUri(mSessionId),
                        SessionSummaryQuery.PROJECTION, null, null, null);
            case SessionsQuery._TOKEN:
                boolean futureSessions = false;
                if (args != null) {
                    futureSessions = args.getBoolean(LOADER_SESSIONS_ARG, false);
                }
                final long currentTime = UIUtils.getCurrentTime(this);
                String selection = Sessions.LIVESTREAM_SELECTION + " and ";
                String[] selectionArgs;
                if (!futureSessions) {
                    selection += Sessions.AT_TIME_SELECTION;
                    selectionArgs = Sessions.buildAtTimeSelectionArgs(currentTime);
                } else {
                    selection += Sessions.UPCOMING_LIVE_SELECTION;
                    selectionArgs = Sessions.buildUpcomingSelectionArgs(currentTime);
                }
                return new CursorLoader(this, Sessions.CONTENT_URI, SessionsQuery.PROJECTION,
                        selection, selectionArgs, SessionsQuery.SORT_ORDER);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case SessionSummaryQuery._TOKEN:
                loadSessionSummary(data);
                break;
            case SessionsQuery._TOKEN:
                loadSessionsList(data);
                break;
        }
    }

    /**
     * Locates which item should be selected in the action bar drop-down spinner based on the
     * current active session uri
     * @param data The data
     * @return The row num of the item that should be selected or 0 if not found
     */
    private int locateSelectedItem(Cursor data) {
        int selected = 0;
        if (data != null && mSessionId != null) {
            while (data.moveToNext()) {
                if (mSessionId.equals(data.getString(SessionsQuery.SESSION_ID))) {
                    selected = data.getPosition();
                    mCaptionsUrl = null;
                }
            }
        }
        return selected;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case SessionsQuery._TOKEN:
                mLivestreamAdapter.swapCursor(null);
                break;
        }
    }

    @Override
    public void onFullscreen(boolean fullscreen) {
        layoutFullscreenVideo(fullscreen);
    }

    @Override
    public void onLoading() {
    }

    @Override
    public void onLoaded(String s) {
    }

    @Override
    public void onAdStarted() {
    }

    @Override
    public void onVideoStarted() {
    }

    @Override
    public void onVideoEnded() {
    }

    @Override
    public void onError(YouTubePlayer.ErrorReason errorReason) {
        Toast.makeText(this, R.string.session_livestream_error_playback, Toast.LENGTH_LONG).show();
        LOGE(TAG, errorReason.toString());
    }

    /**
     * Load the session summary info for the currently selected live session.
     */
    private void loadSessionSummary(Cursor data) {
        if (data != null && data.moveToFirst()) {
            mCaptionsUrl = data.getString(SessionSummaryQuery.CAPTIONS_URL);
            final long currentTime = UIUtils.getCurrentTime(this);
            if (currentTime > data.getLong(SessionSummaryQuery.SESSION_END)) {
                getLoaderManager().restartLoader(SessionsQuery._TOKEN, null, this);
                return;
            }
            updateSessionViews(
                    data.getString(SessionSummaryQuery.LIVESTREAM_URL),
                    data.getString(SessionSummaryQuery.TITLE),
                    data.getString(SessionSummaryQuery.ABSTRACT),
                    data.getString(SessionSummaryQuery.HASHTAGS),
                    mCaptionsUrl);
        }
    }

    /**
     * Load the list of currently live sessions or upcoming live sessions. This
     * populates the Action Bar (either the title or as list navigation.
     */
    private void loadSessionsList(Cursor data) {
        mLivestreamAdapter.swapCursor(data);
        if (data != null && data.getCount() > 0) {
            mSessionsFound = true;
            final ActionBar actionBar = getActionBar();
            if (data.getCount() == 1) {
                // Just one session on, display title in Action Bar
                if (data.moveToFirst()) {
                    actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                    actionBar.setDisplayShowTitleEnabled(true);
                    actionBar.setTitle(data.getString(SessionsQuery.TITLE));
                }
            } else if (data.getCount() > 1) {
                // 2+ sessions found, set Action Bar to list navigation (spinner)
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
                actionBar.setListNavigationCallbacks(mLivestreamAdapter, this);
                actionBar.setDisplayShowTitleEnabled(false);
                getActionBar().setSelectedNavigationItem(locateSelectedItem(data));
            }
        } else if (mSessionsFound) {
            // Sessions were previously found but no sessions are currently live,
            // adjust query to see if there are any future sessions at all
            mSessionsFound = false;
            final Bundle bundle = new Bundle();
            bundle.putBoolean(LOADER_SESSIONS_ARG, true);
            getLoaderManager().restartLoader(SessionsQuery._TOKEN, bundle, this);
        } else {
            // No sessions live right now and no sessions coming up, get out
            finish();
        }
    }

    /**
     * Updates views that rely on session data from explicit strings.
     */
    private void updateSessionViews(final String youtubeUrl, final String title,
            final String sessionAbstract, final String hashTag, final String captionsUrl) {

        if (youtubeUrl == null) {
            // Get out, nothing to do here
            finish();
            return;
        }

        mCaptionsUrl = captionsUrl;
        String youtubeVideoId = getVideoIdFromUrl(youtubeUrl);

        // Play the video
        playVideo(youtubeVideoId);

        if (mTrackPlay) {
            /* [ANALYTICS:SCREEN]
             * TRIGGER:   View the Live Stream screen for a sessino.
             * LABEL:     'Live Streaming' + session title/subtitle
             * [/ANALYTICS]
             */
            AnalyticsManager.sendScreenView("Live Streaming: " + title);
            LOGD("Tracker", "Live Streaming: " + title);
        }

        final String newYoutubeUrl = Config.YOUTUBE_SHARE_URL_PREFIX + youtubeVideoId;
        mSessionShareData = new SessionShareData(title, hashTag, newYoutubeUrl);
        mShareMenuDeferredSetup = new Runnable() {
            @Override
            public void run() {
                new SessionsHelper(SessionLivestreamActivity.this)
                        .tryConfigureShareMenuItem(mShareMenuItem,
                                R.string.share_livestream_template,
                                title, hashTag, newYoutubeUrl);
            }
        };

        if (mShareMenuItem != null) {
            mShareMenuDeferredSetup.run();
            mShareMenuDeferredSetup = null;
        }

        mSessionSummaryDeferredSetup = new Runnable() {
            @Override
            public void run() {
                updateSessionSummaryFragment(title, sessionAbstract);
                updateSessionLiveCaptionsFragment(captionsUrl);
            }
        };

        if (!mLoadFromExtras) {
            mSessionSummaryDeferredSetup.run();
            mSessionSummaryDeferredSetup = null;
        }
    }

    private void updateSessionSummaryFragment(String title, String sessionAbstract) {
        SessionSummaryFragment sessionSummaryFragment =
                (SessionSummaryFragment) mTabsAdapter.getTabFragment(TABNUM_SESSION_SUMMARY);
        if (sessionSummaryFragment != null) {
            sessionSummaryFragment.setSessionSummaryInfo(title, sessionAbstract);
        }
    }

    private void updateSessionLiveCaptionsFragment(String captionsUrl) {
        SessionCaptionsFragment captionsFragment = (SessionCaptionsFragment)
                    mTabsAdapter.getTabFragment(TABNUM_LIVE_CAPTIONS);
        if (captionsFragment != null) {
            captionsFragment.updateViews(captionsUrl);
        }
    }

    private void playVideo(String videoId) {
        if ((TextUtils.isEmpty(mYouTubeVideoId) || !mYouTubeVideoId.equals(videoId))
                && !TextUtils.isEmpty(videoId)) {
            mYouTubeVideoId = videoId;
            if (mYouTubePlayer != null) {
                mYouTubePlayer.loadVideo(mYouTubeVideoId);
            }
            mTrackPlay = true;

            if (mSessionShareData != null) {
                mSessionShareData.sessionUrl = Config.YOUTUBE_SHARE_URL_PREFIX + mYouTubeVideoId;
            }
        }
    }

    @Override
    public Intent getParentActivityIntent() {
        if (mLoadFromExtras || mSessionUri == null) {
            return new Intent(this, MyScheduleActivity.class);
        } else {
            return new Intent(Intent.ACTION_VIEW, mSessionUri);
        }
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider,
            YouTubePlayer youTubePlayer, boolean wasRestored) {

        // Set up YouTube player
        mYouTubePlayer = youTubePlayer;
        mYouTubePlayer.setPlayerStateChangeListener(this);
        mYouTubePlayer.setFullscreen(mIsFullscreen);
        mYouTubePlayer.setOnFullscreenListener(this);

        // YouTube player flags: use a custom full screen layout; let the YouTube player control
        // the system UI (hiding navigation controls, ActionBar etc); and let the YouTube player
        // handle the orientation state of the activity.
        mYouTubeFullscreenFlags = YouTubePlayer.FULLSCREEN_FLAG_CUSTOM_LAYOUT |
                YouTubePlayer.FULLSCREEN_FLAG_CONTROL_SYSTEM_UI |
                YouTubePlayer.FULLSCREEN_FLAG_CONTROL_ORIENTATION;

        // On smaller screen devices always switch to full screen in landscape mode
        if (!mIsTablet) {
            mYouTubeFullscreenFlags |= YouTubePlayer.FULLSCREEN_FLAG_ALWAYS_FULLSCREEN_IN_LANDSCAPE;
        }

        // Apply full screen flags
        mYouTubePlayer.setFullscreenControlFlags(mYouTubeFullscreenFlags);

        // Load the requested video
        if (!TextUtils.isEmpty(mYouTubeVideoId)) {
            mYouTubePlayer.loadVideo(mYouTubeVideoId);
        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider,
            YouTubeInitializationResult result) {
        LOGE(TAG, result.toString());
        if (result.isUserRecoverableError()) {
            result.getErrorDialog(this, YOUTUBE_RECOVERY_RESULT).show();
        } else {
            String errorMessage =
                    getString(R.string.session_livestream_error_init, result.toString());
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == YOUTUBE_RECOVERY_RESULT) {
            if (mYouTubeFragment != null) {
                mYouTubeFragment.initialize(Config.YOUTUBE_API_KEY, this);
            }
        }
    }

    /**
     * Updates the layout based on the provided configuration
     */
    private void updateLayout(Configuration config) {
        if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (mIsTablet) {
                layoutTabletForLandscape();
            } else {
                layoutPhoneForLandscape();
            }
        } else {
            if (mIsTablet) {
                layoutTabletForPortrait();
            } else {
                layoutPhoneForPortrait();
            }
        }
    }

    private void layoutPhoneForLandscape() {
        layoutFullscreenVideo(true);
    }

    private void layoutPhoneForPortrait() {
        layoutFullscreenVideo(false);
        SessionSummaryFragment sessionSummaryFragment =
                (SessionSummaryFragment) mTabsAdapter.getTabFragment(TABNUM_SESSION_SUMMARY);
        if (sessionSummaryFragment != null) {
            sessionSummaryFragment.updateViews();
        }
    }

    private void layoutTabletForLandscape() {
        mMainLayout.setOrientation(LinearLayout.HORIZONTAL);

        final LayoutParams videoLayoutParams = (LayoutParams) mVideoLayout.getLayoutParams();
        videoLayoutParams.height = LayoutParams.MATCH_PARENT;
        videoLayoutParams.width = 0;
        videoLayoutParams.weight = 2;
        mVideoLayout.setLayoutParams(videoLayoutParams);

        final LayoutParams extraLayoutParams = (LayoutParams) mTabsContentLayout.getLayoutParams();
        extraLayoutParams.height = LayoutParams.MATCH_PARENT;
        extraLayoutParams.width = 0;
        extraLayoutParams.weight = 1;
        mTabsContentLayout.setLayoutParams(extraLayoutParams);
    }

    private void layoutTabletForPortrait() {
        mMainLayout.setOrientation(LinearLayout.VERTICAL);

        final LayoutParams videoLayoutParams = (LayoutParams) mVideoLayout.getLayoutParams();
        videoLayoutParams.width = LayoutParams.MATCH_PARENT;
        videoLayoutParams.height = LayoutParams.WRAP_CONTENT;
        videoLayoutParams.weight = 0;
        mVideoLayout.setLayoutParams(videoLayoutParams);

        final LayoutParams extraLayoutParams = (LayoutParams) mTabsContentLayout.getLayoutParams();
        extraLayoutParams.width = LayoutParams.MATCH_PARENT;
        extraLayoutParams.height = 0;
        extraLayoutParams.weight = 5;
        mTabsContentLayout.setLayoutParams(extraLayoutParams);
    }

    private void layoutFullscreenVideo(boolean fullscreen) {
        if (mIsFullscreen != fullscreen) {
            mIsFullscreen = fullscreen;
            if (mIsTablet) {
                // Tablet specific layout
                layoutTabletFullscreen(fullscreen);
            } else {
                // Phone specific layout
                layoutPhoneFullscreen(fullscreen);
            }

            // Full screen layout changes for all form factors
            if (fullscreen) {
                if (mCaptionsMenuItem != null) {
                    mCaptionsMenuItem.setVisible(true);
                }
                mMainLayout.setPadding(0, 0, 0, 0);
            } else {
                getActionBar().show();
                if (mCaptionsMenuItem != null) {
                    mCaptionsMenuItem.setVisible(false);
                }
                mFullscreenCaptions.setVisibility(View.GONE);
                adjustMainLayoutForActionBar();
            }
        }
    }

    private void adjustMainLayoutForActionBar() {
        // On ICS+ we use FEATURE_ACTION_BAR_OVERLAY so full screen mode doesn't need to
        // re-adjust layouts when hiding action bar. To account for this we add action bar
        // height pack to the padding when not in full screen mode.
        mMainLayout.setPadding(0, getActionBarHeightPx(), 0, 0);
    }

    /**
     * Adjusts tablet layouts for full screen video.
     *
     * @param fullscreen True to layout in full screen, false to switch to regular layout
     */
    private void layoutTabletFullscreen(boolean fullscreen) {
        if (fullscreen) {
            mTabsContentLayout.setVisibility(View.GONE);
        } else {
            mTabsContentLayout.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Adjusts phone layouts for full screen video.
     */
    private void layoutPhoneFullscreen(boolean fullscreen) {
        View youTubePlayerView = mYouTubeFragment.getView();
        if (youTubePlayerView != null) {
            ViewGroup.LayoutParams playerParams = mYouTubeFragment.getView().getLayoutParams();
            playerParams.height = fullscreen ? LayoutParams.MATCH_PARENT
                    : LayoutParams.WRAP_CONTENT;
            youTubePlayerView.setLayoutParams(playerParams);
        }

        final LayoutParams containerParams = (LayoutParams) mPlayerContainer.getLayoutParams();
        containerParams.height = fullscreen ? LayoutParams.MATCH_PARENT : LayoutParams.WRAP_CONTENT;
        mPlayerContainer.setLayoutParams(containerParams);

        if (fullscreen) {
            mTabsContentLayout.setVisibility(View.GONE);
            if (mYouTubePlayer != null) {
                mYouTubePlayer.setFullscreen(true);
            }
        } else {
            mTabsContentLayout.setVisibility(View.VISIBLE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private int getActionBarHeightPx() {
        int[] attrs = new int[] { R.attr.actionBarSize };
        return (int) getTheme().obtainStyledAttributes(attrs).getDimension(0, 0f);
    }

    public static String getVideoIdFromUrl(String youtubeUrl) {
        if (!TextUtils.isEmpty(youtubeUrl) && youtubeUrl.startsWith("http")) {
            Uri youTubeUri = Uri.parse(youtubeUrl);
            return youTubeUri.getQueryParameter("v");
        }
        return youtubeUrl;
    }

    /**
     * Adapter that backs the action bar drop-down spinner.
     */
    private class LivestreamAdapter extends CursorAdapter {
        LayoutInflater mLayoutInflater;

        public LivestreamAdapter(Context context) {
            super(context, null, 0);
            mLayoutInflater = (LayoutInflater) getActionBar().getThemedContext()
                    .getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public Object getItem(int position) {
            Cursor c = getCursor();
            c.moveToPosition(position);
            return c;
        }

        @Override
        public View newDropDownView(Context context, Cursor cursor, ViewGroup parent) {
            // Inflate view that appears in the drop-down spinner views
            return mLayoutInflater.inflate(
                    R.layout.livestream_spinner_item_dropdown, parent, false);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            // Inflate view that appears in the selected spinner
            return mLayoutInflater.inflate(R.layout.livestream_spinner_item_actionbar,
                    parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Bind view that appears in the selected spinner or in the drop-down
            final TextView titleView = (TextView) view.findViewById(android.R.id.text1);
            titleView.setText(cursor.getString(SessionsQuery.TITLE));
        }
    }

    /**
     * Adapter that backs the ViewPager tabs on the phone UI.
     */
    private static class TabsAdapter extends FragmentPagerAdapter
            implements ViewPager.OnPageChangeListener {

        private final HashMap<Integer, Fragment> mFragments;
        private final ArrayList<Integer> mTabNums;
        private final ArrayList<CharSequence> mTabTitles;

        @SuppressLint("UseSparseArrays")
        public TabsAdapter(FragmentManager fm) {
            super(fm);
            mFragments = new HashMap<Integer, Fragment>(3);
            mTabNums = new ArrayList<Integer>(3);
            mTabTitles = new ArrayList<CharSequence>(2);
        }

        public void addTab(String tabTitle, Fragment newFragment, int tabId) {
            mTabTitles.add(tabTitle);
            mFragments.put(tabId, newFragment);
            mTabNums.add(tabId);
            notifyDataSetChanged();
        }

        public Fragment getTabFragment(int tabNum) {
            if (mFragments.containsKey(tabNum)) {
                return mFragments.get(tabNum);
            }
            return null;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTabTitles.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(mTabNums.get(position));
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    }

    /**
     * Simple fragment that inflates a session summary layout that displays session title and
     * abstract.
     */
    public static class SessionSummaryFragment extends Fragment {
        private TextView mTitleView;
        private TextView mAbstractView;
        private String mTitle;
        private String mAbstract;

        public SessionSummaryFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_session_summary, null);
            mTitleView = (TextView) view.findViewById(R.id.session_title);
            mAbstractView = (TextView) view.findViewById(R.id.session_abstract);
            updateViews();
            return view;
        }

        public void setSessionSummaryInfo(String sessionTitle, String sessionAbstract) {
            mTitle = sessionTitle;
            mAbstract = sessionAbstract;
            updateViews();
        }

        public void updateViews() {
            if (mTitleView != null && mAbstractView != null) {
                mTitleView.setText(mTitle);
                if (!TextUtils.isEmpty(mAbstract)) {
                    mAbstractView.setVisibility(View.VISIBLE);
                    mAbstractView.setText(mAbstract);
                } else {
                    mAbstractView.setVisibility(View.GONE);
                }
            }
        }
    }

    /**
     * Simple fragment that shows the live captions.
     */
    public static class SessionCaptionsFragment extends Fragment {
        private FrameLayout mContainer;
        private WebView mWebView;
        private TextView mNoCaptionsTextView;
        private boolean mDarkTheme = false;
        private String mLocalCaptionsUrl;

        public SessionCaptionsFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.fragment_session_captions, null);
            mContainer = (FrameLayout) view.findViewById(R.id.session_caption_container);
            if (!UIUtils.isTablet(getActivity())) {
                mContainer.setBackgroundColor(Color.WHITE);
            }
            mNoCaptionsTextView = (TextView) view.findViewById(android.R.id.empty);
            mWebView = (WebView) view.findViewById(R.id.session_caption_area);
            mWebView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    // Disable text selection in WebView (doesn't work well with the YT player
                    // triggering full screen chrome after a timeout)
                    return true;
                }
            });
            mWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onReceivedError(WebView view, int errorCode, String description,
                        String failingUrl) {
                    showNoCaptionsAvailable();
                }
            });
            updateViews(mLocalCaptionsUrl);
            return view;
        }

        public void setDarkTheme(boolean dark) {
            mDarkTheme = dark;
        }

        @SuppressLint("SetJavaScriptEnabled")
        public void updateViews(String captionsUrl) {
            mLocalCaptionsUrl = captionsUrl;
            if (mWebView != null && !TextUtils.isEmpty(captionsUrl)) {
                if (mDarkTheme) {
                    mWebView.setBackgroundColor(Color.BLACK);
                    mContainer.setBackgroundColor(Color.BLACK);
                    mNoCaptionsTextView.setTextColor(Color.WHITE);
                } else {
                    mWebView.setBackgroundColor(Color.WHITE);
                }

                String finalCaptionsUrl = captionsUrl;
                if (finalCaptionsUrl != null) {
                    if (mDarkTheme) {
                        finalCaptionsUrl += Config.LIVESTREAM_CAPTIONS_DARK_THEME_URL_PARAM;
                    }
                    mWebView.getSettings().setJavaScriptEnabled(true);
                    mWebView.loadUrl(finalCaptionsUrl);
                    mNoCaptionsTextView.setVisibility(View.GONE);
                    mWebView.setVisibility(View.VISIBLE);
                } else {
                    showNoCaptionsAvailable();
                }
            }
        }

        private void showNoCaptionsAvailable() {
            mWebView.setVisibility(View.GONE);
            mNoCaptionsTextView.setVisibility(View.VISIBLE);
        }
    }

    private static class SessionShareData {
        String title;
        String hashtag;
        String sessionUrl;

        public SessionShareData(String title, String hashTag, String url) {
            this.title = title;
            hashtag = hashTag;
            sessionUrl = url;
        }
    }

    /**
     * Single session query
     */
    public interface SessionSummaryQuery {
        final static int _TOKEN = 0;

        final static String[] PROJECTION = {
                Sessions.SESSION_ID,
                Sessions.SESSION_TITLE,
                Sessions.SESSION_ABSTRACT,
                Sessions.SESSION_HASHTAG,
                Sessions.SESSION_LIVESTREAM_URL,
                Sessions.SESSION_CAPTIONS_URL,
                Sessions.SESSION_END,
        };

        final static int ID = 0;
        final static int TITLE = 1;
        final static int ABSTRACT = 2;
        final static int HASHTAGS = 3;
        final static int LIVESTREAM_URL = 4;
        final static int CAPTIONS_URL = 5;
        final static int SESSION_END = 6;
    }

    /**
     * List of sessions query
     */
    public interface SessionsQuery {
        final static int _TOKEN = 1;

        final static String[] PROJECTION = {
                BaseColumns._ID,
                Sessions.SESSION_ID,
                Sessions.SESSION_TITLE,
        };

        final static int _ID = 0;
        final static int SESSION_ID = 1;
        final static int TITLE = 2;

        final static String SORT_ORDER = Sessions.SESSION_TITLE;
    }
}
