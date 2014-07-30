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
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.samples.apps.iosched.BuildConfig;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.widget.CollectionView;
import com.google.samples.apps.iosched.ui.widget.CollectionViewCallbacks;
import com.google.samples.apps.iosched.ui.widget.MessageCardView;
import com.google.samples.apps.iosched.util.ImageLoader;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.ArrayList;
import java.util.Locale;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class ExpertsDirectoryFragment extends Fragment implements
        LoaderManager.LoaderCallbacks<Cursor>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = makeLogTag(ExpertsDirectoryFragment.class);

    /**
     * Preference key for whether the explanation header should be shown
     */
    private static final String PREF_SHOW_HEADER = "experts_directory_show_header";

    private CollectionView mCollectionView;
    private ExpertsAdapter mExpertsAdapter;
    private int mDisplayCols;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mDisplayCols = getResources().getInteger(R.integer.experts_directory_columns);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_experts_directory, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mCollectionView = (CollectionView) view.findViewById(R.id.collection_view);
        ((BaseActivity) getActivity()).enableActionBarAutoHide(mCollectionView);
    }

    @Override
    public void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void reload(String country, String city) {
        Bundle args = new Bundle();
        args.putString(ScheduleContract.Experts.EXPERT_COUNTRY, country);
        if (null != city) {
            args.putString(ScheduleContract.Experts.EXPERT_CITY, city);
        }

        getLoaderManager().restartLoader(ExpertsQuery.TOKEN, args, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case ExpertsQuery.TOKEN:
                StringBuilder selection = new StringBuilder();
                ArrayList<String> selectionArgs = new ArrayList<String>();
                if (null != args) {
                    String country = args.getString(ScheduleContract.Experts.EXPERT_COUNTRY);
                    String city = args.getString(ScheduleContract.Experts.EXPERT_CITY);

                    if (null != country) {
                        selection.append(ScheduleContract.Experts.EXPERT_COUNTRY);
                        selection.append(" = ?");
                        selectionArgs.add(country);
                        if (null != city) {
                            selection.append(" AND ");
                            selection.append(ScheduleContract.Experts.EXPERT_CITY);
                            selection.append(" = ?");
                            selectionArgs.add(city);
                        }
                    }
                }
                String s = null;
                String[] ss = null;
                if (0 < selection.length()) {
                    s = selection.toString();
                    ss = selectionArgs.toArray(new String[selectionArgs.size()]);
                }
                return new CursorLoader(getActivity(), ScheduleContract.Experts.CONTENT_URI,
                        ExpertsQuery.PROJECTION, s, ss,
                        "-" + ScheduleContract.Experts.EXPERT_ATTENDING
                                + "," + ScheduleContract.Experts.EXPERT_NAME);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        switch (loader.getId()) {
            case ExpertsQuery.TOKEN: {
                LOGD(TAG, "Expert count: " + cursor.getCount());
                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
                mExpertsAdapter = new ExpertsAdapter(cursor);
                mExpertsAdapter.setShowHeader(sp.getBoolean(PREF_SHOW_HEADER, true));
                mCollectionView.setCollectionAdapter(mExpertsAdapter);
                mCollectionView.updateInventory(mExpertsAdapter.getInventory());
                break;
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        switch (loader.getId()) {
            case ExpertsQuery.TOKEN: {
                mCollectionView.updateInventory(new CollectionView.Inventory());
                break;
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sp, String key) {
        if (TextUtils.equals(key, PREF_SHOW_HEADER)) {
            if (null != mExpertsAdapter) {
                mExpertsAdapter.setShowHeader(sp.getBoolean(PREF_SHOW_HEADER, true));
                mCollectionView.updateInventory(mExpertsAdapter.getInventory());
            }
        }
    }

    public void setContentTopClearance(int clearance) {
        if (mCollectionView != null) {
            mCollectionView.setContentTopClearance(clearance);
        }
    }

    /**
     * An adapter for experts. It can also be used with {@link CollectionView}. In that case, use
     * {@link ExpertsAdapter#getInventory()} to get a {@link CollectionView.Inventory} and
     * set it to the {@link CollectionView} by
     * {@link CollectionView#updateInventory(CollectionView.Inventory)}.
     */
    private class ExpertsAdapter extends CursorAdapter implements CollectionViewCallbacks {

        private final ImageLoader mImageLoader;
        private final Cursor mCursor;
        private boolean mShowHeader = true;

        public ExpertsAdapter(Cursor cursor) {
            super(getActivity(), cursor, 0);
            mCursor = cursor;
            mImageLoader = new ImageLoader(getActivity(), R.drawable.person_image_empty);
        }

        private String getSummary(Cursor cursor) {
            StringBuilder summary = new StringBuilder();
            String city = cursor.getString(ExpertsQuery.EXPERT_CITY);
            summary.append(city);
            String countryName = getCountryName(cursor.getString(ExpertsQuery.EXPERT_COUNTRY));
            if (!TextUtils.isEmpty(countryName)) {
                if (!TextUtils.isEmpty(city)) {
                    summary.append(", ");
                }
                summary.append(countryName);
            }
            return summary.toString();
        }

        private CharSequence getBio(Cursor cursor) {
            StringBuilder bio = new StringBuilder();
            if (0 != cursor.getLong(ExpertsQuery.EXPERT_ATTENDING)) {
                bio.append("<b>(");
                bio.append(getString(R.string.experts_directory_attending));
                bio.append(")</b>&nbsp;");
            }
            bio.append(cursor.getString(ExpertsQuery.EXPERT_ABSTRACT));
            return Html.fromHtml(bio.toString());
        }

        public void setShowHeader(boolean showHeader) {
            mShowHeader = showHeader;
        }

        /**
         * Returns a new instance of {@link CollectionView.Inventory}. It always contains only one
         * {@link CollectionView.InventoryGroup}.
         *
         * @return A new instance of {@link CollectionView.Inventory}
         */
        public CollectionView.Inventory getInventory() {
            CollectionView.Inventory inventory = new CollectionView.Inventory();
            inventory.addGroup(new CollectionView.InventoryGroup(ExpertsQuery.TOKEN)
                    .setDisplayCols(mDisplayCols)
                    .setItemCount(mCursor.getCount())
                    .setDataIndexStart(0)
                    .setShowHeader(mShowHeader)
                    .setHeaderLabel(getString(R.string.title_experts_directory)));
            return inventory;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            // The argument cursor is null when this adapter is used as a CollectionViewCallbacks.
            View view = LayoutInflater.from(context).inflate(R.layout.list_item_expert, parent, false);
            assert view != null;
            ViewHolder holder = new ViewHolder();
            holder.image = (ImageView) view.findViewById(R.id.image);
            holder.name = (TextView) view.findViewById(R.id.name);
            holder.summary = (TextView) view.findViewById(R.id.summary);
            holder.bio = (TextView) view.findViewById(R.id.bio);
            view.setTag(holder);
            return view;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            final String url = mCursor.getString(ExpertsQuery.EXPERT_URL);
            view.findViewById(R.id.expert_target).setOnClickListener(new View.OnClickListener() {
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

            ViewHolder holder = (ViewHolder) view.getTag();
            mImageLoader.loadImage(cursor.getString(ExpertsQuery.EXPERT_IMAGE_URL), holder.image);
            holder.name.setText(cursor.getString(ExpertsQuery.EXPERT_NAME));
            holder.summary.setText(getSummary(cursor));
            holder.bio.setText(getBio(cursor));
        }

        @Override
        public View newCollectionHeaderView(Context context, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.header_experts_directory, parent, false);
        }

        @Override
        public void bindCollectionHeaderView(Context context, View view, int groupId, String headerLabel) {
            final MessageCardView messageCardView = (MessageCardView) view.findViewById(R.id.message_card);
            messageCardView.setListener(new MessageCardView.OnMessageCardButtonClicked() {
                @Override
                public void onMessageCardButtonClicked(String tag) {
                    SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    sp.edit().putBoolean(PREF_SHOW_HEADER, false).commit();
                    messageCardView.dismiss(true);
                }
            });
        }

        /**
         * {@inheritDoc}
         * This implementation just delegates to {@link ExpertsAdapter#newView(Context, Cursor, ViewGroup)}.
         */
        @Override
        public View newCollectionItemView(Context context, int groupId, ViewGroup parent) {
            return newView(context, null, parent);
        }

        /**
         * {@inheritDoc}
         * This implementation just delegates to {@link ExpertsAdapter#bindView(View, Context, Cursor)}
         */
        @Override
        public void bindCollectionItemView(Context context, View view, int groupId, int indexInGroup, int dataIndex, Object tag) {
            setCursorPosition(indexInGroup);
            bindView(view, context, mCursor);
        }

        private void setCursorPosition(int position) {
            if (!mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }
        }
    }

    private static String getCountryName(String countryCode) {
        return new Locale("", countryCode).getDisplayCountry();
    }

    private static class ViewHolder {
        ImageView image;
        TextView name;
        TextView summary;
        TextView bio;
    }

    private interface ExpertsQuery {
        int TOKEN = 0x3;

        String[] PROJECTION = {
                ScheduleContract.Experts._ID,
                ScheduleContract.Experts.EXPERT_NAME,
                ScheduleContract.Experts.EXPERT_IMAGE_URL,
                ScheduleContract.Experts.EXPERT_COUNTRY,
                ScheduleContract.Experts.EXPERT_CITY,
                ScheduleContract.Experts.EXPERT_ABSTRACT,
                ScheduleContract.Experts.EXPERT_ATTENDING,
                ScheduleContract.Experts.EXPERT_URL,
        };

        int EXPERT_NAME = 1;
        int EXPERT_IMAGE_URL = 2;
        int EXPERT_COUNTRY = 3;
        int EXPERT_CITY = 4;
        int EXPERT_ABSTRACT = 5;
        int EXPERT_ATTENDING = 6;
        int EXPERT_URL = 7;
    }
}
