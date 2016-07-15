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
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.explore.ExploreSessionsModel.ExploreSessionsQuery;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.BaseActivity;
import com.google.samples.apps.iosched.ui.widget.DrawShadowFrameLayout;
import com.google.samples.apps.iosched.util.LogUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import java.lang.ref.WeakReference;

/**
 * A fragment that shows the sessions based on the specific {@code Uri} that is part of the
 * arguments.
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

    /**
     * The delay before actual re-querying in milli seconds.
     */
    private static final long QUERY_UPDATE_DELAY_MILLIS = 100;

    private RecyclerView mSessionList;

    private View mEmptyView;

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
     * Boolean that indicates whether the collectionView data is being fully reloaded in the case of
     * filters and other query arguments changing VS just a data refresh.
     */
    private boolean mFullReload = true;

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.explore_sessions_frag, container, false);
        mSessionList = (RecyclerView) rootView.findViewById(R.id.sessions_list);
        mEmptyView = rootView.findViewById(android.R.id.empty);
        getActivity().overridePendingTransition(0, 0);
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getLoaderManager().initLoader(TAG_METADATA_TOKEN, null, this);
        // Setup the tag filters
        if (savedInstanceState != null) {
            mCurrentUri = savedInstanceState.getParcelable(STATE_CURRENT_URI);
            mSessionQueryToken = savedInstanceState.getInt(STATE_SESSION_QUERY_TOKEN);
            mShowLiveStreamedSessions = savedInstanceState
                    .getBoolean(STATE_SHOW_LIVESTREAMED_SESSIONS);
            if (mSessionQueryToken > 0) {
                // Only if this is a config change should we initLoader(), to reconnect with an
                // existing loader. Otherwise, the loader will be initStaticDataAndObservers'd
                // when reloadFromArguments
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

    @Override
    public void onResume() {
        super.onResume();
        getActivity().invalidateOptionsMenu();

        final DrawShadowFrameLayout drawShadowFrameLayout =
                (DrawShadowFrameLayout) getActivity().findViewById(R.id.main_content);
        if (drawShadowFrameLayout != null) {
            // configure session fragment's top clearance to take our overlaid Toolbar into account.
            drawShadowFrameLayout.setShadowTopOffset(UIUtils.calculateActionBarSize(getActivity()));
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case ExploreSessionsModel.ExploreSessionsQuery.NORMAL_TOKEN:
                return new CursorLoader(getActivity(),
                        mCurrentUri, ExploreSessionsModel.ExploreSessionsQuery.NORMAL_PROJECTION,
                        mShowLiveStreamedSessions ?
                                ScheduleContract.Sessions.LIVESTREAM_OR_YOUTUBE_URL_SELECTION :
                                null,
                        null,
                        ScheduleContract.Sessions.SORT_BY_TYPE_THEN_TIME);
            case ExploreSessionsModel.ExploreSessionsQuery.SEARCH_TOKEN:
                return new CursorLoader(getActivity(),
                        mCurrentUri, ExploreSessionsModel.ExploreSessionsQuery.SEARCH_PROJECTION,
                        mShowLiveStreamedSessions ?
                                ScheduleContract.Sessions.LIVESTREAM_OR_YOUTUBE_URL_SELECTION :
                                null,
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
        mSessionList.setAdapter(null);
        mSessionsAdapter = null;
        final ExploreSessionsModel model = new ExploreSessionsModel(cursor, getActivity());
        if (model.getSessionData() == null || model.getSessionData().isEmpty()) {
            mEmptyView.setVisibility(View.VISIBLE);
            return;
        }

        final GridLayoutManager glm = (GridLayoutManager) mSessionList.getLayoutManager();
        mSessionsAdapter = SessionsAdapter.createVerticalGrid(
                getActivity(), model.getSessionData(), glm.getSpanCount());
        glm.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(final int position) {
                if (mSessionsAdapter == null) {
                    return 0;
                }
                return mSessionsAdapter.getSpanSize(position);
            }
        });
        mSessionList.setAdapter(mSessionsAdapter);
        mEmptyView.setVisibility(View.GONE);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) { }

    public void reloadFromArguments(Bundle bundle) {
        Uri oldUri = mCurrentUri;
        int oldSessionQueryToken = mSessionQueryToken;
        boolean oldShowLivestreamedSessions = mShowLiveStreamedSessions;
        mCurrentUri = bundle.getParcelable("_uri");

        if (ScheduleContract.Sessions.isSearchUri(mCurrentUri)) {
            mSessionQueryToken = ExploreSessionsModel.ExploreSessionsQuery.SEARCH_TOKEN;
        } else {
            mSessionQueryToken = ExploreSessionsModel.ExploreSessionsQuery.NORMAL_TOKEN;
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
            switch (msg.what) {
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