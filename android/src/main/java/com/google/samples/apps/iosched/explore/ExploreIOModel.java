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

import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.common.annotations.VisibleForTesting;
import com.google.samples.apps.iosched.Config;
import com.google.samples.apps.iosched.R;
import com.google.samples.apps.iosched.archframework.Model;
import com.google.samples.apps.iosched.archframework.ModelWithLoaderManager;
import com.google.samples.apps.iosched.archframework.QueryEnum;
import com.google.samples.apps.iosched.archframework.UserActionEnum;
import com.google.samples.apps.iosched.explore.data.ItemGroup;
import com.google.samples.apps.iosched.explore.data.LiveStreamData;
import com.google.samples.apps.iosched.explore.data.MessageData;
import com.google.samples.apps.iosched.explore.data.SessionData;
import com.google.samples.apps.iosched.model.TagMetadata;
import com.google.samples.apps.iosched.provider.ScheduleContract;
import com.google.samples.apps.iosched.settings.ConfMessageCardUtils;
import com.google.samples.apps.iosched.settings.SettingsUtils;
import com.google.samples.apps.iosched.util.TimeUtils;
import com.google.samples.apps.iosched.util.WiFiUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import static com.google.samples.apps.iosched.util.LogUtils.LOGD;
import static com.google.samples.apps.iosched.util.LogUtils.LOGW;
import static com.google.samples.apps.iosched.util.LogUtils.makeLogTag;

/**
 * This is an implementation of a {@link Model} that queries the sessions at Google I/O and extracts
 * the data needed to present the Explore I/O user interface.
 */
public class ExploreIOModel extends ModelWithLoaderManager<ExploreIOModel.ExploreIOQueryEnum,
        ExploreIOModel.ExploreIOUserActionEnum> {

    private static final String TAG = makeLogTag(ExploreIOModel.class);

    private final Context mContext;

    /**
     * Topic groups loaded from the database pre-randomly filtered and stored by topic name.
     */
    private Map<String, ItemGroup> mTracks = new HashMap<>();

    /**
     * Theme groups loaded from the database pre-randomly filtered and stored by topic name.
     */
    private Map<String, ItemGroup> mThemes = new HashMap<>();

    private Map<String, String> mTagTitles;

    private SessionData mKeynoteData;

    private LiveStreamData mLiveStreamData;

    private Uri mSessionsUri;

    private TagMetadata mTagMetadata;

    public ExploreIOModel(Context context, Uri sessionsUri, LoaderManager loaderManager) {
        super(ExploreIOQueryEnum.values(), ExploreIOUserActionEnum.values(), loaderManager);
        mContext = context;
        mSessionsUri = sessionsUri;
    }

    public Collection<ItemGroup> getTracks() {
        return mTracks.values();
    }

    public Collection<ItemGroup> getThemes() {
        return mThemes.values();
    }

    public Map<String, String> getTagTitles() { return mTagTitles; }

    public SessionData getKeynoteData() { return mKeynoteData; }

    public LiveStreamData getLiveStreamData() { return mLiveStreamData; }


    @Override
    public void cleanUp() {
        mThemes.clear();
        mThemes = null;
        mTracks.clear();
        mTracks = null;
        mTagTitles.clear();
        mTagTitles = null;
        mKeynoteData = null;
        mLiveStreamData = null;
    }

    @Override
    public void processUserAction(final ExploreIOUserActionEnum action,
            @Nullable final Bundle args, final UserActionCallback callback) {
        /**
         * The only user action in this model fires off a query (using {@link #KEY_RUN_QUERY_ID},
         * so this method isn't used.
         */
    }

    @Override
    public Loader<Cursor> createCursorLoader(final ExploreIOQueryEnum query, final Bundle args) {
        CursorLoader loader = null;

        switch (query) {
            case SESSIONS:
                // Create and return the Loader.
                loader = getCursorLoaderInstance(mContext, mSessionsUri,
                        ExploreIOQueryEnum.SESSIONS.getProjection(), null, null,
                        ScheduleContract.Sessions.SORT_BY_TYPE_THEN_TIME);
                break;
            case TAGS:
                LOGW(TAG, "Starting sessions tag query");
                loader = TagMetadata.createCursorLoader(mContext);
        }

        return loader;
    }

    @Override
    public boolean readDataFromCursor(final Cursor cursor, final ExploreIOQueryEnum query) {
        switch (query) {
            case SESSIONS:
                readDataFromSessionsCursor(cursor);
                return true;
            case TAGS:
                readDataFromTagsCursor(cursor);
                return true;
        }
        return false;
    }

    /**
     * Get the list of {@link MessageData} to be displayed to the user, based upon time, location
     * etc.
     *
     * @return messages to be displayed.
     */
    public List<MessageData> getMessages() {
        final List<MessageData> messages = new ArrayList<>();
        if (SettingsUtils.isAttendeeAtVenue(mContext)) {
            // Users are required to opt in or out of whether they want conference message cards
            if (!ConfMessageCardUtils.hasAnsweredConfMessageCardsPrompt(mContext)) {
                // User has not answered whether they want to opt in.
                // Build a opt-in/out card.
                messages.add(MessageCardHelper.getConferenceOptInMessageData(mContext));
            } else if (ConfMessageCardUtils.isConfMessageCardsEnabled(mContext)) {
                ConfMessageCardUtils.enableActiveCards(mContext);

                // Note that for these special cards, we'll never show more than one at a time
                // to prevent overloading the user with messages.
                // We want each new message to be notable.
                if (shouldShowCard(
                        ConfMessageCardUtils.ConfMessageCard.CONFERENCE_CREDENTIALS)) {
                    messages.add(
                            MessageCardHelper.getConferenceCredentialsMessageData(mContext));
                } else if (shouldShowCard(
                        ConfMessageCardUtils.ConfMessageCard.KEYNOTE_ACCESS)) {
                    messages.add(MessageCardHelper.getKeynoteAccessMessageData(mContext));
                } else if (shouldShowCard(ConfMessageCardUtils.ConfMessageCard.AFTER_HOURS)) {
                    messages.add(MessageCardHelper.getAfterHoursMessageData(mContext));
                } else if (shouldShowCard(
                        ConfMessageCardUtils.ConfMessageCard.WIFI_FEEDBACK)) {
                    if (WiFiUtils.isWiFiEnabled(mContext) &&
                            WiFiUtils.isWiFiApConfigured(mContext)) {
                        messages.add(MessageCardHelper.getWifiFeedbackMessageData(mContext));
                    }
                }
            }
            // Check whether a wifi setup card should be offered.
            if (WiFiUtils.shouldOfferToSetupWifi(mContext, true)) {
                // Build card asking users whether they want to enable wifi.
                messages.add(MessageCardHelper.getWifiSetupMessageData(mContext));
            }
        }
        return messages;
    }

    /**
     * Check if this card should be shown and hasn't previously been dismissed.
     *
     * @return {@code true} if the given message card should be displayed.
     */
    private boolean shouldShowCard(ConfMessageCardUtils.ConfMessageCard card) {
        final boolean shouldShow = ConfMessageCardUtils.shouldShowConfMessageCard(mContext, card);
        if (!shouldShow) {
            return shouldShow;
        }
        return !ConfMessageCardUtils.hasDismissedConfMessageCard(mContext, card);
    }

    /**
     * As we go through the session query results we will be collecting X numbers of session data
     * per Topic and Y numbers of sessions per Theme. When new topics or themes are seen a group
     * will be created.
     * <p/>
     * As we iterate through the list of sessions we are also watching out for the keynote and any
     * live sessions streaming right now.
     */
    private void readDataFromSessionsCursor(Cursor cursor) {
        LOGD(TAG, "Reading session data from cursor.");

        boolean atVenue = SettingsUtils.isAttendeeAtVenue(mContext);

        LiveStreamData liveStreamData = new LiveStreamData();
        Map<String, ItemGroup> trackGroups = new HashMap<>();
        Map<String, ItemGroup> themeGroups = new HashMap<>();

        if (cursor != null && cursor.moveToFirst()) {
            do {
                SessionData session = new SessionData();
                populateSessionFromCursorRow(session, cursor);

                if (isSessionDataInvalid(session)) {
                    continue;
                }

                if (!atVenue &&
                        (!session.isLiveStreamAvailable()) && !session.isVideoAvailable()) {
                    // Skip the opportunity to present the session for those not on site
                    // since it
                    // won't be viewable as there is neither a live stream nor video available.
                    continue;
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
                        if (rawTag.startsWith("TRACK_")) {
                            ItemGroup trackGroup = trackGroups.get(rawTag);
                            if (trackGroup == null) {
                                trackGroup = new ItemGroup();
                                trackGroup.setTitle(rawTag);
                                trackGroup.setId(rawTag);
                                if (mTagMetadata != null && mTagMetadata.getTag(rawTag) != null) {
                                    trackGroup
                                            .setPhotoUrl(mTagMetadata.getTag(rawTag).getPhotoUrl());
                                }
                                trackGroups.put(rawTag, trackGroup);
                            }
                            trackGroup.addSessionData(session);

                        } else if (rawTag.startsWith("THEME_")) {
                            ItemGroup themeGroup = themeGroups.get(rawTag);
                            if (themeGroup == null) {
                                themeGroup = new ItemGroup();
                                themeGroup.setTitle(rawTag);
                                themeGroup.setId(rawTag);
                                if (mTagMetadata != null && mTagMetadata.getTag(rawTag) != null) {
                                    themeGroup
                                            .setPhotoUrl(mTagMetadata.getTag(rawTag).getPhotoUrl());
                                }
                                themeGroups.put(rawTag, themeGroup);
                            }
                            themeGroup.addSessionData(session);
                        }
                    }
                }
            } while (cursor.moveToNext());
        }

        if (liveStreamData.getSessions().size() > 0) {
            mLiveStreamData = liveStreamData;
        }
        mThemes = themeGroups;
        mTracks = trackGroups;
    }

    /**
     * A session missing title, description, id, or image isn't eligible for the Explore screen.
     */
    private boolean isSessionDataInvalid(SessionData session) {
        return TextUtils.isEmpty(session.getSessionName()) ||
                TextUtils.isEmpty(session.getDetails()) ||
                TextUtils.isEmpty(session.getSessionId()) ||
                TextUtils.isEmpty(session.getImageUrl());
    }

    private void readDataFromTagsCursor(Cursor cursor) {
        LOGW(TAG, "TAGS query loaded");
        if (cursor != null && cursor.moveToFirst()) {
            mTagMetadata = new TagMetadata(cursor);
            cursor.moveToFirst();
            Map<String, String> newTagTitles = new HashMap<>();
            do {
                String tagId = cursor.getString(cursor.getColumnIndex(
                        ScheduleContract.Tags.TAG_ID));
                String tagName = cursor.getString(cursor.getColumnIndex(
                        ScheduleContract.Tags.TAG_NAME));
                newTagTitles.put(tagId, tagName);
            } while (cursor.moveToNext());
            mTagTitles = newTagTitles;
        }

        addPhotoUrlToTopicsAndThemes();
    }

    private void addPhotoUrlToTopicsAndThemes() {
        if (mTracks != null) {
            for (ItemGroup topic : mTracks.values()) {
                if (mTagMetadata != null && mTagMetadata.getTag(topic.getTitle()) != null) {
                    topic.setPhotoUrl(mTagMetadata.getTag(topic.getTitle()).getPhotoUrl());
                }
            }
        }
        if (mThemes != null) {
            for (ItemGroup theme : mThemes.values()) {
                if (mTagMetadata != null && mTagMetadata.getTag(theme.getTitle()) != null) {
                    theme.setPhotoUrl(mTagMetadata.getTag(theme.getTitle()).getPhotoUrl());
                }
            }
        }
    }

    private void rewriteKeynoteDetails(SessionData keynoteData) {
        long startTime, endTime, currentTime;
        currentTime = TimeUtils.getCurrentTime(mContext);
        if (keynoteData.getStartDate() != null) {
            startTime = keynoteData.getStartDate().getTimeInMillis();
        } else {
            LOGD(TAG, "Keynote start time wasn't set");
            startTime = 0;
        }
        if (keynoteData.getEndDate() != null) {
            endTime = keynoteData.getEndDate().getTimeInMillis();
        } else {
            LOGD(TAG, "Keynote end time wasn't set");
            endTime = Long.MAX_VALUE;
        }

        StringBuilder stringBuilder = new StringBuilder();
        if (currentTime >= startTime && currentTime < endTime) {
            stringBuilder.append(mContext.getString(R.string
                    .live_now));
        } else {
            stringBuilder.append(
                    TimeUtils.formatShortDateTime(mContext, keynoteData.getStartDate().getTime()));
        }
        keynoteData.setDetails(stringBuilder.toString());
    }

    private void populateSessionFromCursorRow(SessionData session, Cursor cursor) {
        session.updateData(mContext,
                cursor.getString(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_TITLE)),
                cursor.getString(cursor.getColumnIndex(
                        ScheduleContract.Sessions.SESSION_ABSTRACT)),
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
                        ScheduleContract.Sessions.SESSION_IN_MY_SCHEDULE)) == 1L);
    }

    @VisibleForTesting
    public CursorLoader getCursorLoaderInstance(Context context, Uri uri, String[] projection,
            String selection, String[] selectionArgs, String sortOrder) {
        return new CursorLoader(context, uri, projection, selection, selectionArgs, sortOrder);
    }

    /**
     * Enumeration of the possible queries that can be done by this Model to retrieve data.
     */
    public static enum ExploreIOQueryEnum implements QueryEnum {

        /**
         * Query that retrieves a list of sessions.
         * <p/>
         * Once the data has been loaded it can be retrieved using {@code getThemes()} and {@code
         * getTracks()}.
         */
        SESSIONS(0x1, new String[]{
                ScheduleContract.Sessions.SESSION_ID,
                ScheduleContract.Sessions.SESSION_TITLE,
                ScheduleContract.Sessions.SESSION_ABSTRACT,
                ScheduleContract.Sessions.SESSION_TAGS,
                ScheduleContract.Sessions.SESSION_MAIN_TAG,
                ScheduleContract.Sessions.SESSION_PHOTO_URL,
                ScheduleContract.Sessions.SESSION_START,
                ScheduleContract.Sessions.SESSION_END,
                ScheduleContract.Sessions.SESSION_LIVESTREAM_ID,
                ScheduleContract.Sessions.SESSION_YOUTUBE_URL,
                ScheduleContract.Sessions.SESSION_IN_MY_SCHEDULE,
                ScheduleContract.Sessions.SESSION_START,
                ScheduleContract.Sessions.SESSION_END,
        }),

        TAGS(0x2, new String[]{
                ScheduleContract.Tags.TAG_ID,
                ScheduleContract.Tags.TAG_NAME,
        });


        private int id;

        private String[] projection;

        ExploreIOQueryEnum(int id, String[] projection) {
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
    public static enum ExploreIOUserActionEnum implements UserActionEnum {
        /**
         * Event that is triggered when a user re-enters the video library this triggers a reload so
         * that we can display another set of randomly selected videos.
         */
        RELOAD(1);

        private int id;

        ExploreIOUserActionEnum(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }

    }
}
