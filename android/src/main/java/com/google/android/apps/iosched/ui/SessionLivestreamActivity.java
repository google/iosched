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

package com.google.android.apps.iosched.ui;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Presentation;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.media.MediaRouter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.view.*;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.iosched.Config;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.android.apps.iosched.provider.ScheduleContract.Tracks;
import com.google.android.apps.iosched.util.SessionsHelper;
import com.google.android.apps.iosched.util.UIUtils;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerSupportFragment;

import java.util.ArrayList;
import java.util.HashMap;

import static com.google.android.apps.iosched.util.LogUtils.LOGD;
import static com.google.android.apps.iosched.util.LogUtils.LOGE;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * An activity that displays the session live stream video which is pulled in from YouTube. The
 * UI adapts for both phone and tablet. As we want to prevent the YouTube player from restarting
 * and buffering again on orientation change, we handle configuration changes manually.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
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
    public static final String EXTRA_TRACK = EXTRA_PREFIX + "track";
    public static final String EXTRA_TITLE = EXTRA_PREFIX + "title";
    public static final String EXTRA_ABSTRACT = EXTRA_PREFIX + "abstract";
    public static final String KEYNOTE_TRACK_NAME = "Keynote";

    private static final String TAG_SESSION_SUMMARY = "session_summary";
    private static final String TAG_CAPTIONS = "captions";
    private static final int TABNUM_SESSION_SUMMARY = 0;
    private static final int TABNUM_SOCIAL_STREAM = 1;
    private static final int TABNUM_LIVE_CAPTIONS = 2;
    private static final String EXTRA_TAB_STATE = "tag";

    private static final int STREAM_REFRESH_TIME = 5 * 60 * 1000; // 5 minutes

    private boolean mIsTablet;
    private boolean mIsFullscreen = false;
    private boolean mLoadFromExtras = false;
    private boolean mTrackPlay = true;

    private TabHost mTabHost;
    private TabsAdapter mTabsAdapter;
    private YouTubePlayer mYouTubePlayer;
    private YouTubePlayerSupportFragment mYouTubeFragment;
    private LinearLayout mPlayerContainer;
    private String mYouTubeVideoId;
    private LinearLayout mMainLayout;
    private LinearLayout mVideoLayout;
    private LinearLayout mExtraLayout;
    private FrameLayout mPresentationControls;
    private FrameLayout mSummaryLayout;
    private FrameLayout mFullscreenCaptions;
    private MenuItem mCaptionsMenuItem;
    private MenuItem mShareMenuItem;
    private MenuItem mPresentationMenuItem;
    private Runnable mShareMenuDeferredSetup;
    private Runnable mSessionSummaryDeferredSetup;
    private SessionShareData mSessionShareData;
    private boolean mSessionsFound;
    private boolean isKeynote = false;
    private Handler mHandler = new Handler();
    private int mYouTubeFullscreenFlags;

    private LivestreamAdapter mLivestreamAdapter;

    private Uri mSessionUri;
    private String mSessionId;
    private String mTrackName;

    private MediaRouter mMediaRouter;
    private YouTubePresentation mPresentation;
    private MediaRouter.SimpleCallback mMediaRouterCallback;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (UIUtils.hasICS()) {
            // We can't use this mode on HC as compatible ActionBar doesn't work well with the YT
            // player in full screen mode (no overlays allowed).
            requestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_session_livestream);
        mIsTablet = UIUtils.isHoneycombTablet(this);

        // Set up YouTube player
        mYouTubeFragment = (YouTubePlayerSupportFragment)
                getSupportFragmentManager().findFragmentById(R.id.livestream_player);
        mYouTubeFragment.initialize(Config.YOUTUBE_API_KEY, this);

        // Views that are common over all layouts
        mMainLayout = (LinearLayout) findViewById(R.id.livestream_mainlayout);
        mPresentationControls = (FrameLayout) findViewById(R.id.presentation_controls);
        adjustMainLayoutForActionBar();
        mPlayerContainer = (LinearLayout) findViewById(R.id.livestream_player_container);
        mFullscreenCaptions = (FrameLayout) findViewById(R.id.fullscreen_captions);
        final LayoutParams params = (LayoutParams) mFullscreenCaptions.getLayoutParams();
        params.setMargins(0, getActionBarHeightPx(), 0, getActionBarHeightPx());
        mFullscreenCaptions.setLayoutParams(params);

        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        viewPager.setOffscreenPageLimit(2);
        if (!mIsTablet) {
            viewPager.setPageMarginDrawable(R.drawable.grey_border_inset_lr);
        }
        viewPager.setPageMargin(getResources().getDimensionPixelSize(R.dimen.page_margin_width));

        // Set up tabs w/ViewPager
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();
        mTabsAdapter = new TabsAdapter(this, mTabHost, viewPager);

        if (mIsTablet) {
            // Tablet UI specific views
            getSupportFragmentManager().beginTransaction().add(R.id.livestream_summary,
                    new SessionSummaryFragment(), TAG_SESSION_SUMMARY).commit();
            mVideoLayout = (LinearLayout) findViewById(R.id.livestream_videolayout);
            mExtraLayout = (LinearLayout) findViewById(R.id.livestream_extralayout);
            mSummaryLayout = (FrameLayout) findViewById(R.id.livestream_summary);
        } else {
            // Handset UI specific views
            mTabsAdapter.addTab(
                    getString(R.string.session_livestream_info),
                    new SessionSummaryFragment(),
                    TABNUM_SESSION_SUMMARY);
        }

        mTabsAdapter.addTab(getString(R.string.title_stream), new SocialStreamFragment(),
                TABNUM_SOCIAL_STREAM);
        mTabsAdapter.addTab(getString(R.string.session_livestream_captions),
                new SessionLiveCaptionsFragment(), TABNUM_LIVE_CAPTIONS);

        if (savedInstanceState != null) {
            mTabHost.setCurrentTabByTag(savedInstanceState.getString(EXTRA_TAB_STATE));
        }

        // Reload all other data in this activity
        reloadFromIntent(getIntent());

        // Update layout based on current configuration
        updateLayout(getResources().getConfiguration());

        // Set up action bar
        if (!mLoadFromExtras) {
            // Start sessions query to populate action bar navigation spinner
            getSupportLoaderManager().initLoader(SessionsQuery._TOKEN, null, this);

            // Set up action bar
            mLivestreamAdapter = new LivestreamAdapter(getSupportActionBar().getThemedContext());
            final ActionBar actionBar = getSupportActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            actionBar.setListNavigationCallbacks(mLivestreamAdapter, this);
            actionBar.setDisplayShowTitleEnabled(false);
        }

        // Media Router and Presentation set up
        if (UIUtils.hasJellyBeanMR1()) {
            mMediaRouterCallback = new MediaRouter.SimpleCallback() {
                @Override
                public void onRouteSelected(MediaRouter router, int type,
                        MediaRouter.RouteInfo info) {
                    LOGD(TAG, "onRouteSelected: type=" + type + ", info=" + info);
                    updatePresentation();
                }

                @Override
                public void onRouteUnselected(MediaRouter router, int type,
                        MediaRouter.RouteInfo info) {
                    LOGD(TAG, "onRouteUnselected: type=" + type + ", info=" + info);
                    updatePresentation();
                }

                @Override
                public void onRoutePresentationDisplayChanged(MediaRouter router,
                        MediaRouter.RouteInfo info) {
                    LOGD(TAG, "onRoutePresentationDisplayChanged: info=" + info);
                    updatePresentation();
                }
            };

            mMediaRouter = (MediaRouter) getSystemService(Context.MEDIA_ROUTER_SERVICE);
            final ImageButton playPauseButton = (ImageButton) findViewById(R.id.play_pause_button);
            playPauseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mYouTubePlayer != null) {
                        if (mYouTubePlayer.isPlaying()) {
                            mYouTubePlayer.pause();
                            playPauseButton.setImageResource(R.drawable.ic_livestream_play);
                        } else {
                            mYouTubePlayer.play();
                            playPauseButton.setImageResource(R.drawable.ic_livestream_pause);
                        }
                    }
                }
            });
            updatePresentation();
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onResume() {
        super.onResume();
        if (mSessionSummaryDeferredSetup != null) {
            mSessionSummaryDeferredSetup.run();
            mSessionSummaryDeferredSetup = null;
        }
        mHandler.postDelayed(mStreamRefreshRunnable, STREAM_REFRESH_TIME);
        if (UIUtils.hasJellyBeanMR1()) {
            mMediaRouter.addCallback(MediaRouter.ROUTE_TYPE_LIVE_VIDEO, mMediaRouterCallback);
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mStreamRefreshRunnable);
        if (UIUtils.hasJellyBeanMR1()) {
            mMediaRouter.removeCallback(mMediaRouterCallback);
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        // Dismiss the presentation when the activity is not visible
        if (mPresentation != null) {
            LOGD(TAG, "Dismissing presentation because the activity is no longer visible.");
            mPresentation.dismiss();
            mPresentation = null;
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
            String trackName = intent.getStringExtra(EXTRA_TRACK);
            String actionBarTitle;
            if (trackName == null) {
                actionBarTitle = getString(R.string.session_livestream_title);
            } else {
                isKeynote = KEYNOTE_TRACK_NAME.equals(trackName);
                actionBarTitle = trackName + " - " + getString(R.string.session_livestream_title);
            }
            getSupportActionBar().setTitle(actionBarTitle);
            updateSessionViews(youtubeUrl, intent.getStringExtra(EXTRA_TITLE),
                    intent.getStringExtra(EXTRA_ABSTRACT),
                    UIUtils.CONFERENCE_HASHTAG, trackName);
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
            getSupportLoaderManager().restartLoader(SessionSummaryQuery._TOKEN, null, this);
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
     * @param track The track title (appears as part of action bar title), can be null
     * @param title The title to show in the session info fragment, can be null
     * @param sessionAbstract The session abstract to show in the session info fragment, can be null
     */
    public static void startFromExtras(Context context, String youtubeUrl, String track,
            String title, String sessionAbstract) {
        if (youtubeUrl == null) {
            return;
        }
        final Intent i = new Intent();
        i.setClass(context, SessionLivestreamActivity.class);
        i.putExtra(EXTRA_YOUTUBE_URL, youtubeUrl);
        i.putExtra(EXTRA_TRACK, track);
        i.putExtra(EXTRA_TITLE, title);
        i.putExtra(EXTRA_ABSTRACT, sessionAbstract);
        context.startActivity(i);
    }

    /**
     * Start a keynote live stream from extras. This sets the track name correctly so captions
     * will be correctly displayed.
     *
     * This will play the video given in youtubeUrl, however if a number ranging from 1-99 is
     * passed in this param instead it will be parsed and used to lookup a youtube ID from a
     * google spreadsheet using the param as the spreadsheet row number.
     */
    public static void startKeynoteFromExtras(Context context, String youtubeUrl, String title,
            String sessionAbstract) {
        startFromExtras(context, youtubeUrl, KEYNOTE_TRACK_NAME, title, sessionAbstract);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mTabHost != null) {
            outState.putString(EXTRA_TAB_STATE, mTabHost.getCurrentTabTag());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.session_livestream, menu);
        mCaptionsMenuItem = menu.findItem(R.id.menu_captions);
        mPresentationMenuItem = menu.findItem(R.id.menu_presentation);
        mPresentationMenuItem.setVisible(mPresentation != null);
        updatePresentationMenuItem(mPresentation != null);
        mShareMenuItem = menu.findItem(R.id.menu_share);
        if (mShareMenuDeferredSetup != null) {
            mShareMenuDeferredSetup.run();
        }
        if (!mIsTablet && Configuration.ORIENTATION_LANDSCAPE ==
                getResources().getConfiguration().orientation) {
            mCaptionsMenuItem.setVisible(true);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_captions:
                if (mIsFullscreen) {
                    if (mFullscreenCaptions.getVisibility() == View.GONE) {
                        mFullscreenCaptions.setVisibility(View.VISIBLE);
                        SessionLiveCaptionsFragment captionsFragment;
                        captionsFragment = (SessionLiveCaptionsFragment)
                                getSupportFragmentManager().findFragmentByTag(TAG_CAPTIONS);
                        if (captionsFragment == null) {
                            captionsFragment = new SessionLiveCaptionsFragment();
                            captionsFragment.setDarkTheme(true);
                            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                            ft.add(R.id.fullscreen_captions, captionsFragment, TAG_CAPTIONS);
                            ft.commit();
                        }
                        captionsFragment.setTrackName(mTrackName);
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
            case R.id.menu_presentation:
                if (mPresentation != null) {
                    mPresentation.dismiss();
                } else {
                    updatePresentation();
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
        String trackName = cursor.getString(SessionsQuery.TRACK_NAME);
        int trackColor = cursor.getInt(SessionsQuery.TRACK_COLOR);
        setActionBarTrackIcon(trackName, trackColor);

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
                return new CursorLoader(this, Sessions.buildWithTracksUri(mSessionId),
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
                    selection += Sessions.UPCOMING_SELECTION;
                    selectionArgs = Sessions.buildUpcomingSelectionArgs(currentTime);
                }
                return new CursorLoader(this, Sessions.buildWithTracksUri(),
                        SessionsQuery.PROJECTION, selection, selectionArgs, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        switch (loader.getId()) {
            case SessionSummaryQuery._TOKEN:
                loadSession(data);
                break;

            case SessionsQuery._TOKEN:
                mLivestreamAdapter.swapCursor(data);
                if (data != null && data.getCount() > 0) {
                    mSessionsFound = true;
                    final int selected = locateSelectedItem(data);
                    getSupportActionBar().setSelectedNavigationItem(selected);
                } else if (mSessionsFound) {
                    mSessionsFound = false;
                    final Bundle bundle = new Bundle();
                    bundle.putBoolean(LOADER_SESSIONS_ARG, true);
                    getSupportLoaderManager().restartLoader(SessionsQuery._TOKEN, bundle, this);
                } else {
                    finish();
                }
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
        if (data != null && (mSessionId != null || mTrackName != null)) {
            final boolean findNextSessionByTrack = mTrackName != null;
            while (data.moveToNext()) {
                if (findNextSessionByTrack) {
                    if (mTrackName.equals(data.getString(SessionsQuery.TRACK_NAME))) {
                        selected = data.getPosition();
                        mTrackName = null;
                        break;
                    }
                } else {
                    if (mSessionId.equals(data.getString(SessionsQuery.SESSION_ID))) {
                        selected = data.getPosition();
                    }
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

    private void loadSession(Cursor data) {
        if (data != null && data.moveToFirst()) {
            mTrackName = data.getString(SessionSummaryQuery.TRACK_NAME);
            if (TextUtils.isEmpty(mTrackName)) {
                mTrackName = KEYNOTE_TRACK_NAME;
                isKeynote = true;
            }
            final long currentTime = UIUtils.getCurrentTime(this);
            if (currentTime > data.getLong(SessionSummaryQuery.BLOCK_END)) {
                getSupportLoaderManager().restartLoader(SessionsQuery._TOKEN, null, this);
                return;
            }
            updateTagStreamFragment(data.getString(SessionSummaryQuery.HASHTAGS));
            updateSessionViews(
                    data.getString(SessionSummaryQuery.LIVESTREAM_URL),
                    data.getString(SessionSummaryQuery.TITLE),
                    data.getString(SessionSummaryQuery.ABSTRACT),
                    data.getString(SessionSummaryQuery.HASHTAGS), mTrackName);
        }
    }

    /**
     * Updates views that rely on session data from explicit strings.
     */
    private void updateSessionViews(final String youtubeUrl, final String title,
            final String sessionAbstract, final String hashTag, final String trackName) {

        if (youtubeUrl == null) {
            // Get out, nothing to do here
            finish();
            return;
        }

        mTrackName = trackName;
        String youtubeVideoId = youtubeUrl;
        if (youtubeUrl.startsWith("http")) {
            final Uri youTubeUri = Uri.parse(youtubeUrl);
            youtubeVideoId = youTubeUri.getQueryParameter("v");
        }

        // Play the video
        playVideo(youtubeVideoId);

        if (mTrackPlay) {
            EasyTracker.getTracker().sendView("Live Streaming: " + title);
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
                updateSessionLiveCaptionsFragment(trackName);
            }
        };

        if (!mLoadFromExtras) {
            mSessionSummaryDeferredSetup.run();
            mSessionSummaryDeferredSetup = null;
        }
    }

    private void updateSessionSummaryFragment(String title, String sessionAbstract) {
        SessionSummaryFragment sessionSummaryFragment = null;
        if (mIsTablet) {
            sessionSummaryFragment = (SessionSummaryFragment)
                    getSupportFragmentManager().findFragmentByTag(TAG_SESSION_SUMMARY);
        } else {
            if (mTabsAdapter.mFragments.containsKey(TABNUM_SESSION_SUMMARY)) {
                sessionSummaryFragment = (SessionSummaryFragment)
                        mTabsAdapter.mFragments.get(TABNUM_SESSION_SUMMARY);
            }
        }
        if (sessionSummaryFragment != null) {
            sessionSummaryFragment.setSessionSummaryInfo(
                    isKeynote ? getString(R.string.session_livestream_keynote_title, title) : title,
                    (isKeynote && TextUtils.isEmpty(sessionAbstract)) ?
                            getString(R.string.session_livestream_keynote_desc)
                            : sessionAbstract);
        }
    }

    private Runnable mStreamRefreshRunnable = new Runnable() {
        @Override
        public void run() {
            if (mTabsAdapter != null && mTabsAdapter.mFragments != null &&
                    mTabsAdapter.mFragments.containsKey(TABNUM_SOCIAL_STREAM)) {
                final SocialStreamFragment socialStreamFragment =
                        (SocialStreamFragment) mTabsAdapter.mFragments.get(TABNUM_SOCIAL_STREAM);
                socialStreamFragment.refresh();
            }
            mHandler.postDelayed(mStreamRefreshRunnable, STREAM_REFRESH_TIME);
        }
    };

    private void updateTagStreamFragment(String trackHashTag) {
        String hashTags = UIUtils.CONFERENCE_HASHTAG;
        if (!TextUtils.isEmpty(trackHashTag)) {
            hashTags += " " + trackHashTag;
        }

        if (mTabsAdapter.mFragments.containsKey(TABNUM_SOCIAL_STREAM)) {
            final SocialStreamFragment socialStreamFragment =
                    (SocialStreamFragment) mTabsAdapter.mFragments.get(TABNUM_SOCIAL_STREAM);
            socialStreamFragment.refresh(hashTags);
        }
    }

    private void updateSessionLiveCaptionsFragment(String trackName) {
        if (mTabsAdapter.mFragments.containsKey(TABNUM_LIVE_CAPTIONS)) {
            final SessionLiveCaptionsFragment captionsFragment = (SessionLiveCaptionsFragment)
                    mTabsAdapter.mFragments.get(TABNUM_LIVE_CAPTIONS);
            captionsFragment.setTrackName(trackName);
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
        if (mLoadFromExtras || isKeynote || mSessionUri == null) {
            return new Intent(this, HomeActivity.class);
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
        setFullscreenFlags();

        // If a presentation display is available, set YouTube player style to minimal
        if (mPresentation != null) {
            mYouTubePlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.MINIMAL);
        }

        // Load the requested video
        if (!TextUtils.isEmpty(mYouTubeVideoId)) {
            mYouTubePlayer.loadVideo(mYouTubeVideoId);
        }

        updatePresentation();
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
        if (mTabsAdapter.mFragments.containsKey(TABNUM_SESSION_SUMMARY)) {
            ((SessionSummaryFragment)
                    mTabsAdapter.mFragments.get(TABNUM_SESSION_SUMMARY)).updateViews();
        }
    }

    private void layoutTabletForLandscape() {
        mMainLayout.setOrientation(LinearLayout.HORIZONTAL);

        final LayoutParams videoLayoutParams = (LayoutParams) mVideoLayout.getLayoutParams();
        videoLayoutParams.height = LayoutParams.MATCH_PARENT;
        videoLayoutParams.width = 0;
        videoLayoutParams.weight = 1;
        mVideoLayout.setLayoutParams(videoLayoutParams);

        final LayoutParams extraLayoutParams = (LayoutParams) mExtraLayout.getLayoutParams();
        extraLayoutParams.height = LayoutParams.MATCH_PARENT;
        extraLayoutParams.width = 0;
        extraLayoutParams.weight = 1;
        mExtraLayout.setLayoutParams(extraLayoutParams);
    }

    private void layoutTabletForPortrait() {
        mMainLayout.setOrientation(LinearLayout.VERTICAL);

        final LayoutParams videoLayoutParams = (LayoutParams) mVideoLayout.getLayoutParams();
        videoLayoutParams.width = LayoutParams.MATCH_PARENT;
        if (mLoadFromExtras) {
            // Loading from extras, let the top fragment wrap_content
            videoLayoutParams.height = LayoutParams.WRAP_CONTENT;
            videoLayoutParams.weight = 0;
        } else {
            // Otherwise the session description will be longer, give it some space
            videoLayoutParams.height = 0;
            videoLayoutParams.weight = 7;
        }
        mVideoLayout.setLayoutParams(videoLayoutParams);

        final LayoutParams extraLayoutParams = (LayoutParams) mExtraLayout.getLayoutParams();
        extraLayoutParams.width = LayoutParams.MATCH_PARENT;
        extraLayoutParams.height = 0;
        extraLayoutParams.weight = 5;
        mExtraLayout.setLayoutParams(extraLayoutParams);
    }

    private void layoutFullscreenVideo(boolean fullscreen) {
        if (mIsFullscreen != fullscreen) {
            mIsFullscreen = fullscreen;
            if (mIsTablet) {
                // Tablet specific full screen layout
                layoutTabletFullscreen(fullscreen);
            } else {
                // Phone specific full screen layout
                layoutPhoneFullscreen(fullscreen);
            }

            // Full screen layout changes for all form factors
            final LayoutParams params = (LayoutParams) mPlayerContainer.getLayoutParams();
            if (fullscreen) {
                if (mCaptionsMenuItem != null) {
                    mCaptionsMenuItem.setVisible(true);
                }
                params.height = LayoutParams.MATCH_PARENT;
                mMainLayout.setPadding(0, 0, 0, 0);
            } else {
                getSupportActionBar().show();
                if (mCaptionsMenuItem != null) {
                    mCaptionsMenuItem.setVisible(false);
                }
                mFullscreenCaptions.setVisibility(View.GONE);
                params.height = LayoutParams.WRAP_CONTENT;
                adjustMainLayoutForActionBar();
            }
            View youTubePlayerView = mYouTubeFragment.getView();
            if (youTubePlayerView != null) {
              ViewGroup.LayoutParams playerParams = mYouTubeFragment.getView().getLayoutParams();
              playerParams.height = fullscreen ? LayoutParams.MATCH_PARENT
                  : LayoutParams.WRAP_CONTENT;
              youTubePlayerView.setLayoutParams(playerParams);
            }
            mPlayerContainer.setLayoutParams(params);
        }
    }

    private void adjustMainLayoutForActionBar() {
        if (UIUtils.hasICS()) {
            // On ICS+ we use FEATURE_ACTION_BAR_OVERLAY so full screen mode doesn't need to
            // re-adjust layouts when hiding action bar. To account for this we add action bar
            // height pack to the padding when not in full screen mode.
            mMainLayout.setPadding(0, getActionBarHeightPx(), 0, 0);
        }
    }

    /**
     * Adjusts tablet layouts for full screen video.
     *
     * @param fullscreen True to layout in full screen, false to switch to regular layout
     */
    private void layoutTabletFullscreen(boolean fullscreen) {
        if (fullscreen) {
            mExtraLayout.setVisibility(View.GONE);
            mSummaryLayout.setVisibility(View.GONE);
            mVideoLayout.setPadding(0, 0, 0, 0);
            final LayoutParams videoLayoutParams = (LayoutParams) mVideoLayout.getLayoutParams();
            videoLayoutParams.setMargins(0, 0, 0, 0);
            mVideoLayout.setLayoutParams(videoLayoutParams);
        } else {
            final int padding =
                    getResources().getDimensionPixelSize(R.dimen.multipane_half_padding);
            mExtraLayout.setVisibility(View.VISIBLE);
            mSummaryLayout.setVisibility(View.VISIBLE);
            mVideoLayout.setBackgroundResource(R.drawable.grey_frame_on_white);
            final LayoutParams videoLayoutParams = (LayoutParams) mVideoLayout.getLayoutParams();
            videoLayoutParams.setMargins(padding, padding, padding, padding);
            mVideoLayout.setLayoutParams(videoLayoutParams);
        }
    }

    /**
     * Adjusts phone layouts for full screen video.
     */
    private void layoutPhoneFullscreen(boolean fullscreen) {
        if (fullscreen) {
            mTabHost.setVisibility(View.GONE);
            if (mYouTubePlayer != null) {
                mYouTubePlayer.setFullscreen(true);
            }
        } else {
            mTabHost.setVisibility(View.VISIBLE);
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    private int getActionBarHeightPx() {
        int[] attrs = new int[] { android.R.attr.actionBarSize };
        return (int) getTheme().obtainStyledAttributes(attrs).getDimension(0, 0f);
    }

    /**
     * Adapter that backs the action bar drop-down spinner.
     */
    private class LivestreamAdapter extends CursorAdapter {
        LayoutInflater mLayoutInflater;

        public LivestreamAdapter(Context context) {
            super(context, null, 0);
            if (UIUtils.hasICS()) {
                mLayoutInflater = (LayoutInflater) getSupportActionBar().getThemedContext()
                        .getSystemService(LAYOUT_INFLATER_SERVICE);
            } else {
                mLayoutInflater =
                        (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            }
        }

        @Override
        public Object getItem(int position) {
            mCursor.moveToPosition(position);
            return mCursor;
        }

        @Override
        public View newDropDownView(Context context, Cursor cursor, ViewGroup parent) {
            // Inflate view that appears in the drop-down spinner views
            return mLayoutInflater.inflate(
                    R.layout.spinner_item_session_livestream, parent, false);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            // Inflate view that appears in the selected spinner
            return mLayoutInflater.inflate(android.R.layout.simple_spinner_item,
                    parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            // Bind view that appears in the selected spinner or in the drop-down
            final TextView titleView = (TextView) view.findViewById(android.R.id.text1);
            final TextView subTitleView = (TextView) view.findViewById(android.R.id.text2);

            String trackName = cursor.getString(SessionsQuery.TRACK_NAME);
            if (TextUtils.isEmpty(trackName)) {
                trackName = getString(R.string.app_name);
            } else {
                trackName = getString(R.string.session_livestream_track_title, trackName);
            }

            String sessionTitle = cursor.getString(SessionsQuery.TITLE);

            if (subTitleView != null) { // Drop-down view
                titleView.setText(trackName);
                subTitleView.setText(sessionTitle);
            } else { // Selected view
                titleView.setText(getString(R.string.session_livestream_title) + ": " + trackName);
            }
        }
    }

    /**
     * Adapter that backs the ViewPager tabs on the phone UI.
     */
    private static class TabsAdapter extends FragmentPagerAdapter
            implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
        private final FragmentActivity mContext;
        private final TabHost mTabHost;
        private final ViewPager mViewPager;
        public final HashMap<Integer, Fragment> mFragments;
        private final ArrayList<Integer> mTabNums;
        private int tabCount = 0;

        static class DummyTabFactory implements TabHost.TabContentFactory {
            private final Context mContext;

            public DummyTabFactory(Context context) {
                mContext = context;
            }

            @Override
            public View createTabContent(String tag) {
                View v = new View(mContext);
                v.setMinimumWidth(0);
                v.setMinimumHeight(0);
                return v;
            }
        }

        @SuppressLint("UseSparseArrays")
        public TabsAdapter(FragmentActivity activity, TabHost tabHost, ViewPager pager) {
            super(activity.getSupportFragmentManager());
            mFragments = new HashMap<Integer, Fragment>(3);
            mContext = activity;
            mTabHost = tabHost;
            mViewPager = pager;
            mTabHost.setOnTabChangedListener(this);
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
            mTabNums = new ArrayList<Integer>(3);
        }

        public void addTab(String tabTitle, Fragment newFragment, int tabId) {
            ViewGroup tabWidget = (ViewGroup) mTabHost.findViewById(android.R.id.tabs);
            TextView tabIndicatorView = (TextView) mContext.getLayoutInflater().inflate(
                    R.layout.tab_indicator_color, tabWidget, false);
            tabIndicatorView.setText(tabTitle);

            final TabHost.TabSpec tabSpec = mTabHost.newTabSpec(String.valueOf(tabCount++));
            tabSpec.setIndicator(tabIndicatorView);
            tabSpec.setContent(new DummyTabFactory(mContext));
            mTabHost.addTab(tabSpec);
            mFragments.put(tabId, newFragment);
            mTabNums.add(tabId);
            notifyDataSetChanged();
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
        public void onTabChanged(String tabId) {
            int position = mTabHost.getCurrentTab();
            mViewPager.setCurrentItem(position);
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            // Unfortunately when TabHost changes the current tab, it kindly also takes care of
            // putting focus on it when not in touch mode. The jerk. This hack tries to prevent
            // this from pulling focus out of our ViewPager.
            TabWidget widget = mTabHost.getTabWidget();
            int oldFocusability = widget.getDescendantFocusability();
            widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            mTabHost.setCurrentTab(position);
            widget.setDescendantFocusability(oldFocusability);
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
    public static class SessionLiveCaptionsFragment extends Fragment {
        private static final String CAPTIONS_DARK_THEME_URL_PARAM = "&theme=dark";

        private FrameLayout mContainer;
        private WebView mWebView;
        private TextView mNoCaptionsTextView;
        private boolean mDarkTheme = false;
        private String mSessionTrack;

        public SessionLiveCaptionsFragment() {
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
            updateViews();
            return view;
        }

        public void setTrackName(String sessionTrack) {
            mSessionTrack = sessionTrack;
            updateViews();
        }

        public void setDarkTheme(boolean dark) {
            mDarkTheme = dark;
        }

        @SuppressLint("SetJavaScriptEnabled")
        public void updateViews() {
            if (mWebView != null && !TextUtils.isEmpty(mSessionTrack)) {
                if (mDarkTheme) {
                    mWebView.setBackgroundColor(Color.BLACK);
                    mContainer.setBackgroundColor(Color.BLACK);
                    mNoCaptionsTextView.setTextColor(Color.WHITE);
                } else {
                    mWebView.setBackgroundColor(Color.WHITE);
                    mContainer.setBackgroundResource(R.drawable.grey_frame_on_white);
                }

                String captionsUrl;
                final String trackLowerCase = mSessionTrack.toLowerCase();
                if (mSessionTrack.equals(KEYNOTE_TRACK_NAME)||
                        trackLowerCase.equals(Config.PRIMARY_LIVESTREAM_TRACK)) {
                    // if keynote or primary track, use primary captions url
                    captionsUrl = Config.PRIMARY_LIVESTREAM_CAPTIONS_URL;
                } else if (trackLowerCase.equals(Config.SECONDARY_LIVESTREAM_TRACK)) {
                    // else if secondary track use secondary captions url
                    captionsUrl = Config.SECONDARY_LIVESTREAM_CAPTIONS_URL;
                } else {
                    // otherwise we don't have captions
                    captionsUrl = null;
                }
                if (captionsUrl != null) {
                    if (mDarkTheme) {
                        captionsUrl += CAPTIONS_DARK_THEME_URL_PARAM;
                    }
                    mWebView.getSettings().setJavaScriptEnabled(true);
                    mWebView.loadUrl(captionsUrl);
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

    /*
     * Presentation and Media Router methods and classes. This allows use of an externally
     * connected display to show video content full screen while still showing the regular views
     * on the primary device display.
     */

    /**
     * Updates the presentation display. If a suitable external display is attached. The video
     * will be displayed on that screen instead. If not, the video will be displayed normally.
     */
    private void updatePresentation() {
        updatePresentation(false);
    }

    /**
     * Updates the presentation display. If a suitable external display is attached. The video
     * will be displayed on that screen instead. If not, the video will be displayed normally.
     * @param forceDismiss If true, the external display will be dismissed even if it is still
     *                     available. This is useful for if the user wants to explicitly toggle
     *                     the external display off even if it's still connected. Setting this to
     *                     false will adjust the display based on if a suitable external display
     *                     is available or not.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void updatePresentation(boolean forceDismiss) {
        if (!UIUtils.hasJellyBeanMR1()) {
            return;
        }

        // Get the current route and its presentation display
        MediaRouter.RouteInfo route =
                mMediaRouter.getSelectedRoute(MediaRouter.ROUTE_TYPE_LIVE_VIDEO);
        Display presentationDisplay = route != null ? route.getPresentationDisplay() : null;

        // Dismiss the current presentation if the display has changed
        if (mPresentation != null &&
                (mPresentation.getDisplay() != presentationDisplay || forceDismiss)) {
            hidePresentation();
        }

        // Show a new presentation if needed.
        if (mPresentation == null && presentationDisplay != null && !forceDismiss) {
            showPresentation(presentationDisplay);
        }

        // Show/hide toggle presentation action item
        if (mPresentationMenuItem != null) {
            mPresentationMenuItem.setVisible(presentationDisplay != null);
        }
    }

    /**
     * Shows the Presentation on the designated Display. Some UI components are also updated:
     * a play/pause button is displayed where the video would normally be; the YouTube player style
     * is updated to MINIMAL (more suitable for an external display); and an action item is updated
     * to indicate the external display is being used.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void showPresentation(Display presentationDisplay) {
        LOGD(TAG, "Showing video presentation on display: " + presentationDisplay);
        if (mIsFullscreen) {
            if (!mIsTablet) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
            if (mYouTubePlayer != null) {
                mYouTubePlayer.setFullscreen(false);
            }
        }
        View presentationView = mYouTubeFragment.getView();
        ((ViewGroup) presentationView.getParent()).removeView(presentationView);
        mPresentation = new YouTubePresentation(this, presentationDisplay, presentationView);
        mPresentation.setOnDismissListener(mOnDismissListener);
        try {
            mPresentation.show();
            mPresentationControls.setVisibility(View.VISIBLE);
            updatePresentationMenuItem(true);
            if (mYouTubePlayer != null) {
                mYouTubePlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.MINIMAL);
                mYouTubePlayer.play();
                setFullscreenFlags(true);
            }
        } catch (WindowManager.InvalidDisplayException e) {
            LOGE(TAG, "Couldn't show presentation! Display was removed in the meantime.", e);
            hidePresentation();
        }
    }

    /**
     * Hides the Presentation. Some UI components are also updated:
     * the video view is returned to its normal place on the primary display; the YouTube player
     * style is updated to the default; and an action item is updated to indicate the external
     * display is no longer in use.
     */
    private void hidePresentation() {
        LOGD(TAG, "Hiding video presentation");
        View presentationView = mPresentation.getYouTubeFragmentView();
        ((ViewGroup) presentationView.getParent()).removeView(presentationView);
        mPlayerContainer.addView(presentationView);
        mPresentationControls.setVisibility(View.GONE);
        updatePresentationMenuItem(false);
        mPresentation.dismiss();
        mPresentation = null;
        if (mYouTubePlayer != null) {
            mYouTubePlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.DEFAULT);
            mYouTubePlayer.play();
            setFullscreenFlags(false);
        }
    }

    /**
     * Updates the presentation action item. Toggles the icon between two drawables to indicate
     * if the external display is currently being used or not.
     *
     * @param enabled If the external display is enabled or not.
     */
    private void updatePresentationMenuItem(boolean enabled) {
        if (mPresentationMenuItem != null) {
            mPresentationMenuItem.setIcon(
                    enabled ?
                            R.drawable.ic_media_route_on_holo_light :
                            R.drawable.ic_media_route_off_holo_light);
        }
    }


    /**
     * Applies YouTube player full screen flags. Calls through to
     * {@link #setFullscreenFlags(boolean)}.
     */
    private void setFullscreenFlags() {
        setFullscreenFlags(true);
    }

    /**
     * Applies YouTube player full screen flags plus forces portrait orientation on small screens
     * when presentation (secondary display) is enabled (as the full screen layout won't have
     * anything to display when an external display is connected).
     *
     * @param usePresentationMode Whether or not to use presentation mode. This is used when an
     *                            external display is connected and the user toggles between
     *                            presentation mode.
     */
    private void setFullscreenFlags(boolean usePresentationMode) {
        if (mYouTubePlayer != null) {
            if (usePresentationMode && mPresentation != null && !mIsTablet) {
                mYouTubePlayer.setFullscreenControlFlags(0);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                mYouTubePlayer.setFullscreenControlFlags(mYouTubeFullscreenFlags);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        }
    }

    /**
     * Listens for when the Presentation (which is a type of Dialog) is dismissed.
     */
    private final DialogInterface.OnDismissListener mOnDismissListener =
            new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    if (dialog == mPresentation) {
                        LOGD(TAG, "Presentation was dismissed.");
                        updatePresentation(true);
                    }
                }
            };

    /**
     * The main subclass of Presentation that is used to show content on an externally connected
     * display. There is no way to add a {@link android.app.Fragment}, and therefore a
     * YouTubePlayerFragment to a {@link android.app.Presentation} (which is just a subclass of
     * {@link android.app.Dialog}) so instead we pass in a View directly which is the extracted
     * YouTubePlayerView from the {@link SessionLivestreamActivity} YouTubePlayerFragment. Not
     * ideal and fairly hacky but there aren't any better alternatives unfortunately.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    private final static class YouTubePresentation extends Presentation {
        private View mYouTubeFragmentView;

        public YouTubePresentation(Context context, Display display, View presentationView) {
            super(context, display);
            mYouTubeFragmentView = presentationView;
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(mYouTubeFragmentView);
        }

        public View getYouTubeFragmentView() {
            return mYouTubeFragmentView;
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
                ScheduleContract.Sessions.SESSION_ID,
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.SESSION_ABSTRACT,
                ScheduleContract.Sessions.SESSION_HASHTAGS,
                ScheduleContract.Sessions.SESSION_LIVESTREAM_URL,
                Tracks.TRACK_NAME,
                ScheduleContract.Blocks.BLOCK_START,
                ScheduleContract.Blocks.BLOCK_END,
        };

        final static int ID = 0;
        final static int TITLE = 1;
        final static int ABSTRACT = 2;
        final static int HASHTAGS = 3;
        final static int LIVESTREAM_URL = 4;
        final static int TRACK_NAME = 5;
        final static int BLOCK_START= 6;
        final static int BLOCK_END = 7;
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
                Tracks.TRACK_NAME,
                Tracks.TRACK_COLOR,
                ScheduleContract.Blocks.BLOCK_START,
                ScheduleContract.Blocks.BLOCK_END,
        };

        final static int _ID = 0;
        final static int SESSION_ID = 1;
        final static int TITLE = 2;
        final static int TRACK_NAME = 3;
        final static int TRACK_COLOR = 4;
        final static int BLOCK_START= 5;
        final static int BLOCK_END = 6;
    }
}
