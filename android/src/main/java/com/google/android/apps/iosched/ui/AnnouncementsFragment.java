/*
 * Copyright 2013 Google Inc.
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
import com.google.android.apps.iosched.util.ImageLoader;
import com.google.android.apps.iosched.util.UIUtils;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.plus.model.Activity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.google.android.apps.iosched.util.LogUtils.LOGE;
import static com.google.android.apps.iosched.util.LogUtils.makeLogTag;

/**
 * A fragment that shows announcements.
 */
public class AnnouncementsFragment extends ListFragment implements
        AbsListView.OnScrollListener, LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = makeLogTag(AnnouncementsFragment.class);

    public static final String EXTRA_ADD_VERTICAL_MARGINS
            = "com.google.android.apps.iosched.extra.ADD_VERTICAL_MARGINS";

    private static final String STATE_POSITION = "position";
    private static final String STATE_TOP = "top";

    private Cursor mCursor;
    private StreamAdapter mStreamAdapter;
    private int mListViewStatePosition;
    private int mListViewStateTop;

    private ImageLoader mImageLoader;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mImageLoader =
                PlusStreamRowViewBinder.getPlusStreamImageLoader(getActivity(), getResources());

        mCursor = null;
        mStreamAdapter = new StreamAdapter(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            mListViewStatePosition = savedInstanceState.getInt(STATE_POSITION, -1);
            mListViewStateTop = savedInstanceState.getInt(STATE_TOP, 0);
        } else {
            mListViewStatePosition = -1;
            mListViewStateTop = 0;
        }
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setEmptyText(getString(R.string.empty_announcements));
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ListView listView = getListView();
        if (!UIUtils.isTablet(getActivity())) {
            view.setBackgroundColor(getResources().getColor(R.color.plus_stream_spacer_color));
        }

        if (getArguments() != null
                && getArguments().getBoolean(EXTRA_ADD_VERTICAL_MARGINS, false)) {
            int verticalMargin = getResources().getDimensionPixelSize(
                    R.dimen.plus_stream_padding_vertical);
            if (verticalMargin > 0) {
                listView.setClipToPadding(false);
                listView.setPadding(0, verticalMargin, 0, verticalMargin);
            }
        }

        listView.setOnScrollListener(this);
        listView.setDrawSelectorOnTop(true);
        listView.setDivider(getResources().getDrawable(android.R.color.transparent));
        listView.setDividerHeight(getResources()
                .getDimensionPixelSize(R.dimen.page_margin_width));

        TypedValue v = new TypedValue();
        getActivity().getTheme().resolveAttribute(R.attr.activatableItemBackground, v, true);
        listView.setSelector(v.resourceId);

        setListAdapter(mStreamAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (isAdded()) {
            View v = getListView().getChildAt(0);
            int top = (v == null) ? 0 : v.getTop();
            outState.putInt(STATE_POSITION, getListView().getFirstVisiblePosition());
            outState.putInt(STATE_TOP, top);
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        if (mCursor == null) {
            return;
        }

        mCursor.moveToPosition(position);
        String url = mCursor.getString(AnnouncementsQuery.ANNOUNCEMENT_URL);
        Intent postDetailIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        postDetailIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        UIUtils.preferPackageForIntent(getActivity(), postDetailIntent,
                UIUtils.GOOGLE_PLUS_PACKAGE_NAME);
        startActivity(postDetailIntent);
    }

    @Override
    public void onScrollStateChanged(AbsListView listView, int scrollState) {
        // Pause disk cache access to ensure smoother scrolling
        if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
            mImageLoader.stopProcessingQueue();
        } else {
            mImageLoader.startProcessingQueue();
        }
    }

    @Override
    public void onScroll(AbsListView absListView, int i, int i2, int i3) {
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), ScheduleContract.Announcements.CONTENT_URI,
                AnnouncementsQuery.PROJECTION, null, null,
                ScheduleContract.Announcements.DEFAULT_SORT);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        mCursor = cursor;
        mStreamAdapter.changeCursor(mCursor);
        mStreamAdapter.notifyDataSetChanged();
        if (mListViewStatePosition != -1 && isAdded()) {
            getListView().setSelectionFromTop(mListViewStatePosition, mListViewStateTop);
            mListViewStatePosition = -1;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private class StreamAdapter extends CursorAdapter {
        private JsonFactory mFactory = new AndroidJsonFactory();
        private Map<Long, Activity> mActivityCache = new HashMap<Long, Activity>();

        public StreamAdapter(Context context) {
            super(context, null, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup container) {
            return LayoutInflater.from(getActivity()).inflate(
                    R.layout.list_item_stream_activity, container, false);
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            long id = cursor.getLong(AnnouncementsQuery._ID);
            String activityJson = cursor.getString(AnnouncementsQuery.ANNOUNCEMENT_ACTIVITY_JSON);
            Activity activity = mActivityCache.get(id);
            // TODO: this should be async
            if (activity == null) {
                try {
                    activity = mFactory.fromString(activityJson, Activity.class);
                } catch (IOException e) {
                    LOGE(TAG, "Couldn't parse activity JSON: " + activityJson, e);
                }
                mActivityCache.put(id, activity);
            }

            PlusStreamRowViewBinder.bindActivityView(view, activity, mImageLoader, true);
        }
    }

    private interface AnnouncementsQuery {
        String[] PROJECTION = {
                ScheduleContract.Announcements._ID,
                ScheduleContract.Announcements.ANNOUNCEMENT_ACTIVITY_JSON,
                ScheduleContract.Announcements.ANNOUNCEMENT_URL,
        };

        int _ID = 0;
        int ANNOUNCEMENT_ACTIVITY_JSON = 1;
        int ANNOUNCEMENT_URL = 2;
    }
}
