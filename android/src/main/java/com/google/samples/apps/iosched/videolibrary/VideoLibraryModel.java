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

import android.app.LoaderManager;
import android.content.AsyncQueryHandler;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.appwidget.ScheduleWidgetProvider;
import com.google.samples.apps.iosched.archframework.Model;
import com.google.samples.apps.iosched.archframework.ModelWithLoaderManager;
import com.google.samples.apps.iosched.archframework.QueryEnum;
import com.google.samples.apps.iosched.archframework.UserActionEnum;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.sync.SyncHelper;
import com.google.samples.apps.iosched.util.AccountUtils;
import com.google.samples.apps.iosched.util.ParserUtils;
import com.google.samples.apps.iosched.videolibrary.VideoLibraryModel.VideoLibraryQueryEnum;
import com.google.samples.apps.iosched.videolibrary.VideoLibraryModel.VideoLibraryUserActionEnum;
import com.google.samples.apps.iosched.videolibrary.data.Video;
import com.google.samples.apps.iosched.videolibrary.data.VideoTrack;

import java.util.ArrayList;
import java.util.Calendar;
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
 * <p/>
 * Two types of Data can be queried: A list of Videos which can be filtered by year and Topic and a
 * list of available years and topics which can be used for filtering.
 * <p/>
 * The data can be loaded using two queries: {@link VideoLibraryModel.VideoLibraryQueryEnum#VIDEOS}
 * and {@link VideoLibraryModel.VideoLibraryQueryEnum#FILTERS}. The query for videos can be filtered
 * by year and topic. These filters values needs to be set in a {@code Bundle} passed to the {@link
 * #createCursorLoader(VideoLibraryQueryEnum, Bundle)}  like this:
 * <p/>
 * {@code Bundle args = new Bundle(); args.putInt(VideoLibraryModel.KEY_YEAR, selectedYear);
 * args.putString(VideoLibraryModel.KEY_TOPIC, selectedTopic);}
 * <p/>
 * Once the data has been loaded it can be retrieved using {@link #getVideos()}, {@link #getYears()}
 * and {@link #getTopics()}.
 * <p/>
 * The process of loading and reading video library data is typically done in the lifecycle of a
 * {@link com.google.samples.apps.iosched.archframework.PresenterImpl}.
 */
public class VideoLibraryModel
        extends ModelWithLoaderManager<VideoLibraryQueryEnum, VideoLibraryUserActionEnum> {

    public static final int TRACK_ID_NEW = 0;

    public static final int TRACK_ID_KEYNOTES = 1;

    protected static final String KEY_YEAR = "com.google.samples.apps.iosched.KEY_YEAR";

    protected static final String KEY_TOPIC = "com.google.samples.apps.iosched.KEY_TOPIC";

    protected static final String KEY_VIDEO_ID = "com.google.samples.apps.iosched.KEY_VIDEO_ID";

    protected static final int ALL_YEARS = 0;

    protected static final String ALL_TOPICS = "__";

    protected static final String KEYNOTES_TOPIC = "Keynote";

    private static final String TAG = makeLogTag(VideoLibraryModel.class);

    private List<Integer> mYears;

    private List<String> mTopics;

    private VideoTrack mKeynoteVideos;

    private VideoTrack mCurrentYearVideos;

    private List<VideoTrack> mVideos;

    private Set<String> mViewedVideosIds = new HashSet<>();

    private int mSelectedYear = ALL_YEARS;

    private String mSelectedTopic = ALL_TOPICS;

    private Context mContext;

    private Uri mVideoUri;

    private Uri mMyVideosUri;

    private Uri mFilterUri;

    private TagMetadata mTagMetadata;

    public VideoLibraryModel(Context context, LoaderManager loaderManager, Uri videoUri,
            Uri myVideosUri, Uri filterUri) {
        super(VideoLibraryQueryEnum.values(), VideoLibraryUserActionEnum.values(), loaderManager);
        mContext = context;
        mVideoUri = videoUri;
        mMyVideosUri = myVideosUri;
        mFilterUri = filterUri;
    }

    public String getSelectedTopic() {
        return mSelectedTopic;
    }

    public void setSelectedTopic(String selectedTopic) {
        mSelectedTopic = selectedTopic;
    }

    public int getSelectedYear() {
        return mSelectedYear;
    }

    public void setSelectedYear(int selectedYear) {
        mSelectedYear = selectedYear;
    }

    public @Nullable String getSelectedTopicImageUrl() {
        if (mSelectedTopic != null && mTagMetadata != null) {
            final TagMetadata.Tag tag = mTagMetadata.getTag(mSelectedTopic);
            if (tag != null) {
                return tag.getPhotoUrl();
            }
        }
        return null;
    }

    public @ColorInt int getSelectedTopicColor() {
        if (mSelectedTopic != null && mTagMetadata != null) {
            final TagMetadata.Tag tag = mTagMetadata.getTag(mSelectedTopic);
            if (tag != null) {
                return tag.getColor();
            }
        }
        return Color.TRANSPARENT;
    }

    /**
     * Returns the keynote {@link VideoTrack} as retrieved by the last run of a {@link
     * VideoLibraryModel.VideoLibraryQueryEnum#VIDEOS} query or {@code null} if no VIDEOS queries
     * have been ran before.
     */
    public VideoTrack getKeynoteVideos() {
        return mKeynoteVideos;
    }

    /**
     * Returns the {@link VideoTrack} listing any videos released this year, as retrieved by the
     * last run of a {@link VideoLibraryModel.VideoLibraryQueryEnum#VIDEOS} query or {@code null} if
     * no VIDEOS queries have been ran before.
     */
    public VideoTrack getCurrentYearVideos() {
        return mCurrentYearVideos;
    }

    /**
     * Returns the list of {@link VideoTrack}s retrieved by the last run of a {@link
     * VideoLibraryModel.VideoLibraryQueryEnum#VIDEOS} query or {@code null} if no VIDEOS queries
     * have been ran before.
     */
    public List<VideoTrack> getVideos() {
        return mVideos;
    }

    /**
     * Convenience method for retrieving a flat list of <b>all</b> videos retrieved by the last run
     * of a {@link VideoLibraryModel.VideoLibraryQueryEnum#VIDEOS} query or an empty {@code List} if
     * no VIDEOS queries have been ran before.
     *
     * @return
     */
    public List<Video> getAllVideos() {
        List<Video> allVideos = new ArrayList<>();
        if (mKeynoteVideos != null && mKeynoteVideos.hasVideos()) {
            allVideos.addAll(mKeynoteVideos.getVideos());
        }
        if (mCurrentYearVideos != null && mCurrentYearVideos.hasVideos()) {
            allVideos.addAll(mCurrentYearVideos.getVideos());
        }
        if (mVideos != null && !mVideos.isEmpty()) {
            for (final VideoTrack videoTrack : mVideos) {
                if (videoTrack.hasVideos()) {
                    allVideos.addAll(videoTrack.getVideos());
                }
            }
        }

        return allVideos;
    }

    public boolean hasVideos() {
        return (mVideos != null && !mVideos.isEmpty()) ||
                (mKeynoteVideos != null && mKeynoteVideos.hasVideos()) ||
                (mCurrentYearVideos != null && mCurrentYearVideos.hasVideos());
    }

    /**
     * Returns the alphabetically ordered list of topics for all videos or {@code null} if the
     * {@link VideoLibraryModel.VideoLibraryQueryEnum#FILTERS} query have never been ran.
     */
    public List<String> getTopics() {
        return mTopics;
    }

    /**
     * Returns the alphabetically ordered list of years for all videos or {@code null} if the {@link
     * VideoLibraryModel.VideoLibraryQueryEnum#FILTERS} query have never been ran.
     */
    public List<Integer> getYears() {
        return mYears;
    }

    @Override
    public void cleanUp() {

    }

    @Override
    public void processUserAction(final VideoLibraryUserActionEnum action,
            @Nullable final Bundle args, final UserActionCallback callback) {
        switch (action) {
            case VIDEO_PLAYED:
                // If the action is a VIDEO_VIEWED we save the information that the video has
                // been viewed by the user in AppData.
                if (args != null && args.containsKey(KEY_VIDEO_ID)) {
                    String playedVideoId = args.getString(KEY_VIDEO_ID);

                    LOGD(TAG, "setVideoViewed id=" + playedVideoId);
                    Uri myPlayedVideoUri = ScheduleContract.MyViewedVideos.buildMyViewedVideosUri(
                            AccountUtils.getActiveAccountName(mContext));

                    AsyncQueryHandler handler =
                            new AsyncQueryHandler(mContext.getContentResolver()) {};
                    final ContentValues values = new ContentValues();
                    values.put(ScheduleContract.MyViewedVideos.VIDEO_ID, playedVideoId);
                    handler.startInsert(-1, null, myPlayedVideoUri, values);

                    // Because change listener is set to null during initialization, these
                    // won't fire on pageview.
                    mContext.sendBroadcast(
                            ScheduleWidgetProvider.getRefreshBroadcastIntent(mContext,
                                    false));

                    // Request an immediate user data sync to reflect the viewed video in the cloud.
                    SyncHelper.requestManualSync(true);
                } else {
                    LOGE(TAG,
                            "The VideoLibraryUserActionEnum.VIDEO_VIEWED action was called " +
                                    "without a "
                                    + "proper Bundle.");
                }
                break;
        }
    }

    @Override
    public Loader<Cursor> createCursorLoader(final VideoLibraryQueryEnum query,
            final Bundle args) {
        CursorLoader loader = null;
        switch (query) {
            case VIDEOS:
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
                String[] selectionArgsArray =
                        selectionArgs.isEmpty() ? null : selectionArgs.toArray(
                                new String[selectionArgs.size()]);

                LOGD(TAG,
                        "Starting videos query, selection=" + selection + " (year=" + mSelectedYear
                                + ", topic=" + mSelectedTopic);

                // Create and return the Loader.
                loader = getCursorLoaderInstance(mContext, mVideoUri,
                        VideoLibraryQueryEnum.VIDEOS.getProjection(), selection, selectionArgsArray,
                        ScheduleContract.Videos.DEFAULT_SORT);
                break;
            case FILTERS:
                LOGD(TAG, "Starting Video Filters query");
                loader = getCursorLoaderInstance(mContext, mFilterUri,
                        VideoLibraryQueryEnum.FILTERS.getProjection(), null, null, null);
                break;
            case MY_VIEWED_VIDEOS:
                LOGD(TAG, "Starting My Viewed Videos query");
                loader = getCursorLoaderInstance(mContext, mMyVideosUri,
                        VideoLibraryQueryEnum.MY_VIEWED_VIDEOS.getProjection(), null, null, null);
                break;
            case TAGS:
                loader = TagMetadata.createCursorLoader(mContext);
        }

        return loader;
    }

    @Override
    public boolean readDataFromCursor(final Cursor cursor, final VideoLibraryQueryEnum query) {
        LOGD(TAG, "readDataFromCursor");
        switch (query) {
            case VIDEOS:
                LOGD(TAG, "Reading video library collection Data from cursor.");
                if (cursor.moveToFirst()) {
                    processVideos(cursor);
                    markVideosAsViewed();
                }
                return true;

            case MY_VIEWED_VIDEOS:
                LOGD(TAG, "Reading my viewed videos Data from cursor.");
                mViewedVideosIds.clear();
                if (cursor.moveToFirst()) {
                    do {
                        mViewedVideosIds.add(cursor.getString(cursor.getColumnIndex(
                                ScheduleContract.MyViewedVideos.VIDEO_ID)));
                    } while (cursor.moveToNext());

                    markVideosAsViewed();
                    return true;
                }
                return true;
            case FILTERS:

                // Read all the Years and Topics from the Cursor.
                LOGD(TAG, "Reading video library collection Data from cursor.");
                mYears = new ArrayList<>();
                mTopics = new ArrayList<>();
                if (cursor != null && cursor.moveToFirst()) {
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
            case TAGS:
                mTagMetadata = new TagMetadata(cursor);
                addImageUrlToVideoTracksIfAvailable();
                return true;
            default:
                return false;
        }
    }

    private void addImageUrlToVideoTracksIfAvailable() {
        if (mTagMetadata != null) {
            if (mKeynoteVideos != null) {
                mKeynoteVideos.setTrackImageUrlIfAvailable(mTagMetadata);
            }
            if (mCurrentYearVideos != null) {
                mCurrentYearVideos.setTrackImageUrlIfAvailable(mTagMetadata);
            }
            if (mVideos != null) {
                for (VideoTrack track : mVideos) {
                    track.setTrackImageUrlIfAvailable(mTagMetadata);
                }
            }
        }
    }

    /**
     * Populate the model objects (mKeynoteVideos, mCurrentYearVideos & mVideos) from the given
     * cursor. Note we assume that the data is already sorted by track (per {@link
     * ScheduleContract.Videos#DEFAULT_SORT}).
     *
     * @param cursor The cursor to read data from.
     */
    private void processVideos(final Cursor cursor) {
        final int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        String currentTrack = null;
        List<Video> keynoteVideos = new ArrayList<>();
        List<Video> twentySixteenVideos = new ArrayList<>();
        List<Video> currentTrackVideos = new ArrayList<>();
        List<VideoTrack> videoTracks = new ArrayList<>();

        do {
            final Video video = readVideo(cursor);
            final String track = video.getTopic();
            if (track == null) {
                continue;
            }

            // Special handling for keynotes & videos from this year
            if (KEYNOTES_TOPIC.equals(track)) {
                keynoteVideos.add(video);
            } else if (video.getYear() == currentYear) {
                twentySixteenVideos.add(video);
            } else {
                // Otherwise group by track
                if (!track.equals(currentTrack)) {
                    // New track reached, store current track and update working vars
                    if (!currentTrackVideos.isEmpty()) {
                        videoTracks.add(new VideoTrack(currentTrack,
                                currentTrack.hashCode(), currentTrackVideos));
                    }
                    currentTrack = track;
                    currentTrackVideos = new ArrayList<>();
                }
                currentTrackVideos.add(video);
            }
        } while (cursor.moveToNext());

        // After looping there should be one populated track not added to the list
        if (!currentTrackVideos.isEmpty()) {
            videoTracks.add(
                    new VideoTrack(currentTrack, currentTrack.hashCode(), currentTrackVideos));
        }
        // Store the (non keynote or current year) video tracks
        mVideos = videoTracks;

        // Store any videos from this year
        if (!twentySixteenVideos.isEmpty()) {
            final String newThisYear = mContext.getString(R.string.new_videos_title, currentYear);
            mCurrentYearVideos = new VideoTrack(newThisYear, TRACK_ID_NEW, twentySixteenVideos);
        }

        // Store any keynote videos
        if (!keynoteVideos.isEmpty()) {
            mKeynoteVideos = new VideoTrack(KEYNOTES_TOPIC, TRACK_ID_KEYNOTES, keynoteVideos);
        }

        addImageUrlToVideoTracksIfAvailable();
    }

    /**
     * Create a single {@link Video} object form the given cursor.
     */
    private
    @NonNull
    Video readVideo(final Cursor cursor) {
        return new Video(
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
    }

    /**
     * Mark videos as viewed if they are listed in {@code mViewedVideosIds}.
     */
    private void markVideosAsViewed() {
        if (mViewedVideosIds == null) {
            return;
        }
        if (mKeynoteVideos != null && mKeynoteVideos.getVideos() != null) {
            for (Video video : mKeynoteVideos.getVideos()) {
                video.setAlreadyPlayed(mViewedVideosIds.contains(video.getId()));
            }
        }
        if (mCurrentYearVideos != null && mCurrentYearVideos.getVideos() != null) {
            for (Video video : mCurrentYearVideos.getVideos()) {
                video.setAlreadyPlayed(mViewedVideosIds.contains(video.getId()));
            }
        }
        if (mVideos != null && !mVideos.isEmpty()) {
            for (final VideoTrack videoTrack : mVideos) {
                if (videoTrack.getVideos() != null) {
                    for (final Video video : videoTrack.getVideos()) {
                        video.setAlreadyPlayed(mViewedVideosIds.contains(video.getId()));
                    }
                }
            }
        }
    }

    @VisibleForTesting
    public CursorLoader getCursorLoaderInstance(Context context, Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        return new CursorLoader(context, uri, projection, selection, selectionArgs, sortOrder);
    }

    /**
     * Enumeration of the possible queries that can be done by this Model to retrieve data.
     */
    public enum VideoLibraryQueryEnum implements QueryEnum {

        /**
         * Query that retrieves a list of available videos.
         * <p/>
         * The query for videos can be filtered by year and topic. These filters values needs to be
         * set in a {@code Bundle} passed to the {@link #createCursorLoader(VideoLibraryQueryEnum,
         * Bundle)} like this:
         * <p/>
         * {@code Bundle args = new Bundle(); args.putInt(VideoLibraryModel.KEY_YEAR, selectedYear);
         * args.putString(VideoLibraryModel.KEY_TOPIC, selectedTopic);}
         * <p/>
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
         * <p/>
         * Once the data has been loaded it can be retrieved using {@link #getVideos()}.
         */
        MY_VIEWED_VIDEOS(0x2, new String[]{
                ScheduleContract.MyViewedVideos.VIDEO_ID
        }),

        /**
         * Query that retrieves the list of possible filter values such as all Years and Topics of
         * existing videos.
         * <p/>
         * Once the data has been loaded it can be retrieved using {@link #getYears()} and {@link
         * #getTopics()}.
         */
        FILTERS(0x3, new String[]{
                ScheduleContract.Videos.VIDEO_YEAR,
                ScheduleContract.Videos.VIDEO_TOPIC
        }),

        /**
         * Query that retrieves all the possible tags
         */
        TAGS(0x4, null);

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
     * Enumeration of the possible events that a user can trigger that would affect the state of the
     * date of this Model.
     */
    public enum VideoLibraryUserActionEnum implements UserActionEnum {

        /**
         * Event that is triggered when a user changes the filters of the Video Library. For
         * instance when the year or topic filters are changed.
         */
        CHANGE_FILTER(1),

        /**
         * Event that is triggered when a user clicks on a video to play it. We save that
         * information because we grey out videos that have been played already.
         */
        VIDEO_PLAYED(2),

        /**
         * Event that is triggered when a user changes the account.
         */
        RELOAD_USER_VIDEOS(3);

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
