/*
 * Copyright 2014 Google Inc. All rights reserved.
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

package com.google.samples.apps.iosched.ui;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.widget.CollectionView;
import com.google.samples.apps.iosched.ui.widget.CollectionViewCallbacks;
import com.google.samples.apps.iosched.util.ImageLoader;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.regex.Pattern;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A {@link android.app.Fragment} subclass used to present the list of partners.
 */
public class PartnersFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = makeLogTag(PartnersFragment.class);
    public static String ARG_HAS_HEADER = "hasHeader";

    private ImageLoader mImageLoader;

    private static final Pattern TRIM_FROM_DISPLAY_URL_PATTERN = Pattern.compile("(^https?://)|(/$)");

    private CollectionView mCollectionView;
    private PartnersAdapter mPartnersAdapter;
    private int mDisplayCols;

    public static PartnersFragment newInstance(boolean hasHeader) {
        PartnersFragment fragment = new PartnersFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_HAS_HEADER, hasHeader);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_partners, container, false);
        if (getArguments() != null && !getArguments().getBoolean(ARG_HAS_HEADER, true)) {
            rootView.findViewById(R.id.headerbar).setVisibility(View.GONE);
        }

        rootView.findViewById(R.id.close_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO: refactor to use fragment callbacks
                getFragmentManager().popBackStack();
            }
        });

        mCollectionView = (CollectionView) rootView.findViewById(R.id.collection_view);
        return rootView;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDisplayCols = getResources().getInteger(R.integer.partners_columns);
        mImageLoader = new ImageLoader(getActivity(), R.drawable.person_image_empty);
        LoaderManager manager = getLoaderManager();
        manager.initLoader(PartnersQuery._TOKEN, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle bundle) {
        CursorLoader loader = null;
        if (id == PartnersQuery._TOKEN) {
            loader = new CursorLoader(getActivity(), ScheduleContract.Partners.CONTENT_URI,
                    PartnersQuery.PROJECTION, null, null, PartnersQuery.SORT);
        }
        return loader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() == PartnersQuery._TOKEN) {
            mPartnersAdapter = new PartnersAdapter(cursor);
            mCollectionView.setCollectionAdapter(mPartnersAdapter);
            mCollectionView.updateInventory(mPartnersAdapter.getInventory());
        } else {
            cursor.close();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {}

    /**
     * An adapter for partners. It can also be used with {@link CollectionView}. In that case, use
     * {@link PartnersAdapter#getInventory()} to get a {@link CollectionView.Inventory} and
     * set it to the {@link CollectionView} by
     * {@link CollectionView#updateInventory(CollectionView.Inventory)}.
     */
    private class PartnersAdapter extends CursorAdapter implements CollectionViewCallbacks {
        private final Cursor mCursor;

        public PartnersAdapter(Cursor cursor) {
            super(getActivity(), cursor, 0);
            mCursor = cursor;
        }

        /**
         * Returns a new instance of {@link CollectionView.Inventory}. It always contains only one
         * {@link CollectionView.InventoryGroup}.
         *
         * @return A new instance of {@link CollectionView.Inventory}
         */
        public CollectionView.Inventory getInventory() {
            CollectionView.Inventory inventory = new CollectionView.Inventory();
            inventory.addGroup(new CollectionView.InventoryGroup(PartnersQuery._TOKEN)
                    .setDisplayCols(mDisplayCols)
                    .setItemCount(mCursor.getCount())
                    .setDataIndexStart(0)
                    .setShowHeader(false)
                    .setHeaderLabel(getString(R.string.title_experts_directory)));
            return inventory;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.list_item_partner, parent, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final String url = mCursor.getString(PartnersQuery.WEBSITE_URL);
            view.findViewById(R.id.partner_target).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!TextUtils.isEmpty(url)) {
                        Intent expertProfileIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        UIUtils.preferPackageForIntent(getActivity(), expertProfileIntent,
                                UIUtils.GOOGLE_PLUS_PACKAGE_NAME);
                        expertProfileIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                        startActivity(expertProfileIntent);
                    }
                }
            });

            ImageView imageView = (ImageView) view.findViewById(R.id.image);
            mImageLoader.loadImage(cursor.getString(PartnersQuery.LOGO_URL), imageView);
            ((TextView) view.findViewById(R.id.name)).setText(cursor.getString(PartnersQuery.NAME));
            ((TextView) view.findViewById(R.id.url)).setText(
                    TRIM_FROM_DISPLAY_URL_PATTERN.matcher(url).replaceAll(""));
            ((TextView) view.findViewById(R.id.desc)).setText(cursor.getString(PartnersQuery.DESC));
        }

        @Override
        public View newCollectionHeaderView(Context context, ViewGroup parent) {
            return null;
        }

        @Override
        public void bindCollectionHeaderView(Context context, View view, int groupId, String headerLabel) {
        }

        @Override
        public View newCollectionItemView(Context context, int groupId, ViewGroup parent) {
            return newView(context, null, parent);
        }

        @Override
        public void bindCollectionItemView(Context context, View view, int groupId, int indexInGroup,
                int dataIndex, Object tag) {
            setCursorPosition(indexInGroup);
            bindView(view, context, mCursor);
        }

        private void setCursorPosition(int position) {
            if (!mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }
        }
    }

    private interface PartnersQuery {
        static final int _TOKEN = 0;
        static final String[] PROJECTION = {
                ScheduleContract.Partners._ID,
                ScheduleContract.Partners.PARTNER_ID,
                ScheduleContract.Partners.PARTNER_NAME,
                ScheduleContract.Partners.PARTNER_LOGO_URL,
                ScheduleContract.Partners.PARTNER_DESC,
                ScheduleContract.Partners.PARTNER_WEBSITE_URL,
        };

        static final String SORT = ScheduleContract.Partners.PARTNER_NAME + " ASC";

        static final int ID = 1;
        static final int NAME = 2;
        static final int LOGO_URL = 3;
        static final int DESC = 4;
        static final int WEBSITE_URL = 5;
    }
}
