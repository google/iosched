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

package com.google.android.apps.iosched.ui.gtv;

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.iosched.Config;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.provider.ScheduleContract.Sessions;
import com.google.android.apps.iosched.sync.SyncHelper;
import com.google.android.apps.iosched.ui.BaseActivity;
import com.google.android.apps.iosched.ui.SessionLivestreamActivity.SessionSummaryFragment;
import com.google.android.apps.iosched.util.UIUtils;
import com.google.android.youtube.api.YouTube;
import com.google.android.youtube.api.YouTubePlayer;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import static com.google.android.apps.iosched.util.LogUtils.LOGD;
import static com.google.android.apps.iosched.util.LogUtils.LOGE;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A Google TV home activity for Google I/O 2012 that displays session live streams pulled in
 * from YouTube.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
public class GoogleTVSessionLivestreamActivity extends BaseActivity implements
        LoaderCallbacks<Cursor>,
        YouTubePlayer.OnPlaybackEventsListener,
        YouTubePlayer.OnFullscreenListener,
        OnItemClickListener {

    private static final String TAG = makeLogTag(GoogleTVSessionLivestreamActivity.class);

    private static final String TAG_SESSION_SUMMARY = "session_summary";
    private static final int UPCOMING_SESSIONS_QUERY_ID = 3;

    private static final String PROMO_VIDEO_URL = "http://www.youtube.com/watch?v=gTA-5HM8Zhs";

    private boolean mIsFullscreen = false;
    private boolean mTrackPlay = true;

    private YouTubePlayer mYouTubePlayer;
    private FrameLayout mPlayerContainer;
    private String mYouTubeVideoId;
    private LinearLayout mMainLayout;
    private LinearLayout mVideoLayout;
    private LinearLayout mExtraLayout;
    private FrameLayout mSummaryLayout;
    private ListView mLiveListView;

    private SyncHelper mGTVSyncHelper;
    private LivestreamAdapter mLivestreamAdapter;

    private String mSessionId;

    private Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        YouTube.initialize(this, Config.YOUTUBE_API_KEY);

        setContentView(R.layout.activity_session_livestream);
        
        // Set up YouTube player
        mYouTubePlayer = (YouTubePlayer) getSupportFragmentManager().findFragmentById(
                R.id.livestream_player);
        mYouTubePlayer.setOnPlaybackEventsListener(this);
        mYouTubePlayer.enableCustomFullscreen(this);
        mYouTubePlayer.setFullscreenControlFlags(
                YouTubePlayer.FULLSCREEN_FLAG_CONTROL_SYSTEM_UI);

        // Views that are common over all layouts
        mMainLayout = (LinearLayout) findViewById(R.id.livestream_mainlayout);
        mPlayerContainer = (FrameLayout) findViewById(R.id.livestream_player_container);

        // Tablet UI specific views
        getSupportFragmentManager().beginTransaction().add(R.id.livestream_summary,
                new SessionSummaryFragment(), TAG_SESSION_SUMMARY).commit();
        
        mVideoLayout = (LinearLayout) findViewById(R.id.livestream_videolayout);
        mExtraLayout = (LinearLayout) findViewById(R.id.googletv_livesextra_layout);
        mSummaryLayout = (FrameLayout) findViewById(R.id.livestream_summary);

        // Reload all other data in this activity
        reloadFromUri(getIntent().getData());

        // Start sessions query to populate action bar navigation spinner
        getSupportLoaderManager().initLoader(SessionsQuery._TOKEN, null, this);

        // Set up left side listview
        mLivestreamAdapter = new LivestreamAdapter(this);
        mLiveListView = (ListView) findViewById(R.id.live_session_list);
        mLiveListView.setAdapter(mLivestreamAdapter);
        mLiveListView.setOnItemClickListener(this);
        mLiveListView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        mLiveListView.setSelector(android.R.color.transparent);

        getSupportActionBar().hide();

        // Sync data from Conference API
        new SyncOperationTask().execute((Void) null);
    }

    /**
     * Reloads all data in the activity and fragments from a given uri
     */
    private void reloadFromUri(Uri newUri) {
        if (newUri != null && newUri.getPathSegments().size() >= 2) {
            mSessionId = Sessions.getSessionId(newUri);
            getSupportLoaderManager().restartLoader(SessionSummaryQuery._TOKEN, null, this);
        } 
    }

    @Override
    public void onBackPressed() {
        if (mIsFullscreen) {
            // Exit full screen mode on back key
            mYouTubePlayer.setFullscreen(false);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int itemPosition, long itemId) {
        final Cursor cursor = (Cursor) mLivestreamAdapter.getItem(itemPosition);
        final String sessionId = cursor.getString(SessionsQuery.SESSION_ID);
        if (sessionId != null) {
            reloadFromUri(Sessions.buildSessionUri(sessionId));
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case SessionSummaryQuery._TOKEN:
                return new CursorLoader(
                        this, Sessions.buildWithTracksUri(mSessionId),
                        SessionSummaryQuery.PROJECTION, null, null, null);

            case SessionsQuery._TOKEN:
                final long currentTime = UIUtils.getCurrentTime(this);
                String selection = Sessions.LIVESTREAM_SELECTION + " and "
                        + Sessions.AT_TIME_SELECTION;
                String[] selectionArgs = Sessions.buildAtTimeSelectionArgs(currentTime);
                return new CursorLoader(this, Sessions.buildWithTracksUri(),
                        SessionsQuery.PROJECTION, selection,
                        selectionArgs, null);

            case UPCOMING_SESSIONS_QUERY_ID:
                final long newCurrentTime = UIUtils.getCurrentTime(this);
                String sessionsSelection = Sessions.LIVESTREAM_SELECTION + " and "
                        + Sessions.UPCOMING_SELECTION;
                String[] sessionsSelectionArgs =
                        Sessions.buildUpcomingSelectionArgs(newCurrentTime);
                return new CursorLoader(this, Sessions.buildWithTracksUri(),
                        SessionsQuery.PROJECTION, sessionsSelection,
                        sessionsSelectionArgs, null);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mHandler.removeCallbacks(mNextSessionStartsInCountdownRunnable);

        switch (loader.getId()) {
            case SessionSummaryQuery._TOKEN:
                loadSession(data);
                break;

            case SessionsQuery._TOKEN:
                mLivestreamAdapter.swapCursor(data);
                final int selected = locateSelectedItem(data);
                if (data.getCount() == 0) {
                    handleNoLiveSessionsAvailable();
                } else {
                    mLiveListView.setSelection(selected);
                    mLiveListView.requestFocus(selected);
                    final Cursor cursor = (Cursor) mLivestreamAdapter.getItem(selected);
                    final String sessionId = cursor.getString(SessionsQuery.SESSION_ID);
                    if (sessionId != null) {
                        reloadFromUri(Sessions.buildSessionUri(sessionId));
                    }
                }
                break;

            case UPCOMING_SESSIONS_QUERY_ID:
                if (data != null && data.getCount() > 0) {
                    data.moveToFirst();
                    handleUpdateNextUpcomingSession(data);
                }
                break;
        }
    }

    public void handleNoLiveSessionsAvailable() {
        getSupportLoaderManager().initLoader(UPCOMING_SESSIONS_QUERY_ID, null, this);
        updateSessionViews(PROMO_VIDEO_URL,
                getString(R.string.missed_io_title),
                getString(R.string.missed_io_subtitle), UIUtils.CONFERENCE_HASHTAG);

        //Make link in abstract view clickable
        TextView abstractLinkTextView = (TextView) findViewById(R.id.session_abstract);
        abstractLinkTextView.setMovementMethod(new LinkMovementMethod());
    }

    private String mNextSessionTitle;
    private long mNextSessionStartTime;

    private final Runnable mNextSessionStartsInCountdownRunnable = new Runnable() {
        public void run() {
            int remainingSec = (int) Math.max(0,
                    (mNextSessionStartTime - UIUtils.getCurrentTime(
                            GoogleTVSessionLivestreamActivity.this)) / 1000);

            final int secs = remainingSec % 86400;
            final int days = remainingSec / 86400;
            final String str;
            if (days == 0) {
                str = getResources().getString(
                        R.string.starts_in_template_0_days,
                        DateUtils.formatElapsedTime(secs));
            } else {
                str = getResources().getQuantityString(
                        R.plurals.starts_in_template, days, days,
                        DateUtils.formatElapsedTime(secs));
            }

            updateSessionSummaryFragment(mNextSessionTitle, str);

            if (remainingSec == 0) {
                // Next session starting now!
                mHandler.postDelayed(mRefreshSessionsRunnable, 1000);
                return;
            }

            // Repost ourselves to keep updating countdown
            mHandler.postDelayed(mNextSessionStartsInCountdownRunnable, 1000);
        }
    };

    private final Runnable mRefreshSessionsRunnable = new Runnable() {
        @Override
        public void run() {
            getSupportLoaderManager().restartLoader(SessionsQuery._TOKEN, null,
                    GoogleTVSessionLivestreamActivity.this);
        }
    };

    public void handleUpdateNextUpcomingSession(Cursor data) {
        mNextSessionTitle = getString(R.string.next_live_stream_session_template,
                data.getString(SessionsQuery.TITLE));
        mNextSessionStartTime = data.getLong(SessionsQuery.BLOCK_START);
        updateSessionViews(PROMO_VIDEO_URL, mNextSessionTitle, "", UIUtils.CONFERENCE_HASHTAG);

        // Begin countdown til next session
        mHandler.post(mNextSessionStartsInCountdownRunnable);
    }

    /**
     * Locates which item should be selected in the action bar drop-down spinner based on the
     * current active session uri
     */
    private int locateSelectedItem(Cursor data) {
        int selected = 0;
        if (data != null && mSessionId != null) {
            while (data.moveToNext()) {
                if (mSessionId.equals(data.getString(SessionsQuery.SESSION_ID))) {
                    selected = data.getPosition();
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
    public void onFullscreen(boolean fullScreen) {
        layoutFullscreenVideo(fullScreen);
    }

    @Override
    public void onLoaded(String s) {
    }

    @Override
    public void onPlaying() {
    }

    @Override
    public void onPaused() {
    }

    @Override
    public void onBuffering(boolean b) {
    }

    @Override
    public void onEnded() {
        mHandler.post(mRefreshSessionsRunnable);
    }

    @Override
    public void onError() {
        Toast.makeText(this, R.string.session_livestream_error, Toast.LENGTH_LONG).show();
        LOGE(TAG, getString(R.string.session_livestream_error));
    }

    private void loadSession(Cursor data) {
        if (data != null && data.moveToFirst()) {
            // Schedule a data refresh after the session ends.
            // NOTE: using postDelayed instead of postAtTime helps during debugging, using
            // mock times.
            mHandler.postDelayed(mRefreshSessionsRunnable,
                    data.getLong(SessionSummaryQuery.BLOCK_END) + 1000
                            - UIUtils.getCurrentTime(this));

            updateSessionViews(
                    data.getString(SessionSummaryQuery.LIVESTREAM_URL),
                    data.getString(SessionSummaryQuery.TITLE),
                    data.getString(SessionSummaryQuery.ABSTRACT),
                    data.getString(SessionSummaryQuery.HASHTAGS));
        }
    }

    /**
     * Updates views that rely on session data from explicit strings.
     */
    private void updateSessionViews(final String youtubeUrl, final String title,
            final String sessionAbstract, final String hashTag) {

        if (youtubeUrl == null) {
            // Get out, nothing to do here
            Toast.makeText(this, R.string.error_tv_no_url, Toast.LENGTH_SHORT).show();
            LOGE(TAG, getString(R.string.error_tv_no_url));
            return;
        }

        String youtubeVideoId = youtubeUrl;
        if (youtubeUrl.startsWith("http")) {
            final Uri youTubeUri = Uri.parse(youtubeUrl);
            youtubeVideoId = youTubeUri.getQueryParameter("v");
        }
        playVideo(youtubeVideoId);

        if (mTrackPlay) {
            EasyTracker.getTracker().trackView("Live Streaming: " + title);
            LOGD("Tracker", "Live Streaming: " + title);
        }

        updateSessionSummaryFragment(title, sessionAbstract);

        mLiveListView.requestFocus();
    }

    private void updateSessionSummaryFragment(String title, String sessionAbstract) {
        SessionSummaryFragment sessionSummaryFragment = (SessionSummaryFragment)
                 getSupportFragmentManager().findFragmentByTag(TAG_SESSION_SUMMARY);
        if (sessionSummaryFragment != null) {
            sessionSummaryFragment.setSessionSummaryInfo(title, sessionAbstract);
        }
    }

    private void playVideo(String videoId) {
        if ((mYouTubeVideoId == null || !mYouTubeVideoId.equals(videoId))
                && !TextUtils.isEmpty(videoId)) {
            mYouTubeVideoId = videoId;
            mYouTubePlayer.loadVideo(mYouTubeVideoId);
            mTrackPlay = true;
        } else {
            mTrackPlay = false;
        }
    }

    private void layoutFullscreenVideo(boolean fullscreen) {
        if (mIsFullscreen != fullscreen) {
            mIsFullscreen = fullscreen;
            mYouTubePlayer.setFullscreen(fullscreen);
            layoutGoogleTVFullScreen(fullscreen);

            // Full screen layout changes for all form factors
            final LayoutParams params = (LayoutParams) mPlayerContainer.getLayoutParams();
            if (fullscreen) {
                params.height = LayoutParams.MATCH_PARENT;
                mMainLayout.setPadding(0, 0, 0, 0);
            } else {
                params.height = LayoutParams.WRAP_CONTENT;
            }
            mPlayerContainer.setLayoutParams(params);
        }
    }

    /**
     * Adjusts tablet layouts for full screen video.
     * 
     * @param fullscreen True to layout in full screen, false to switch to regular layout
     */
    private void layoutGoogleTVFullScreen(boolean fullscreen) {
        if (fullscreen) {
            mExtraLayout.setVisibility(View.GONE);
            mSummaryLayout.setVisibility(View.GONE);
            mMainLayout.setPadding(0, 0, 0, 0);
            mVideoLayout.setPadding(0, 0, 0, 0);
            final LayoutParams videoLayoutParams = (LayoutParams) mVideoLayout.getLayoutParams();
            videoLayoutParams.setMargins(0, 0, 0, 0);
            mVideoLayout.setLayoutParams(videoLayoutParams);
        } else {
            final int padding =
                    getResources().getDimensionPixelSize(R.dimen.multipane_half_padding);
            mExtraLayout.setVisibility(View.VISIBLE);
            mSummaryLayout.setVisibility(View.VISIBLE);
            mMainLayout.setPadding(padding, padding, padding, padding);
            mVideoLayout.setBackgroundResource(R.drawable.grey_frame_on_white);
            final LayoutParams videoLayoutParams = (LayoutParams) mVideoLayout.getLayoutParams();
            videoLayoutParams.setMargins(padding, padding, padding, padding);
            mVideoLayout.setLayoutParams(videoLayoutParams);
        }
    }

    /**
     * Adapter that backs the action bar drop-down spinner.
     */
    private class LivestreamAdapter extends CursorAdapter {
        private LayoutInflater mLayoutInflater;

        public LivestreamAdapter(Context context) {
            super(context, null, false);
                mLayoutInflater = (LayoutInflater)
                        context.getSystemService(LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public Object getItem(int position) {
            mCursor.moveToPosition(position);
            return mCursor;
        }

        @Override
        public View newDropDownView(Context context, Cursor cursor, ViewGroup parent) {
            // Inflate view that appears in the side list view
            return mLayoutInflater.inflate(
                    R.layout.list_item_live_session, parent, false);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            // Inflate view that appears in the side list view
             return mLayoutInflater.inflate(R.layout.list_item_live_session,
                                     parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {

            final TextView titleView = (TextView) view.findViewById(R.id.live_session_title);
            final TextView subTitleView = (TextView) view.findViewById(R.id.live_session_subtitle);

            String trackName = cursor.getString(SessionsQuery.TRACK_NAME);
            if (TextUtils.isEmpty(trackName)) {
                trackName = getString(R.string.app_name);
            } else {
                trackName = getString(R.string.session_livestream_track_title, trackName);
            }

            cursor.getInt(SessionsQuery.TRACK_COLOR);
            String sessionTitle = cursor.getString(SessionsQuery.TITLE);

            if (subTitleView != null) { 
                titleView.setText(trackName);
                subTitleView.setText(sessionTitle);
            } else { // Selected view
                titleView.setText(getString(R.string.session_livestream_title) + ": " + trackName);
            }
        }
    }

    // Enabling media keys for Google TV Devices
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                mYouTubePlayer.pause();
                return true;

            case KeyEvent.KEYCODE_MEDIA_PLAY:
                mYouTubePlayer.play();
                return true;

            case KeyEvent.KEYCODE_MEDIA_FAST_FORWARD:
                mYouTubePlayer.seekRelativeMillis(20000);
                return true;

            case KeyEvent.KEYCODE_MEDIA_REWIND:
                mYouTubePlayer.seekRelativeMillis(-20000);
                return true;

            default:
                return super.onKeyDown(keyCode, event);
        }
    }

    // Need to sync with the conference API to get the livestream URLs
    private class SyncOperationTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            // Perform a sync using SyncHelper
            if (mGTVSyncHelper == null) {
                mGTVSyncHelper = new SyncHelper(getApplicationContext());
            }
            try {
                mGTVSyncHelper.performSync(null, SyncHelper.FLAG_SYNC_LOCAL | SyncHelper.FLAG_SYNC_REMOTE);
            } catch (IOException e) {
                LOGE(TAG, "Error loading data for Google I/O 2012.", e);
            }
            return null;
        }
    }

    /**
     * Single session query
     */
    public interface SessionSummaryQuery {
        int _TOKEN = 0;

        String[] PROJECTION = {
                ScheduleContract.Sessions.SESSION_ID,
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.SESSION_ABSTRACT,
                ScheduleContract.Sessions.SESSION_HASHTAGS,
                ScheduleContract.Sessions.SESSION_LIVESTREAM_URL,
                ScheduleContract.Tracks.TRACK_NAME,
                ScheduleContract.Blocks.BLOCK_START,
                ScheduleContract.Blocks.BLOCK_END,
        };

        int SESSION_ID = 0;
        int TITLE = 1;
        int ABSTRACT = 2;
        int HASHTAGS = 3;
        int LIVESTREAM_URL = 4;
        int TRACK_NAME = 5;
        int BLOCK_START= 6;
        int BLOCK_END = 7;
    }

    /**
     * List of sessions query
     */
    public interface SessionsQuery {
        int _TOKEN = 1;

        String[] PROJECTION = {
                BaseColumns._ID,
                Sessions.SESSION_ID,
                Sessions.SESSION_TITLE,
                ScheduleContract.Tracks.TRACK_NAME,
                ScheduleContract.Tracks.TRACK_COLOR,
                ScheduleContract.Blocks.BLOCK_START,
                ScheduleContract.Blocks.BLOCK_END,
        };

        int ID = 0;
        int SESSION_ID = 1;
        int TITLE = 2;
        int TRACK_NAME = 3;
        int TRACK_COLOR = 4;
        int BLOCK_START= 5;
        int BLOCK_END = 6;
    }
}
