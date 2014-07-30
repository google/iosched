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

import android.app.Application;
import android.content.Context;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.widget.*;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.widget.CollectionView;
import com.google.samples.apps.iosched.ui.widget.CollectionViewCallbacks;
import com.google.samples.apps.iosched.util.AnalyticsManager;
import com.google.samples.apps.iosched.util.UIUtils;

import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class HashtagsFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = makeLogTag(HashtagsFragment.class);
    private CollectionView mCollectionView;

    private static final int HERO_GROUP_ID = 1337;

    public static HashtagsFragment newInstance() {
        return new HashtagsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_hashtags, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        mCollectionView = (CollectionView) view.findViewById(R.id.social_collection_vew);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (!getActivity().isFinishing()) {
            getLoaderManager().restartLoader(HashtagsQuery.TOKEN, null, this);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id == HashtagsQuery.TOKEN) {
            return new CursorLoader(getActivity(), ScheduleContract.Hashtags.CONTENT_URI,
                    HashtagsQuery.PROJECTION, null, null, ScheduleContract.Hashtags.HASHTAG_ORDER);
        }
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (loader.getId() == HashtagsQuery.TOKEN) {
            final HashtagsAdapter adapter = new HashtagsAdapter(getActivity(), cursor);
            mCollectionView.setCollectionAdapter(adapter);
            mCollectionView.updateInventory(adapter.getInventory());
            mCollectionView.setOnScrollListener(new AbsListView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(AbsListView view, int scrollState) {
                  adapter.hideDescriptionToast();
                }
                @Override
                public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                }
            });
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private interface HashtagsQuery {
        int TOKEN = 0x1;
        String[] PROJECTION = {
                ScheduleContract.Hashtags._ID,
                ScheduleContract.Hashtags.HASHTAG_NAME,
                ScheduleContract.Hashtags.HASHTAG_DESCRIPTION,
                ScheduleContract.Hashtags.HASHTAG_COLOR,
        };

        int HASHTAG_NAME = 1;
        int HASHTAG_DESCRIPTION = 2;
        int HASHTAG_COLOR = 3;
    }

    private static class HashtagsAdapter extends CursorAdapter implements CollectionViewCallbacks {

        private final Context mContext;
        private final Cursor mCursor;
        private Toast mCurrentToast;

        public HashtagsAdapter(Context context, Cursor c) {
            super(context, c, 0);
            mContext = context;
            mCursor = c;
        }

        public CollectionView.Inventory getInventory() {
            CollectionView.Inventory inventory = new CollectionView.Inventory();
            // setup hero hashtag
            inventory.addGroup(new CollectionView.InventoryGroup(HERO_GROUP_ID)
                    .setDisplayCols(1)
                    .setItemCount(1)
                    .setShowHeader(false));

            // setup other hashtags
            inventory.addGroup(new CollectionView.InventoryGroup(HashtagsQuery.TOKEN)
                    .setDisplayCols(mContext.getResources().getInteger(R.integer.social_grid_columns))
                    .setItemCount(mCursor.getCount() - 1)
                    .setDataIndexStart(1)
                    .setShowHeader(false));
            return inventory;
        }

        private boolean isHeroView(int groupId) {
            return groupId == HERO_GROUP_ID;
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
            return isHeroView(groupId) ? newHeroView(context, parent) : newView(context, null, parent);
        }

        @Override
        public void bindCollectionItemView(Context context, View view, int groupId, int indexInGroup, int dataIndex, Object tag) {
            setCursorPosition(dataIndex);
            if (isHeroView(groupId)) {
                bindHeroView(view, context, mCursor);
            } else {
                bindView(view, context, mCursor);
            }
        }

        private View newHeroView(Context context, ViewGroup parent) {
            return LayoutInflater.from(context).inflate(R.layout.list_item_hashtag_hero, parent, false);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View view = LayoutInflater.from(context).inflate(R.layout.list_item_hashtags, parent, false);
            ViewHolder holder = new ViewHolder();
            assert view != null;
            holder.name = (TextView) view.findViewById(R.id.name);
            holder.description = (ImageButton) view.findViewById(R.id.description);
            view.setTag(holder);
            return view;
        }

        public void bindHeroView(View view, Context context, Cursor cursor) {
            final String hashtag = cursor.getString(HashtagsQuery.HASHTAG_NAME);
            ((TextView) view.findViewById(R.id.name)).setText(hashtag);
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    UIUtils.showHashtagStream(mContext, hashtag);

                    /* [ANALYTICS:EVENT]
                     * TRIGGER:   Click on a hashtag on the Social screen to launch Google+
                     * CATEGORY:  'Social'
                     * ACTION:    selecthashtag
                     * LABEL:     the selected hashtag, e.g. '#io14 #design'
                     * [/ANALYTICS]
                     */
                    AnalyticsManager.sendEvent("Social", "selecthashtag", hashtag, 0L);
                }
            });
        }

        @Override
        public void bindView(View view, final Context context, Cursor cursor) {
            ViewHolder holder = (ViewHolder) view.getTag();
            final String hashtag = cursor.getString(HashtagsQuery.HASHTAG_NAME);
            view.setBackgroundColor(cursor.getInt(HashtagsQuery.HASHTAG_COLOR));
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    UIUtils.showHashtagStream(mContext, hashtag);
                }
            });

            final String desc = cursor.getString(HashtagsQuery.HASHTAG_DESCRIPTION);
            holder.name.setText(hashtag.replace("#io14 ", ""));
            if (!TextUtils.isEmpty(desc)) {
                holder.description.setVisibility(View.VISIBLE);
                holder.description.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        displayDescription(view, desc);
                    }
                });
            } else {
                holder.description.setVisibility(View.GONE);
            }
        }

        private void setCursorPosition(int position) {
            if (!mCursor.moveToPosition(position)) {
                throw new IllegalStateException("couldn't move cursor to position " + position);
            }
        }

        private void displayDescription(View view, String desc) {
            hideDescriptionToast();
            mCurrentToast = Toast.makeText(mContext.getApplicationContext(), desc, Toast.LENGTH_LONG);
            mCurrentToast.show();
            if (Build.VERSION.SDK_INT >= 16) {
                view.announceForAccessibility(desc);
            }
        }

        public void hideDescriptionToast() {
            if (mCurrentToast != null) {
                mCurrentToast.cancel();
                mCurrentToast = null;
            }
        }

        private static final class ViewHolder {
            TextView name;
            ImageButton description;
        }
    }
}
