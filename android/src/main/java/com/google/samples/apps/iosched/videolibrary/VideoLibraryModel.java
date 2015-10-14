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

package com.google.samples.apps.iosched.videolibrary;

import com.google.common.annotations.VisibleForTesting;
import com.google.samples.apps.iosched.appwidget.ScheduleWidgetProvider;

import com.google.samples.apps.iosched.framework.Model;
import com.google.samples.apps.iosched.framework.QueryEnum;
import com.google.samples.apps.iosched.framework.UserActionEnum;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.sync.SyncHelper;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.ParserUtils;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * This is an implementation of a {@link Model} that queries data for the Video library feature.
 *
 * Two types of Data can be queried: A list of Videos which can be filtered by year and Topic and a
 * list of available years and topics which can be used for filtering.
 *
 * The data can be loaded using two queries: {@link VideoLibraryModel.VideoLibraryQueryEnum#VIDEOS}
 * and {@link VideoLibraryModel.VideoLibraryQueryEnum#FILTERS}.
 * The query for videos can be filtered by year and topic. These filters values needs to be set in a
 * {@code Bundle} passed to the {@link #createCursorLoader(int, android.net.Uri, android.os.Bundle)}
 * like this:
 *
 * {@code Bundle args = new Bundle();
 * args.putInt(VideoLibraryModel.KEY_YEAR, selectedYear);
 * args.putString(VideoLibraryModel.KEY_TOPIC, selectedTopic);}
 *
 * Once the data has been loaded it can be retrieved using {@link #getVideos()}, {@link #getYears()}
 * and {@link #getTopics()}.
 *
 * The process of loading and reading video library data is typically done in the lifecycle of a
 * {@link com.google.samples.apps.iosched.framework.PresenterFragmentImpl}.
 */
public class VideoLibraryModel implements Model {

    private static final String TAG = makeLogTag(VideoLibraryModel.class);

    protected static final String KEY_YEAR = "com.google.samples.apps.iosched.KEY_YEAR";

    protected static final String KEY_TOPIC = "com.google.samples.apps.iosched.KEY_TOPIC";

    protected static final String KEY_VIDEO_ID = "com.google.samples.apps.iosched.KEY_VIDEO_ID";

    protected static final int ALL_YEARS = 0;

    protected static final String ALL_TOPICS = "__";

    protected static final String KEYNOTES_TOPIC = "Keynote";

    private final Context mContext;

    private List<Integer> mYears;

    private List<String> mTopics;

    private List<Video> mVideos;

    private Set<String> mViewedVideosIds = new HashSet<>();

    private int mSelectedYear = ALL_YEARS;

    private String mSelectedTopic = ALL_TOPICS;

    private Activity mActivity;

    /**
     * This represent a Video that is pulled from the Video Library.
     */
    public static class Video {

        final private String mId;

        final private int mYear;

        final private String mTopic;

        final private String mTitle;

        final private String mDesc;

        final private String mVid;

        final private String mSpeakers;

        final private String mThumbnailUrl;

        private boolean mAlreadyPlayed = false;

        public Video(String id, int year, String topic, String title, String desc, String vid,
                String speakers, String thumbnailUrl) {
            mId = id;
            mYear = year;
            mTopic = topic;
            mTitle = title;
            mDesc = desc;
            mVid = vid;
            mSpeakers = speakers;
            mThumbnailUrl = thumbnailUrl;
        }

        public String getId() {
            return mId;
        }

        public int getYear() {
            return mYear;
        }

        public String getTopic() {
            return mTopic;
        }

        public String getTitle() {
            return mTitle;
        }

        public String getDesc() {
            return mDesc;
        }

        public String getVid() {
            return mVid;
        }

        public String getSpeakers() {
            return mSpeakers;
        }

        public String getThumbnailUrl() {
            return mThumbnailUrl;
        }

        public boolean getAlreadyPlayed() {
            return mAlreadyPlayed;
        }

        public void setAlreadyPlayed(boolean alreadyPlayed) {
            mAlreadyPlayed = alreadyPlayed;
        }
    }

    public VideoLibraryModel(Context context, Activity activity) {
        mContext = context;
        mActivity = activity;
    }

    public void setSelectedYear(int selectedYear) {
        mSelectedYear = selectedYear;
    }

    public void setSelectedTopic(String selectedTopic) {
        mSelectedTopic = selectedTopic;
    }

    public int getSelectedYear() {
        return mSelectedYear;
    }

    public String getSelectedTopic() {
        return mSelectedTopic;
    }

    /**
     * Returns the list of videos retrieved by the last run of a
     * {@link VideoLibraryModel.VideoLibraryQueryEnum#VIDEOS} query or {@code null} if no VIDEOS
     * queries have been ran before.
     */
    public List<Video> getVideos() {
        return mVideos;
    }

    /**
     * Returns the alphabetically ordered list of topics for all videos or {@code null} if the
     * {@link VideoLibraryModel.VideoLibraryQueryEnum#FILTERS} query have never been ran.
     */
    public List<String> getTopics() {
        return mTopics;
    }

    /**
     * Returns the alphabetically ordered list of years for all videos or {@code null} if the
     * {@link VideoLibraryModel.VideoLibraryQueryEnum#FILTERS} query have never been ran.
     */
    public List<Integer> getYears() {
        return mYears;
    }

    @Override
    public QueryEnum[] getQueries() {
        return VideoLibraryQueryEnum.values();
    }

    @Override
    public boolean readDataFromCursor(Cursor cursor, QueryEnum query) {
        LOGD(TAG, "readDataFromCursor");
        if (query == VideoLibraryQueryEnum.VIDEOS) {
            LOGD(TAG, "Reading video library collection Data from cursor.");
            mVideos = new ArrayList<>();
            if(cursor.moveToFirst()) {
                do {
                    // Create Video objects and add to the video list.
                    Video video = new Video(
                            cursor.getString(cursor.getColumnIndex(
                                    ScheduleContract.Videos.VIDEO_ID)),
                            cursor.getInt(cursor.getColumnIndex(
                                    ScheduleContract.Videos.VIDEO_YEAR)),
                            cursor.getString(cursor.getColumnIndex(
                                    ScheduleContract.Videos.VIDEO_TOPIC)),
                            cursor.getString(cursor.getColumnIndex(
                                    ScheduleContract.Videos.VIDEO_TITLE)),
                            cursor.getString(cursor.getColumnIndex(
                                    ScheduleContract.Videos.VIDEO_DESC)),
                            cursor.getString(cursor.getColumnIndex(
                                    ScheduleContract.Videos.VIDEO_VID)),
                            cursor.getString(cursor.getColumnIndex(
                                    ScheduleContract.Videos.VIDEO_SPEAKERS)),
                            cursor.getString(cursor.getColumnIndex(
                                    ScheduleContract.Videos.VIDEO_THUMBNAIL_URL)));
                    mVideos.add(video);
                } while (cursor.moveToNext());
                markVideosAsViewed();
            }
            return true;
        } else if (query == VideoLibraryQueryEnum.MY_VIEWED_VIDEOS) {
            LOGD(TAG, "Reading my viewed videos Data from cursor.");
            if(cursor.moveToFirst()) {
                Set<String> viewedVideoIds = new HashSet<>();
                do {
                    viewedVideoIds.add(cursor.getString(cursor.getColumnIndex(
                            ScheduleContract.MyViewedVideos.VIDEO_ID)));
                } while (cursor.moveToNext());

                if (!mViewedVideosIds.containsAll(viewedVideoIds)) {
                    mViewedVideosIds = viewedVideoIds;
                    markVideosAsViewed();
                    return true;
                }
            }
            return false;
        } else if (query == VideoLibraryQueryEnum.FILTERS) {

            // Read all the Years and Topics from the Cursor.
            LOGD(TAG, "Reading video library collection Data from cursor.");
            mYears = new ArrayList<>();
            mTopics = new ArrayList<>();
            if (cursor.moveToFirst()) {
                do {
                    int year = cursor.getInt(
                            cursor.getColumnIndex(ScheduleContract.Videos.VIDEO_YEAR));
                    String topic = cursor.getString(
                            cursor.getColumnIndex(ScheduleContract.Videos.VIDEO_TOPIC));

                    // Build a list of unique Years and Topics.
                    if (!mYears.contains(year)) {
                        mYears.add(year);
                    }
                    if (!TextUtils.isEmpty(topic) && !mTopics.contains(topic)) {
                        mTopics.add(topic);
                    }
                } while (cursor.moveToNext());
            }

            // Sort years in decreasing order (start with most recent).
            Collections.sort(mYears, new Comparator<Integer>() {
                @Override
                public int compare(Integer a, Integer b) {
                    return b.compareTo(a);
                }
            });
            Collections.sort(mTopics);
            return true;
        }
        return false;
    }

    /**
     * Mark videos as viewed if they are listed in {@code mViewedVideosIds}.
     */
    private void markVideosAsViewed() {
        if (mVideos == null || mViewedVideosIds == null) {
            return;
        }
        for (Video video : mVideos) {
            if (mViewedVideosIds.contains(video.getId())) {
                video.setAlreadyPlayed(true);
            }
        }
    }

    @Override
    public Loader<Cursor> createCursorLoader(int loaderId, Uri uri, @Nullable Bundle args) {
        CursorLoader loader = null;

        if (loaderId == VideoLibraryQueryEnum.VIDEOS.getId()) {

            ArrayList<String> selectionArgs = new ArrayList<>();
            ArrayList<String> selectionClauses = new ArrayList<>();

            // Extract possible filter values from the Bundle.
            if (args != null && args.containsKey(KEY_YEAR)) {
                mSelectedYear = args.getInt(KEY_YEAR);
            }
            if (args != null && args.containsKey(KEY_TOPIC)) {
                mSelectedTopic = args.getString(KEY_TOPIC);
            }

            // If filter values have been set we add the filter clause to the Loader.
            if (mSelectedYear > ALL_YEARS) {
                selectionClauses.add(ScheduleContract.Videos.VIDEO_YEAR + "=?");
                selectionArgs.add(Integer.toString(mSelectedYear));
            }
            if (mSelectedTopic != null && !mSelectedTopic.equals(ALL_TOPICS)) {
                selectionClauses.add(ScheduleContract.Videos.VIDEO_TOPIC + "=?");
                selectionArgs.add(mSelectedTopic);
            }
            String selection = selectionClauses.isEmpty() ? null :
                    ParserUtils.joinStrings(" AND ", selectionClauses, null);
            String[] selectionArgsArray = selectionArgs.isEmpty() ? null : selectionArgs.toArray(
                    new String[selectionArgs.size()]);

            LOGD(TAG, "Starting videos query, selection=" + selection + " (year=" + mSelectedYear
                    + ", topic=" + mSelectedTopic);

            // Create and return the Loader.
            loader = getCursorLoaderInstance(mContext, uri,
                    VideoLibraryQueryEnum.VIDEOS.getProjection(), selection, selectionArgsArray,
                    ScheduleContract.Videos.DEFAULT_SORT);
        } else if (loaderId == VideoLibraryQueryEnum.FILTERS.getId()) {
            LOGD(TAG, "Starting Video Filters query");
            loader = getCursorLoaderInstance(mContext, uri,
                    VideoLibraryQueryEnum.FILTERS.getProjection(), null, null, null);
        } else if (loaderId == VideoLibraryQueryEnum.MY_VIEWED_VIDEOS.getId()) {
            LOGD(TAG, "Starting My Viewed Videos query");
            loader = getCursorLoaderInstance(mContext, uri,
                    VideoLibraryQueryEnum.MY_VIEWED_VIDEOS.getProjection(), null, null, null);
        } else {
            LOGE(TAG, "Invalid query loaderId: " + loaderId);
        }
        return loader;
    }

    @VisibleForTesting
    public CursorLoader getCursorLoaderInstance(Context context, Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        return new CursorLoader(context, uri, projection, selection, selectionArgs, sortOrder);
    }

    @Override
    public boolean requestModelUpdate(UserActionEnum action, @Nullable Bundle args) {
        // If the action is a VIDEO_VIEWED we save the information that the video has been viewed by
        // the user in AppData.
        if (action.equals(VideoLibraryUserActionEnum.VIDEO_PLAYED)) {
            if (args != null && args.containsKey(KEY_VIDEO_ID)) {
                String playedVideoId = args.getString(KEY_VIDEO_ID);

                LOGD(TAG, "setVideoViewed id=" + playedVideoId);
                Uri myPlayedVideoUri = ScheduleContract.MyViewedVideos.buildMyViewedVideosUri(
                        AccountUtils.getActiveAccountName(mActivity));

                AsyncQueryHandler handler =
                        new AsyncQueryHandler(mActivity.getContentResolver()) {};
                final ContentValues values = new ContentValues();
                values.put(ScheduleContract.MyViewedVideos.VIDEO_ID, playedVideoId);
                handler.startInsert(-1, null, myPlayedVideoUri, values);

                // Because change listener is set to null during initialization, these
                // won't fire on pageview.
                mActivity.sendBroadcast(ScheduleWidgetProvider.getRefreshBroadcastIntent(mActivity,
                        false));

                // Request an immediate user data sync to reflect the viewed video in the cloud.
                SyncHelper.requestManualSync(AccountUtils.getActiveAccount(mActivity), true);
            } else {
                LOGE(TAG, "The VideoLibraryUserActionEnum.VIDEO_VIEWED action was called without a "
                        + "proper Bundle.");
                return false;
            }
        }
        return true;
    }

    /**
     * Enumeration of the possible queries that can be done by this Model to retrieve data.
     */
    public static enum VideoLibraryQueryEnum implements QueryEnum {

        /**
         * Query that retrieves a list of available videos.
         *
         * The query for videos can be filtered by year and topic. These filters values needs to be
         * set in a {@code Bundle} passed to the
         * {@link #createCursorLoader(int, android.net.Uri, android.os.Bundle)} like this:
         *
         * {@code Bundle args = new Bundle();
         * args.putInt(VideoLibraryModel.KEY_YEAR, selectedYear);
         * args.putString(VideoLibraryModel.KEY_TOPIC, selectedTopic);}
         *
         * Once the data has been loaded it can be retrieved using {@link #getVideos()}.
         */
        VIDEOS(0x1, new String[]{
                ScheduleContract.Videos.VIDEO_ID,
                ScheduleContract.Videos.VIDEO_YEAR,
                ScheduleContract.Videos.VIDEO_TITLE,
                ScheduleContract.Videos.VIDEO_DESC,
                ScheduleContract.Videos.VIDEO_VID,
                ScheduleContract.Videos.VIDEO_TOPIC,
                ScheduleContract.Videos.VIDEO_SPEAKERS,
                ScheduleContract.Videos.VIDEO_THUMBNAIL_URL,
        }),

        /**
         * Query that retrieves a list of already viewed videos.
         *
         * Once the data has been loaded it can be retrieved using {@link #getVideos()}.
         */
        MY_VIEWED_VIDEOS(0x2, new String[]{
                ScheduleContract.MyViewedVideos.VIDEO_ID
        }),

        /**
         * Query that retrieves the list of possible filter values such as all Years and Topics of
         * existing videos.
         *
         * Once the data has been loaded it can be retrieved using {@link #getYears()} and
         * {@link #getTopics()}.
         */
        FILTERS(0x3, new String[]{
                ScheduleContract.Videos.VIDEO_YEAR,
                ScheduleContract.Videos.VIDEO_TOPIC
        });

        private int id;

        private String[] projection;

        VideoLibraryQueryEnum(int id, String[] projection) {
            this.id = id;
            this.projection = projection;
        }

        @Override
        public int getId() {
            return id;
        }

        @Override
        public String[] getProjection() {
            return projection;
        }
    }

    /**
     * Enumeration of the possible events that a user can trigger that would affect the state of
     * the date of this Model.
     */
    public static enum VideoLibraryUserActionEnum implements UserActionEnum {

        /**
         * Event that is triggered when a user changes the filters of the Video Library. For
         * instance when the year or topic filters are changed.
         */
        CHANGE_FILTER(1),

        /**
         * Event that is triggered when a user re-enters the video library this triggers a reload
         * so that we can display another set of randomly selected videos.
         */
        RELOAD(2),

        /**
         * Event that is triggered when a user clicks on a video to play it. We save that
         * information because we grey out videos that have been played already.
         */
        VIDEO_PLAYED(3);

        private int id;

        VideoLibraryUserActionEnum(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }

    }
}
