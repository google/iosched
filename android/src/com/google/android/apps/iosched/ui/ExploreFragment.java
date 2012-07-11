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
import com.google.android.apps.iosched.ui.TracksAdapter.TracksQuery;
import com.google.android.apps.iosched.util.UIUtils;

import com.actionbarsherlock.app.SherlockListFragment;

import android.app.Activity;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * A simple {@link ListFragment} that renders a list of tracks and a map button at the top.
 */
public class ExploreFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private TracksAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAdapter = new TracksAdapter(getActivity());
        setListAdapter(mAdapter);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final ListView listView = getListView();
        listView.setSelector(android.R.color.transparent);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // As of support library r8, calling initLoader for a fragment in a
        // FragmentPagerAdapter in the fragment's onCreate may cause the same LoaderManager to be
        // dealt to multiple fragments because their mIndex is -1 (haven't been added to the
        // activity yet). Thus, we do this in onActivityCreated.
        getLoaderManager().initLoader(TracksQuery._TOKEN, null, this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(
                R.layout.fragment_list_with_empty_container, container, false);
        root.setBackgroundColor(Color.WHITE);
        return root;
    }

    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (getActivity() == null) {
                return;
            }

            Loader<Cursor> loader = getLoaderManager().getLoader(TracksQuery._TOKEN);
            if (loader != null) {
                loader.forceLoad();
            }
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        activity.getContentResolver().registerContentObserver(
                ScheduleContract.Sessions.CONTENT_URI, true, mObserver);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().getContentResolver().unregisterContentObserver(mObserver);
    }

    /** {@inheritDoc} */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mAdapter.isMapItem(position)) {
            // Launch map of conference venue
            EasyTracker.getTracker().trackEvent(
                    "Home Screen Dashboard", "Click", "Map", 0L);
            startActivity(new Intent(getActivity(),
                    UIUtils.getMapActivityClass(getActivity())));
            return;
        }

        final Cursor cursor = (Cursor) mAdapter.getItem(position);
        final String trackId;

        if (cursor != null) {
            trackId = cursor.getString(TracksAdapter.TracksQuery.TRACK_ID);
        } else {
            trackId = ScheduleContract.Tracks.ALL_TRACK_ID;
        }

        final Intent intent = new Intent(Intent.ACTION_VIEW);
        final Uri trackUri = ScheduleContract.Tracks.buildTrackUri(trackId);

        intent.setData(trackUri);
        startActivity(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        Intent intent = BaseActivity.fragmentArgumentsToIntent(getArguments());
        Uri tracksUri = intent.getData();
        if (tracksUri == null) {
            tracksUri = ScheduleContract.Tracks.CONTENT_URI;
        }

        // Filter our tracks query to only include those with valid results
        String[] projection = TracksAdapter.TracksQuery.PROJECTION;
        String selection = null;

        return new CursorLoader(getActivity(), tracksUri, projection, selection, null,
                ScheduleContract.Tracks.DEFAULT_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (getActivity() == null) {
            return;
        }

        mAdapter.setHasMapItem(true);
        mAdapter.setHasAllItem(true);
        mAdapter.changeCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
