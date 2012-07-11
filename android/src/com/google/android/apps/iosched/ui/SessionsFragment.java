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

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.util.SessionsHelper;
import com.google.android.apps.iosched.util.UIUtils;
import com.google.android.apps.iosched.util.actionmodecompat.ActionMode;
import com.google.android.apps.iosched.util.actionmodecompat.MultiChoiceModeListener;

import com.actionbarsherlock.app.SherlockListFragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Spannable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.LinkedHashSet;

import static com.google.android.apps.iosched.util.LogUtils.LOGD;
import static com.google.android.apps.iosched.util.LogUtils.LOGV;
import static com.google.android.apps.iosched.util.LogUtils.LOGW;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;
import static com.google.android.apps.iosched.util.UIUtils.buildStyledSnippet;
import static com.google.android.apps.iosched.util.UIUtils.formatSessionSubtitle;

/**
 * A {@link ListFragment} showing a list of sessions. This fragment supports multiple-selection
 * using the contextual action bar (on API 11+ devices), and also supports a separate 'activated'
 * state for indicating the currently-opened detail view on tablet devices.
 */
public class SessionsFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        MultiChoiceModeListener {

    private static final String TAG = makeLogTag(SessionsFragment.class);

    private static final String STATE_SELECTED_ID = "selectedId";

    private CursorAdapter mAdapter;
    private String mSelectedSessionId;
    private MenuItem mStarredMenuItem;
    private MenuItem mMapMenuItem;
    private MenuItem mShareMenuItem;
    private MenuItem mSocialStreamMenuItem;
    private boolean mHasSetEmptyText = false;
    private int mSessionQueryToken;

    private LinkedHashSet<Integer> mSelectedSessionPositions = new LinkedHashSet<Integer>();
    private Handler mHandler = new Handler();

    public interface Callbacks {
        /** Return true to select (activate) the session in the list, false otherwise. */
        public boolean onSessionSelected(String sessionId);
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public boolean onSessionSelected(String sessionId) {
            return true;
        }
    };

    private Callbacks mCallbacks = sDummyCallbacks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mSelectedSessionId = savedInstanceState.getString(STATE_SELECTED_ID);
        }

        reloadFromArguments(getArguments());
    }

    protected void reloadFromArguments(Bundle arguments) {
        // Teardown from previous arguments
        setListAdapter(null);

        // Load new arguments
        final Intent intent = BaseActivity.fragmentArgumentsToIntent(arguments);
        final Uri sessionsUri = intent.getData();

        if (sessionsUri == null) {
            return;
        }

        if (!ScheduleContract.Sessions.isSearchUri(sessionsUri)) {
            mAdapter = new SessionsAdapter(getActivity());
            mSessionQueryToken = SessionsQuery._TOKEN;

        } else {
            mAdapter = new SearchAdapter(getActivity());
            mSessionQueryToken = SearchQuery._TOKEN;
        }
        setListAdapter(mAdapter);

        // Force start background query to load sessions
        getLoaderManager().restartLoader(mSessionQueryToken, arguments, this);
    }

    public void setSelectedSessionId(String id) {
        mSelectedSessionId = id;
        if (mAdapter != null) {
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setBackgroundColor(Color.WHITE);
        final ListView listView = getListView();
        listView.setSelector(android.R.color.transparent);
        listView.setCacheColorHint(Color.WHITE);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (!mHasSetEmptyText) {
            // Could be a bug, but calling this twice makes it become visible
            // when it shouldn't
            // be visible.
            setEmptyText(getString(R.string.empty_sessions));
            mHasSetEmptyText = true;
        }

        ActionMode.setMultiChoiceMode(getListView(), getActivity(), this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new ClassCastException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
        activity.getContentResolver().registerContentObserver(
                ScheduleContract.Sessions.CONTENT_URI, true, mObserver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
        getActivity().getContentResolver().unregisterContentObserver(mObserver);
    }

    @Override
    public void onResume() {
        super.onResume();
        mHandler.post(mRefreshSessionsRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mRefreshSessionsRunnable);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelectedSessionId != null) {
            outState.putString(STATE_SELECTED_ID, mSelectedSessionId);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        final Cursor cursor = (Cursor) mAdapter.getItem(position);
        String sessionId = cursor.getString(cursor.getColumnIndex(
                ScheduleContract.Sessions.SESSION_ID));
        if (mCallbacks.onSessionSelected(sessionId)) {
            mSelectedSessionId = sessionId;
            mAdapter.notifyDataSetChanged();
        }
    }

    // LoaderCallbacks interface
    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        final Intent intent = BaseActivity.fragmentArgumentsToIntent(data);
        final Uri sessionsUri = intent.getData();
        Loader<Cursor> loader = null;
        if (id == SessionsQuery._TOKEN) {
            loader = new CursorLoader(getActivity(), sessionsUri, SessionsQuery.PROJECTION,
                    null, null, ScheduleContract.Sessions.DEFAULT_SORT);
        } else if (id == SearchQuery._TOKEN) {
            loader = new CursorLoader(getActivity(), sessionsUri, SearchQuery.PROJECTION, null,
                    null, ScheduleContract.Sessions.DEFAULT_SORT);
        }
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (getActivity() == null) {
            return;
        }

        int token = loader.getId();
        if (token == SessionsQuery._TOKEN || token == SearchQuery._TOKEN) {
            mAdapter.changeCursor(cursor);
            Bundle arguments = getArguments();
            
            if (arguments != null && arguments.containsKey("_uri")) {
                String uri = arguments.get("_uri").toString();
                
                if(uri != null && uri.contains("blocks")) {
                    String title = arguments.getString(Intent.EXTRA_TITLE);
                    if (title == null) {
                        title = (String) this.getActivity().getTitle();
                    }
                    EasyTracker.getTracker().trackView("Session Block: " + title);
                    LOGD("Tracker", "Session Block: " + title);
                }
            }
        } else {
            LOGD(TAG, "Query complete, Not Actionable: " + token);
            cursor.close();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    // MultiChoiceModeListener interface
    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        SessionsHelper helper = new SessionsHelper(getActivity());
        mode.finish();
        switch (item.getItemId()) {
            case R.id.menu_map: {
                // multiple selection not supported
                int position = mSelectedSessionPositions.iterator().next();
                Cursor cursor = (Cursor) mAdapter.getItem(position);
                String roomId = cursor.getString(SessionsQuery.ROOM_ID);
                helper.startMapActivity(roomId);

                String title = cursor.getString(SessionsQuery.TITLE);
                EasyTracker.getTracker().trackEvent(
                        "Session", "Mapped", title, 0L);
                LOGV(TAG, "Starred: " + title);
                
                return true;
            }
            case R.id.menu_star: {
                // multiple selection supported
                boolean starred = false;
                int numChanged = 0;
                for (int position : mSelectedSessionPositions) {
                    Cursor cursor = (Cursor) mAdapter.getItem(position);
                    String title = cursor.getString(SessionsQuery.TITLE);
                    
                    String sessionId = cursor.getString(SessionsQuery.SESSION_ID);
                    Uri sessionUri = ScheduleContract.Sessions.buildSessionUri(sessionId);
                    starred = cursor.getInt(SessionsQuery.STARRED) == 0;
                    helper.setSessionStarred(sessionUri, starred, title);
                    ++numChanged;
                    EasyTracker.getTracker().trackEvent(
                            "Session", starred ? "Starred" : "Unstarred", title, 0L);
                    LOGV(TAG, "Starred: " + title);
                }
                Toast.makeText(
                        getActivity(),
                        getResources().getQuantityString(starred
                                ? R.plurals.toast_added_to_schedule
                                : R.plurals.toast_removed_from_schedule, numChanged, numChanged),
                        Toast.LENGTH_SHORT).show();
                setSelectedSessionStarred(starred);
                return true;
            }
            case R.id.menu_share: {
                // multiple selection not supported
                int position = mSelectedSessionPositions.iterator().next();
                // On ICS+ devices, we normally won't reach this as ShareActionProvider will handle
                // sharing.
                Cursor cursor = (Cursor) mAdapter.getItem(position);
                new SessionsHelper(getActivity()).shareSession(getActivity(),
                        R.string.share_template,
                        cursor.getString(SessionsQuery.TITLE),
                        cursor.getString(SessionsQuery.HASHTAGS),
                        cursor.getString(SessionsQuery.URL));
                return true;
            }
            case R.id.menu_social_stream:
                StringBuilder hashtags = new StringBuilder();
                for (int position : mSelectedSessionPositions) {
                    Cursor cursor = (Cursor) mAdapter.getItem(position);
                    String term = cursor.getString(SessionsQuery.HASHTAGS);
                    if (!term.startsWith("#")) {
                        term = "#" + term;
                    }
                    if (hashtags.length() > 0) {
                        hashtags.append(" OR ");
                    }
                    hashtags.append(term);
                    
                    String title = cursor.getString(SessionsQuery.TITLE);
                    EasyTracker.getTracker().trackEvent(
                            "Session", "Mapped", title, 0L);
                    LOGV(TAG, "Starred: " + title);
                }

                helper.startSocialStream(hashtags.toString());
                return true;

            default:
                LOGW(TAG, "CAB unknown selection=" + item.getItemId());
                return false;
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(R.menu.sessions_context, menu);
        mStarredMenuItem = menu.findItem(R.id.menu_star);
        mMapMenuItem = menu.findItem(R.id.menu_map);
        mShareMenuItem = menu.findItem(R.id.menu_share);
        mSocialStreamMenuItem = menu.findItem(R.id.menu_social_stream);
        mSelectedSessionPositions.clear();
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {}

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
        if (checked) {
            mSelectedSessionPositions.add(position);
        } else {
            mSelectedSessionPositions.remove(position);
        }

        int numSelectedSessions = mSelectedSessionPositions.size();
        mode.setTitle(getResources().getQuantityString(
                R.plurals.title_selected_sessions,
                numSelectedSessions, numSelectedSessions));

        if (numSelectedSessions == 1) {
            // activate all the menu item
            mMapMenuItem.setVisible(true);
            mShareMenuItem.setVisible(true);
            mSocialStreamMenuItem.setVisible(true);
            mStarredMenuItem.setVisible(true);
            position = mSelectedSessionPositions.iterator().next();
            Cursor cursor = (Cursor) mAdapter.getItem(position);
            boolean starred = cursor.getInt(SessionsQuery.STARRED) != 0;
            setSelectedSessionStarred(starred);
        } else {
            mMapMenuItem.setVisible(false);
            mShareMenuItem.setVisible(false);
            mSocialStreamMenuItem.setVisible(false);
            boolean allStarred = true;
            boolean allUnstarred = true;
            for (int pos : mSelectedSessionPositions) {
                Cursor cursor = (Cursor) mAdapter.getItem(pos);
                boolean starred = cursor.getInt(SessionsQuery.STARRED) != 0;
                allStarred = allStarred && starred;
                allUnstarred = allUnstarred && !starred;
            }
            if (allStarred) {
                setSelectedSessionStarred(true);
                mStarredMenuItem.setVisible(true);
            } else if (allUnstarred) {
                setSelectedSessionStarred(false);
                mStarredMenuItem.setVisible(true);
            } else {
                mStarredMenuItem.setVisible(false);
            }
        }
    }

    private void setSelectedSessionStarred(boolean starred) {
        mStarredMenuItem.setTitle(starred
                ? R.string.description_remove_schedule
                : R.string.description_add_schedule);
        mStarredMenuItem.setIcon(starred
                ? R.drawable.ic_action_remove_schedule
                : R.drawable.ic_action_add_schedule);
    }

    private final Runnable mRefreshSessionsRunnable = new Runnable() {
        public void run() {
            if (mAdapter != null) {
                // This is used to refresh session title colors.
                mAdapter.notifyDataSetChanged();
            }

            // Check again on the next quarter hour, with some padding to
            // account for network
            // time differences.
            long nextQuarterHour = (SystemClock.uptimeMillis() / 900000 + 1) * 900000 + 5000;
            mHandler.postAtTime(mRefreshSessionsRunnable, nextQuarterHour);
        }
    };

    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (getActivity() == null) {
                return;
            }

            Loader<Cursor> loader = getLoaderManager().getLoader(mSessionQueryToken);
            if (loader != null) {
                loader.forceLoad();
            }
        }
    };

    /**
     * {@link CursorAdapter} that renders a {@link SessionsQuery}.
     */
    private class SessionsAdapter extends CursorAdapter {

        public SessionsAdapter(Context context) {
            super(context, null, false);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getActivity().getLayoutInflater().inflate(R.layout.list_item_session, parent,
                    false);
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            String sessionId = cursor.getString(SessionsQuery.SESSION_ID);
            if (sessionId == null) {
                return;
            }

            if (sessionId.equals(mSelectedSessionId)){
                UIUtils.setActivatedCompat(view, true);
            } else {
                UIUtils.setActivatedCompat(view, false);
            }
            final TextView titleView = (TextView) view.findViewById(R.id.session_title);
            final TextView subtitleView = (TextView) view.findViewById(R.id.session_subtitle);

            final String sessionTitle = cursor.getString(SessionsQuery.TITLE);
            titleView.setText(sessionTitle);

            // Format time block this session occupies
            final long blockStart = cursor.getLong(SessionsQuery.BLOCK_START);
            final long blockEnd = cursor.getLong(SessionsQuery.BLOCK_END);
            final String roomName = cursor.getString(SessionsQuery.ROOM_NAME);
            final String subtitle = formatSessionSubtitle(
                    sessionTitle, blockStart, blockEnd, roomName, context);

            final boolean starred = cursor.getInt(SessionsQuery.STARRED) != 0;
            view.findViewById(R.id.indicator_in_schedule).setVisibility(
                    starred ? View.VISIBLE : View.INVISIBLE);

            final boolean hasLivestream = !TextUtils.isEmpty(
                    cursor.getString(SessionsQuery.LIVESTREAM_URL));

            // Show past/present/future and livestream status for this block.
            UIUtils.updateTimeAndLivestreamBlockUI(context,
                    blockStart, blockEnd, hasLivestream,
                    view.findViewById(R.id.list_item_session), titleView, subtitleView, subtitle);
        }
    }

    /**
     * {@link CursorAdapter} that renders a {@link SearchQuery}.
     */
    private class SearchAdapter extends CursorAdapter {
        public SearchAdapter(Context context) {
            super(context, null, false);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getActivity().getLayoutInflater().inflate(R.layout.list_item_session, parent,
                    false);
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            UIUtils.setActivatedCompat(view, cursor.getString(SessionsQuery.SESSION_ID)
                    .equals(mSelectedSessionId));

            ((TextView) view.findViewById(R.id.session_title)).setText(cursor
                    .getString(SearchQuery.TITLE));

            final String snippet = cursor.getString(SearchQuery.SEARCH_SNIPPET);

            final Spannable styledSnippet = buildStyledSnippet(snippet);
            ((TextView) view.findViewById(R.id.session_subtitle)).setText(styledSnippet);

            final boolean starred = cursor.getInt(SearchQuery.STARRED) != 0;
            view.findViewById(R.id.indicator_in_schedule).setVisibility(
                    starred ? View.VISIBLE : View.INVISIBLE);
        }
    }

    /**
     * {@link com.google.android.apps.iosched.provider.ScheduleContract.Sessions}
     * query parameters.
     */
    private interface SessionsQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Sessions.SESSION_ID,
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.SESSION_STARRED,
                ScheduleContract.Blocks.BLOCK_START,
                ScheduleContract.Blocks.BLOCK_END,
                ScheduleContract.Rooms.ROOM_NAME,
                ScheduleContract.Rooms.ROOM_ID,
                ScheduleContract.Sessions.SESSION_HASHTAGS,
                ScheduleContract.Sessions.SESSION_URL,
                ScheduleContract.Sessions.SESSION_LIVESTREAM_URL,
        };

        int _ID = 0;
        int SESSION_ID = 1;
        int TITLE = 2;
        int STARRED = 3;
        int BLOCK_START = 4;
        int BLOCK_END = 5;
        int ROOM_NAME = 6;
        int ROOM_ID = 7;
        int HASHTAGS = 8;
        int URL = 9;
        int LIVESTREAM_URL = 10;
    }

    /**
     * {@link com.google.android.apps.iosched.provider.ScheduleContract.Sessions}
     * search query parameters.
     */
    private interface SearchQuery {
        int _TOKEN = 0x3;

        String[] PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Sessions.SESSION_ID,
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.SESSION_STARRED,
                ScheduleContract.Sessions.SEARCH_SNIPPET,
                ScheduleContract.Sessions.SESSION_LEVEL,
                ScheduleContract.Rooms.ROOM_NAME,
                ScheduleContract.Rooms.ROOM_ID,
                ScheduleContract.Sessions.SESSION_HASHTAGS,
                ScheduleContract.Sessions.SESSION_URL
        };
        int _ID = 0;
        int SESSION_ID = 1;
        int TITLE = 2;
        int STARRED = 3;
        int SEARCH_SNIPPET = 4;
        int LEVEL = 5;
        int ROOM_NAME = 6;
        int ROOM_ID = 7;
        int HASHTAGS = 8;
        int URL = 9;
    }
}
