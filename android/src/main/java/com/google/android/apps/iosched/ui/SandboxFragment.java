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
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import com.google.android.apps.iosched.R;
import com.google.android.apps.iosched.provider.ScheduleContract;
import com.google.android.apps.iosched.util.UIUtils;

import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A {@link ListFragment} showing a list of sandbox comapnies.
 */
public class SandboxFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = makeLogTag(SandboxFragment.class);

    private static final String STATE_SELECTED_ID = "selectedId";

    private Uri mSandboxUri;
    private CursorAdapter mAdapter;
    private String mSelectedCompanyId;

    public interface Callbacks {
        /** Return true to select (activate) the company in the list, false otherwise. */
        public boolean onCompanySelected(String companyId);
    }

    private static Callbacks sDummyCallbacks = new Callbacks() {
        @Override
        public boolean onCompanySelected(String companyId) {
            return true;
        }
    };

    private Callbacks mCallbacks = sDummyCallbacks;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mSelectedCompanyId = savedInstanceState.getString(STATE_SELECTED_ID);
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // As of support library r12, calling initLoader for a fragment in a FragmentPagerAdapter
        // in the fragment's onCreate may cause the same LoaderManager to be dealt to multiple
        // fragments because their mIndex is -1 (haven't been added to the activity yet). Thus,
        // we call reloadFromArguments (which calls restartLoader/initLoader) in onActivityCreated.
        reloadFromArguments(getArguments());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_list_with_empty_container_inset,
                container, false);
        TextView emptyView = new TextView(getActivity(), null, R.attr.emptyText);
        emptyView.setText(R.string.empty_sandbox);
        ((ViewGroup) rootView.findViewById(android.R.id.empty)).addView(emptyView);
        return rootView;
    }

    void reloadFromArguments(Bundle arguments) {
        // Teardown from previous arguments
        setListAdapter(null);

        // Load new arguments
        final Intent intent = BaseActivity.fragmentArgumentsToIntent(arguments);
        mSandboxUri = intent.getData();
        if (mSandboxUri == null) {
            mSandboxUri = ScheduleContract.Sandbox.CONTENT_URI;
        }

        final int sandboxQueryToken;

        mAdapter = new SandboxAdapter(getActivity());
        sandboxQueryToken = SandboxQuery._TOKEN;

        setListAdapter(mAdapter);

        // Start background query to load sandbox
        getLoaderManager().initLoader(sandboxQueryToken, null, this);
    }

    public void setSelectedCompanyId(String id) {
        mSelectedCompanyId = id;
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
        if (mSelectedCompanyId != null) {
            outState.putString(STATE_SELECTED_ID, mSelectedCompanyId);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        final Cursor cursor = (Cursor) mAdapter.getItem(position);
        String companyId = cursor.getString(SandboxQuery.COMPANY_ID);
        if (mCallbacks.onCompanySelected(companyId)) {
            mSelectedCompanyId = companyId;
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * {@link CursorAdapter} that renders a {@link com.google.android.apps.iosched.ui.SandboxFragment.SandboxQuery}.
     */
    private class SandboxAdapter extends CursorAdapter {
        public SandboxAdapter(Context context) {
            super(context, null, 0);
        }

        /** {@inheritDoc} */
        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return getActivity().getLayoutInflater().inflate(R.layout.list_item_sandbox,
                    parent, false);
        }

        /** {@inheritDoc} */
        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            UIUtils.setActivatedCompat(view, cursor.getString(SandboxQuery.COMPANY_ID)
                    .equals(mSelectedCompanyId));

            ((TextView) view.findViewById(R.id.company_name)).setText(
                    cursor.getString(SandboxQuery.NAME));
        }
    }

    /**
     * {@link com.google.android.apps.iosched.provider.ScheduleContract.Sandbox}
     * query parameters.
     */
    private interface SandboxQuery {
        int _TOKEN = 0x1;

        String[] PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Sandbox.COMPANY_ID,
                ScheduleContract.Sandbox.COMPANY_NAME,
        };

        int _ID = 0;
        int COMPANY_ID = 1;
        int NAME = 2;
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle data) {
        return new CursorLoader(getActivity(), mSandboxUri, SandboxQuery.PROJECTION, null, null,
                ScheduleContract.Sandbox.DEFAULT_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (getActivity() == null) {
            return;
        }
        int token = loader.getId();
        if (token == SandboxQuery._TOKEN) {
            mAdapter.changeCursor(cursor);

        } else {
            cursor.close();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursor) {
    }
}
