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

import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.util.UIUtils;

import com.actionbarsherlock.app.SherlockListFragment;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Spannable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ListView;
import android.widget.TextView;

import static com.google.android.apps.iosched.util.LogUtils.LOGD;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;
import static com.google.android.apps.iosched.util.UIUtils.buildStyledSnippet;

/**
 * A {@link ListFragment} showing a list of developer sandbox companies.
 */
public class VendorsFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = makeLogTag(VendorsFragment.class);

    private static final String STATE_SELECTED_ID = "selectedId";

    private Uri mVendorsUri;
    private CursorAdapter mAdapter;
    private String mSelectedVendorId;
    private boolean mHasSetEmptyText = false;

    public interface Callbacks {
        /** Return true to select (activate) the vendor in the list, false otherwise. */
        public boolean onVendorSelected(String vendorId);
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public boolean onVendorSelected(String vendorId) {
            return true;
        }
    };

    private Callbacks mCallbacks = sDummyCallbacks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mSelectedVendorId = savedInstanceState.getString(STATE_SELECTED_ID);
        }

        reloadFromArguments(getArguments());
    }

    public void reloadFromArguments(Bundle arguments) {
        // Teardown from previous arguments
        setListAdapter(null);

        // Load new arguments
        final Intent intent = BaseActivity.fragmentArgumentsToIntent(arguments);
        mVendorsUri = intent.getData();
        final int vendorQueryToken;

        if (mVendorsUri == null) {
            return;
        }

        mAdapter = new VendorsAdapter(getActivity());
        vendorQueryToken = VendorsQuery._TOKEN;

        setListAdapter(mAdapter);

        // Start background query to load vendors
        getLoaderManager().initLoader(vendorQueryToken, null, this);
    }

    public void setSelectedVendorId(String id) {
        mSelectedVendorId = id;
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
            // when it shouldn't be visible.
            setEmptyText(getString(R.string.empty_vendors));
            mHasSetEmptyText = true;
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof Callbacks)) {
            throw new ClassCastException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = sDummyCallbacks;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mSelectedVendorId != null) {
            outState.putString(STATE_SELECTED_ID, mSelectedVendorId);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        final Cursor cursor = (Cursor) mAdapter.getItem(position);
        String vendorId = cursor.getString(VendorsQuery.VENDOR_ID);
        if (mCallbacks.onVendorSelected(vendorId)) {
            mSelectedVendorId = vendorId;
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        return new CursorLoader(getActivity(), mVendorsUri, VendorsQuery.PROJECTION, null, null,
                ScheduleContract.Vendors.DEFAULT_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (getActivity() == null) {
            return;
        }
        int token = loader.getId();
        if (token == VendorsQuery._TOKEN) {
            mAdapter.changeCursor(cursor);

        } else {
            cursor.close();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursor) {
    }

    /**
     * {@link CursorAdapter} that renders a {@link VendorsQuery}.
     */
    private class VendorsAdapter extends CursorAdapter {
        public VendorsAdapter(Context context) {
            super(context, null, false);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getActivity().getLayoutInflater().inflate(R.layout.list_item_vendor,
                    parent, false);
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            UIUtils.setActivatedCompat(view, cursor.getString(VendorsQuery.VENDOR_ID)
                    .equals(mSelectedVendorId));

            ((TextView) view.findViewById(R.id.vendor_name)).setText(
                    cursor.getString(VendorsQuery.NAME));
        }
    }

    /**
     * {@link com.google.android.apps.iosched.provider.ScheduleContract.Vendors}
     * query parameters.
     */
    private interface VendorsQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Vendors.VENDOR_ID,
                ScheduleContract.Vendors.VENDOR_NAME,
        };

        int _ID = 0;
        int VENDOR_ID = 1;
        int NAME = 2;
    }
}
