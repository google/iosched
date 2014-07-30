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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.BaseColumns;
import android.app.Fragment;
import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.ui.widget.CollectionView;
import com.google.samples.apps.iosched.ui.widget.CollectionViewCallbacks;
import com.google.samples.apps.iosched.util.AnalyticsManager;
import com.google.samples.apps.iosched.util.ImageLoader;
import com.google.samples.apps.iosched.util.ParserUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import java.util.ArrayList;
import java.util.Locale;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

public class VideoLibraryFragment extends Fragment
        implements LoaderManager.LoaderCallbacks<Cursor>, CollectionViewCallbacks {
    private static final String TAG = makeLogTag("VideoLibraryFragment");

    boolean mInformedYearsAndTopics = false;
    ImageLoader mImageLoader;

    CollectionView mCollectionView = null;
    View mEmptyView = null;
    Cursor mCursor = null;

    int mFilterYear = 0;
    String mFilterTopic = null;

    private static final String LOADER_ARG_YEAR = "LOADER_ARG_YEAR";
    private static final String LOADER_ARG_TOPIC = "LOADER_ARG_TOPIC";

    private static final int GROUP_ID_HERO = 1000;
    private static final int GROUP_ID_NORMAL = 1001;

    public interface Callbacks {
        public void onAvailableVideoYearsChanged(ArrayList<Integer> years);
        public void onAvailableVideoTopicsChanged(ArrayList<String> availableTopics);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_video_library, container, false);
        mCollectionView = (CollectionView) root.findViewById(R.id.videos_collection_view);
        mEmptyView = root.findViewById(android.R.id.empty);
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mImageLoader = new ImageLoader(getActivity(), android.R.color.transparent);
        setFilterAndReload(0, null);
    }

    public void setContentTopClearance(int clearance) {
        if (mCollectionView != null) {
            mCollectionView.setContentTopClearance(clearance);
        }
    }

    public void setFilterAndReload(int year, String topic) {
        mFilterYear = year;
        mFilterTopic = topic;
        Bundle args = new Bundle();
        args.putInt(LOADER_ARG_YEAR, mFilterYear);
        args.putString(LOADER_ARG_TOPIC, mFilterTopic == null ? "" : mFilterTopic);
        getLoaderManager().restartLoader(VideosQuery._TOKEN, args, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int token, Bundle bundle) {
        if (token == VideosQuery._TOKEN) {
            int year = bundle.getInt(LOADER_ARG_YEAR);
            String topic = bundle.getString(LOADER_ARG_TOPIC);

            ArrayList<String> selectionArgs = new ArrayList<String>();
            ArrayList<String> selectionClauses = new ArrayList<String>();

            if (year > 0) {
                selectionClauses.add(ScheduleContract.Videos.VIDEO_YEAR + "=?");
                selectionArgs.add(String.format(Locale.US, "%d", year));
            }
            if (!TextUtils.isEmpty(topic)) {
                selectionClauses.add(ScheduleContract.Videos.VIDEO_TOPIC + "=?");
                selectionArgs.add(topic);
            }

            String selection = selectionClauses.isEmpty() ? null :
                    ParserUtils.joinStrings(" AND ", selectionClauses, null);
            String[] args = selectionArgs.isEmpty() ? null : selectionArgs.toArray(new String[0]);

            LOGD(TAG, "Starting videos query, selection=" + selection + " (year=" + year
                    + ", topic=" + topic);
            return new CursorLoader(getActivity(), ScheduleContract.Videos.CONTENT_URI,
                    VideosQuery.PROJECTION, selection, args, ScheduleContract.Videos.DEFAULT_SORT);
        }
        LOGW(TAG, "Invalid query token: " + token);
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mCursor = data;
        updateCollectionView();

        Callbacks callbacks = getCallbacks();
        if (callbacks != null) {
            if (!mInformedYearsAndTopics) {
                informAvailableYearsTo(callbacks);
                informAvailableTopicsTo(callbacks);
                mInformedYearsAndTopics = true;
            }
        } else {
            LOGW(TAG, "No callbacks to inform filters to. Video filtering will probably not work.");
        }
    }

    private void informAvailableYearsTo(Callbacks callbacks) {
        ArrayList<Integer> years = new ArrayList<Integer>();
        mCursor.moveToPosition(-1);
        while (mCursor.moveToNext()) {
            if (!years.contains(mCursor.getInt(VideosQuery.YEAR))) {
                years.add(mCursor.getInt(VideosQuery.YEAR));
            }
        }
        callbacks.onAvailableVideoYearsChanged(years);
    }

    private void informAvailableTopicsTo(Callbacks callbacks) {
        ArrayList<String> topics = new ArrayList<String>();
        mCursor.moveToPosition(-1);
        while (mCursor.moveToNext()) {
            String topic = mCursor.getString(VideosQuery.TOPIC);
            if (!TextUtils.isEmpty(topic) && !topics.contains(topic)) {
                topics.add(mCursor.getString(VideosQuery.TOPIC));
            }
        }
        callbacks.onAvailableVideoTopicsChanged(topics);
    }

    private void updateCollectionView() {
        LOGD(TAG, "Updating video library collection view.");
        CollectionView.InventoryGroup curGroup = null;
        CollectionView.Inventory inventory = new CollectionView.Inventory();
        mCursor.moveToPosition(-1);
        int dataIndex = -1;
        int normalColumns = getResources().getInteger(R.integer.video_library_columns);

        boolean isEmpty = mCursor.getCount() == 0;

        while (mCursor.moveToNext()) {
            ++dataIndex;
            String topic = mCursor.getString(VideosQuery.TOPIC);
            boolean isHero = TextUtils.isEmpty(topic);
            int year = mCursor.getInt(VideosQuery.YEAR);
            String groupName = TextUtils.isEmpty(topic) ?
                    getString(R.string.google_i_o_year, year) : topic + " (" + year + ")";

            if (curGroup == null || !curGroup.getHeaderLabel().equals(groupName)) {
                if (curGroup != null) {
                    inventory.addGroup(curGroup);
                }
                curGroup = new CollectionView.InventoryGroup(
                        isHero ? GROUP_ID_HERO : GROUP_ID_NORMAL)
                        .setDataIndexStart(dataIndex)
                        .setHeaderLabel(groupName)
                        .setShowHeader(true)
                        .setDisplayCols(isHero ? 1 : normalColumns)
                        .setItemCount(1);
            } else {
                curGroup.incrementItemCount();
            }
        }

        if (curGroup != null) {
            inventory.addGroup(curGroup);
        }

        mCollectionView.setCollectionAdapter(this);
        mCollectionView.updateInventory(inventory);

        mEmptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
    }

    @Override
    public View newCollectionHeaderView(Context context, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        return inflater.inflate(R.layout.list_item_explore_header, parent, false);
    }

    @Override
    public void bindCollectionHeaderView(Context context, View view, int groupId, String headerLabel) {
        ((TextView) view.findViewById(android.R.id.text1)).setText(headerLabel);
    }

    @Override
    public View newCollectionItemView(Context context, int groupId, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        // TODO: if groupId is GROUP_ID_HERO, inflate different layout
        return inflater.inflate(R.layout.video_library_item, parent, false);
    }

    @Override
    public void bindCollectionItemView(Context context, View view, int groupId,
                int indexInGroup, int dataIndex, Object tag) {
        if (!mCursor.moveToPosition(dataIndex)) {
            return;
        }
        ImageView thumbnailView = (ImageView) view.findViewById(R.id.thumbnail);
        TextView titleView = (TextView) view.findViewById(R.id.title);
        TextView speakersView = (TextView) view.findViewById(R.id.speakers);
        TextView descriptionView = (TextView) view.findViewById(R.id.description);
        titleView.setText(mCursor.getString(VideosQuery.TITLE));
        speakersView.setText(mCursor.getString(VideosQuery.SPEAKERS));
        descriptionView.setText(mCursor.getString(VideosQuery.DESC));

        String thumbUrl = mCursor.getString(VideosQuery.THUMBNAIL_URL);
        if (TextUtils.isEmpty(thumbUrl)) {
            thumbnailView.setImageResource(android.R.color.transparent);
        } else {
            mImageLoader.loadImage(thumbUrl, thumbnailView);
        }

        final String videoId = mCursor.getString(VideosQuery.VIDEO_ID);
        final String youtubeLink = TextUtils.isEmpty(videoId) ? "" :
                videoId.contains("://") ? videoId :
                String.format(Locale.US, Config.VIDEO_LIBRARY_URL_FMT, videoId);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!TextUtils.isEmpty(youtubeLink)) {
                    LOGD(TAG, "Launching Youtube video: " + youtubeLink);
                    /* [ANALYTICS:EVENT]
                     * TRIGGER:   Click on a video on the Video Library screen.
                     * CATEGORY:  'Video Library'
                     * ACTION:    selectvideo
                     * LABEL:     video's YouTube URL, http://www.youtube.com/...
                     * [/ANALYTICS]
                     */
                    AnalyticsManager.sendEvent("Video Library", "selectvideo", youtubeLink, 0L);
                    Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(youtubeLink));
                    UIUtils.preferPackageForIntent(getActivity(), i,
                            UIUtils.YOUTUBE_PACKAGE_NAME);
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
                    startActivity(i);
                }
            }
        });
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    private Callbacks getCallbacks() {
        Activity act = getActivity();
        if (act == null || !(act instanceof Callbacks)) {
            return null;
        }
        return (Callbacks) act;
    }


    private interface VideosQuery {
        int _TOKEN = 0x1;
        String[] PROJECTION = {
                BaseColumns._ID,
                ScheduleContract.Videos.VIDEO_ID,
                ScheduleContract.Videos.VIDEO_YEAR,
                ScheduleContract.Videos.VIDEO_TITLE,
                ScheduleContract.Videos.VIDEO_DESC,
                ScheduleContract.Videos.VIDEO_VID,
                ScheduleContract.Videos.VIDEO_TOPIC,
                ScheduleContract.Videos.VIDEO_SPEAKERS,
                ScheduleContract.Videos.VIDEO_THUMBNAIL_URL
        };

        int _ID = 0;
        int VIDEO_ID = 1;
        int YEAR = 2;
        int TITLE = 3;
        int DESC = 4;
        int VID = 5;
        int TOPIC = 6;
        int SPEAKERS = 7;
        int THUMBNAIL_URL = 8;
    }
}
