/*
 * Copyright 2011 Google Inc.
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

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.util.AnalyticsUtils;
import com.google.android.apps.iosched.util.NotifyingAsyncQueryHandler;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

/**
 * A simple {@link ListFragment} that renders a list of tracks with available sessions or vendors
 * (depending on {@link TracksFragment#EXTRA_NEXT_TYPE}) using a {@link TracksAdapter}.
 */
public class TracksFragment extends ListFragment implements
        NotifyingAsyncQueryHandler.AsyncQueryListener {

    public static final String EXTRA_NEXT_TYPE = "com.google.android.iosched.extra.NEXT_TYPE";

    public static final String NEXT_TYPE_SESSIONS = "sessions";
    public static final String NEXT_TYPE_VENDORS = "vendors";

    private TracksAdapter mAdapter;
    private NotifyingAsyncQueryHandler mHandler;
    private String mNextType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = BaseActivity.fragmentArgumentsToIntent(getArguments());
        final Uri tracksUri = intent.getData();
        mNextType = intent.getStringExtra(EXTRA_NEXT_TYPE);

        mAdapter = new TracksAdapter(getActivity());
        setListAdapter(mAdapter);

        // Filter our tracks query to only include those with valid results
        String[] projection = TracksAdapter.TracksQuery.PROJECTION;
        String selection = null;
        if (NEXT_TYPE_SESSIONS.equals(mNextType)) {
            // Only show tracks with at least one session
            projection = TracksAdapter.TracksQuery.PROJECTION_WITH_SESSIONS_COUNT;
            selection = ScheduleContract.Tracks.SESSIONS_COUNT + ">0";
            AnalyticsUtils.getInstance(getActivity()).trackPageView("/Tracks");

        } else if (NEXT_TYPE_VENDORS.equals(mNextType)) {
            // Only show tracks with at least one vendor
            projection = TracksAdapter.TracksQuery.PROJECTION_WITH_VENDORS_COUNT;
            selection = ScheduleContract.Tracks.VENDORS_COUNT + ">0";
            AnalyticsUtils.getInstance(getActivity()).trackPageView("/Sandbox");
        }

        // Start background query to load tracks
        mHandler = new NotifyingAsyncQueryHandler(getActivity().getContentResolver(), this);
        mHandler.startQuery(tracksUri, projection, selection, null,
                ScheduleContract.Tracks.DEFAULT_SORT);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_list_with_spinner, null);

        // For some reason, if we omit this, NoSaveStateFrameLayout thinks we are
        // FILL_PARENT / WRAP_CONTENT, making the progress bar stick to the top of the activity.
        root.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT,
                ViewGroup.LayoutParams.FILL_PARENT));
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    }

    /** {@inheritDoc} */
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        if (getActivity() == null) {
            return;
        }

        getActivity().startManagingCursor(cursor);
        mAdapter.setHasAllItem(true);
        mAdapter.setIsSessions(TracksFragment.NEXT_TYPE_SESSIONS.equals(mNextType));
        mAdapter.changeCursor(cursor);
    }

    /** {@inheritDoc} */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        final Cursor cursor = (Cursor) mAdapter.getItem(position);
        final String trackId;

        if (cursor != null) {
            trackId = cursor.getString(TracksAdapter.TracksQuery.TRACK_ID);
        } else {
            trackId = ScheduleContract.Tracks.ALL_TRACK_ID;
        }

        final Intent intent = new Intent(Intent.ACTION_VIEW);
        final Uri trackUri = ScheduleContract.Tracks.buildTrackUri(trackId);
        intent.putExtra(SessionDetailFragment.EXTRA_TRACK, trackUri);

        if (NEXT_TYPE_SESSIONS.equals(mNextType)) {
            if (cursor == null) {
                intent.setData(ScheduleContract.Sessions.CONTENT_URI);
            } else {
                intent.setData(ScheduleContract.Tracks.buildSessionsUri(trackId));
            }
        } else if (NEXT_TYPE_VENDORS.equals(mNextType)) {
            if (cursor == null) {
                intent.setData(ScheduleContract.Vendors.CONTENT_URI);
            } else {
                intent.setData(ScheduleContract.Tracks.buildVendorsUri(trackId));
            }
        }

        ((BaseActivity) getActivity()).openActivityOrFragment(intent);

        getListView().setItemChecked(position, true);
    }
}
