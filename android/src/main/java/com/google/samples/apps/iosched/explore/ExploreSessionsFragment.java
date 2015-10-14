/*
 * Copyright 2015 Google Inc. All rights reserved.
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
package com.google.samples.apps.iosched.explore;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.os.Handler;
import android.os.Message;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.session.SessionDetailActivity;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.ui.widget.CollectionView;
import com.google.samples.apps.iosched.ui.widget.CollectionViewCallbacks;
import com.google.samples.apps.iosched.ui.widget.DrawShadowFrameLayout;
import com.google.samples.apps.iosched.util.ImageLoader;
import com.google.samples.apps.iosched.util.LogUtils;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import java.lang.ref.WeakReference;
import java.util.Date;

import static com.google.samples.apps.iosched.util.LogUtils.LOGE;

/**
 * A fragment that shows the sessions based on the specific {@code Uri} that is
 * part of the arguments.
 */
public class ExploreSessionsFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = LogUtils.makeLogTag(ExploreSessionsFragment.class);
    private static final int TAG_METADATA_TOKEN = 0x8;
    private static final String STATE_CURRENT_URI =
            "com.google.samples.apps.iosched.explore.STATE_CURRENT_URI";
    private static final String STATE_SESSION_QUERY_TOKEN =
            "com.google.samples.apps.iosched.explore.STATE_SESSION_QUERY_TOKEN";
    private static final String STATE_SHOW_LIVESTREAMED_SESSIONS =
            "com.google.samples.apps.iosched.explore.EXTRA_SHOW_LIVESTREAMED_SESSIONS";

    public static final String EXTRA_SHOW_LIVESTREAMED_SESSIONS =
            "com.google.samples.apps.iosched.explore.EXTRA_SHOW_LIVESTREAMED_SESSIONS";

    /** The delay before actual re-querying in milli seconds. */
    private static final long QUERY_UPDATE_DELAY_MILLIS = 100;

    private ImageLoader mImageLoader;
    private CollectionView mCollectionView;
    private View mEmptyView;
    private int mDisplayColumns;
    private SessionsAdapter mSessionsAdapter;
    private Uri mCurrentUri;
    private int mSessionQueryToken;
    private TagMetadata mTagMetadata;
    private SearchHandler mSearchHandler = new SearchHandler(this);

    /**
     * Whether we should limit our selection to live streamed events.
     */
    private boolean mShowLiveStreamedSessions;
    /**
     * Boolean that indicates whether the collectionView data is being fully reloaded in the
     * case of filters and other query arguments changing VS just a data refresh.
     */
    private boolean mFullReload = true;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.explore_sessions_frag, container, false);
        mCollectionView = (CollectionView) rootView.findViewById(R.id.collection_view);
        mCollectionView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (mSessionsAdapter != null) {
                    mSessionsAdapter.handleOnClick(position);
                }
            }
        });
        mEmptyView = rootView.findViewById(android.R.id.empty);
        getActivity().overridePendingTransition(0, 0);
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImageLoader = new ImageLoader(getActivity(), R.drawable.io_logo);
        mDisplayColumns = getResources().getInteger(R.integer.deprecated_explore_sessions_columns);
        getLoaderManager().initLoader(TAG_METADATA_TOKEN, null, this);
        // Setup the tag filters
        if (savedInstanceState != null) {
            mCurrentUri = savedInstanceState.getParcelable(STATE_CURRENT_URI);
            mSessionQueryToken = savedInstanceState.getInt(STATE_SESSION_QUERY_TOKEN);
            mShowLiveStreamedSessions = savedInstanceState
                    .getBoolean(STATE_SHOW_LIVESTREAMED_SESSIONS);
            if (mSessionQueryToken > 0) {
                // Only if this is a config change should we initLoader(), to reconnect with an
                // existing loader. Otherwise, the loader will be init'd when reloadFromArguments
                // is called.
                getLoaderManager().initLoader(mSessionQueryToken, null, this);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(STATE_CURRENT_URI, mCurrentUri);
        outState.putInt(STATE_SESSION_QUERY_TOKEN, mSessionQueryToken);
        outState.putBoolean(STATE_SHOW_LIVESTREAMED_SESSIONS, mShowLiveStreamedSessions);
    }

    private void setContentTopClearance(int clearance) {
        if (mCollectionView != null) {
            mCollectionView.setContentTopClearance(clearance);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().invalidateOptionsMenu();
        // configure session fragment's top clearance to take our overlaid controls (Action Bar
        // and spinner box) into account.
        int actionBarSize = UIUtils.calculateActionBarSize(getActivity());
        DrawShadowFrameLayout drawShadowFrameLayout =
                (DrawShadowFrameLayout) getActivity().findViewById(R.id.main_content);
        if (drawShadowFrameLayout != null) {
            drawShadowFrameLayout.setShadowTopOffset(actionBarSize);
        }
        setContentTopClearance(actionBarSize
                + getResources().getDimensionPixelSize(R.dimen.explore_grid_padding));
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case ExploreSessionsQuery.NORMAL_TOKEN:
                return new CursorLoader(getActivity(),
                        mCurrentUri, ExploreSessionsQuery.NORMAL_PROJECTION,
                        mShowLiveStreamedSessions ?
                                ScheduleContract.Sessions.LIVESTREAM_OR_YOUTUBE_URL_SELECTION : null,
                        null,
                        ScheduleContract.Sessions.SORT_BY_TYPE_THEN_TIME);
            case ExploreSessionsQuery.SEARCH_TOKEN:
                return new CursorLoader(getActivity(),
                        mCurrentUri, ExploreSessionsQuery.SEARCH_PROJECTION,
                        mShowLiveStreamedSessions ?
                                ScheduleContract.Sessions.LIVESTREAM_OR_YOUTUBE_URL_SELECTION : null,
                        null,
                        ScheduleContract.Sessions.SORT_BY_TYPE_THEN_TIME);
            case TAG_METADATA_TOKEN:
                return TagMetadata.createCursorLoader(getActivity());
            default:
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case ExploreSessionsQuery.NORMAL_TOKEN: // fall through
            case ExploreSessionsQuery.SEARCH_TOKEN:
                reloadSessionData(cursor);
                break;
            case TAG_METADATA_TOKEN:
                mTagMetadata = new TagMetadata(cursor);
                break;
            default:
                cursor.close();
        }
    }

    private void reloadSessionData(Cursor cursor) {
        mEmptyView.setVisibility(cursor.getCount() == 0 ? View.VISIBLE : View.GONE);
        if (mSessionsAdapter == null) {
            mSessionsAdapter = new SessionsAdapter(cursor);
        } else {
            Cursor oldCursor = mSessionsAdapter.swapCursor(cursor);
            // If the cursor is the same as the old one, swapCursor returns a null.
            if (oldCursor == null) {
                mFullReload = false;
            }
        }
        Parcelable state = null;
        if (!mFullReload) {
            state = mCollectionView.onSaveInstanceState();
        }
        mCollectionView.setCollectionAdapter(mSessionsAdapter);
        mCollectionView.updateInventory(mSessionsAdapter.getInventory(), mFullReload);
        if (state != null) {
            mCollectionView.onRestoreInstanceState(state);
        }
        mFullReload = false;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    public void reloadFromArguments(Bundle bundle) {
        Uri oldUri = mCurrentUri;
        int oldSessionQueryToken = mSessionQueryToken;
        boolean oldShowLivestreamedSessions = mShowLiveStreamedSessions;
        mCurrentUri = bundle.getParcelable("_uri");

        if (ScheduleContract.Sessions.isSearchUri(mCurrentUri)) {
            mSessionQueryToken = ExploreSessionsQuery.SEARCH_TOKEN;
        } else {
            mSessionQueryToken = ExploreSessionsQuery.NORMAL_TOKEN;
        }
        mShowLiveStreamedSessions = bundle.getBoolean(EXTRA_SHOW_LIVESTREAMED_SESSIONS, false);

        if ((oldUri != null && oldUri.equals(mCurrentUri)) &&
                oldSessionQueryToken == mSessionQueryToken &&
                oldShowLivestreamedSessions == mShowLiveStreamedSessions) {
            mFullReload = false;
            getLoaderManager().initLoader(mSessionQueryToken, null, this);
        } else {
            // We need to re-run the query
            mFullReload = true;
            getLoaderManager().restartLoader(mSessionQueryToken, null, this);
        }
    }

    public void requestQueryUpdate(String query) {
        mSearchHandler.removeMessages(SearchHandler.MESSAGE_QUERY_UPDATE);
        mSearchHandler.sendMessageDelayed(Message.obtain(mSearchHandler,
                        SearchHandler.MESSAGE_QUERY_UPDATE, query), QUERY_UPDATE_DELAY_MILLIS);
    }

    private class SessionsAdapter extends CursorAdapter implements CollectionViewCallbacks {

        public SessionsAdapter(Cursor cursor) {
            super(getActivity(), cursor, 0);
        }

        /**
         * Returns a new instance of {@link CollectionView.Inventory}. It always contains a single
         * {@link CollectionView.InventoryGroup}.
         *
         * @return A new instance of {@link CollectionView.Inventory}.
         */
        public CollectionView.Inventory getInventory() {
            CollectionView.Inventory inventory = new CollectionView.Inventory();
            inventory.addGroup(new CollectionView.InventoryGroup(ExploreSessionsQuery.NORMAL_TOKEN)
                    .setDisplayCols(mDisplayColumns)
                    .setItemCount(getCursor().getCount())
                    .setDataIndexStart(0)
                    .setShowHeader(false));
            return inventory;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.explore_sessions_list_item,
                    parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ImageView thumbnailView = (ImageView) view.findViewById(R.id.thumbnail);
            ImageView inScheduleIndicator =
                    (ImageView) view.findViewById(R.id.indicator_in_schedule);
            TextView titleView = (TextView) view.findViewById(R.id.title);
            TextView infoView = (TextView) view.findViewById(R.id.info_view);
            TextView sessionTypeView = (TextView) view.findViewById(R.id.session_type_text);

            titleView.setText(cursor.getString(ExploreSessionsQuery.TITLE));
            // Format: Day 1/ 9:00 AM - 11:00 AM/ Room 1
            String room = cursor.getString(ExploreSessionsQuery.ROOM_NAME);
            long startTime = cursor.getLong(ExploreSessionsQuery.SESSION_START);
            long endTime = cursor.getLong(ExploreSessionsQuery.SESSION_END);

            int day = UIUtils.startTimeToDayIndex(startTime);
            if (day == 0) {
                // We have a problem!
                LOGE(TAG, "Invalid Day for Session: " +
                        cursor.getString(ExploreSessionsQuery.SESSION_ID) + " " +
                        " startTime " + new Date(startTime));
            }

            String tags = cursor.getString(ExploreSessionsQuery.TAGS);
            if (mTagMetadata != null) {
                TagMetadata.Tag groupTag = mTagMetadata.getSessionGroupTag(tags.split(","));
                sessionTypeView.setText(groupTag == null ? "" : groupTag.getName());
            }
            String infoText = "";
            if (day != 0) {
                final Date startDate = new Date(startTime);
                infoText = getString(R.string.explore_sessions_show_day_hour_and_room,
                        TimeUtils.formatShortDate(getActivity(), startDate),
                        getString(R.string.explore_sessions_show_day_n, day),
                        TimeUtils.formatShortTime(getActivity(), startDate),
                        TimeUtils.formatShortTime(getActivity(), new Date(endTime)),
                        room != null ? room : context.getString(R.string.unknown_room));
            }
            infoView.setText(infoText);

            String thumbUrl = cursor.getString(ExploreSessionsQuery.PHOTO_URL);
            view.setTag(cursor.getString(ExploreSessionsQuery.SESSION_ID));
            if (TextUtils.isEmpty(thumbUrl)) {
                thumbnailView.setImageResource(R.drawable.io_logo);
            } else {
                mImageLoader.loadImage(thumbUrl, thumbnailView);
            }
            inScheduleIndicator.setVisibility(
                    cursor.getLong(ExploreSessionsQuery.IN_MY_SCHEDULE) == 1L ? View.VISIBLE
                            : View.GONE);
        }

        @Override
        public View newCollectionHeaderView(Context context, int groupId, ViewGroup parent) {
            return LayoutInflater.from(context)
                    .inflate(R.layout.list_item_explore_header, parent, false);
        }

        @Override
        public void bindCollectionHeaderView(Context context, View view, int groupId,
                                             String headerLabel, Object headerTag) {
            ((TextView) view.findViewById(android.R.id.text1)).setText(headerLabel);
        }

        @Override
        public View newCollectionItemView(Context context, int groupId, ViewGroup parent) {
            return newView(context, null, parent);
        }

        @Override
        public void bindCollectionItemView(Context context, View view, int groupId,
                                           int indexInGroup, int dataIndex, Object tag) {
            setCursorPosition(indexInGroup);
            bindView(view, context, getCursor());
        }

        private void setCursorPosition(int position) {
            if (!getCursor().moveToPosition(position)) {
                throw new IllegalStateException("Invalid position: " + position);
            }
        }

        public void handleOnClick(int position) {
            setCursorPosition(position);
            String sessionId = getCursor().getString(ExploreSessionsQuery.SESSION_ID);
            if (sessionId != null) {
                Uri data = ScheduleContract.Sessions.buildSessionUri(sessionId);
                Intent intent = new Intent(ExploreSessionsFragment.this.getActivity(),
                        SessionDetailActivity.class);
                intent.setData(data);
                startActivity(intent);
            }
        }
    }

    private interface ExploreSessionsQuery {
        int NORMAL_TOKEN = 0x1;
        int SEARCH_TOKEN = 0x3;
        String[] NORMAL_PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Sessions.SESSION_ID,
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.SESSION_START,
                ScheduleContract.Sessions.SESSION_END,
                ScheduleContract.Rooms.ROOM_NAME,
                ScheduleContract.Sessions.SESSION_URL,
                ScheduleContract.Sessions.SESSION_TAGS,
                ScheduleContract.Sessions.SESSION_PHOTO_URL,
                ScheduleContract.Sessions.SESSION_IN_MY_SCHEDULE,
        };
        String[] SEARCH_PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Sessions.SESSION_ID,
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.SESSION_START,
                ScheduleContract.Sessions.SESSION_END,
                ScheduleContract.Rooms.ROOM_NAME,
                ScheduleContract.Sessions.SESSION_URL,
                ScheduleContract.Sessions.SESSION_TAGS,
                ScheduleContract.Sessions.SESSION_PHOTO_URL,
                ScheduleContract.Sessions.SESSION_IN_MY_SCHEDULE,
                ScheduleContract.Sessions.SEARCH_SNIPPET,
        };
        int _ID = 0;
        int SESSION_ID = 1;
        int TITLE = 2;
        int SESSION_START = 3;
        int SESSION_END = 4;
        int ROOM_NAME = 5;
        int URL = 6;
        int TAGS = 7;
        int PHOTO_URL = 8;
        int IN_MY_SCHEDULE = 9;
        int SEARCH_SNIPPET = 10;
    }

    /**
     * {@code Handler} that sends search queries to the ExploreSessionsFragment.
     */
    private static class SearchHandler extends Handler {

        public static final int MESSAGE_QUERY_UPDATE = 1;

        private final WeakReference<ExploreSessionsFragment> mFragmentReference;

        SearchHandler(ExploreSessionsFragment fragment) {
            mFragmentReference = new WeakReference<>(fragment);
        }

        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MESSAGE_QUERY_UPDATE:
                    String query = (String) msg.obj;
                    ExploreSessionsFragment instance = mFragmentReference.get();
                    if (instance != null) {
                        instance.reloadFromArguments(BaseActivity.intentToFragmentArguments(
                                new Intent(Intent.ACTION_SEARCH,
                                        ScheduleContract.Sessions.buildSearchUri(query))));
                    }
                    break;
                default:
                    break;
            }
        }

    }
}