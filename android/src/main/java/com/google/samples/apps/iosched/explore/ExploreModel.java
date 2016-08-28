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

package com.google.samples.apps.iosched.explore;

import com.google.common.annotations.VisibleForTesting;
import com.google.samples.apps.iosched.Config;

import no.java.schedule.v2.R;

import com.google.samples.apps.iosched.explore.data.ItemGroup;
import com.google.samples.apps.iosched.explore.data.LiveStreamData;
import com.google.samples.apps.iosched.explore.data.SessionData;
import com.google.samples.apps.iosched.explore.data.SessionScheduleGroup;
import com.google.samples.apps.iosched.explore.data.ThemeGroup;
import com.google.samples.apps.iosched.explore.data.TopicGroup;
import com.google.samples.apps.iosched.framework.Model;
import com.google.samples.apps.iosched.framework.QueryEnum;
import com.google.samples.apps.iosched.framework.UserActionEnum;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.samples.apps.iosched.util.UIUtils;

import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGE;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * This is an implementation of a {@link Model} that queries the sessions at Google I/O and extracts
 * the data needed to present the Explore I/O user interface.
 * <p/>
 * The process of loading and reading the data is typically done in the lifecycle of a
 * {@link com.google.samples.apps.iosched.framework.PresenterFragmentImpl}.
 */
public class ExploreModel implements Model {

    private static final String TAG = makeLogTag(ExploreModel.class);

    private final Context mContext;

    /**
     * Topic groups loaded from the database pre-randomly filtered and stored by topic name.
     */
    private Map<String, TopicGroup> mTopics = new HashMap<>();

    /**
     * Theme groups loaded from the database pre-randomly filtered and stored by topic name.
     */
    private Map<String, ThemeGroup> mThemes = new HashMap<>();

    private TreeMap<Date, SessionScheduleGroup> mScheduleSessionGroups = new TreeMap<>();

    private Map<String, String> mTrackTitles;

    private SessionData mKeynoteData;

    private LiveStreamData mLiveStreamData;

    public ExploreModel(Context context) {
        mContext = context;
    }

    public Collection<TopicGroup> getTopics() {
        return mTopics.values();
    }

    public Collection<SessionScheduleGroup> getSessionScheduleGroups() {
        return mScheduleSessionGroups.values();
    }

    public Collection<ThemeGroup> getThemes() {
        return mThemes.values();
    }

    public Map<String, String> getTrackTitles() {
        return mTrackTitles;
    }

    public SessionData getKeynoteData() {
        return mKeynoteData;
    }

    public LiveStreamData getLiveStreamData() {
        return mLiveStreamData;
    }


    @Override
    public QueryEnum[] getQueries() {
        return ExploreQueryEnum.values();
    }

    @Override
    public boolean readDataFromCursor(Cursor cursor, QueryEnum query) {
        LOGD(TAG, "readDataFromCursor");
        if (query == ExploreQueryEnum.SESSIONS) {
            LOGD(TAG, "Reading session data from cursor.");

            boolean atVenue = SettingsUtils.isAttendeeAtVenue(mContext);
            int themeSessionLimit = getThemeSessionLimit(mContext);

            int topicSessionLimit = getTopicSessionLimit(mContext);

            LiveStreamData liveStreamData = new LiveStreamData();
            Map<String, TopicGroup> topicGroups = new HashMap<>();
            Map<String, ThemeGroup> themeGroups = new HashMap<>();
            TreeMap<Date, SessionScheduleGroup> scheduleSessionGroups =
                    new TreeMap<Date, SessionScheduleGroup>();
            // TODO format time
            SimpleDateFormat dateFormat = new SimpleDateFormat("E dd.MMMM yyyy", Locale.getDefault());


            // Iterating through rows in Sessions query.
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    SessionData session = new SessionData();
                    populateSessionFromCursorRow(session, cursor);

                    // Sessions missing titles, descriptions, ids, or images aren't eligible for the
                    // Explore screen.
                    if (TextUtils.isEmpty(session.getSessionName()) ||
                            TextUtils.isEmpty(session.getDetails()) ||
                            TextUtils.isEmpty(session.getSessionId())) {
                        continue;
                    }

                    if (!atVenue &&
                            (!session.isLiveStreamAvailable()) && !session.isVideoAvailable()) {
                        // Skip the opportunity to present the session for those not on site since it
                        // won't be viewable as there is neither a live stream nor video available.
                        continue;
                    }

                    String sessionDateTime = dateFormat.format(session.getStartDate());
                    try {

                        if (!scheduleSessionGroups.containsKey(dateFormat.parse(sessionDateTime))) {
                            scheduleSessionGroups.put(dateFormat.parse(sessionDateTime),
                                    buildSessionScheduleItemTopic(session));
                        } else {
                            String sessionScheduledDate = dateFormat.format(session.getStartDate());
                            SessionScheduleGroup sessionScheduleGroup =
                                    scheduleSessionGroups.get(dateFormat.parse(sessionScheduledDate));
                            if (sessionScheduleGroup == null) {
                                sessionScheduleGroup = new SessionScheduleGroup();
                                sessionScheduleGroup.setTitle(sessionScheduledDate);
                                sessionScheduleGroup.setId("" + session.getStartDate().getTime());scheduleSessionGroups.put(dateFormat.parse(sessionDateTime),
                                        sessionScheduleGroup);
                            }

                            sessionScheduleGroup.addSessionData(session);
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                    String tags = session.getTags();

                    if (Config.Tags.SPECIAL_KEYNOTE.equals(session.getMainTag())) {
                        SessionData keynoteData = new SessionData();
                        populateSessionFromCursorRow(keynoteData, cursor);
                        rewriteKeynoteDetails(keynoteData);
                        mKeynoteData = keynoteData;
                    } else if (session.isLiveStreamNow(mContext)) {
                        liveStreamData.addSessionData(session);
                    }

                    // TODO: Refactor into a system wide way of parsing these tags.
                    if (!TextUtils.isEmpty(tags)) {
                        StringTokenizer tagsTokenizer = new StringTokenizer(tags, ",");
                        while (tagsTokenizer.hasMoreTokens()) {
                            String rawTag = tagsTokenizer.nextToken();
                            //  if (rawTag.startsWith("topic:")) {
                            ThemeGroup themeGroup = themeGroups.get(rawTag);
                            if (themeGroup == null) {
                                themeGroup = new ThemeGroup();
                                themeGroup.setTitle(rawTag);
                                themeGroup.setId(rawTag);
                                themeGroups.put(rawTag, themeGroup);
                            }
                            themeGroup.addSessionData(session);

                            TopicGroup topicGroup = topicGroups.get(rawTag);
                            if (topicGroup == null) {
                                topicGroup = new TopicGroup();
                                topicGroup.setTitle(rawTag);
                                topicGroup.setId(rawTag);
                                topicGroups.put(rawTag, topicGroup);
                            }
                            topicGroup.addSessionData(session);
                        }
                    }
                } while (cursor.moveToNext());
            }

            for (ItemGroup group : themeGroups.values()) {
                group.trimSessionData(themeSessionLimit);
            }
            for (ItemGroup group : topicGroups.values()) {
                group.trimSessionData(topicSessionLimit);
            }
            if (liveStreamData.getSessions().size() > 0) {
                mLiveStreamData = liveStreamData;
            }
            mThemes = themeGroups;
            mTopics = topicGroups;
            mScheduleSessionGroups = scheduleSessionGroups;
            return true;
        } else if (query == ExploreQueryEnum.TRACKS) {
            LOGW(TAG, "TAGS query loaded");
            Map<String, String> newTrackTitles = new HashMap<>();
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    String trackId = cursor.getString(cursor.getColumnIndex(
                            ScheduleContract.Tracks.TRACK_ID));
                    String trackName = cursor.getString(cursor.getColumnIndex(
                            ScheduleContract.Tracks.TRACK_NAME));
                    newTrackTitles.put(trackId, trackName);
                } while (cursor.moveToNext());
                mTrackTitles = newTrackTitles;
            }
            return true;
        }
        return false;
    }

    private SessionScheduleGroup buildSessionScheduleItemTopic(SessionData session) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("hh:mm", Locale.getDefault());
        String startTimeString = dateFormat.format(session.getStartDate());
        long scheduleSessionTime = session.getStartDate().getTime();

        SessionScheduleGroup sScheduleGroup = new SessionScheduleGroup();
        SimpleDateFormat simpleDateFormatter = new SimpleDateFormat("E dd.MMMM yyyy");
        String date = simpleDateFormatter.format(session.getStartDate());
        sScheduleGroup.setTitle(date);
        sScheduleGroup.setId(scheduleSessionTime + "");


        return sScheduleGroup;
    }

    public static int getTopicSessionLimit(Context context) {
        boolean atVenue = SettingsUtils.isAttendeeAtVenue(context);
        int topicSessionLimit;
        if (atVenue) {
            topicSessionLimit = context.getResources().getInteger(R.integer
                    .explore_topic_theme_onsite_max_item_count);
        } else {
            topicSessionLimit = 0;
        }
        return topicSessionLimit;
    }

    public static int getThemeSessionLimit(Context context) {
        boolean atVenue = SettingsUtils.isAttendeeAtVenue(context);
        int themeSessionLimit;
        if (atVenue) {
            themeSessionLimit = context.getResources().getInteger(R.integer
                    .explore_topic_theme_onsite_max_item_count);
        } else {
            themeSessionLimit = context.getResources().getInteger(R.integer
                    .explore_theme_max_item_count_offsite);
        }
        return themeSessionLimit;
    }

    private void rewriteKeynoteDetails(SessionData keynoteData) {
        long startTime, endTime, currentTime;
        currentTime = UIUtils.getCurrentTime(mContext);
        if (keynoteData.getStartDate() != null) {
            startTime = keynoteData.getStartDate().getTime();
        } else {
            LOGD(TAG, "Keynote start time wasn't set");
            startTime = 0;
        }
        if (keynoteData.getEndDate() != null) {
            endTime = keynoteData.getEndDate().getTime();
        } else {
            LOGD(TAG, "Keynote end time wasn't set");
            endTime = Long.MAX_VALUE;
        }

        StringBuilder stringBuilder = new StringBuilder();
        if (currentTime >= startTime && currentTime < endTime) {
            stringBuilder.append(mContext.getString(R.string
                    .live_now));
        } else {
            String shortDate = TimeUtils.formatShortDate(mContext, keynoteData.getStartDate());
            stringBuilder.append(shortDate);

            if (startTime > 0) {
                stringBuilder.append(" / ");
                stringBuilder.append(TimeUtils.formatShortTime(mContext,
                        new java.util.Date(startTime)));
            }
        }
        keynoteData.setDetails(stringBuilder.toString());
    }

    private void populateSessionFromCursorRow(SessionData session, Cursor cursor) {
        session.updateData(
                cursor.getString(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_TITLE)),
                cursor.getString(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_ABSTRACT)),
                cursor.getString(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_AUDIENCE)),
                cursor.getString(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_ID)),
                cursor.getString(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_PHOTO_URL)),
                cursor.getString(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_MAIN_TAG)),
                cursor.getLong(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_START)),
                cursor.getLong(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_END)),
                cursor.getString(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_LIVESTREAM_ID)),
                cursor.getString(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_YOUTUBE_URL)),
                cursor.getString(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_TAGS)),
                cursor.getLong(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_STARRED)) == 1L);
    }

    @Override
    public Loader<Cursor> createCursorLoader(int loaderId, Uri uri, @Nullable Bundle args) {
        CursorLoader loader = null;

        if (loaderId == ExploreQueryEnum.SESSIONS.getId()) {

            // Create and return the Loader.
            loader = getCursorLoaderInstance(mContext, uri,
                    ExploreQueryEnum.SESSIONS.getProjection(), null, null,
                    null);
        } else if (loaderId == ExploreQueryEnum.TRACKS.getId()) {
            LOGW(TAG, "Starting sessions tag query");
            loader = new CursorLoader(mContext, ScheduleContract.Tracks.CONTENT_URI,
                    ExploreQueryEnum.TRACKS.getProjection(), null, null, null);
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
        return true;
    }

    /**
     * Enumeration of the possible queries that can be done by this Model to retrieve data.
     */
    public static enum ExploreQueryEnum implements QueryEnum {

        /**
         * Query that retrieves a list of sessions.
         * <p/>
         * Once the data has been loaded it can be retrieved using {@code getThemes()} and
         * {@code getTopics()}.
         */
        SESSIONS(0x1, new String[]{
                ScheduleContract.Sessions._ID,
                ScheduleContract.Sessions.SESSION_ID,
                ScheduleContract.Sessions.SESSION_START,
                ScheduleContract.Sessions.SESSION_ABSTRACT,
                ScheduleContract.Sessions.SESSION_AUDIENCE,
                ScheduleContract.Sessions.SESSION_PHOTO_URL,
                ScheduleContract.Sessions.SESSION_END,
                ScheduleContract.Sessions.SESSION_MAIN_TAG,
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.ROOM_NAME,
                ScheduleContract.Sessions.SESSION_LIVESTREAM_ID,
                ScheduleContract.Sessions.SESSION_YOUTUBE_URL,
                ScheduleContract.Sessions.SESSION_TAGS,
                ScheduleContract.Sessions.SESSION_STARRED,
        }),

        TRACKS(0x2, new String[]{
                ScheduleContract.Tracks.TRACK_ID,
                ScheduleContract.Tracks.TRACK_NAME,
                ScheduleContract.Tracks.TRACK_ABSTRACT,
                ScheduleContract.Tracks.TRACK_COLOR,
        });


        private int id;

        private String[] projection;

        ExploreQueryEnum(int id, String[] projection) {
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
    public static enum ExploreUserActionEnum implements UserActionEnum {
        /**
         * Event that is triggered when a user re-enters the video library this triggers a reload
         * so that we can display another set of randomly selected videos.
         */
        RELOAD(2);

        private int id;

        ExploreUserActionEnum(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }

    }
}
