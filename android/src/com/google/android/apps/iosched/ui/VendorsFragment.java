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
import com.google.android.apps.iosched.util.ActivityHelper;
import com.google.android.apps.iosched.util.AnalyticsUtils;
import com.google.android.apps.iosched.util.NotifyingAsyncQueryHandler;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.text.Spannable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import static com.google.android.apps.iosched.util.UIUtils.buildStyledSnippet;

/**
 * A {@link ListFragment} showing a list of sandbox comapnies.
 */
public class VendorsFragment extends ListFragment implements
        NotifyingAsyncQueryHandler.AsyncQueryListener {

    private static final String STATE_CHECKED_POSITION = "checkedPosition";

    private Uri mTrackUri;
    private Cursor mCursor;
    private CursorAdapter mAdapter;
    private int mCheckedPosition = -1;
    private boolean mHasSetEmptyText = false;

    private NotifyingAsyncQueryHandler mHandler;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mHandler = new NotifyingAsyncQueryHandler(getActivity().getContentResolver(), this);
        reloadFromArguments(getArguments());
    }

    public void reloadFromArguments(Bundle arguments) {
        // Teardown from previous arguments
        if (mCursor != null) {
            getActivity().stopManagingCursor(mCursor);
            mCursor = null;
        }

        mCheckedPosition = -1;
        setListAdapter(null);

        mHandler.cancelOperation(SearchQuery._TOKEN);
        mHandler.cancelOperation(VendorsQuery._TOKEN);

        // Load new arguments
        final Intent intent = BaseActivity.fragmentArgumentsToIntent(arguments);
        final Uri vendorsUri = intent.getData();
        final int vendorQueryToken;

        if (vendorsUri == null) {
            return;
        }

        String[] projection;
        if (!ScheduleContract.Vendors.isSearchUri(vendorsUri)) {
            mAdapter = new VendorsAdapter(getActivity());
            projection = VendorsQuery.PROJECTION;
            vendorQueryToken = VendorsQuery._TOKEN;

        } else {
            Log.d("VendorsFragment/reloadFromArguments", "A search URL definitely gets passed in.");
            mAdapter = new SearchAdapter(getActivity());
            projection = SearchQuery.PROJECTION;
            vendorQueryToken = SearchQuery._TOKEN;
        }

        setListAdapter(mAdapter);

        // Start background query to load vendors
        mHandler.startQuery(vendorQueryToken, null, vendorsUri, projection, null, null,
                ScheduleContract.Vendors.DEFAULT_SORT);

        // If caller launched us with specific track hint, pass it along when
        // launching vendor details. Also start a query to load the track info.
        mTrackUri = intent.getParcelableExtra(SessionDetailFragment.EXTRA_TRACK);
        if (mTrackUri != null) {
            mHandler.startQuery(TracksQuery._TOKEN, mTrackUri, TracksQuery.PROJECTION);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        if (savedInstanceState != null) {
            mCheckedPosition = savedInstanceState.getInt(STATE_CHECKED_POSITION, -1);
        }

        if (!mHasSetEmptyText) {
            // Could be a bug, but calling this twice makes it become visible when it shouldn't
            // be visible.
            setEmptyText(getString(R.string.empty_vendors));
            mHasSetEmptyText = true;
        }
    }


    /** {@inheritDoc} */
    public void onQueryComplete(int token, Object cookie, Cursor cursor) {
        if (getActivity() == null) {
            return;
        }

        if (token == VendorsQuery._TOKEN || token == SearchQuery._TOKEN) {
            onVendorsOrSearchQueryComplete(cursor);
        } else if (token == TracksQuery._TOKEN) {
            onTrackQueryComplete(cursor);
        } else {
            cursor.close();
        }
    }

    /**
     * Handle {@link VendorsQuery} {@link Cursor}.
     */
    private void onVendorsOrSearchQueryComplete(Cursor cursor) {
        if (mCursor != null) {
            // In case cancelOperation() doesn't work and we end up with consecutive calls to this
            // callback.
            getActivity().stopManagingCursor(mCursor);
            mCursor = null;
        }

        // TODO(romannurik): stopManagingCursor on detach (throughout app)
        mCursor = cursor;
        getActivity().startManagingCursor(mCursor);
        mAdapter.changeCursor(mCursor);
        if (mCheckedPosition >= 0 && getView() != null) {
            getListView().setItemChecked(mCheckedPosition, true);
        }
    }

    /**
     * Handle {@link TracksQuery} {@link Cursor}.
     */
    private void onTrackQueryComplete(Cursor cursor) {
        try {
            if (!cursor.moveToFirst()) {
                return;
            }

            // Use found track to build title-bar
            ActivityHelper activityHelper = ((BaseActivity) getActivity()).getActivityHelper();
            String trackName = cursor.getString(TracksQuery.TRACK_NAME);
            activityHelper.setActionBarTitle(trackName);
            activityHelper.setActionBarColor(cursor.getInt(TracksQuery.TRACK_COLOR));

            AnalyticsUtils.getInstance(getActivity()).trackPageView("/Sandbox/Track/" + trackName);

        } finally {
            cursor.close();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().getContentResolver().registerContentObserver(
                ScheduleContract.Vendors.CONTENT_URI, true, mVendorChangesObserver);
        if (mCursor != null) {
            mCursor.requery();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        getActivity().getContentResolver().unregisterContentObserver(mVendorChangesObserver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_CHECKED_POSITION, mCheckedPosition);
    }

    /** {@inheritDoc} */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Launch viewer for specific vendor.
        final Cursor cursor = (Cursor)mAdapter.getItem(position);
        final String vendorId = cursor.getString(VendorsQuery.VENDOR_ID);
        final Uri vendorUri = ScheduleContract.Vendors.buildVendorUri(vendorId);
        ((BaseActivity) getActivity()).openActivityOrFragment(new Intent(Intent.ACTION_VIEW,
                vendorUri));

        getListView().setItemChecked(position, true);
        mCheckedPosition = position;
    }

    public void clearCheckedPosition() {
        if (mCheckedPosition >= 0) {
            getListView().setItemChecked(mCheckedPosition, false);
            mCheckedPosition = -1;
        }
    }

    /**
     * {@link CursorAdapter} that renders a {@link VendorsQuery}.
     */
    private class VendorsAdapter extends CursorAdapter {
        public VendorsAdapter(Context context) {
            super(context, null);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getActivity().getLayoutInflater().inflate(R.layout.list_item_vendor_oneline,
                    parent, false);
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ((TextView) view.findViewById(R.id.vendor_name)).setText(
                    cursor.getString(VendorsQuery.NAME));

            final boolean starred = cursor.getInt(VendorsQuery.STARRED) != 0;
            view.findViewById(R.id.star_button).setVisibility(
                    starred ? View.VISIBLE : View.INVISIBLE);
        }
    }

    /**
     * {@link CursorAdapter} that renders a {@link SearchQuery}.
     */
    private class SearchAdapter extends CursorAdapter {
        public SearchAdapter(Context context) {
            super(context, null);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getActivity().getLayoutInflater().inflate(R.layout.list_item_vendor, parent,
                    false);
        }

        /** {@inheritDoc} */ 
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            ((TextView) view.findViewById(R.id.vendor_name)).setText(cursor
                    .getString(SearchQuery.NAME));

            final String snippet = cursor.getString(SearchQuery.SEARCH_SNIPPET);
            final Spannable styledSnippet = buildStyledSnippet(snippet);
            ((TextView) view.findViewById(R.id.vendor_location)).setText(styledSnippet);

            final boolean starred = cursor.getInt(VendorsQuery.STARRED) != 0;
            view.findViewById(R.id.star_button).setVisibility(
                    starred ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private ContentObserver mVendorChangesObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            if (mCursor != null) {
                mCursor.requery();
            }
        }
    };

    /**
     * {@link com.google.android.apps.iosched.provider.ScheduleContract.Vendors} query parameters.
     */
    private interface VendorsQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Vendors.VENDOR_ID,
                ScheduleContract.Vendors.VENDOR_NAME,
                ScheduleContract.Vendors.VENDOR_LOCATION,
                ScheduleContract.Vendors.VENDOR_STARRED,
        };

        int _ID = 0;
        int VENDOR_ID = 1;
        int NAME = 2;
        int LOCATION = 3;
        int STARRED = 4;
    }

    /**
     * {@link com.google.android.apps.iosched.provider.ScheduleContract.Tracks} query parameters.
     */
    private interface TracksQuery {
        int _TOKEN = 0x2;

        String[] PROJECTION = {
                ScheduleContract.Tracks.TRACK_NAME,
                ScheduleContract.Tracks.TRACK_COLOR,
        };

        int TRACK_NAME = 0;
        int TRACK_COLOR = 1;
    }

    /** {@link com.google.android.apps.iosched.provider.ScheduleContract.Vendors} search query
     * parameters. */
    private interface SearchQuery {
        int _TOKEN = 0x3;

        String[] PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Vendors.VENDOR_ID,
                ScheduleContract.Vendors.VENDOR_NAME,
                ScheduleContract.Vendors.SEARCH_SNIPPET,
                ScheduleContract.Vendors.VENDOR_STARRED,
        };

        int _ID = 0;
        int VENDOR_ID = 1;
        int NAME = 2;
        int SEARCH_SNIPPET = 3;
        int STARRED = 4;
    }
}
