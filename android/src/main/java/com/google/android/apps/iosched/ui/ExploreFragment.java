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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
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

import com.google.analytics.tracking.android.EasyTracker;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.ui.TracksAdapter.TracksQuery;
import com.google.android.apps.iosched.ui.tablet.SessionsSandboxMultiPaneActivity;
import com.google.android.apps.iosched.ui.tablet.TracksDropdownFragment;
import com.google.android.apps.iosched.util.UIUtils;

/**
 * A simple {@link ListFragment} that renders a list of tracks with available
 * sessions or sandbox companies (depending on {@link ExploreFragment#VIEW_TYPE}) using a
 * {@link TracksAdapter}.
 */
public class ExploreFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private TracksAdapter mAdapter;
    private View mEmptyView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(
                R.layout.fragment_list_with_empty_container_inset, container, false);
        mEmptyView = rootView.findViewById(android.R.id.empty);
        inflater.inflate(R.layout.empty_waiting_for_sync, (ViewGroup) mEmptyView, true);
        return rootView;
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.setBackgroundColor(Color.WHITE);

        final ListView listView = getListView();
        listView.setSelector(android.R.color.transparent);
        listView.setCacheColorHint(Color.WHITE);
        addMapHeaderView();

        mAdapter = new TracksAdapter(getActivity(), false);
        setListAdapter(mAdapter);

        // Override default ListView empty-view handling
        listView.setEmptyView(null);
        mEmptyView.setVisibility(View.VISIBLE);
        mAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                if (mAdapter.getCount() > 0) {
                    mEmptyView.setVisibility(View.GONE);
                    mAdapter.unregisterDataSetObserver(this);
                }
            }
        });
    }

    private void addMapHeaderView() {
        ListView listView = getListView();
        final Context context = listView.getContext();
        View mapHeaderContainerView = LayoutInflater.from(context).inflate(
                R.layout.list_item_track_map, listView, false);

        View mapButton = mapHeaderContainerView.findViewById(R.id.map_button);
        mapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Launch map of conference venue
                EasyTracker.getTracker().sendEvent(
                        "Explore Tab", "Click", "Map", 0L);
                startActivity(new Intent(context,
                        UIUtils.getMapActivityClass(getActivity())));
            }
        });

        listView.addHeaderView(mapHeaderContainerView);
        listView.setHeaderDividersEnabled(false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // As of support library r12, calling initLoader for a fragment in a FragmentPagerAdapter
        // in the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we do this in onActivityCreated.
        getLoaderManager().initLoader(TracksQuery._TOKEN, null, this);
    }

    private final ContentObserver mObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (getActivity() == null) {
                return;
            }

            getLoaderManager().restartLoader(TracksQuery._TOKEN, null, ExploreFragment.this);
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
        final Cursor cursor = (Cursor) mAdapter.getItem(position - 1); // - 1 to account for header

        String trackId = ScheduleContract.Tracks.ALL_TRACK_ID;
        int trackMeta = ScheduleContract.Tracks.TRACK_META_NONE;

        if (cursor != null) {
            trackId = cursor.getString(TracksAdapter.TracksQuery.TRACK_ID);
            trackMeta = cursor.getInt(TracksAdapter.TracksQuery.TRACK_META);
        }

        final Intent intent = new Intent(Intent.ACTION_VIEW);
        final Uri trackUri = ScheduleContract.Tracks.buildTrackUri(trackId);
        intent.setData(trackUri);

        if (trackMeta == ScheduleContract.Tracks.TRACK_META_SANDBOX_OFFICE_HOURS_ONLY) {
            intent.putExtra(SessionsSandboxMultiPaneActivity.EXTRA_DEFAULT_VIEW_TYPE,
                    TracksDropdownFragment.VIEW_TYPE_SANDBOX);
        } else if (trackMeta == ScheduleContract.Tracks.TRACK_META_OFFICE_HOURS_ONLY) {
            intent.putExtra(SessionsSandboxMultiPaneActivity.EXTRA_DEFAULT_VIEW_TYPE,
                    TracksDropdownFragment.VIEW_TYPE_OFFICE_HOURS);
        }

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

        mAdapter.setHasAllItem(true);
        mAdapter.swapCursor(cursor);
        if (cursor.getCount() > 0) {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }
}
